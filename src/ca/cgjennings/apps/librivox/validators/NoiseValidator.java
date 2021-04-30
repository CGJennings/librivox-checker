package ca.cgjennings.apps.librivox.validators;

import ca.cgjennings.apps.librivox.Checker;
import ca.cgjennings.apps.librivox.decoder.AudioFrame;
import ca.cgjennings.apps.librivox.decoder.AudioHeader;
import java.util.logging.Level;
import static ca.cgjennings.apps.librivox.Checker.string;

/**
 * Measures background noise by capturing the quietest part of the audio.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 * @since 0.92
 */
public class NoiseValidator extends AbstractValidator {

    /**
     * Creates a new noise validator.
     */
    public NoiseValidator() {
    }

    @Override
    public Category getCategory() {
        return Category.AUDIO;
    }

    @Override
    public boolean isAudioProcessor() {
        return true;
    }

    /**
     * Size of window to capture.
     */
    private static int WINDOW_SIZE_MS;

    /**
     * The current leader for quietest window.
     */
    private short[] leaderBuff;

    /**
     * The buffer used to capture the next segment to compare to the leader;
     * when a new leader is found, the buffers are simply swapped.
     */
    private short[] captureBuff;

    /**
     * The smallest means of squares of the windows we have found so far. That
     * is, we take the RMS amplitude of each window, and look for the one with
     * the smallest value. The only difference is that we don't bother taking
     * the square root since we don't care about the exact RMS value, just which
     * one would be smallest.
     */
    private double minMS = Double.MAX_VALUE;

    @Override
    public void beginAnalysis(AudioHeader header, Validator[] predecessors) {
        WINDOW_SIZE_MS = getSettings().getInt("noise-window", 500);

        // calculate window size in samples
        int winSize = header.getFrequency() * WINDOW_SIZE_MS / 1000;
        winSize *= header.getChannelFormat().getChannelCount();
        // just in case win size is so large it wraps the int
        if (winSize < 0) {
            throw new AssertionError();
        }

        leaderBuff = new short[winSize];
        captureBuff = new short[winSize];
    }

    /**
     * The pointer to the position in the buffer where the next frame should be
     * written
     */
    private int p;

    @Override
    public void analyzeFrame(AudioFrame frame) {
        // if the current leader is all zeroes, there is no way we will beat
        // so don't bother doing the work
        if (minMS <= 0d) {
            return;
        }

        // we need the basic stats from a frame in endAnalysis
        frameTemplate = frame;

        // all we do here is fill up the capture buffer;
        // whenever we get a full window (buffer), we pass that to analyzeWindow
        // to see if we have a new winner
        int winSize = captureBuff.length;
        int nSamples = frame.getSampleCount();
        short[] samples = frame.getSamples();

        if (p + nSamples >= winSize) {
            // fill up the buffer and analyze
            final int samplesToCopy = winSize - p;
            System.arraycopy(samples, 0, captureBuff, p, samplesToCopy);
            analyzeWindow();
            p = 0;
            // if there are leftover samples from the frame, capture them
            // for next time
            final int leftovers = nSamples - samplesToCopy;
            if (leftovers > 0) {
                System.arraycopy(samples, samplesToCopy, captureBuff, 0, leftovers);
                p = leftovers;
            }
        } else {
            System.arraycopy(samples, 0, captureBuff, p, nSamples);
            p += nSamples;
        }

    }

    private void analyzeWindow() {
        final int winSize = captureBuff.length;
        double sum = 0d;
        for (int i = 0; i < winSize; ++i) {
            final double sample = (double) captureBuff[i];
            sum += sample * sample;
        }

        // is this window the new leader for quietest?
        sum /= (double) winSize;
        if (sum < minMS) {
            minMS = sum;
            short[] temp = leaderBuff;
            leaderBuff = captureBuff;
            captureBuff = temp;
        }
    }

    /**
     * Keep a frame around so we can duplicate it to create a frame in
     * endAnalysis().
     */
    private AudioFrame frameTemplate;

    @Override
    public void endAnalysis() {
        if (minMS < Double.MAX_VALUE) {
            AudioFrame f = new AudioFrame();
            f.set(frameTemplate.getChannelFormat(), frameTemplate.getFrequency(), leaderBuff, leaderBuff.length);
            ReplayGainAnalyzer rga = new ReplayGainAnalyzer();
            rga.processFrame(f);
            float noiseVolume = rga.done().getVolume();

            feature("bn-noise", string("av-volume-val", noiseVolume));

            if (noiseVolume > getSettings().getFloat("noise-limit-fail", 48f)) {
                fail("must-have-noise-under-limit", string("bn-must-have-noise-under-limit"));
            } else if (noiseVolume > getSettings().getFloat("noise-limit-warn", 46f)) {
                warn("must-have-noise-under-limit", string("bn-must-have-noise-under-limit-warn"));
            } else if (noiseVolume < getSettings().getFloat("suspect-zero-noise-level", 1f)) {
                fail("suspect-zero-noise", string("bn-suspect-zero-noise"));
            }

        } else {
            // not enough samples to capture a full window
            Checker.getLogger().log(Level.WARNING, "not enough samples to measure noise: {0}", getLibriVoxFile().getFileName());
        }
    }

    private static double ratio(double db) {
        return Math.pow(10d, db / 20d);
    }

    private static double dB(double ratio) {
        return 20d * Math.log10(ratio);
    }

    @Override
    public String toString() {
        return string("bn-name");
    }

    @Override
    public String getDescription() {
        return string("bn-desc");
    }
}
