package ca.cgjennings.apps.librivox.metadata;

import ca.cgjennings.apps.librivox.Checker;
import ca.cgjennings.apps.librivox.LibriVoxAudioFile;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.HashMap;
import java.util.logging.Level;
import javax.swing.JFrame;

/**
 * This is a skeletal implementation of a class that could potentially be used
 * to editable sections to reports. Currently, it is only used to display the
 * large version of the cover art thumbnail when the user clicks this in the
 * information report.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 * @since 0.91
 */
public class MetadataEditorLinkFactory {

    private MetadataEditorLinkFactory() {
    }

    public static void setEditorParentFrame(JFrame app) {
        if (app == null) {
            throw new NullPointerException("app");
        }
        if (parent != null) {
            throw new IllegalStateException("parent already set");
        }
        parent = app;
    }
    private static JFrame parent;

    public static void showEditor(String linkText, Rectangle rect) {
        try {
            DecodedLink dl = decodeLink(linkText);
            if (dl.view != null) {
                switch (dl.type) {
                    case ART:
                        ArtViewer v = new ArtViewer(parent, dl.view.getImage());
                        center(v, rect);
                        v.setVisible(true);
                        return;
                }
            }
        } catch (Exception e) {
            Checker.getLogger().log(Level.SEVERE, null, e);
        }
        Checker.getLogger().log(Level.WARNING, "bad link: {0}", linkText);
    }

    private static void center(Window w, Rectangle r) {
        if (r != null) {
            // If we put the window over the center of the app,
            // will the window's rect overlap the "target" rect?
            // If so, do it. Otherwise, center the window over
            // the target rect.
//			int wx = parent.getX() + (parent.getWidth() - w.getWidth())/2;
//			int wy = parent.getY() + (parent.getHeight() - w.getHeight())/2;
//			Rectangle overlapCheck = new Rectangle( wx, wy, w.getWidth(), w.getHeight() );
//			if( overlapCheck.intersects( r ) ) {
//				w.setLocation( wx, wy );
//				return;
//			}

            int cx = r.x + r.width / 2;
            int cy = r.y + r.height / 2;
            cx -= w.getWidth() / 2;
            cy -= w.getHeight() / 2;
            w.setLocation(cx, cy);
        } else {
            w.setLocationRelativeTo(parent);
        }
    }

    public static boolean isLink(String linkText) {
        return linkText.startsWith("!");
    }

    public static String getLink(LibriVoxAudioFile file, LinkType type, Container c) {
        return getLinkID(file) + "!" + type + "!" + c;
    }

    public static synchronized String getLinkID(LibriVoxAudioFile file) {
        String link = idMap.get(file);
        if (link == null) {
            link = "!" + (id++);
            idMap.put(file, link);
            linkMap.put(link, file);
        }
        return link;
    }

    public static synchronized void unlink(LibriVoxAudioFile file) {
        String linkText = idMap.remove(file);
        if (linkText != null) {
            linkMap.remove(linkText);
        }
    }

    private static HashMap<LibriVoxAudioFile, String> idMap = new HashMap<LibriVoxAudioFile, String>();
    private static HashMap<String, LibriVoxAudioFile> linkMap = new HashMap<String, LibriVoxAudioFile>();
    private static long id = 0;

    public enum LinkType {
        ART
    }

    public enum Container {
        ID3V1, ID3V2
    }

    static DecodedLink decodeLink(String text) {
        if (!isLink(text)) {
            throw new IllegalArgumentException("not a metadata link");
        }
        String[] tokens = text.substring(1).split("\\!");
        DecodedLink dl = new DecodedLink();
        dl.file = linkMap.get("!" + tokens[0]);
        if (dl.file == null) {
            throw new IllegalStateException("file not in map");
        }
        dl.type = LinkType.valueOf(tokens[1]);
        dl.ver = Container.valueOf(tokens[2]);
        dl.md = dl.file.getMetadata();
        if (dl.ver == Container.ID3V1) {
            dl.view = dl.md.getID3v1Metadata();
        } else if (dl.ver == Container.ID3V2) {
            dl.view = dl.md.getID3v2Metadata();
        }
        return dl;
    }

    static class DecodedLink {

        public LibriVoxAudioFile file;
        public LinkType type;
        public Container ver;
        public MP3FileMetadata md;
        public MetadataView view;
    }
}
