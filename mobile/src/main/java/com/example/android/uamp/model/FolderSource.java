package com.example.android.uamp.model;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Environment;
import android.support.v4.media.MediaMetadataCompat;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;

import org.jboss.aerogear.android.store.DataManager;
import org.jboss.aerogear.android.store.Store;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author edewit
 */

public class FolderSource implements MusicProviderSource {

    @Override
    public Iterator<MediaMetadataCompat> iterator() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                File music = new File(Environment.getExternalStorageDirectory(), "Music");

                Store<Media> store = (Store<Media>) DataManager.getStore("musicStore");
                traverse(music, store).iterator();
            }
        }).start();

        return null;
    }

    private List<MediaMetadataCompat> traverse(File dir, Store<Media> store) {

        ArrayList<MediaMetadataCompat> tracks = new ArrayList<>();
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    tracks.addAll(traverse(file, store));
                } else try {
                    if (store.read(file) == null) {
                        Mp3File mp3file = new Mp3File(file.getPath());
                        MediaMetadataCompat mediaItem = buildMediaItem(file, mp3file);
                        tracks.add(mediaItem);
                        store.save(new Media(mediaItem));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return tracks;
    }

    private MediaMetadataCompat buildMediaItem(File file, Mp3File mp3File) {
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        ID3v2 id3v2 = mp3File.getId3v2Tag();
        builder
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, file.getAbsolutePath())
                .putString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE, file.toURI().toString())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, id3v2.getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, id3v2.getArtist())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mp3File.getLengthInMilliseconds())
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, file.getParentFile().getName());

        File cover;
        try {
            cover = File.createTempFile("cover", file.getName());
            new Thread(new AlbumArtSaver(cover, id3v2.getAlbumImage())).start();
            builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, cover.toURI().toString());
        } catch (IOException e) {
            //ignore
        }

        return builder
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, id3v2.getTitle())
                .build();
    }

    private class AlbumArtSaver implements Runnable {

        private final File cover;
        private final byte[] picture;

        AlbumArtSaver(File cover, byte[] picture) {
            this.cover = cover;
            this.picture = picture;
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            if (picture != null) {
                try (FileOutputStream stream = new FileOutputStream(cover)) {
                    stream.write(picture);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }
}
