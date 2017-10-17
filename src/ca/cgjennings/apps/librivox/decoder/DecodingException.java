package ca.cgjennings.apps.librivox.decoder;

import ca.cgjennings.apps.librivox.Report;
import java.io.IOException;

/**
 * This excpetion is thrown when the stream appears to be corrupt.
 * Typically, this means that a number of frames have been decoded
 * successfully, but that a number of decoding errors have occurred which
 * exceeds some internal limit. In other words, a typical decoder will
 * skip a small number of corrupt frames (adding appropriate messages
 * to a {@link Report}), but will give up if too many errors occur.
 * 
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
public class DecodingException extends IOException {
	public DecodingException( String message ) {
		super( message );
	}
}
