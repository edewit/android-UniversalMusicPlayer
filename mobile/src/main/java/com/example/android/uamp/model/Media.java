package com.example.android.uamp.model;

import android.support.v4.media.MediaMetadataCompat;

import org.jboss.aerogear.android.core.RecordId;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author edewit
 */

public class Media {
    @RecordId
    private File file;
    private String title;
    private String album;
    private String artist;
    private Long duration;
    private URI albumArt;

    public Media(MediaMetadataCompat mediaItem) {
        this.file = new File(mediaItem.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID));
        this.title = mediaItem.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
        this.album = mediaItem.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
        this.artist = mediaItem.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
        this.duration = mediaItem.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        try {
            this.albumArt = new URI(mediaItem.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI));
        } catch (URISyntaxException e) {
            //could not pars albumArt
        }
    }

    public MediaMetadataCompat buildMediaItem() {
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        builder
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, file.getAbsolutePath())
                .putString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE, file.toURI().toString())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, file.getParentFile().getName())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, albumArt.toString());

        return builder.build();
    }

    public void setFile(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public URI getAlbumArt() {
        return albumArt;
    }

    public void setAlbumArt(URI albumArt) {
        this.albumArt = albumArt;
    }
}
