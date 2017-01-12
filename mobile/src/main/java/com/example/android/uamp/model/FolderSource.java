package com.example.android.uamp.model;

import android.annotation.TargetApi;
import android.media.MediaMetadataRetriever;
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

/**
 * Created by edewit on 11/1/17.
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
                } else {
                    tracks.add(buildMediaItem(file));
                }
            }
        }
        return tracks;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private MediaMetadataCompat buildMediaItem(File file) {
        try {
            retriever.setDataSource(file.getPath());
        } catch (RuntimeException e) {
            return new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, file.getAbsolutePath())
                    .putString(MediaMetadataCompat.METADATA_KEY_GENRE, file.getParentFile().getName())
                    .build();
        }
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] embeddedPicture = retriever.getEmbeddedPicture();
        if (embeddedPicture != null) {
            try (FileOutputStream stream = new FileOutputStream(cover)) {
                stream.write(embeddedPicture);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        builder
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, cover.toURI().toString())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, retriever.extractMetadata(METADATA_KEY_ALBUM));
//        String[] trackNumber = retriever.extractMetadata(METADATA_KEY_CD_TRACK_NUMBER).split("/");
//        return builder
//                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, Long.parseLong(trackNumber[0]))
//                .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, Long.parseLong(trackNumber[1]))
        return builder
                .build();
    }
}
