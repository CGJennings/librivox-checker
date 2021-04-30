package ca.cgjennings.apps.librivox.validators;

import ca.cgjennings.apps.librivox.decoder.AudioFrame;
import ca.cgjennings.apps.librivox.*;
import ca.cgjennings.apps.librivox.decoder.AudioHeader;
import ca.cgjennings.apps.librivox.validators.Validator.Category;
import ca.cgjennings.util.Settings;

/**
 * A validator for the audio data: volume, sample rate, channel format, and so
 * on.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
public class AmplitudeValidator extends AbstractValidator {

    public static final float STANDARD_TARGET_VOLUME = 89f;

    @Override
    public Category getCategory() {
        return Category.AUDIO;
    }

    @Override
    public boolean isAudioProcessor() {
        return true;
    }

    @Override
    public void beginAnalysis(AudioHeader header, Validator[] predecessors) {
        maxChannelCount = 0;

        for (int i = 0; i < MAX_CHANNELS; ++i) {
            sum[i] = 0d;
            n[i] = 0d;
            dcBias[i] = 0d;
        }

        // clipping
        clippedSamples = totalSamples = clipRunLength = 0;
        clipLength = getLibriVoxFile().getMetadata().getTrackLength();

        // range
        samplesAbove80 = 0;
        maxAmplitude = Short.MIN_VALUE;
        minAmplitude = Short.MAX_VALUE;

        gainAnalyzer = new ReplayGainAnalyzer();
    }

    @Override
    public void analyzeFrame(AudioFrame frame) {
        gainAnalyzer.processFrame(frame);

        int channels = frame.getChannelCount();
        maxChannelCount = Math.max(maxChannelCount, channels);

        short[] buff = frame.getSamples();
        int len = frame.getSampleCount();

        for (int c = 0; c < channels; ++c) {
            int off = c;
            for (int s = 0; s < len; ++s, off += channels) {
                short amplitude = buff[off];
                sum[c] += amplitude;

                // Clipping --- does not distinguish between channels
                if (amplitude >= Short.MAX_VALUE || amplitude <= Short.MIN_VALUE) {
                    ++clipRunLength;
                    if (clipRunLength >= MINIMUM_CLIP_RUN) {
                        if (clipRunLength == MINIMUM_CLIP_RUN) {
                            clippedSamples += MINIMUM_CLIP_RUN;
                        } else {
                            clippedSamples += 1;
                        }
                    }
                } else {
                    clipRunLength = 0;
                }

                // Range --- does not distinguish between channels
                if (amplitude > maxAmplitude) {
                    maxAmplitude = amplitude;
                }
                if (amplitude < minAmplitude) {
                    minAmplitude = amplitude;
                }
                if (amplitude < m80Amplitude) {
                    ++samplesAbove80;
                }
                if (amplitude > p80Amplitude) {
                    ++samplesAbove80;
                }
            }
        }
        for (int c = 0; c < channels; ++c) {
            // Clipping (total samples in *all* channels)
            totalSamples += len;
            // Total samples per channel
            n[c] += len;
        }
    }

    /**
     * If the number of channels is one, return <code>channel1</code>. Otherwise
     * return a string that formats both <code>channel1</code> and
     * <code>channel2</code> as a two channel data value.
     *
     * @param channel1 a string representing channel 1's data value
     * @param channel2 a string representing channel 2's data value
     * @return a string representing one or both data values depending on the
     * number of channels
     */
    private String getTwoChannelString(String channel1, String channel2) {
        if (maxChannelCount == 1) {
            return channel1;
        } else {
            return string("av-two-channel", channel1, channel2);
        }
    }

    @Override
    public void endAnalysis() {
        Settings settings = getSettings();

        final float targetVolume = settings.getFloat("target-volume", STANDARD_TARGET_VOLUME);
        float volume, gain;
        try {
            ReplayGainAnalyzer.Analysis ga = gainAnalyzer.done();
            volume = ga.getVolume();
            gain = ga.getAdjustment(targetVolume);
        } catch (IllegalStateException e) {
            // no samples were processed
            volume = 0f;
            gain = targetVolume;
        }

        feature("av-volume", string("av-volume-val", volume));

        double minVol = settings.getFloat("volume-min", 86f);
        double maxVol = settings.getFloat("volume-max", 92f);
        if (volume < minVol || volume > maxVol) {
            fail("must-have-minimum-volume", string("av-must-have-minimum-volume", minVol, maxVol));
        }

        String val1, val2;
        dcBias[0] = (double) sum[0] / (double) n[0];
        val1 = string("av-dc-bias-val",
                Math.round(dcBias[0]),
                dcBias[0] / 32768d * 100d
        );

        dcBias[1] = (double) sum[1] / (double) n[1];
        val2 = string("av-dc-bias-val",
                Math.round(dcBias[0]),
                dcBias[1] / 32768d * 100d
        );

        feature("av-dc-bias", getTwoChannelString(val1, val2));

        double dcb0 = Math.abs(dcBias[0]);
        double dcb1 = dcb0;
        if (maxChannelCount > 1) {
            dcb1 = Math.abs(dcBias[1]);
        }
        double failLevel = settings.getDouble("dc-bias-fail", 32768d);
        double warnLevel = settings.getDouble("dc-bias-warn", 250d);
        if (dcb0 > failLevel || dcb1 > failLevel) {
            fail("must-avoid-dc-bias", string("av-must-avoid-dc-bias"));
        } else if (dcb0 > warnLevel || dcb1 > warnLevel) {
            warn("must-avoid-dc-bias", string("av-must-avoid-dc-bias"));
        }

        double clippedRatio = (double) clippedSamples / (double) totalSamples;
        double clippedMs = (clippedRatio * clipLength) / 1000d;
        double clippedPercent = clippedRatio * 100d;
        feature("av-clipped", string("av-clipped-val", clippedMs, clippedRatio));

        if (clippedPercent > settings.getDouble("clipping-limit-fail", 8.333d)) {
            fail("must-avoid-clipping", string("av-must-avoid-clipping-fail"));
        } else if (clippedPercent > settings.getDouble("clipping-limit-warn", 0.277d)) {
            fail("must-avoid-clipping", string("av-must-avoid-clipping-warn"));
        }
    }

    private int maxChannelCount;
    double[] sum = new double[MAX_CHANNELS];
    double[] n = new double[MAX_CHANNELS];
    double[] dcBias = new double[MAX_CHANNELS];
    private int clippedSamples;
    private int totalSamples;
    private int clipRunLength;

    // there must be at least this many clipped samples in a row to count as clipped
    private static final int MINIMUM_CLIP_RUN = Checker.getSettings().getInt("clipping-run-length", 2);

    private static final int MAX_CHANNELS = 2;

    private int maxAmplitude;
    private int minAmplitude;

    private int p80Amplitude = Math.round(Short.MAX_VALUE * 0.8f);
    private int m80Amplitude = Math.round(Short.MIN_VALUE * 0.8f);
    private int samplesAbove80;

    private double clipLength;

    private ReplayGainAnalyzer gainAnalyzer;

    @Override
    public String toString() {
        return string("av-name");
    }

    @Override
    public String getDescription() {
        return string("av-desc");
    }
}
