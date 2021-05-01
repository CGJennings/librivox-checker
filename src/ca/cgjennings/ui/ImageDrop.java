package ca.cgjennings.ui;

import ca.cgjennings.apps.librivox.Checker;
import ca.cgjennings.apps.librivox.ImageUtils;
import ca.cgjennings.ui.dnd.FileDrop;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.UIManager;

/**
 * A control that image files can be dragged and dropped onto.
 *
 * @author Christopher G. Jennings https://cgjennings.ca/contact/
 * @since 0.92
 */
public class ImageDrop extends JLabel {

    public ImageDrop() {
        setBackground(Color.LIGHT_GRAY);
        setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        setOpaque(true);
        new FileDrop(this, BorderFactory.createLineBorder(Color.BLUE, 1), new FileDrop.Listener() {
            @Override
            public void filesDropped(File[] files) {
                if (files.length > 0 && isEnabled()) {
                    for (int i = 0; i < files.length; ++i) {
                        if (setFile(files[i])) {
                            receivedFile = true;
                            break;
                        }
                    }
                }
            }
        });

        Dimension sz = new Dimension(130, 130);
        setMinimumSize(sz);
        setMaximumSize(sz);
        setPreferredSize(sz);
    }

    private File file;
    private BufferedImage image;
    private BufferedImage thumbnail;
    private boolean receivedFile;

    public boolean setFile(File f) {
        if (f == null) {
            file = null;
            setImage(null);
            return true;
        } else {
            try {
                BufferedImage bi = ImageIO.read(f);
                if (bi != null) {
                    file = f;
                    setImage(bi);
                    return true;
                }
            } catch (IOException e) {
                Checker.getLogger().log(Level.SEVERE, null, e);
                UIManager.getLookAndFeel().provideErrorFeedback(this);
            }
        }
        return false;
    }

    public File getFile() {
        return file;
    }

    public void setImage(BufferedImage bi) {
        if (bi == null) {
            image = null;
            thumbnail = null;
            setIcon(null);
        } else {
            bi = ImageUtils.ensureRGB(bi);
            image = bi;
            thumbnail = ImageUtils.fitImage(bi, 128);
            setIcon(new ImageIcon(thumbnail));
        }
    }

    public BufferedImage getImage() {
        return image;
    }

    /**
     * Returns <code>true</code> if the user has set a file on the control.
     * (Excludes files that have been set programmatically.)
     *
     * @return <code>true</code> if the user has dropped an image file
     */
    public boolean hasReceivedFile() {
        return receivedFile;
    }
}
