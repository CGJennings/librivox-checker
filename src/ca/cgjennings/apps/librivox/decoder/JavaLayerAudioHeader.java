package ca.cgjennings.apps.librivox.decoder;

import javazoom.jl.decoder.Header;

/**
 * An implementation of {@link AudioHeader} for the JavaLayer library.
 *
 * @author Christopher G. Jennings https://cgjennings.ca/contact/
 * @since 0.91
 */
final class JavaLayerAudioHeader implements AudioHeader {

    /**
     * Creates an {@link AudioHeader} instance from the header of a JavaLayer
     * stream.
     *
     * @param h the header to adapt
     */
    JavaLayerAudioHeader(Header h) {
        switch (h.version()) {
            case Header.MPEG1:
                version = MPEGVersion.MPEG1;
                break;
            case Header.MPEG2_LSF:
                version = MPEGVersion.MPEG2;
                break;
            case Header.MPEG25_LSF:
                version = MPEGVersion.MPEG2_5;
                break;
            default:
                throw new AssertionError();
        }

        switch (h.layer()) {
            case 1:
                layerType = LayerType.LAYERI;
                break;
            case 2:
                layerType = LayerType.LAYERII;
                break;
            case 3:
                layerType = LayerType.LAYERIII;
                break;
            default:
                throw new AssertionError();
        }

        chFormat = JavaLayerStreamDecoder.getChannelFormatFromHeader(h);
        frequency = h.frequency();
        vbr = h.vbr();
        bitRate = h.bitrate() / 1000;
        copyright = h.copyright();
        original = h.original();
    }

    @Override
    public LayerType getLayerType() {
        return layerType;
    }

    @Override
    public MPEGVersion getMPEGVersion() {
        return version;
    }

    @Override
    public ChannelFormat getChannelFormat() {
        return chFormat;
    }

    @Override
    public int getBitRate() {
        return bitRate;
    }

    @Override
    public int getFrequency() {
        return frequency;
    }

    public int getChannelCount() {
        return chFormat == ChannelFormat.MONO ? 1 : 2;
    }

    @Override
    public boolean isVariableBitRate() {
        return vbr;
    }

    @Override
    public boolean isCopyrighted() {
        return copyright;
    }

    @Override
    public boolean isOriginal() {
        return original;
    }

    private MPEGVersion version;
    private LayerType layerType;
    private ChannelFormat chFormat;
    private int frequency;
    private int bitRate;
    private boolean vbr, copyright, original;
}
