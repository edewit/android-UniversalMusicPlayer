package com.example.android.uamp.media.library

import android.support.v4.media.MediaMetadataCompat
import org.jboss.aerogear.android.core.RecordId
import java.io.File
import java.net.URI
import java.net.URISyntaxException

/**
 * @author edewit
 */
class Media(mediaItem: MediaMetadataCompat) {
    @RecordId
    var file: File? = null
    var title: String? = null
    var album: String? = null
    var artist: String? = null
    var duration: Long? = null
    var albumArt: URI? = null
    fun buildMediaItem(): MediaMetadataCompat {
        val builder = MediaMetadataCompat.Builder()
        builder
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, file!!.absolutePath)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, file!!.absolutePath)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, file!!.parentFile.name)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration!!)
//                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, file!!.parentFile.name)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, albumArt.toString())
        return builder.build()
    }

    init {
        val mediaItemId = mediaItem.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
        if (mediaItemId != null) {
            file = File(mediaItemId)
            title = mediaItem.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
            album = mediaItem.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)
            artist = mediaItem.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
            duration = mediaItem.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
            try {
                albumArt = URI(mediaItem.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI))
            } catch (e: URISyntaxException) {
                //could not parse albumArt
            }
        }
    }
}