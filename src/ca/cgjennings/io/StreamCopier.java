package ca.cgjennings.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A utility class for copying data between streams with a minimum of fuss.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
public class StreamCopier {

    private StreamCopier() {
    }

    public static long copyFile(File from, File to) throws IOException {
        return copyFile(from, to, null);
    }

    public static long copyFile(File from, File to, byte[] buffer) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        IOException ioe = null;
        long copied = 0L;
        try {
            in = new FileInputStream(from);
            out = new FileOutputStream(to);
            copied = copyStream(in, out, buffer);
        } catch (IOException e) {
            ioe = e;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    if (ioe == null) {
                        ioe = e;
                    }
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    if (ioe == null) {
                        ioe = e;
                    }
                }
            }
            if (ioe != null) {
                throw ioe;
            }
        }
        return copied;
    }

    public static long copyStream(InputStream from, OutputStream to) throws IOException {
        return copyStream(from, to, null);
    }

    public static long copyStream(InputStream from, OutputStream to, byte[] buffer) throws IOException {
        if (buffer == null || buffer.length == 0) {
            buffer = new byte[DEFAULT_BUFFER_SIZE];
        }

        long totalCopied = 0;
        int read;
        while ((read = from.read(buffer)) >= 0) {
            to.write(buffer, 0, read);
            totalCopied += read;
        }
        return totalCopied;
    }

    private static final int DEFAULT_BUFFER_SIZE = 64 * 1024;
}
