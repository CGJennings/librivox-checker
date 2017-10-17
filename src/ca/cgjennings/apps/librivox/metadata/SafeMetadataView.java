package ca.cgjennings.apps.librivox.metadata;

import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * This is a wrapper for a {@link MetadataView} that will return the
 * empty string instead of <code>null</code> for any missing string field.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 * @since 0.91
 */
public class SafeMetadataView implements MetadataView {
	private MetadataView v;

	/**
	 * Creates a safe view of an existing view; string fields of the safe view
	 * are guaranteed not to return <code>null</code>.
	 *
	 * @param toWrap the view to wrap
	 */
	public SafeMetadataView( MetadataView toWrap ) {
		if( toWrap == null ) throw new NullPointerException( "toWrap" );
		v = toWrap;
	}

	private static String S( String s ) {
		return s == null ? "" : s;
	}

	@Override
	public String getAlbum() {
		return S( v.getAlbum() );
	}

	@Override
	public String getArtist() {
		return S( v.getArtist() );
	}

	@Override
	public String getComment() {
		return S( v.getComment() );
	}

	@Override
	public String getGenre() {
		return S( v.getGenre() );
	}

	@Override
	public String getMetadataFormat() {
		return S( v.getMetadataFormat() );
	}

	@Override
	public String getTitle() {
		return S( v.getTitle() );
	}

	@Override
	public String getTrack() {
		return S( v.getTrack() );
	}

	@Override
	public String getYear() {
		return S( v.getYear() );
	}

	@Override
	public URL getThumbnailImage() {
		return v.getThumbnailImage();
	}

	@Override
	public BufferedImage getImage() {
		return v.getImage();
	}
}
