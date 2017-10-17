package ca.cgjennings.apps.librivox.validators;

import ca.cgjennings.apps.librivox.decoder.AudioHeader;
import ca.cgjennings.apps.librivox.metadata.LibriVoxFileNameMetadata;
import ca.cgjennings.apps.librivox.metadata.MetadataView;
import ca.cgjennings.apps.librivox.metadata.SafeMetadataView;
import ca.cgjennings.apps.librivox.validators.Validator.Category;
import ca.cgjennings.util.Settings;
import java.io.File;
import java.util.LinkedList;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A validator for details about the file as a filesystem object
 * (file name, etc.).
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
public class FileValidator extends AbstractValidator {

	@Override
	public Category getCategory() {
		return Category.FILE;
	}


	@Override
	public void beginAnalysis( AudioHeader header, Validator[] predecessors ) {
		File file = getLibriVoxFile().getLocalFile();

		feature( "fv-file-size"	, string( "fv-file-size-val",
				(double) file.length() / (1024d*1024d) )
		);

		String name = getLibriVoxFile().getFileName();

		if( !name.equals( name.toLowerCase( Locale.ENGLISH ) ) ) {
			fail( "must-use-lower-case", string( "fv-must-use-lower-case" ) );
		}

		String illegalChars = getSettings().get( "incompatible-chars", ":\\/*?<>&#$+%!`*|{}=@" );
		for( int i=0; i<illegalChars.length(); ++i ) {
			if( name.indexOf( illegalChars.charAt(i) ) >= 0 ) {
				fail( "must-not-use-incompatible-chars", string( "fv-must-not-use-incompatible-chars", illegalChars ) );
				break;
			}
		}

		if( SPACE_REGEX.matcher( name ).matches() ) {
			fail( "must-not-use-spaces", string( "fv-must-not-use-spaces" ) );
		}

		// only check individual fields if there is at least one '_';
		// otherwise the user probably used spaces or something else---
		// no fields will be found, so there will be a lot of confusing
		// error messages that don't make sense
		if( name.indexOf( '_' ) >= 0 ) {
			LibriVoxFileNameMetadata lvname = new LibriVoxFileNameMetadata( name );
			checkFileNameFields( lvname );
			checkFieldsAgainstID3( lvname );
		} else {
			fail( "must-have-underscore-fields", string( "fv-must-have-underscore-fields" ) );
		}


/*
 * Define a metadata scheme (filename & ID3 tags) in your first post. The filename should be in this format (all lowercase, no spaces): title_chapter/section number(s)_author's last name.mp3. Please keep the filename as short as possible, e.g. blackbeauty_01_sewell.mp3 or wind_willows_01_grahame.mp3.
 */
	}

	private void checkFileNameFields( LibriVoxFileNameMetadata lvname ) {
		if( lvname.hasEmptyField() ) {
			fail( "must-not-have-empty-fields", string( "fv-must-not-have-empty-fields" ) );
		}

		if( lvname.getBitRate() != null ) {
			fail( "must-not-include-bit-rate", string( "fv-must-not-include-bit-rate", lvname.getBitRate() ) );
		}

		if( lvname.getTitle() == null ) {
			fail( "must-have-valid-name", string( "fv-must-have-title" ) );
		}

		if( lvname.getSection() < 0 ) {
			if( lvname.getSectionAsString() == null ) {
				if( lvname.getReaderInitials() == null || !getSettings().getBoolean( "ignore-section-numbers-if-initials-present", false ) ) {
					fail( "must-have-valid-name", string( "fv-must-have-valid-section" ) );
				}
			} else {
				fail( "must-have-valid-name", string( "fv-must-have-valid-section-garbage", lvname.getSectionAsString() ) );
			}
		}

		if( lvname.getLastName() == null ) {
			fail( "must-have-valid-name", string( "fv-must-have-last-name" ) );
		}

		if( lvname.getExtension() == null || (!"mp3".equals(lvname.getExtension().toLowerCase( Locale.US ))) ) {
			fail( "must-have-valid-name", string( "fv-must-use-mp3-extension" ) );
		}

		if( lvname.getSection() >= 0 && lvname.getSection() < 10 && !lvname.getSectionAsString().startsWith("0") ) {
			warn( "must-have-leading-zero", string("fv-missing-zero-prefix") );
		}
	}

