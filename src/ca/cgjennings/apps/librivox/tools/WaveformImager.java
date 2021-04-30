package ca.cgjennings.apps.librivox.tools;

import ca.cgjennings.apps.librivox.validators.*;
import ca.cgjennings.apps.librivox.decoder.AudioFrame;
import ca.cgjennings.apps.librivox.decoder.AudioHeader;
import ca.cgjennings.apps.librivox.decoder.StreamDecoder;
import ca.cgjennings.apps.librivox.validators.Validator.Category;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.JComponent;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;

/**
 * A "validator" that creates an oscilloscope image of the file's waveform.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
class WaveformImager extends AbstractValidator {

    @Override
    public Category getCategory() {
        return Category.AUDIO;
    }

    public enum OverdrawMethod {
        NONE,
        ANTIALIAS
    }

    private OverdrawMethod overdraw = OverdrawMethod.ANTIALIAS;

    //
    // TODO: clean this up generally, and possibly move to non-validator class
    //       could convert to use a stream of samples instead of a frame,
    //       then increase the width for drawing samples
    //
    @Override
    public boolean isAudioProcessor() {
        return true;
    }

    private Graphics2D initGraphics(int channel) {
        Graphics2D g = image.createGraphics();

        if (overdraw == OverdrawMethod.ANTIALIAS) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        if (channel >= 0) {
            double hscale = width / samples;
            double waveheight = 65535;
            double vscale = height / (waveheight * channelCount);

            double ty = height / 2d;
            if (channelCount == 2) {
                if (channel == 0) {
                    ty /= 2d;
                } else {
                    ty += ty / 2d;
                }
            }
            g.translate(0d, ty);
            g.scale(hscale, vscale);

            g.setPaint(new Color(0x44ffff));
        }
        return g;
    }

    private void drawScales() {
        Graphics2D g = image.createGraphics();
        try {
            int w = (int) samples;

            Stroke solid = g.getStroke();
            BasicStroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[]{4, 4}, 0);

            g.setPaint(Color.WHITE);
            int total = 4 * channelCount;
            for (int i = 1; i < total; ++i) {
                g.setStroke((i & 1) == 1 ? dashed : solid);
                int y = i * (height - 1) / total;
                if (i == 4) {
                    g.setPaint(Color.PINK);
                }
                g.drawLine(0, y, width, y);
                if (i == 4) {
                    g.setPaint(Color.WHITE);
                }
            }
        } finally {
            g.dispose();
        }
    }

    @Override
    public void beginAnalysis(AudioHeader header, Validator[] predecessors) {
        File src = getLibriVoxFile().getLocalFile();
        sampleRate = header.getFrequency();

        try {
            MP3AudioHeader metadataHeader = new MP3AudioHeader(src);
            metadataHeader.getPreciseTrackLength();
            samples = metadataHeader.getPreciseTrackLength() * sampleRate;
        } catch (Exception e) {
            // fallback on raw header, which will not account for metadata
            long len = src.length();
            if (len < 0L) {
                len = 0L;
            }
            if (len > Integer.MAX_VALUE) {
                len = Integer.MAX_VALUE;
            }
            samples = decoder.estimateTrackLength((int) len) * sampleRate;
        }

        if (header.getChannelFormat().getChannelCount() > 1) {
            height *= 2;
        }

        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        n = 0;

        lastUpdate = 0;
        updateRate = 2 * (int) samples / width; // the multiplier means more decoding, less window repaints

        // there is less than 1 pixel per MPEG frame; we can speed up drawing by
        // only drawing one line per block
        useFastLines = width * 1152 < (int) samples;
    }

    @Override
    public void analyzeFrame(AudioFrame frame) {
        int channelCount = frame.getChannelCount();
        short[] buff = frame.getSamples();

        if (g == null) {
            // the assumed count for the whole thing is based on the count for frame 0
            this.channelCount = frame.getChannelCount() > 1 ? 2 : 1;

            // create transformed graphics devices for each channel
            g = new Graphics2D[2];
            g[0] = initGraphics(0);
            g[1] = initGraphics(1);

            // this is a per-channel memory of the last sample from the previous frame
            lastAmplitude[0] = frame.getSamples()[0];
            lastAmplitude[1] = channelCount > 1 ? buff[1] : 0;

            drawScales();
            updateViewer();
        }

        int len = frame.getSampleCount();

        for (int channel = 0; channel < channelCount; ++channel) {
            int off = channel;
            int p = n;
            Graphics2D channelGraphics = g[channel];

            if (useFastLines) {
                int p0 = p;
                int min = Short.MAX_VALUE, max = Short.MIN_VALUE;
                for (int s = 0; s < len; ++s, off += channelCount, ++p) {
                    int amplitude = buff[off];
                    if (amplitude < min) {
                        min = amplitude;
                    } else if (amplitude > max) {
                        max = amplitude;
                    }
                }
                min = -min;
                max = -max;
                channelGraphics.fillRect(p0, max, p - p0, min - max);
                if (overdraw == OverdrawMethod.ANTIALIAS) {
                    // AA tends to end up very dark, painting again will brighten it
                    channelGraphics.fillRect(p0, max, p - p0, min - max);
                }
            } else {
                int lastAmplitudeForChannel = lastAmplitude[channel];
                for (int s = 0; s < len; ++s, off += channelCount, ++p) {
                    int amplitude = -((int) buff[off]);
                    channelGraphics.drawLine(p - 1, lastAmplitudeForChannel, p, amplitude);
                    lastAmplitudeForChannel = amplitude;
                }
                lastAmplitude[channel] = lastAmplitudeForChannel;
            }

        }
        n += len;

        if (n > (lastUpdate + updateRate)) {
            drawScales();
            updateViewer();
            lastUpdate = n;
        }
    }

    @Override
    public void endAnalysis() {
        drawScales();
        for (int i = 0; i < g.length; ++i) {
            if (g[i] != null) {
                g[i].dispose();
            }
        }
        updateViewer();
    }

    public JComponent getViewer() {
        if (viewer == null) {
            viewer = new Viewer();
        }
        return viewer;
    }

    private void updateViewer() {
        if (viewer != null) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    viewer.repaint();
                }
            });
        }
    }

    private BufferedImage image;
    private Graphics2D[] g;
    private int width = 620, height = 196;
    private int channelCount;
    private double samples, sampleRate;
    private int n;
    private int[] lastAmplitude = new int[MAX_CHANNEL_COUNT];
    private int lastUpdate = 0;
    private int updateRate;
    private boolean useFastLines = false;

    private volatile Viewer viewer;

    class Viewer extends JComponent {

        public Viewer() {
            setOpaque(false);
            setPreferredSize(new Dimension(width, height));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    lastMouseX = e.getX();
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    lastMouseX = -1;
                    repaint();
                }
            });

            addMouseMotionListener(new MouseMotionListener() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    lastMouseX = e.getX();
                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    lastMouseX = e.getX();
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g1) {
            int w = getWidth();
            int h = getHeight();
            int x = 0;
            if (w != width) {
                x = (w - width) / 2;
            }
            int y = 0;
            if (h != height) {
                y = (h - height) / 2;
            }
            g1.drawImage(image, x, y, null);

            g1.setColor(Color.WHITE);
            g1.setFont(scaleFont);
            label(g1, "+1", 0, -1);
            label(g1, "-1", height, 1);
            if (channelCount > 1) {
                label(g1, "-1", height / 2, 1);
                label(g1, "+1", height / 2, -1);
            }

            if (lastMouseX >= 0) {
                setFont(timeFont);
                Graphics2D g = (Graphics2D) g1;
                g.setPaint(Color.ORANGE);
                g.drawLine(lastMouseX, 0, lastMouseX, getHeight());
                g.setFont(getFont());
                double time = (((double) lastMouseX / (double) width) * samples) / sampleRate;
                int mins = ((int) time) / 60;
                time -= mins * 60d;
                String text = String.format("%d:%02.2f", mins, time);
                FontMetrics fm = g.getFontMetrics();

                final int XINSET = 4, YINSET = 3, BOXGAP = 2;

                int stringWidth = fm.stringWidth(text);
                int tx = width - stringWidth - XINSET;
                int ty = height - fm.getDescent() - YINSET;
                int bw = stringWidth + BOXGAP * 2;
                int bh = fm.getAscent() + fm.getDescent() + BOXGAP * 2;

                g.setPaint(Color.BLACK);
                Composite c = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                g.fillRect(tx - BOXGAP, ty - fm.getAscent() - BOXGAP, bw, bh);
                g.setComposite(c);

                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(Color.WHITE);
                g.drawString(text, tx, ty);
            }
        }

        private void label(Graphics g, String s, int y, int bias) {
            FontMetrics fm = g.getFontMetrics();
            y += fm.getAscent();
            int textHeight = fm.getAscent() + fm.getDescent();
            if (bias == 0) {
                y -= textHeight / 2;
            } else if (bias > 0) {
                y -= textHeight + 2;
            } else {
                y += 2;
            }
            g.drawString(s, 2 + fm.stringWidth("+1") - fm.stringWidth(s), y);
        }

        private int lastMouseX = -1;

        // Font.SANS_SERIF requires Java 6
        private Font scaleFont = new Font("SansSerif", Font.PLAIN, 9)
                .deriveFont(AffineTransform.getScaleInstance(1.5d, 1d));
        private Font timeFont = new Font("SansSerif", Font.PLAIN, 14)
                .deriveFont(AffineTransform.getScaleInstance(1.5d, 1d));
    }

    private static final int MAX_CHANNEL_COUNT = 2;

    void setDecoder(StreamDecoder decoder) {
        this.decoder = decoder;
    }

    private StreamDecoder decoder;
}
