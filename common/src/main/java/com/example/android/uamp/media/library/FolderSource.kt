package com.example.android.uamp.media.library

import android.annotation.TargetApi
import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import android.os.Process
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import com.android.volley.Cache
import com.android.volley.Network
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.NoCache
import com.jcraft.jsch.*
import com.mpatric.mp3agic.ID3v2
import com.mpatric.mp3agic.Mp3File
import org.apache.commons.io.IOUtils
import org.jboss.aerogear.android.store.DataManager
import org.jboss.aerogear.android.store.Store
import org.jboss.aerogear.android.store.sql.SQLStore
import org.jboss.aerogear.android.store.sql.SQLStoreConfiguration
import org.json.JSONException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/**
 * @author edewit
 */
class FolderSource(context: Context, filesDir: File) : AbstractMusicSource() {
    private val TAG = "FolderSource"
    private val filesDir: File
    private val mRequestQueue: RequestQueue
    private val store: SQLStore<Media>

    override suspend fun load() {
        val music = File(Environment.getExternalStorageDirectory(), "Music")
        traverse(music, store, true).iterator()
        state = STATE_INITIALIZED
        cleanupStore(store)
    }

    override operator fun iterator(): Iterator<MediaMetadataCompat> {
        return store.readAll().map { m -> m.buildMediaItem() }.iterator()
//        for (m in media) {
//            val mediaItem = m.buildMediaItem()
//            val musicId = mediaItem.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
//            mMusicListById.put(musicId, MutableMediaMetadata(musicId, mediaItem))
//        }

//        return emptyList<MediaMetadataCompat>().iterator()
    }

    private fun getSongsForPlaylist(toFolder: File, playlist: String) {
        val url = "http://$IP_ADDRESS:8080/playlist/$playlist"
        val request = JsonArrayRequest(url, Response.Listener { response ->
            val songList: MutableList<String> = ArrayList(response.length())
            for (i in 0 until response.length()) {
                try {
                    songList.add(response.getString(i))
                } catch (e: JSONException) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
            transferFromPc(File(toFolder, playlist), songList)
        }, Response.ErrorListener { Log.i(TAG,"could not sync skipping") })
        mRequestQueue.add(request)
    }

    private fun transferFromPc(dir: File, songList: List<String>) {
        AsyncTask.execute {
            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            val ssh = JSch()
            val session: Session
            try {
                session = ssh.getSession(login, IP_ADDRESS, 22)
                session.setConfig(config)
                session.setPassword(password)
                session.setConfig("PreferredAuthentications", "password")
                session.connect()
                val channel: Channel = session.openChannel("sftp")
                channel.connect()
                val sftp: ChannelSftp = channel as ChannelSftp
                for (song in songList) {
                    val file = File(song)
                    val localFile = File(dir, file.name)
                    if (!localFile.exists()) {
                        try {
                            IOUtils.copy(sftp.get(song), FileOutputStream(localFile))
                        } catch (e: SftpException) {
                            Log.e(TAG, "couldn't copy file from remote $song")
                        }
                    }
                }
                channel.disconnect()
                session.disconnect()
            } catch (e: JSchException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val songs: MutableList<File> = ArrayList(songList.size)
            for (remotePath in songList) {
                val songName = File(remotePath).name
                songs.add(File(dir, songName))
            }
            for (file in dir.listFiles()) {
                if (!songs.contains(file)) {
                    file.delete()
                }
            }
        }
    }

    private fun traverse(dir: File, store: Store<Media>, playlist: Boolean): List<MediaMetadataCompat> {
        val tracks = ArrayList<MediaMetadataCompat>()
        if (!dir.exists()) return tracks
        for (file in dir.listFiles()) {
            if (file.isDirectory) {
                if (playlist) {
                    getSongsForPlaylist(dir, file.name)
                }
                tracks.addAll(traverse(file, store, false))
            } else try {
                if (store.read(file) == null) {
                    val mp3file = Mp3File(file.path)
                    val mediaItem = buildMediaItem(file, mp3file)
                    tracks.add(mediaItem)
                    store.save(Media(mediaItem))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "bad mp3 file $file")
            }
        }
        return tracks
    }

    private fun cleanupStore(store: Store<Media>) {
        val medias = store.readAll()
        for (media in medias) {
            val file: File? = media.file
            if (file != null && !file.exists()) {
                store.remove(file)
            }
        }
    }

    private fun buildMediaItem(file: File, mp3File: Mp3File): MediaMetadataCompat {
        val builder = MediaMetadataCompat.Builder()
        val id3v2: ID3v2 = mp3File.getId3v2Tag()
        builder
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, file.absolutePath)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, file.absolutePath)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, id3v2.getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, id3v2.getArtist())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mp3File.getLengthInMilliseconds())
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, file.parentFile.name)
        val cover: File
        try {
            cover = File(filesDir, file.name)
            cover.createNewFile()
            Thread(AlbumArtSaver(cover, id3v2.getAlbumImage())).start()
            builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, cover.toURI().toString())
        } catch (e: IOException) {
            e.printStackTrace()
        }
        builder
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, id3v2.getTitle())
        return builder
                .build()
    }

    private inner class AlbumArtSaver internal constructor(private val cover: File, private val picture: ByteArray?) : Runnable {
        @TargetApi(Build.VERSION_CODES.KITKAT)
        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            if (picture != null) {
                try {
                    FileOutputStream(cover).use { stream -> stream.write(picture) }
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
        }

    }

    companion object {
        private const val IP_ADDRESS = "192.168.0.206"
    }

    init {
        val cache: Cache = NoCache()
        this.filesDir = filesDir
        val network: Network = BasicNetwork(HurlStack())
        mRequestQueue = RequestQueue(cache, network)
        mRequestQueue.start()

        store = DataManager.config("musicStore", SQLStoreConfiguration::class.java)
                .withContext(context).store(Media::class.java) as SQLStore<Media>
        store.openSync()

    }
}