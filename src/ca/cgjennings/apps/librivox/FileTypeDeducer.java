package ca.cgjennings.apps.librivox;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Guess the type of an audio file using magic numbers (byte patterns found at
 * the start of the file).
 *
 * @author Christopher G. Jennings https://cgjennings.ca/contact/
 * @since 0.90
 */
class FileTypeDeducer {

    private FileTypeDeducer() {
    }

    /**
     * A magic number is a byte pattern that indicates a particular file type.
     */
    private static class MagicNumber {

        private int[] magic;
        private String id;

        /**
         * Creates a new magic number for some type of file, represented by
         * <code>id</code>, and detected by matching the byte sequence
         * <code>magic</code>. If the integer <code>-1</code> appears in the
         * byte sequence, that position will match any byte. The provided
         * <code>id</code> value is looked up in the application's string
         * resources by prefixing it with <code>"format-" </code>. The value of
         * this key should be a localized description of the file format.
         *
         * @param id the identifier, and resource key fragment, for this fiel
         * type
         * @param magic an array of bytes that must match for this file type
         * @throws NullPointerException if either parameter is <code>null</code>
         */
        public MagicNumber(String id, int... magic) {
            if (id == null) {
                throw new NullPointerException("null id");
            }
            if (magic == null) {
                throw new NullPointerException("null magic");
            }
            this.id = Checker.string("format-" + id);
            this.magic = magic;
        }

        /**
         * Returns <code>true</code> if this magic number matches the start of
         * the byte sequence in <code>buffer</code>.
         *
         * @param buffer the byte sequence to match against this magic number
         * @return <code>true</code> if the byte sequence matches this magic
         * number
         */
        public boolean matches(byte[] buffer) {
            if (buffer.length < magic.length) {
                return false;
            }
            for (int i = 0; i < magic.length && i < buffer.length; ++i) {
                if (magic[i] < 0) {
                    continue;
                }
                if ((((int) buffer[i]) & 0xff) != magic[i]) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns the identifier that this magic number was originally created
         * with.
         *
         * @return an identifier for this magic numner type
         */
        @Override
        public String toString() {
            return id;
        }
    }

    /**
     * An abbreviated way to create a magic number, used to define the
     * <code>types</code> array.
     *
     * @param id the <code>id</code> to pass to the <code>MagicNumber</code>
     * constructor
     * @param magic the <code>magic</code> to pass to the
     * <code>MagicNumber</code> constructor
     * @return a new <code>MagicNumber</code> created as if by calling
     * <code>new MagicNumber( id, magic )</code>
     */
    private static final MagicNumber type(String id, int... magic) {
        return new MagicNumber(id, magic);
    }

    /**
     * An array of predefined <code>MagicNumber</code>s used to deduce file
     * types
     */
    private static MagicNumber[] types = new MagicNumber[]{
        type("au", 0x2E, 0x73, 0x6e, 0x64),
        type("wav", 0x52, 0x49, 0x46, 0x46, -1, -1, -1, -1, 0x57, 0x41, 0x56, 0x45, 0x66, 0x6D, 0x74, 0x20),
        type("ogg", 0x4F, 0x67, 0x67, 0x53, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
        type("aiff", 0x46, 0x4F, 0x52, 0x4D, 0x00),
        type("wma", 0x30, 0x26, 0xB2, 0x75, 0x8E, 0x66, 0xCF, 0x11, 0xA6, 0xD9, 0x00, 0xAA, 0x00, 0x62, 0xCE, 0x6C),
        type("ra", 0x2E, 0x72, 0x61, 0xFD, 0x00),
        type("midi", 0x4D, 0x54, 0x68, 0x64),
        type("flac", 0x66, 0x4C, 0x61, 0x43, 0x00, 0x00, 0x00, 0x22),
        type("cdr", 0x4D, 0x53, 0x5F, 0x56, 0x4F, 0x49, 0x43, 0x45),};

    /**
     * Attempt to deduce the type of a file from its initial bytes. If the type
     * can be deduced, a string is returned that contains the name of the file
     * type. Otherwise, the method returns <code>null</code>.
     *
     * @param f the <code>File</code> to deduce the type of
     * @return a localized string describing the file type, or <code>null</code>
     * if the file type is not recognized
     * @throws java.io.IOException if an exception occurs while reading the
     * magic bytes
     */
    public static String deduceFileType(File f) throws IOException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            return deduceFileType(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * Attempt to deduce the type of an <code>InputStream</code> from its inital
     * bytes. If the type can be deduced, a string is returned that contains the
     * name of the file type. Otherwise, the method returns <code>null</code>.
     *
     * @param in the <code>InputStream</code> to deduce the type of
     * @return a localized string describing the file type, or <code>null</code>
     * if the file type is not recognized
     * @throws java.io.IOException if an exception occurs while reading the
     * magic bytes
     */
    public static String deduceFileType(InputStream in) throws IOException {
        byte[] buffer = createAndFillBuffer(in);
        MagicNumber mn = findMatch(buffer);
        return mn == null ? null : mn.toString();
    }

    /**
     * Creates a buffer of bytes read from <code>in</code> and the length of the
     * lesser of the length of the stream or the longest predefined magic
     * number.
     *
     * @param in the stream to fill the buffer from
     * @throws java.io.IOException if an error occurs while reading from
     * <code>in</code>
     */
    private static byte[] createAndFillBuffer(InputStream in) throws IOException {
        int length = -1;
        for (int i = 0; i < types.length; ++i) {
            if (types[i].magic.length > length) {
                length = types[i].magic.length;
            }
        }
        byte[] buffer = new byte[length];

        int total = 0;
        int read = 0;
        while ((read = in.read(buffer, total, buffer.length - total)) > 0) {
            total += read;
        }
        if (total < buffer.length) {
            // equivalent to
            //     buffer = java.util.Arrays.copyOfRange( buffer, 0, total );
            // but compatible with Java 5
            byte[] bufferCopy = new byte[total];
            for (int i = 0; i < total; ++i) {
                bufferCopy[i] = buffer[i];
            }
            buffer = bufferCopy;
        }
        return buffer;
    }

    /**
     * Attempts to match <code>buffer</code> against the predefined magic
     * numbers in <code>types</code>. Returns the first match, or
     * <code>null</code> if none of the magic numbers match.
     *
     * @param buffer the bytes to test against the magic numbers
     * @return the first matching <code>MagicNumber</code>, or <code>null</code>
     */
    private static MagicNumber findMatch(byte[] buffer) {
        for (int i = 0; i < types.length; ++i) {
            if (types[i].matches(buffer)) {
                return types[i];
            }
        }
        return null;
    }
}
