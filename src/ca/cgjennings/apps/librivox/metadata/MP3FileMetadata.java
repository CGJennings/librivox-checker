package ca.cgjennings.apps.librivox.metadata;

import ca.cgjennings.apps.librivox.ImageUtils;
import ca.cgjennings.apps.librivox.Checker;
import ca.cgjennings.apps.librivox.decoder.DecoderFactory;
import ca.cgjennings.apps.librivox.decoder.NotAnMP3Exception;
import ca.cgjennings.apps.librivox.decoder.StreamDecoder;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.datatype.DataTypes;
import org.jaudiotagger.tag.id3.AbstractID3Tag;
import org.jaudiotagger.tag.id3.ID3v11Tag;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.id3.ID3v24Frame;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.id3.framebody.FrameBodyAPIC;
import org.jaudiotagger.tag.id3.valuepair.GenreTypes;

/**
 * A class that encapsulates basic metadata about an MP3 file. Most of the data
 * is obtained from ID3 tags, where available.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 * @since 3.0
 */
public class MP3FileMetadata {

    /**
     * Creates a new instance based on the metadata stored in the given file.
     * The file must be writable, or an I/O exception will be thrown.
     *
     * @param file the file to obtain metadata for
     * @throws NotAnMP3Exception if the file does not appear to be an MP3 file
     * @throws IOException if an I/O error occurs while parsing the file
     */
    public MP3FileMetadata(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("file");
        }
        f = file;
        update();
    }

    /**
     * Returns the estimated track length, in seconds.
     *
     * @param streamLength the length of the stream
     * @return an estimate of the track length, in seconds
     */
    public double getTrackLength() {
        return trackLen;
    }

    /**
     * Returns the estimated number of audio frames in the file.
     *
     * @return an estimate of the number of audio frames
     */
    public int getFrameCount() {
        return frames;
    }

    /**
     * Returns the encoding software, if known.
     *
     * @return the encoder description, or <code>null</code>
     */
    public String getEncoder() {
        return encoder;
    }

    /**
     * Returns the offset to the start of audio data within the file. If there
     * is metadata at the start of the file, this will return how many bytes to
     * skip before the audio begins.
     *
     * @return offset from the start of the file to the start of audio data
     */
    public int getStartOfAudio() {
        return startOfAudio;
    }

    /**
     * Returns a view of the ID3 tag metadata. If version 2 is available, that
     * will be returned. Otherwise, if version 1 is available, that will be
     * returned. Otherwise, returns <code>null</code>.
     *
     * @return Returns the best available ID3 metadata, or <code>null</code>
     */
    public MetadataView getID3Metadata() {
        return v2 == null ? v1 : v2;
    }

    /**
     * Returns a view of the ID3 version 1 metadata in the file, or
     * <code>null</code> if it does not exist.
     *
     * @return a view of the ID3 v1 metadata
     */
    public MetadataView getID3v1Metadata() {
        return v1;
    }

    /**
     * Returns a view of the ID3 version 2 metadata in the file, or
     * <code>null</code> if it does not exist.
     *
     * @return a view of the ID3 v2 metadata
     */
    public MetadataView getID3v2Metadata() {
        return v2;
    }

    private File f;
    private double trackLen;
    private int frames;
    private String encoder;
    private int startOfAudio;

    private ViewImpl v1, v2;
    private MP3File mp3f;

    /**
     * Call this method to re-read metadata from the source file after changing
     * its content.
     *
     * @throws IOException if an I/O error occurs
     */
    public void update() throws IOException {
        mp3f = openMP3File(f);

        if (mp3f == null) {
            // assume no valid metadata available
            fillInFallback(f);
        } else {
            // gather metadata
            fillIn(mp3f);
        }
    }

    /**
     * Create an MP3File for reading metadata from a file, or return
     * <code>null</code>
     *
     * @param f
     * @return
     * @throws IOException
     */
    private MP3File openMP3File(File f) throws IOException {
        // the file must be read/write for the tagging library to use it
        // if it is not read/write we try to make it read/write here
        // if that fails, an exception will be thrown later by the tag library
        if (!f.canWrite()) {
            f.setWritable(true);
        }

        MP3File mp3f = null;
        try {
            mp3f = new MP3File(f, MP3File.LOAD_ALL);
        } catch (TagException e) {
            // this is not necessarily an error
            Checker.getLogger().log(Level.WARNING, "TagException while reading metadata", e);
        } catch (ReadOnlyFileException e) {
            throw new IOException(Checker.string("error-io-readonly"));
        } catch (InvalidAudioFrameException e) {
            throw new NotAnMP3Exception();
        }
        return mp3f;
    }

    private void fillInFallback(File f) throws IOException {
        v1 = null;
        v2 = null;
        encoder = null;
        startOfAudio = 0;

        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(f));
            StreamDecoder decoder = DecoderFactory.createDecoder(in);

            long len = f.length();
            if (len < 0L) {
                len = 0L;
            }
            if (len > Integer.MAX_VALUE) {
                len = Integer.MAX_VALUE;
            }

            trackLen = decoder.estimateTrackLength((int) len);
            frames = decoder.estimateFrameCount((int) len);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private void fillIn(MP3File mp3f) {
        v1 = null;
        v2 = null;

        MP3AudioHeader h = mp3f.getMP3AudioHeader();
        startOfAudio = (int) h.getMp3StartByte();
        if (startOfAudio < 0) {
            startOfAudio = 0;
        }
        trackLen = h.getPreciseTrackLength();
        long fcount = h.getNumberOfFramesEstimate();
        if (fcount > Integer.MAX_VALUE) {
            fcount = Integer.MAX_VALUE;
        }
        frames = (int) fcount;
        encoder = h.getEncoder();
        if (encoder.length() == 0) {
            encoder = null;
        }

        if (mp3f.hasID3v2Tag()) {
            v2 = new ViewImpl();
            processID3v2(mp3f.getID3v2Tag().getIdentifier(), mp3f.getID3v2TagAsv24());
        }
        if (mp3f.hasID3v1Tag()) {
            v1 = new ViewImpl();
            processID3v1(mp3f.getID3v1Tag());
        }
    }

    private static String cleanFormatInfo(String id3vX) {
        if (id3vX == null) {
            return null;
        }
        if (id3vX.startsWith("ID3v")) {
            id3vX = "ID3 v" + id3vX.substring("ID3v".length());
        }
        return id3vX;
    }

    private static String convertGenre(AbstractID3Tag id3, String stringValue) {
        if (stringValue == null) {
            return null;
        }
        if (stringValue.matches("\\(\\d+\\)")) {
            try {
                int genreCode = Integer.parseInt(stringValue.substring(1, stringValue.length() - 1));
                stringValue = GenreTypes.getInstanceOf().getValueForId(genreCode);
            } catch (NumberFormatException e) {
                Checker.getLogger().log(Level.SEVERE, null, e);
            }
        }
        return stringValue;
    }

    private void processID3v2(String version, ID3v24Tag id3) {
        v2.format = cleanFormatInfo(version);
        v2.title = fetch(id3, TITLE);
        v2.artist = fetch(id3, ARTIST);
        v2.album = fetch(id3, ALBUM);
        v2.track = fetch(id3, TRACK);
        v2.year = fetch(id3, YEAR);
        v2.genre = convertGenre(id3, fetch(id3, GENRE));
        v2.comment = fetch(id3, COMMENT);

        // try to extract some cover art
        v2.imageURL = null;
        v2.image = null;
        List<TagField> fields = id3.get(ID3v24Frames.FRAME_ID_ATTACHED_PICTURE);
        if (!fields.isEmpty()) try {
            for (TagField t : fields) {
                ID3v24Frame frame = (ID3v24Frame) t;
                FrameBodyAPIC body = (FrameBodyAPIC) frame.getBody();
                Object o = body.getObjectValue(DataTypes.OBJ_PICTURE_DATA);
                if (o != null && (o instanceof byte[])) {
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream((byte[]) o));
                    image = ImageUtils.ensureRGB(image);
                    if (image == null) {
                        continue;
                    }
                    // create a local URL for the image that can be inserted into a report
                    v2.image = ImageUtils.fitImage(image, 300);
                    image = ImageUtils.fitImage(image, 96);
                    v2.imageURL = ImageUtils.getTemporaryURL(image);
                    break;
                }
            }
        } catch (Exception e) {
            // skip the cover art
        }
    }

    private void processID3v1(ID3v1Tag id3) {
        v1.format = cleanFormatInfo(id3.getIdentifier());
        v1.title = fetch(id3, TITLE);
        v1.artist = fetch(id3, ARTIST);
        v1.album = fetch(id3, ALBUM);
        v1.track = null;
        if (id3 instanceof ID3v11Tag) {
            v1.track = fetch(id3, TRACK);
        }
        v1.year = fetch(id3, YEAR);
        v1.genre = fetch(id3, GENRE);
        v1.comment = fetch(id3, COMMENT);
        v1.imageURL = null;
        v1.image = null;
    }

    private static final String TITLE = "getFirstTitle";
    private static final String ARTIST = "getFirstArtist";
    private static final String ALBUM = "getFirstAlbum";
    private static final String TRACK = "getFirstTrack";
    private static final String YEAR = "getFirstYear";
    private static final String GENRE = "getFirstGenre";
    private static final String COMMENT = "getFirstComment";

    /**
     * The tag processor tends to barf when a tag is absent. Rather than
     * surround every attempt to get a tag with a try/catch, we use this generic
     * interface.
     *
     * @param tag the metadata container
     * @param method the name of the method to call for the desired type of
     * metadata
     * @return the value for the requested metadata, or an empty string
     */
    private String fetch(Tag tag, String method) {
        try {
            Method m = tag.getClass().getMethod(method);
            String value = (String) m.invoke(tag);
            return value;
        } catch (NoSuchMethodError nsm) {
            throw new AssertionError("no such method:" + method);
        } catch (Exception e) {
            return null;
        }
    }
}