	private void checkFieldsAgainstID3( LibriVoxFileNameMetadata lvname ) {
		MetadataView tag = getLibriVoxFile().getMetadata().getID3Metadata();
		if( tag == null ) return;
		tag = new SafeMetadataView( tag );

		LinkedList<String> mismatches = new LinkedList<String>();

		Settings s = getSettings();
		if( s.getBoolean( "match-album", false ) ) {
			// check that every letter in the short title at least occurs
			// in the long title
			String id3album = tag.getAlbum();
			String t1 = compactField( lvname.getTitle() );
			String t2 = compactField( id3album );
			for( int i=0; i<t1.length(); ++i ){
				if( t2.indexOf( t1.charAt(i) ) < 0 ) {
					mismatches.add( string( "fv-title-mismatch", lvname.getTitle(), id3album ) );
					break;
				}
			}
		}

		if( s.getBoolean( "match-artist", false ) ) {
			// check that the end of the artist name matches the last name field
			String id3artist = tag.getArtist();
			String t1 = compactField( lvname.getLastName() );
			String t2 = compactField( id3artist );
			if( !t2.endsWith( t1 ) ) {
				mismatches.add( string( "fv-name-mismatch", lvname.getLastName(), id3artist ) );
			}
		}

		if( s.getBoolean( "match-track-to-title", false ) ) {
			String id3title = tag.getTitle();
			String t1 = extractTrack( lvname.getSectionAsString() );
			String t2 = extractTrack( id3title );
			if( !t1.equals( t2 ) ) {
				mismatches.add( string( "fv-track-title-mismatch", lvname.getSectionAsString(), id3title ) );
			}
		}

		if( s.getBoolean( "match-track-to-track", false ) ) {
			try {
				String id3track = tag.getTrack();
				String t1 = extractTrack( lvname.getSectionAsString() );
				String t2 = extractTrack( id3track );
				if( t2.length() > 0 && !t1.equals( t2 ) ) {
					mismatches.add( string( "fv-track-track-mismatch", lvname.getSectionAsString(), id3track ) );
				}
			} catch( UnsupportedOperationException e ) {
				// this happens if the only tag is
				// ID3v10 (which has no track numbers)
			}
		}

		if( mismatches.size() > 0 ) {
			StringBuilder b = new StringBuilder( string( "fv-must-match-metadata" ) );
			for( String message : mismatches ) {
				b.append( "<p>" ).append( message );
			}
			fail( "must-match-metadata", b.toString() );
		}
	}

	private String extractTrack( String field ) {
		if( field == null ) return "";
		StringBuilder b = new StringBuilder( field.length() );
		for( int i=0; i<field.length(); ++i ) {
			char c = field.charAt( i );
			if( c < '0' || c > '9' )
				break;
			b.append( c );
		}
		return b.toString();
	}


	private String compactField( String field ) {
		if( field == null ) return "";
		StringBuilder b = new StringBuilder( field.length() );
		for( int i=0; i<field.length(); ++i ) {
			char c = field.charAt( i );
			if( !(Character.isLetter( c ) || Character.isDigit( c )) )
				continue;
			c = Character.toLowerCase( c );
			b.append( c );
		}
		return b.toString();
	}

	private static final Pattern SPACE_REGEX = Pattern.compile( ".*\\s+.*" );


	@Override
	public String toString() {
		return string("fv-name");
	}

	@Override
	public String getDescription() {
		return string("fv-desc");
	}
}
