package ca.cgjennings.apps.librivox.decoder;

import java.io.IOException;

/**
 * A simple interface to an MP3 stream decoder that prepares {@link AudioFrames}
 * one frame at a time. Although a "frame" here is usually equivalent to an MP3
 * frame, it need not be.
 * <p>
 * This interface insulates the application from whatever MP3 decoder is used.
 *
 * @author Christopher G. Jennings https://cgjennings.ca/contact/
 */
public interface StreamDecoder {

    /**
     * Return the next frame in the stream, or <code>null</code> if there are no
     * more frames in the stream. Note that this method may return
     * <code>null</code> even if {@link #mayHaveMoreFrames()} returns
     * <code>true</code>.
     *
     * @return the next audio frame, or <code>null</code>
     * @throws java.io.IOException if an error occurs and the decoder cannot
     * recover
     */
    AudioFrame getNextFrame() throws IOException;

    /**
     * Skips by the next frame of audio without decoding it. An
     * {@link AudioFrame} is returned that contains frequency and channel
     * information, but no sample data.
     *
     * @return an audio frame with only header information and no samples for
     * the skipped frame
     * @throws java.io.IOException
     */
    AudioFrame skipFrame() throws IOException;

    /**
     * Returns the estimated track length, in seconds.
     *
     * @param streamLength the length of the stream
     * @return an estimate of the track length, in seconds
     */
    double estimateTrackLength(int streamLength);

    /**
     * Returns the estimated maximum number of audio frames.
     *
     * @param streamLength the length of the stream
     * @return an estimate of the number of frames
     */
    int estimateFrameCount(int streamLength);

    /**
     * Return the number of <i>valid</i> frames that have been decoded by this
     * decoder.
     *
     * @return the number of the next frame to be read
     */
    long getValidFramesDecoded();

    /**
     * Returns <code>true</code> if the stream <i>appears</i> to have more
     * frames of audio. The decoder has not actually decoded the next frame, so
     * it may turn out that the frame is corrupt or is metadata and there are no
     * more valid frames. If this returns <code>false</code>, then there are
     * certainly no more frames. If it returns <code>true</code>, then you must
     * check the result of {@link #getNextFrame()} to know for certain.
     *
     * @return <code>true</code> if there appear to be more frames in the stream
     */
    boolean mayHaveMoreFrames();

    /**
     * Returns the audio header for the first frame of the stream. The audio
     * header contains metadata about the audio format such as number of
     * channels, frequency, and so on. Individual {@link AudioFrame}s will also
     * provide some of the same information, as some values (the bit rate in
     * variable bit rate streams) can legally change from frame to frame.
     *
     * @return metadata about the stream based on sampling the headers of one or
     * more frames of audio
     * @throws IllegalStateException if no frames have been read yet
     */
    AudioHeader getAudioHeader() throws IllegalStateException;

    /**
     * An enumeration of hint values that can be used to control how the decoder
     * will handle corrupt or invalid audio frames.
     */
    public enum ErrorTolerance {
        /**
         * A hint that any decoding error should throw an exception.
         */
        NONE,
        /**
         * A hint that the decoder should tolerate a moderate number of errors
         * before giving up.
         */
        MODERATE,
        /**
         * A hint that the decoder should tolerate as many errors as possible.
         */
        ALL
    }
}
