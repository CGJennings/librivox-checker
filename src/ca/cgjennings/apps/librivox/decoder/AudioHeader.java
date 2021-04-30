package ca.cgjennings.apps.librivox.decoder;

import ca.cgjennings.apps.librivox.Checker;

/**
 * Represents the frame header information in an MP3 stream.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 * @since 0.91
 */
public interface AudioHeader {

    /**
     * Returns the version of the MPEG Audio data.
     *
     * @return a version identifier
     */
    MPEGVersion getMPEGVersion();

    /**
     * Returns the layer type (I, II, III) of the audio data.
     *
     * @return the layer type identifier
     */
    LayerType getLayerType();

    /**
     * Returns the channel format for the audio, such as <code>MONO</code> or
     * <code>STEREO</code>.
     *
     * @return the channel format indicated by the header
     */
    ChannelFormat getChannelFormat();

    /**
     * Returns the sampling frequency, in Hz.
     *
     * @return the rate at which the audio data was sampled, in Hz
     */
    int getFrequency();

    /**
     * Returns the bit rate in kb/s. For variable bit rate files, this will be
     * an average.
     *
     * @return the (mean) bit rate, in kb/s
     */
    int getBitRate();

    /**
     * Returns <code>true</code> if the header indicates that the stream uses
     * variable bit rate encoding.
     *
     * @return <code>true</code> if the stream is VBR
     */
    boolean isVariableBitRate();

    /**
     * Returns <code>true</code> if the copyright flag is set, which indicates
     * the file should not be copied.
     *
     * @return <code>true</code> if the copyright bit is set
     */
    boolean isCopyrighted();

    /**
     * Returns <code>true</code> if the original flag is set, which indicates
     * that the frame is located on its "original media" rather than being a
     * copy.
     *
     * @return <code>true</code> if original media flag is set
     */
    boolean isOriginal();
}
