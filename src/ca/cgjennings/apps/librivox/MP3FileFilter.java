package ca.cgjennings.apps.librivox;

import java.io.File;
import java.util.Locale;

/**
 * A filter that can be used to exclude all but MP3 files, ZIP files, and
 * directories. It can be used as either a
 * <code>javax.swing.filechooser.JFileChooser</code> filter, or a
 * <code>java.io.FileFilter</code>.
 *
 * @author Christopher G. Jennings https://cgjennings.ca/contact/
 */
public class MP3FileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }
        String ext = f.getName();
        if (ext.length() >= 4) {
            int len = ext.length();
            ext = ext.substring(len - 4, len).toLowerCase(Locale.US);
        } else {
            return false;
        }
        return ext.equals(".mp3") || ext.equals(".zip");
    }

    @Override
    public String getDescription() {
        return Checker.string("file-type-mp3");
    }
}
