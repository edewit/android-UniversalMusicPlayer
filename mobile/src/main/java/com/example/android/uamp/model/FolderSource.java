package com.example.android.uamp.model;

import android.annotation.TargetApi;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v4.media.MediaMetadataCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM;
import static android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST;
import static android.media.MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER;
import static android.media.MediaMetadataRetriever.METADATA_KEY_DURATION;
import static android.media.MediaMetadataRetriever.METADATA_KEY_TITLE;

/**
 * @author edewit
 */

public class FolderSource implements MusicProviderSource {
    private MediaMetadataRetriever retriever = new MediaMetadataRetriever();

    @Override
    public Iterator<MediaMetadataCompat> iterator() {
        File music = new File(Environment.getExternalStorageDirectory(), "Music");

        return traverse(music).iterator();
    }

    private List<MediaMetadataCompat> traverse(File dir) {
        ArrayList<MediaMetadataCompat> tracks = new ArrayList<>();
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    tracks.addAll(traverse(file));
                } else try {
                    retriever.setDataSource(file.getPath());
                    tracks.add(buildMediaItem(file));
                } catch (RuntimeException e) {
                    //ignore this file
                }
            }
        }
        return tracks;
    }

    private MediaMetadataCompat buildMediaItem(File file) {
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        builder
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, file.getAbsolutePath())
                .putString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE, file.toURI().toString())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, retriever.extractMetadata(METADATA_KEY_ALBUM))
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, retriever.extractMetadata(METADATA_KEY_ARTIST));

        String duration = retriever.extractMetadata(METADATA_KEY_DURATION);
        if (duration != null) {
            builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, Long.parseLong(retriever.extractMetadata(METADATA_KEY_DURATION)));
        }
        builder.putString(MediaMetadataCompat.METADATA_KEY_GENRE, file.getParentFile().getName());

        File cover;
        try {
            cover = File.createTempFile("cover", file.getName());
            AsyncTask.execute(new AlbumArtSaver(cover, retriever.getEmbeddedPicture()));
            builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, cover.toURI().toString());
        } catch (IOException e) {
            //ignore
        }

        return builder
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, retriever.extractMetadata(METADATA_KEY_TITLE))
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
