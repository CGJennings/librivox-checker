package ca.cgjennings.apps.librivox.metadata;

import java.awt.image.BufferedImage;
import java.net.URL;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.id3.AbstractTag;
import org.jaudiotagger.tag.id3.ID3v24Tag;

/**
 * This is the implementation of MetadataView used internally by
 * {@link MP3FileMetadata} as an adapter for tags from the
 * Jaudiotagger library.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 * @since 0.91
 */
class ViewImpl implements MetadataView {
	String format;
	String title;
	String artist;
	String album;
	String track;
	String year;
	String genre;
	String comment;
	URL imageURL;
	BufferedImage image;

	ViewImpl() {}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public String getArtist() {
		return artist;
	}

	@Override
	public String getAlbum() {
		return album;
	}

	@Override
	public String getTrack() {
		return track;
	}

	@Override
	public String getYear() {
		return year;
	}

	@Override
	public String getGenre() {
		return genre;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public String getMetadataFormat() {
		return format;
	}

	@Override
	public URL getThumbnailImage() {
		return imageURL;
	}

	@Override
	public BufferedImage getImage() {
		return image;
	}

	private ID3v24Tag copyTo( AbstractTag src ) {
		ID3v24Tag tag;
		if( src == null ) {
			tag = new ID3v24Tag();
		} else {
			tag = new ID3v24Tag( src );
		}
		try {
			if( title != null ) {
				tag.setTitle( title );
			}
			if( artist != null ) {
				tag.setArtist( artist );
			}
			if( album != null ) {
				tag.setAlbum( album );
			}
			if( track != null ) {
				tag.setTrack( track );
			}
			if( year != null ) {
				tag.setYear( year );
			}
			if( genre != null ) {
				tag.setGenre( genre );
			}
			if( comment != null ) {
				tag.setComment( year );
			}
		} catch( FieldDataInvalidException e ) {
			throw new AssertionError( e );
		}
		return tag;
	}
}
