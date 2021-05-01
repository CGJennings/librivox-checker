package ca.cgjennings.apps.librivox.metadata;

import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * Instances of this interface are returned by {@link MP3FileMetadata} to
 * provide abstract views of metadata contained in an MP3 file.
 *
 * @author Christopher G. Jennings https://cgjennings.ca/contact/
 * @since 0.91
 */
public interface MetadataView {

    /**
     * Returns the title contained in the metadata or <code>null</code>.
     *
     * @return the title, or <code>null</code>
     */
    String getTitle();

    /**
     * Returns the artist name contained in the metadata, or <code>null</code>.
     *
     * @return the artist name, or <code>null</code>
     */
    String getArtist();

    /**
     * Returns the album name contained in the metadata, or <code>null</code>.
     *
     * @return the album name, or <code>null</code>
     */
    String getAlbum();

    /**
     * Returns the track number contained in the metadata, or <code>null</code>.
     *
     * @return the track number, or <code>null</code>
     */
    String getTrack();

    /**
     * Returns the year contained in the metadata, or <code>null</code>.
     *
     * @return the year, or <code>null</code>
     */
    String getYear();

    /**
     * Returns the genre contained in the metadata, or <code>null</code>.
     *
     * @return the genre, or <code>null</code>
     */
    String getGenre();

    /**
     * Returns the comment contained in the metadata, or <code>null</code>.
     *
     * @return the comment, or <code>null</code>
     */
    String getComment();

    /**
     * Returns the first (and typically only) embedded image, or
     * <code>null</code> if no image is embedded.
     *
     * @return an image URL, or <code>null</code>
     */
    URL getThumbnailImage();

    /**
     * Returns the full size version of the embedded image, or
     * <code>null</code>.
     *
     * @return the full size image, if any
     */
    BufferedImage getImage();

    /**
     * Returns a description of the metadata format and version.
     *
     * @return a description of the format, such as "ID3v1.0.0"
     */
    String getMetadataFormat();
}
