package ca.cgjennings.apps.librivox.metadata;

import ca.cgjennings.apps.librivox.Checker;
import java.util.logging.Level;

/**
 * Interprets a LibriVox file name as a metadata container.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
public class LibriVoxFileNameMetadata {

    private String title;
    private String lastName;
    private String extension;
    private String initials;
    private String bitrate;
    private int section;
    private String sectionAsString;
    private boolean emptyFieldWarning;

    public LibriVoxFileNameMetadata(String name) {
        try {
            parseFrom(name);
        } catch (Exception e) {
            // prevent validators from being killed by bugs in the parser
            Checker.getLogger().log(Level.WARNING, "failed to parse '" + name + "'", e);
        }
    }

    protected void parseFrom(String name) {
        // mark all fields invalid initially
        title = null;
        lastName = null;
        extension = null;
        initials = null;
        bitrate = null;
        section = -1;
        emptyFieldWarning = false;

        // use indexOf to catch problems such as .mp3.mp3
//		int dot = name.lastIndexOf( '.' );
        int dot = name.indexOf('.');
        if (dot >= 0) {
            extension = name.substring(dot + 1);
            name = name.substring(0, dot);
        }

        // Derived files typically include a bitrate in the form of XXkb.
        // If this exists we will strip it off before starting.
        // If later on we find that the last name is empty, but there are initials,
        // we will instead assume that the initials are actually a short name.
        if (name.endsWith("kb") && name.length() >= 3 && Character.isDigit(name.charAt(name.length() - 3))) {
            int pos = findPreviousField(name, name.length());
            bitrate = name.substring(pos);
            while (bitrate.indexOf('_') == 0) {
                bitrate = bitrate.substring(1);
            }
            name = name.substring(0, pos);
        }

        // an author's name shouldn't contain a number:
        // scan backwards until we find an '_' that is preceeded by a digit
        int pos = findPreviousField(name, name.length());
        while (pos > 0 && section < 0) {
            char digit = name.charAt(pos - 1);
            if (digit >= '0' && digit <= '9') {
                if (pos < name.length() - 1) {
                    lastName = name.substring(pos + 1);
                }
                int start = findPreviousField(name, pos) + 1;
                sectionAsString = name.substring(start < 0 ? 0 : start, pos);
                // if field has multiple '_'s, the sect string starts with them;
                // this will already be flagged as an empty field, so try to
                // parse the number without them
                int underscore = sectionAsString.lastIndexOf('_');
                if (underscore >= 0) {
                    sectionAsString = sectionAsString.substring(underscore + 1);
                }

                try {
                    section = Integer.parseInt(sectionAsString);
                } catch (NumberFormatException e) {
                    // section will still be < 0
                }
                pos = start - 1;
                break;
            }
            pos = findPreviousField(name, pos);
        }

        if (pos >= 0) {
            title = name.substring(0, pos);
        } else {
            //
            // Couldn't find a section number in the name; use the last
            // segment of the name as the author's last name and use the
            // rest as the book name.
            //
            // If there are at least 3 segments and the last segment is
            // 3 characters or less, assume that the third segment
            // represents reader initials, the second is the last name
            //
            pos = findPreviousField(name, name.length());
            if (pos >= 0) {
                lastName = name.substring(pos + 1);
                title = name.substring(0, pos);

                if (lastName.length() > 0 && lastName.length() <= 3) {
                    name = title;
                    pos = findPreviousField(name, name.length());
                    if (pos >= 0) {
                        initials = lastName;
                        lastName = name.substring(pos + 1);
                        title = name.substring(0, pos);
                    }
                }

            } else {
                title = name;
            }
        }
    }

    private int findPreviousField(String name, int pos) {
        while (--pos >= 0) {
            if (name.charAt(pos) == '_') {
                if (pos > 0 && name.charAt(pos - 1) == '_') {
                    emptyFieldWarning = true;
                    while (pos > 0 && name.charAt(pos - 1) == '_') {
                        --pos;
                    }
                }
                return pos;
            }
        }
        return -1;
    }

    public static String prettifyField(String field) {
        if (field == null) {
            return "<null>";
        }
        boolean capNext = true;
        StringBuilder b = new StringBuilder(field.length());
        for (int i = 0; i < field.length(); ++i) {
            char c = field.charAt(i);
            if (c == '_') {
                c = ' ';
                capNext = true;
            } else if (capNext) {
                c = Character.toUpperCase(c);
                capNext = false;
            }
            b.append(c);
        }
        return field;
    }

    @Override
    public String toString() {
        return prettifyField(getTitle()) + ", "
                + getSection() + ", "
                + prettifyField(getLastName()) + ", "
                + prettifyField(getExtension()) + ", "
                + prettifyField(getReaderInitials()) + ", "
                + prettifyField(getBitRate())
                + (hasEmptyField() ? " [HAS EMPTY FIELD]" : "");
    }

    public String getTitle() {
        return title;
    }

    public String getLastName() {
        return lastName;
    }

    public String getReaderInitials() {
        return initials;
    }

    public String getBitRate() {
        return bitrate;
    }

    public String getExtension() {
        return extension;
    }

    public int getSection() {
        return section;
    }

    public String getSectionAsString() {
        return sectionAsString;
    }

    public int getNumberOfSectionDigits() {
        return sectionAsString.length();
    }

    public boolean hasEmptyField() {
        return emptyFieldWarning;
    }

    public boolean hasMissingField() {
        return title == null || section < 0 || lastName == null || extension == null;
    }

//	public static void main( String[] args ) {
//		String[] names = {
//			"blackbeauty_01_sewell.mp3",
//			"wind_willows_01_grahame.mp3",
//			"3.mp3",
//			"wind_willows",
//			"wind_willows_grahame",
//			"name_0_willows_grahame",
//			"_hi_1_there_",
//			"wind__willows__01__grahame.mp3",
//			"__wind__willows__01__grahame__.mp3__",
//			"",
//			".",
//			".mp3",
//			"last_monkey_mcgee_cgj",
//			"goof_xxx",
//			"wedding_chest_lee_ty_64kb.mp3",
//		};
//		for( String s : names )
//			System.err.println( new LibriVoxFileName( s ).toString() );
//	}
}
