package ca.cgjennings.apps.librivox.decoder;

import ca.cgjennings.apps.librivox.Checker;

/**
 * This exception is thrown by a {@link StreamDecoder} that has come to the
 * conclusion that the stream does not represent a valid MP3 audio stream.
 * Typically, this means that the decoder has tried to decode several frames in
 * a row without success, and that no frames have been successfully decoded so
 * far.
 *
 * @author Christopher G. Jennings https://cgjennings.ca/contact/
 */
public class NotAnMP3Exception extends DecodingException {

    public NotAnMP3Exception() {
        super(Checker.string("error-invalid"));
    }
}
