package ca.cgjennings.apps.librivox.validators;

import ca.cgjennings.apps.librivox.decoder.AudioFrame;
import ca.cgjennings.apps.librivox.decoder.AudioHeader;
import ca.cgjennings.apps.librivox.decoder.ChannelFormat;
import ca.cgjennings.apps.librivox.metadata.MP3FileMetadata;
import ca.cgjennings.util.Settings;

/**
 * Validates and gathers information from the MP3 header.
 *
 * @author Christopher G. Jennings https://cgjennings.ca/contact/
 */
public class HeaderValidator extends AbstractValidator {

    @Override
    public Category getCategory() {
        return Category.FORMAT;
    }

    @Override
    public void beginAnalysis(AudioHeader header, Validator[] predecessors) {
        // save this so we can use it in endAnalysis()
        this.header = header;
    }

    @Override
    public void analyzeFrame(AudioFrame frame) {
        // corrupt ID3 tags can give a misleading value for track length
        // we determine an exact length based on the actual number of samples
        trackLength += (double) frame.getSampleCount() / (double) frame.getFrequency();
        ++channelTypeCounts[frame.getChannelFormat().ordinal()];
    }

    @Override
    public boolean isAudioProcessor() {
        return true;
    }

    @Override
    public void endAnalysis() {
        String val; // a temporary variable used to format the value of a feature
        Settings s = getSettings();

        MP3FileMetadata metadata = getLibriVoxFile().getMetadata();

        // ENCODING
        String format = "" + header.getMPEGVersion() + " " + header.getLayerType();
        String encoder = metadata.getEncoder();
        if (encoder != null) {
            val = string("hv-file-type-with-encoder", format, encoder);
        } else {
            val = string("hv-file-type-wo-encoder", format);
        }
        feature("hv-file-type", val);

        // AUDIO FORMAT
        long kbps = header.getBitRate();
        int freq = header.getFrequency();

        // we will decide what channel format is the main one by counting which
        // has the most frames
        int dominantChanneltype = 0, dominantCount = -1;
        int nonZeroChannelTypes = 0;
        for (int i = 0; i < channelTypeCounts.length; ++i) {
            if (channelTypeCounts[i] != 0) {
                ++nonZeroChannelTypes;
            }
            if (channelTypeCounts[i] > dominantCount) {
                dominantChanneltype = i;
                dominantCount = channelTypeCounts[i];
            }
        }

        // convert the dominant type into a channel format for analysis and display
        ChannelFormat dominantFormat = ChannelFormat.values()[dominantChanneltype];
        String channels = dominantFormat.toString();
        int channelCount = dominantFormat.getChannelCount();

        // check audio format
        if (kbps != 128) {
            fail("must-be-128kbps", string("hv-must-be-128kbps"));
        }

        if (freq != 44100) {
            fail("must-be-44.1kHz", string("hv-must-be-44.1kHz"));
        }

        if (channelCount != 1) {
            fail("must-be-mono", string("hv-must-be-mono"));
        }

        // if different frames have different channel formats, it is only
        // OK if the only formats are stereo and joint stereo
        if (nonZeroChannelTypes > 1) {
            if (nonZeroChannelTypes != 2 || (channelTypeCounts[ChannelFormat.STEREO.ordinal()] == 0 || channelTypeCounts[ChannelFormat.JOINT_STEREO.ordinal()] == 0)) {
                // note the discrepency
                StringBuilder b = new StringBuilder(string("hv-ambiguous-channels"));
                for (int i = 0; i < channelTypeCounts.length; ++i) {
                    if (channelTypeCounts[i] > 0) {
                        b.append("<br>");
                        b.append(string("hv-ambiguous-channels-entry", ChannelFormat.values()[i].toString(), channelTypeCounts[i]));
                    }
                }
                fail("must-have-non-ambiguous-channel-layout", b.toString());
            }
        }

        val = string((header.isVariableBitRate() ? "hv-format-vbr" : "hv-format-cbr"), kbps, freq, channels);
        feature("hv-format", val);

        if (header.isVariableBitRate()) {
            fail("must-be-constant-bit-rate", string("hv-must-be-constant-bit-rate"));
        }

        // TRACK LENGTH
        // we compute this exactly instead of estimating it
        long hours = 0L;
        long mins = (long) trackLength / 60L;
        double secs = trackLength - (mins * 60);
        if (mins > 60L) {
            hours = mins / 60L;
            mins -= hours * 60L;
        }
        if (hours > 0L) {
            feature("hv-track-length", string("hv-track-length-val-hours", hours, mins, secs));
        } else {
            feature("hv-track-length", string("hv-track-length-val-mins", mins, secs));
        }

        // convert base length to minutes to check rules
        trackLength /= 60d;
        if (trackLength > s.getDouble("track-length-fail", 60d)) {
            fail("must-obey-track-length", string("hv-must-obey-track-length-fail"));
        } else if (trackLength > s.getDouble("track-length-warn", 120d)) {
            warn("must-obey-track-length", string("hv-must-obey-track-length-warn"));
        }
        if (trackLength < s.getDouble("minimum-track-length", 10d)) {
            fail("must-obey-minimum-track-length", string("hv-must-obey-minimum-track-length"));
        }
    }

    private AudioHeader header;
    private double trackLength = 0d;

    private int[] channelTypeCounts = new int[ChannelFormat.values().length];

    @Override
    public String toString() {
        return string("hv-name");
    }

    @Override
    public String getDescription() {
        return string("hv-desc");
    }
}
