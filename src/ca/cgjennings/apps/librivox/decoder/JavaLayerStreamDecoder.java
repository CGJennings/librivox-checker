package ca.cgjennings.apps.librivox.decoder;

import ca.cgjennings.apps.librivox.*;
import ca.cgjennings.apps.librivox.validators.Validator.Category;
import ca.cgjennings.apps.librivox.validators.Validator.Validity;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamErrors;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderErrors;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.SampleBuffer;

/**
 * An implementation of {@link StreamDecoder} that uses the JavaLayer library to
 * perform decoding.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
final class JavaLayerStreamDecoder implements StreamDecoder {

    private Bitstream bitstream;
    private Decoder decoder;
    private Header header;
    private AudioFrame frame;
    private Report report;
    private long frameNumber = -1;
    private long validFramesDecoded = 0;
    private int errorCount = 0;
    private float msPerFrame;
    private Header firstHeader; // the JavaLayer header
    private AudioHeader firstAudioHeader; // the implementation-independent header

    private static int MAX_ERRORS;
    private static final int NUMBER_OF_VALID_FRAMES_BEFORE_FILE_ASSUMED_MP3 = 3;

    private JavaLayerStreamDecoder() {
    }

    /**
     * Creates a new MP3 decoder.
     *
     * @param in an input stream of MP3 frames
     * @param report a report that will be used to note any problems when
     * decoding; may be <code>null</code>
     * @param tolerance a hint regarding how many errors are acceptable before
     * giving up
     * @throws java.io.IOException if an I/O exception occurs while creating the
     * decoder
     */
    public JavaLayerStreamDecoder(InputStream in, Report report, ErrorTolerance tolerance) throws IOException {
        this.report = report;

        switch (tolerance) {
            case NONE:
                MAX_ERRORS = 0;
                break;
            case MODERATE:
                MAX_ERRORS = 10;
                break;
            case ALL:
                MAX_ERRORS = Integer.MAX_VALUE;
                break;
            default:
                throw new IllegalArgumentException("unknown tolerance " + tolerance);
        }

        // prefetch the first MP3 header so we know if there is one
        // (so mayHaveMoreFrames returns an accurate result)
        decoder = new Decoder();
        bitstream = new Bitstream(in);

        boolean foundValidHeader = false;
        do {
            try {
                ++frameNumber;
                header = bitstream.readFrame();
                foundValidHeader = true;
            } catch (BitstreamException e) {
                // will throw IOException if
                // too many errors occur
                handleDecodingError(e, true);
                bitstream.closeFrame();
            }
        } while (!foundValidHeader);

        if (header == null) {
            throw new NotAnMP3Exception();
        }

        frame = new AudioFrame();
        msPerFrame = header.ms_per_frame();
        firstHeader = header;
        firstAudioHeader = new JavaLayerAudioHeader(header);
    }

    @Override
    public AudioHeader getAudioHeader() {
        return firstAudioHeader;
    }

    /**
     * Returns the estimated track length, in seconds.
     *
     * @param streamLength the length of the stream
     * @return an estimate of the track length, in seconds
     */
    @Override
    public double estimateTrackLength(int streamLength) {
        return ((double) firstHeader.total_ms(streamLength)) / 1000d;
    }

    @Override
    public int estimateFrameCount(int streamLength) {
        return firstHeader.max_number_of_frames(streamLength);
    }

    /**
     * Return the number of valid frames that have been decoded by this decoder.
     *
     * @return the number of the next frame to be read
     */
    @Override
    public long getValidFramesDecoded() {
        return validFramesDecoded;
    }

    /**
     * Returns <code>true</code> if the stream <i>appears</i> to have more
     * frames of audio. The decoder has not actually decoded the next frame, so
     * it may turn out that the frame is corrupt and there are no more valid
     * frames. The result of {@link #getNameFrame()} should always be checked.
     *
     * @return <code>true</code> if there appear to be more frames in the stream
     */
    @Override
    public boolean mayHaveMoreFrames() {
        return header != null;
    }

    /**
     * Skips by the next frame of audio without decoding it. An
     * {@link AudioFrame} is returned that contains frequency and channel
     * information, but no sample data. (Although the sample buffer will be
     * <code>null</code>, the sample count will indicate the number of samples
     * that would have been present if the audio had been decoded.)
     *
     * @throws java.io.IOException
     */
    @Override
    public AudioFrame skipFrame() throws IOException {
        if (header == null) {
            return null;
        }

        if (getNextFrameImpl(false) == null) {
            return null;
        }

        return frame;
    }

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
    @Override
    public AudioFrame getNextFrame() throws IOException {
        return getNextFrameImpl(true);
    }

    private AudioFrame getNextFrameImpl(boolean decode) throws IOException {
        if (header == null) {
            return null;
        }
        try {
            if (decode) {
                SampleBuffer buff = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                frame.set(getChannelFormatFromHeader(header), buff.getSampleFrequency(), buff.getBuffer(), buff.getBufferLength());
            } else {
                frame.set(getChannelFormatFromHeader(header), header.frequency(), null, 0);
            }
            bitstream.closeFrame();
            ++frameNumber;
            ++validFramesDecoded;
            header = bitstream.readFrame();
        } catch (JavaLayerException e) {
            // Something bad happened while getting this frame
            // we will log it and try to skip ahead to the next frame.
            // If we encounter more than MAX_ERRORS errors, we
            // give up. When this happens, log() throws an
            // IOException, which will escape from this method.
            bitstream.closeFrame();
            handleDecodingError(e, true);

            // try to resychronize on a valid frame
            boolean resynchronized = false;
            while (!resynchronized && header != null) {
                try {
                    ++frameNumber;
                    header = bitstream.readFrame();
                    resynchronized = true;
                } catch (BitstreamException synchException) {
                    handleDecodingError(e, false);
                    bitstream.closeFrame();
                }
            }

            // if we reached the EOF, we have no samples to return
            // if we found a new header, we will try to decode it
            // (this will eventually throw IOException if we keep getting
            // decoder errors)
            if (header == null) {
                if (validFramesDecoded < NUMBER_OF_VALID_FRAMES_BEFORE_FILE_ASSUMED_MP3) {
                    throw new NotAnMP3Exception();
                }
                return null;
            } else {
                return getNextFrameImpl(decode);
            }
        }

        // NOTE: we will never reach here if there was a decoder exception
        return frame;
    }

    @SuppressWarnings("fallthrough")
    private void handleDecodingError(JavaLayerException e, boolean logInReport) throws IOException {
        // COMPOSE the error message
        boolean isIOError = false;
        String key;
        Throwable cause = e.getCause();

        int code = Integer.MAX_VALUE;
        if (e instanceof DecoderException) {
            code = ((DecoderException) e).getErrorCode();
        } else if (e instanceof BitstreamException) {
            code = ((BitstreamException) e).getErrorCode();
        }

        switch (code) {
            case BitstreamErrors.INVALIDFRAME:
                key = "invalid-frame";
                break;
            case BitstreamErrors.STREAM_ERROR:
                key = "stream";
                isIOError = true;
                break;
            case BitstreamErrors.STREAM_EOF:
                isIOError = true; // fallthrough
            case BitstreamErrors.UNEXPECTED_EOF:
                key = "eof";
                break;
            case BitstreamErrors.UNKNOWN_SAMPLE_RATE:
                key = "sample-rate";
                break;
            case DecoderErrors.ILLEGAL_SUBBAND_ALLOCATION:
                key = "allocation";
                break;
            case DecoderErrors.UNSUPPORTED_LAYER:
                key = "layer";
                break;
            case DecoderErrors.UNKNOWN_ERROR:
            case BitstreamErrors.UNKNOWN_ERROR:
            default:
                key = "unknown";
                break;
        }

        String message = Checker.string("error-decoder", frameNumber)
                + "\n" + Checker.string("error-decoder-" + key);
        if (cause != null && cause instanceof IOException) {
            String causeMessage = cause.getLocalizedMessage();
            if (causeMessage != null && causeMessage.length() > 0) {
                message += "\n" + causeMessage;
            }
            isIOError = true;
        }

        ++errorCount;
        if (logInReport && (report != null)) {
            report.addValidation(Category.ERROR, null, Validity.WARN, message.replace("\n", "<br>"), null);
        }

        Checker.getLogger().log(Level.WARNING, "Error while decoding frame", e);

        if (isIOError) {
            throw new IOException(message);
        }
        if (errorCount > MAX_ERRORS) {
            if (validFramesDecoded < NUMBER_OF_VALID_FRAMES_BEFORE_FILE_ASSUMED_MP3) {
                throw new NotAnMP3Exception();
            }
            throw new DecodingException(Checker.string("error-decoder-too-many"));
        }
    }

    /**
     * A utility method that returns the implementation-indepedent channel
     * format value for a given frame header.
     *
     * @param h the frame header structure
     * @return the matching channel format
     * @throws AssertionError if the format is not a valid value; this should
     * never happen in practice as decoding the frame should raise a decoding
     * exception
     */
    public static ChannelFormat getChannelFormatFromHeader(Header h) {
        switch (h.mode()) {
            case Header.SINGLE_CHANNEL:
                return ChannelFormat.MONO;
            case Header.STEREO:
                return ChannelFormat.STEREO;
            case Header.JOINT_STEREO:
                return ChannelFormat.JOINT_STEREO;
            case Header.DUAL_CHANNEL:
                return ChannelFormat.DUAL_CHANNEL;
            default:
                throw new AssertionError();
        }
    }
}
