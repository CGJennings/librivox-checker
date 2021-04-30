package ca.cgjennings.apps.librivox.decoder;

import ca.cgjennings.apps.librivox.Report;
import java.io.IOException;
import java.io.InputStream;

/**
 * A factory that creates MP3 decoders for input streams.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
public final class DecoderFactory {

    private DecoderFactory() {
    }

    /**
     * Creates a new stream decoder that reads MP3 data from the specified input
     * stream with a moderate tolerance for decoding errors. It does not write
     * messages to a report when decoding errors occur.
     *
     * @param in an input stream containing MP3 audio data
     * @return a stream decoder with the requested characteristics
     * @throws java.io.IOException if an I/O exception occurs while creating the
     * decoder
     * @see #createDecoder(java.io.InputStream,
     * ca.cgjennings.apps.librivox.Report,
     * ca.cgjennings.apps.librivox.decoder.StreamDecoder.ErrorTolerance)
     */
    public static StreamDecoder createDecoder(InputStream in) throws IOException {
        return createDecoder(in, null, StreamDecoder.ErrorTolerance.MODERATE);
    }

    /**
     * Creates a new stream decoder that reads MP3 data from the specified input
     * stream with a moderate tolerance for decoding errors.
     *
     * @param in an input stream containing MP3 audio data
     * @param report an optional report that will be used to record information
     * about decoding errors; may be <code>null</code>
     * @return a stream decoder with the requested characteristics
     * @throws java.io.IOException if an I/O exception occurs while creating the
     * decoder
     * @see #createDecoder(java.io.InputStream,
     * ca.cgjennings.apps.librivox.Report,
     * ca.cgjennings.apps.librivox.decoder.StreamDecoder.ErrorTolerance)
     */
    public static StreamDecoder createDecoder(InputStream in, Report report) throws IOException {
        return createDecoder(in, report, StreamDecoder.ErrorTolerance.MODERATE);
    }

    /**
     * Creates a new stream decoder that reads MP3 data from the specified input
     * stream and applies the given error tolerance hint. It does not write
     * messages to a report when decoding errors occur.
     *
     * @param in an input stream containing MP3 audio data
     * @param tolerance a hint regarding how many errors are acceptable before
     * giving up
     * @return a stream decoder with the requested characteristics
     * @throws java.io.IOException if an I/O exception occurs while creating the
     * decoder
     * @see #createDecoder(java.io.InputStream,
     * ca.cgjennings.apps.librivox.Report,
     * ca.cgjennings.apps.librivox.decoder.StreamDecoder.ErrorTolerance)
     */
    public static StreamDecoder createDecoder(InputStream in, StreamDecoder.ErrorTolerance tolerance) throws IOException {
        return createDecoder(in, null, tolerance);
    }

    /**
     * Creates a new stream decoder with the specified parameters. Data will be
     * read from the input stream and parsed as MP3 compressed audio. When the
     * decoder encounters a decoding error, such as a corrupt frame in the audio
     * data, it may describe this error in the supplied report if it is
     * non-<code>null</code>, but it is not required to. The decoder may consult
     * the error tolerance hint when deciding whether to give up and throw an
     * exception when decoding errors occur, or it may be ignored.
     *
     * @param in an input stream containing MP3 audio data
     * @param report an optional report that will be used to record information
     * about decoding errors; may be <code>null</code>
     * @param tolerance a hint regarding how many errors are acceptable before
     * giving up
     * @return a stream decoder with the requested characteristics
     * @throws java.io.IOException if an I/O exception occurs while creating the
     * decoder
     * @see #createDecoder(java.io.InputStream,
     * ca.cgjennings.apps.librivox.Report,
     * ca.cgjennings.apps.librivox.decoder.StreamDecoder.ErrorTolerance)
     */
    public static StreamDecoder createDecoder(InputStream in, Report report, StreamDecoder.ErrorTolerance tolerance) throws IOException {
        return new JavaLayerStreamDecoder(in, report, tolerance);
    }
}
