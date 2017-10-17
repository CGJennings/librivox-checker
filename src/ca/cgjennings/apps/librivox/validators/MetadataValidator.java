package ca.cgjennings.apps.librivox.validators;

import ca.cgjennings.apps.librivox.LibriVoxAudioFile;
import ca.cgjennings.apps.librivox.decoder.AudioHeader;
import ca.cgjennings.apps.librivox.metadata.MP3FileMetadata;
import ca.cgjennings.apps.librivox.metadata.MetadataEditorLinkFactory;
import ca.cgjennings.apps.librivox.metadata.MetadataEditorLinkFactory.Container;
import ca.cgjennings.apps.librivox.metadata.MetadataEditorLinkFactory.LinkType;
import ca.cgjennings.apps.librivox.metadata.MetadataView;
import ca.cgjennings.apps.librivox.validators.Validator.Category;
import java.net.URL;
import static ca.cgjennings.apps.librivox.Checker.string;

/**
 * A validator for metadata contained in the ID3 tag(s).
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
public class MetadataValidator extends AbstractValidator {

	@Override
	public Category getCategory() {
		return Category.METADATA;
	}

	@Override
	public void beginAnalysis( AudioHeader header, Validator[] predecessors ) {
		MP3FileMetadata md = getLibriVoxFile().getMetadata();

		// (1) Add metadata to the information report

		boolean hasV2 = md.getID3v2Metadata() != null;
		if( hasV2 ) {
			display( md.getID3v2Metadata() );
		}

		boolean hasV1 = md.getID3v1Metadata() != null;
		if( hasV1 ) {
			if( hasV2 ) {
				getReport().addDivider( Category.METADATA );
			}
			display( md.getID3v1Metadata() );
		}

		// (2) Validate it
		MetadataView view = md.getID3Metadata(); // will get v2, v1, or null in that order

		if( view == null ) {
			// no metadata at all
			fail( "must-have-ID3v2", string("mv-must-have-ID3v2-missing") );
		} else {
			if( !hasV2 ) {
				// has V1, but not V2 metadata
				fail( "must-have-ID3v2", string("mv-must-have-ID3v2-v1") );
			} else {
				// the file HAS V2 metadata
				if( hasV1 ) {
					// has BOTH V1 and V2; some apps prefer V1 so get clipped fields
					fail( "must-not-have-ID3v1v2", string("mv-must-not-have-ID3v1v2") );
				}

				// has V2 metadata, let's check the specific versions
				String version = view.getMetadataFormat();
				if( !version.contains( "v2.3" ) ) {
					fail( "must-use-ID3v23", string( "mv-must-use-ID3v23v24", "2.3" ) );
				}
				if( !version.contains( "v2.4" ) ) {
					fail( "must-use-ID3v24", string( "mv-must-use-ID3v23v24", "2.4" ) );
				}
			}

			// check for author, title, etc.
			checkForMetadataField( "must-have-author", view.getArtist() );
			checkForMetadataField( "must-have-track-title", view.getTitle() );
			checkForMetadataField( "must-have-album-title", view.getAlbum() );

			// check for extra spaces
			checkWhitespace( "mv-title", view.getTitle() );
			checkWhitespace( "mv-artist", view.getArtist() );
			checkWhitespace( "mv-album", view.getAlbum() );
			checkWhitespace( "mv-year", view.getYear() );
			checkWhitespace( "mv-genre", view.getGenre() );
			checkWhitespace( "mv-comment", view.getComment() );
		}
	}

	private void checkWhitespace( String fieldKey, String metadataValue ) {
		if( metadataValue != null && metadataValue.length() > 0 ) {
			if( Character.isWhitespace( metadataValue.codePointAt(0) ) ) {
				warn( "must-not-have-leading-space", string( "mv-must-not-have-leading-space", string( fieldKey ) ) );
			}

			if( metadataValue.length() > 1 && Character.isWhitespace( metadataValue.codePointBefore( metadataValue.length() ) ) ) {
				warn( "must-not-have-trailing-space", string( "mv-must-not-have-trailing-space", string( fieldKey ) ) );
			}
		}
	}

	private void checkForMetadataField( String rule, String metadataValue ) {
		if( metadataValue == null || metadataValue.length() == 0 ) {
			fail( rule, string( "mv-" + rule ) );
		}
	}

	private void display( MetadataView v ) {
		id3Value( "mv-format", "<b>" + v.getMetadataFormat() + "</b>" );
		if( v.getThumbnailImage() != null ) {
			addArtwork( getLibriVoxFile(), v.getThumbnailImage() );
		}
		id3Value( "mv-title", v.getTitle() );
		id3Value( "mv-artist", v.getArtist() );
		id3Value( "mv-album", v.getAlbum() );
		id3Value( "mv-track", v.getTrack() );
		id3Value( "mv-year", v.getYear() );
		id3Value( "mv-genre", v.getGenre() );
		id3Value( "mv-comment", v.getComment() );
	}

	private void id3Value( String key, String value ) {
		if( value != null ) feature( key, value );
	}

	/**
	 * Adds cover art to the information report.
	 *
	 * @param cat the category to add the entry to
	 * @param url the URL of the artwork
	 */
	public void addArtwork( LibriVoxAudioFile file, URL url ) {
		StringBuilder b = new StringBuilder();
		if( file != null ) {
			b.append( "<a href='" ).append( MetadataEditorLinkFactory.getLink( file, LinkType.ART, Container.ID3V2 ) ).append( "'>" );
		}
		b.append( "<img src='" ).append( url ).append( "' style='padding 2px;' border=1>" );
		if( file != null ) {
			b.append( "</a>" );
		}
		getReport().addFeature( Category.METADATA, "", b.toString() );
	}

	@Override
	public String toString() {
		return string("mv-name");
	}

	@Override
	public String getDescription() {
		return string("mv-desc");
	}
}
