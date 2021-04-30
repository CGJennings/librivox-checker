package ca.cgjennings.apps.librivox.decoder;

import ca.cgjennings.apps.librivox.Checker;

/**
 * An enumeration of the possible channel formats. An MP3 stream has either one
 * or two channels of audio data. When two channels are present, it may or may
 * not represent stereo data.
 *
 * <p>
 * <b>Note:</b> The <code>DUAL_CHANNEL</code> mode is not fully supported by the
 * default validators, as they will treat the two channels as stereo data for
 * the purpose of determining volume and other waveform analyses.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 * @since 0.91
 */
public enum ChannelFormat {
    /**
     * The audio has a single channel (mono).
     */
    MONO("mono"),
    /**
     * The audio is two channel, stereo sound.
     */
    STEREO("stereo"),
    /**
     * The audio is encoded in joint stereo mode, which is a kind of compression
     * for stereo audio that exploits the fact that human hearing is not good at
     * determining the direction of all audio frequencies equally.
     */
    JOINT_STEREO("jointstereo"),
    /**
     * The audio has two different channels, but they do not represent stereo
     * sound. For example, one channel may content an English version of the
     * content while another contains a French version.
     */
    DUAL_CHANNEL("dual");

    private ChannelFormat(String key) {
        desc = Checker.string("hv-channels-" + key);
    }
    private String desc;

    /**
     * Returns the number of channels of audio that are present for this format.
     *
     * @return the number of channels present in the audio data
     */
    public int getChannelCount() {
        return ordinal() == 0 ? 1 : 2;
    }

    /**
     * Returns a localized, user-friendly description of this channel format.
     *
     * @return a description of the format, such as "Stereo"
     */
    @Override
    public String toString() {
        return desc;
    }
}
