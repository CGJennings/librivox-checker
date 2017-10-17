/**
 *  @author : Paul Taylor
 *  @author : Eric Farng
 *
 *  Version @version:$Id: ID3v1Tag.java,v 1.22 2007/11/27 17:03:31 paultaylor Exp $
 *
 *  MusicTag Copyright (C)2003,2004
 *
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 *  General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 *  or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 *  you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Description:
 *
 */
package org.jaudiotagger.tag.id3;

import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.audio.generic.Utils;
import org.jaudiotagger.tag.*;
import org.jaudiotagger.tag.id3.valuepair.GenreTypes;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.regex.Matcher;

/**
 * Represents an ID3v1 tag.
 *
 * @author : Eric Farng
 * @author : Paul Taylor
 */
public class ID3v1Tag extends AbstractID3v1Tag implements Tag
{
    static EnumMap<TagFieldKey, ID3v1FieldKey> tagFieldToID3v1Field = new EnumMap<TagFieldKey, ID3v1FieldKey>(TagFieldKey.class);

    static
    {
        tagFieldToID3v1Field.put(TagFieldKey.ARTIST, ID3v1FieldKey.ARTIST);
        tagFieldToID3v1Field.put(TagFieldKey.ALBUM, ID3v1FieldKey.ALBUM);
        tagFieldToID3v1Field.put(TagFieldKey.TITLE, ID3v1FieldKey.TITLE);
        tagFieldToID3v1Field.put(TagFieldKey.TRACK, ID3v1FieldKey.TRACK);
        tagFieldToID3v1Field.put(TagFieldKey.YEAR, ID3v1FieldKey.YEAR);
        tagFieldToID3v1Field.put(TagFieldKey.GENRE, ID3v1FieldKey.GENRE);
        tagFieldToID3v1Field.put(TagFieldKey.COMMENT, ID3v1FieldKey.COMMENT);
    }

    //For writing output
    protected static final String TYPE_COMMENT = "comment";


    protected static final int FIELD_COMMENT_LENGTH = 30;
    protected static final int FIELD_COMMENT_POS = 97;
    protected static final int BYTE_TO_UNSIGNED = 0xff;

    protected static final int GENRE_UNDEFINED = 0xff;

    /**
     *
     */
    protected String album = "";

    /**
     *
     */
    protected String artist = "";

    /**
     *
     */
    protected String comment = "";

    /**
     *
     */
    protected String title = "";

    /**
     *
     */
    protected String year = "";

    /**
     *
     */
    protected byte genre = (byte) -1;


    private static final byte RELEASE = 1;
    private static final byte MAJOR_VERSION = 0;
    private static final byte REVISION = 0;

    /**
     * Retrieve the Release
     */
    public byte getRelease()
    {
        return RELEASE;
    }

    /**
     * Retrieve the Major Version
     */
    public byte getMajorVersion()
    {
        return MAJOR_VERSION;
    }

    /**
     * Retrieve the Revision
     */
    public byte getRevision()
    {
        return REVISION;
    }

    /**
     * Creates a new ID3v1 datatype.
     */
    public ID3v1Tag()
    {

    }

    public ID3v1Tag(ID3v1Tag copyObject)
    {
        super(copyObject);

        this.album = new String(copyObject.album);
        this.artist = new String(copyObject.artist);
        this.comment = new String(copyObject.comment);
        this.title = new String(copyObject.title);
        this.year = new String(copyObject.year);
        this.genre = copyObject.genre;
    }

    public ID3v1Tag(AbstractTag mp3tag)
    {

        if (mp3tag != null)
        {
            ID3v11Tag convertedTag;
            if (mp3tag instanceof ID3v1Tag)
            {
                throw new UnsupportedOperationException("Copy Constructor not called. Please type cast the argument");
            }
            if (mp3tag instanceof ID3v11Tag)
            {
                convertedTag = (ID3v11Tag) mp3tag;
            }
            else
            {
                convertedTag = new ID3v11Tag(mp3tag);
            }
            this.album = new String(convertedTag.album);
            this.artist = new String(convertedTag.artist);
            this.comment = new String(convertedTag.comment);
            this.title = new String(convertedTag.title);
            this.year = new String(convertedTag.year);
            this.genre = convertedTag.genre;
        }
    }

    /**
     * Creates a new ID3v1 datatype.
     *
     * @param file
     * @param loggingFilename
     * @throws TagNotFoundException
     * @throws IOException
     */
    public ID3v1Tag(RandomAccessFile file, String loggingFilename)
            throws TagNotFoundException, IOException
    {
        setLoggingFilename(loggingFilename);
        FileChannel fc;
        ByteBuffer byteBuffer;

        fc = file.getChannel();
        fc.position(file.length() - TAG_LENGTH);
        byteBuffer = ByteBuffer.allocate(TAG_LENGTH);
        fc.read(byteBuffer);
        byteBuffer.flip();
        read(byteBuffer);
    }

    /**
     * Creates a new ID3v1 datatype.
     *
     * @param file
     * @throws TagNotFoundException
     * @throws IOException
     * @deprecated use {@link #ID3v1Tag(RandomAccessFile,String)} instead
     */
    public ID3v1Tag(RandomAccessFile file)
            throws TagNotFoundException, IOException
    {
        this(file, "");
    }

    public void add(TagField field)
    {
        //TODO
    }

    public List get(String id)
    {
        //TODO
        return null;
    }

    public int getFieldCount()
    {
        return 7;
    }

    protected List returnFieldToList(ID3v1TagField field)
    {
        List fields = new ArrayList();
        fields.add(field);
        return fields;
    }

    /**
     * Add Album
     * <p/>
     * <p>Only one album can be added so if one already exists it will be replaced.
     *
     * @param album
     */
    public void addAlbum(String album)
    {
        setAlbum(album);
    }

    /**
     * Set Album
     *
     * @param album
     */
    public void setAlbum(String album)
    {
        this.album = ID3Tags.truncate(album, this.FIELD_ALBUM_LENGTH);
    }

    /**
     * Get Album
     *
     * @return album
     */
    public String getFirstAlbum()
    {
        return album;
    }

    /**
     * @return album within list or empty if does not exist
     */
    public List getAlbum()
    {
        if (getFirstAlbum().length() > 0)
        {
            ID3v1TagField field = new ID3v1TagField(ID3v1FieldKey.ALBUM.name(), getFirstAlbum());
            return returnFieldToList(field);
        }
        else
        {
            return new ArrayList();
        }
    }


    /**
     * Add Artist
     * <p/>
     * <p>Only one artist can be added so if one already exists it will be replaced.
     *
     * @param artist
     */
    public void addArtist(String artist)
    {
        setArtist(album);
    }


    /**
     * Set Artist
     *
     * @param artist
     */
    public void setArtist(String artist)
    {
        this.artist = ID3Tags.truncate(artist, this.FIELD_ARTIST_LENGTH);
    }

    /**
     * Get Artist
     *
     * @return artist
     */
    public String getFirstArtist()
    {
        return artist;
    }

    /**
     * @return Artist within list or empty if does not exist
     */
    public List getArtist()
    {
        if (getFirstArtist().length() > 0)
        {
            ID3v1TagField field = new ID3v1TagField(ID3v1FieldKey.ARTIST.name(), getFirstArtist());
            return returnFieldToList(field);
        }
        else
        {
            return new ArrayList();
        }
    }

    /**
     * Add Comment
     * <p/>
     * <p>Only one comment can be added so if one already exists it will be replaced.
     *
     * @param comment
     */
    public void addComment(String comment)
    {
        setComment(comment);
    }

    /**
     * Set Comment
     *
     * @param comment
     */
    public void setComment(String comment)
    {
        this.comment = ID3Tags.truncate(comment, this.FIELD_COMMENT_LENGTH);
    }

    /**
     * @return comment within list or empty if does not exist
     */
    public List getComment()
    {
        if (getFirstComment().length() > 0)
        {
            ID3v1TagField field = new ID3v1TagField(ID3v1FieldKey.COMMENT.name(), getFirstComment());
            return returnFieldToList(field);
        }
        else
        {
            return new ArrayList();
        }
    }

    /**
     * Get Comment
     *
     * @return comment
     */
    public String getFirstComment()
    {
        return comment;
    }

    /**
     * Add Genre
     * <p/>
     * <p>Only one Genre can be added so if one already exists it will be replaced.
     *
     * @param genre
     */
    public void addGenre(String genre)
    {
        setGenre(genre);
    }

    /**
     * Sets the genreID,
     * <p/>
     * <p>ID3v1 only supports genres defined in a predefined list
     * so if unable to find value in list set 255, which seems to be the value
     * winamp uses for undefined.
     *
     * @param genreVal
     */
    public void setGenre(String genreVal)
    {
        Integer genreID = GenreTypes.getInstanceOf().getIdForValue(genreVal);
        if (genreID != null)
        {
            this.genre = genreID.byteValue();
        }
        else
        {
            this.genre = (byte) GENRE_UNDEFINED;
        }
    }

    /**
     * Get Genre
     *
     * @return genre or empty string if not valid
     */
    public String getFirstGenre()
    {
        Integer genreId = genre & this.BYTE_TO_UNSIGNED;
        String genreValue = GenreTypes.getInstanceOf().getValueForId(genreId);
        if (genreValue == null)
        {
            return "";
        }
        else
        {
            return genreValue;
        }
    }

    /**
     * Get Genre field
     * <p/>
     * <p>Only a single genre is available in ID3v1
     *
     * @return
     */
    public List getGenre()
    {
        if (getFirstGenre().length() > 0)
        {
            ID3v1TagField field = new ID3v1TagField(ID3v1FieldKey.GENRE.name(), getFirstGenre());
            return returnFieldToList(field);
        }
        else
        {
            return new ArrayList();
        }
    }

    /**
     * Add Title
     * <p/>
     * <p>Only one title can be added so if one already exists it will be replaced.
     *
     * @param title
     */
    public void addTitle(String title)
    {
        setTitle(title);
    }

    /**
     * Set Title
     *
     * @param title
     */
    public void setTitle(String title)
    {
        this.title = ID3Tags.truncate(title, this.FIELD_TITLE_LENGTH);
    }

    /**
     * Get title
     *
     * @return Title
     */
    public String getFirstTitle()
    {
        return title;
    }

    /**
     * Get title field
     * <p/>
     * <p>Only a single title is available in ID3v1
     *
     * @return
     */
    public List getTitle()
    {
        if (getFirstTitle().length() > 0)
        {
            ID3v1TagField field = new ID3v1TagField(ID3v1FieldKey.TITLE.name(), getFirstTitle());
            return returnFieldToList(field);
        }
        else
        {
            return new ArrayList();
        }
    }

    /**
     * Add Year
     * <p/>
     * <p>Only one year can be added so if one already exists it will be replaced.
     *
     * @param year
     */
    public void addYear(String year)
    {
        setYear(year);
    }

    /**
     * Set year
     *
     * @param year
     */
    public void setYear(String year)
    {
        this.year = ID3Tags.truncate(year, this.FIELD_YEAR_LENGTH);
    }

    /**
     * Get year
     *
     * @return year
     */
    public String getFirstYear()
    {
        return year;
    }

    /**
     * Get year field
     * <p/>
     * <p>Only a single year is available in ID3v1
     *
     * @return
     */
    public List getYear()
    {
        if (getFirstYear().length() > 0)
        {
            ID3v1TagField field = new ID3v1TagField(ID3v1FieldKey.YEAR.name(), getFirstYear());
            return returnFieldToList(field);
        }
        else
        {
            return new ArrayList();
        }
    }

    public void addTrack(String track)
    {
        throw new UnsupportedOperationException("ID3v10 cannot store track numbers");
    }

    public String getFirstTrack()
    {
        throw new UnsupportedOperationException("ID3v10 cannot store track numbers");
    }

    public void setTrack(String track)
    {
        throw new UnsupportedOperationException("ID3v10 cannot store track numbers");
    }

    public List getTrack()
    {
        throw new UnsupportedOperationException("ID3v10 cannot store track numbers");
    }

    public TagField getFirstField(String id)
    {
       //TODO
       throw new UnsupportedOperationException("TODO:Not done yet");
    }

    public Iterator getFields()
    {
        throw new UnsupportedOperationException("TODO:Not done yet");
    }

    public boolean hasCommonFields()
    {
        //TODO
        return true;
    }

    public boolean hasField(String id)
    {
        //TODO
        throw new UnsupportedOperationException("TODO:Not done yet");
    }

    public boolean isEmpty()
    {
        //TODO
        throw new UnsupportedOperationException("TODO:Not done yet");
    }



    public void set(TagField field)
    {
        TagFieldKey genericKey = TagFieldKey.valueOf(field.getId());
        switch(genericKey)
        {
            case ARTIST:
                setArtist(field.toString());

            case ALBUM:
                setAlbum(field.toString());

            case TITLE:
                setTitle(field.toString());

            case GENRE:
                setGenre(field.toString());

            case YEAR:
                setYear(field.toString());

            case COMMENT:
                setComment(field.toString());
        }
    }

    /**
     *
     * @param encoding
     * @return
     */
    public boolean setEncoding(String encoding)
    {
        return true;
    }

    /**
     * Create Tag Field using generic key
     */
    public TagField createTagField(TagFieldKey genericKey, String value)
    {
        return new ID3v1TagField(tagFieldToID3v1Field.get(genericKey).name(), value);
    }

    public String getEncoding()
    {
        return "ISO-8859-1";
    }

    /**
     * Returns a {@linkplain List list} of {@link TagField} objects whose &quot;{@linkplain TagField#getId() id}&quot;
     * is the specified one.<br>
     *
     * @param genericKey The generic field key
     * @return A list of {@link TagField} objects with the given &quot;id&quot;.
     */
    public List<TagField> get(TagFieldKey genericKey)
    {
        switch(genericKey)
        {
            case ARTIST:
                return getArtist();

            case ALBUM:
                return getAlbum();

            case TITLE:
                return getTitle();

            case GENRE:
                return getGenre();

            case YEAR:
                return getYear();

            case COMMENT:
                return getComment();

            default:
                return new ArrayList<TagField>();                        
        }
    }


    /**
     * Retrieve the first value that exists for this key id
     *
     * @param  genericKey
     * @return
     */
    public String getFirst(String genericKey)
    {
        TagFieldKey matchingKey = TagFieldKey.valueOf(genericKey);
        if(matchingKey!=null)
        {
            return getFirst(matchingKey);
        }
        else
        {
            return "";
        }
    }


    /**
     * Retrieve the first value that exists for this generic key
     *
     * @param genericKey
     * @return
     */
    public String getFirst(TagFieldKey genericKey)
    {
         switch(genericKey)
        {
            case ARTIST:
                return getFirstArtist();

            case ALBUM:
                return getFirstAlbum();

            case TITLE:
                return getFirstTitle();

            case GENRE:
                return getFirstGenre();

            case YEAR:
                return getFirstYear();

            case TRACK:
                return getFirstTrack();

            case COMMENT:
                return getFirstComment();

            default:
                return "";
        }
    }

    /**
     * Delete any instance of tag fields with this key
     *
     * @param genericKey
     */
    public void deleteTagField(TagFieldKey genericKey)
    {
         switch(genericKey)
        {
            case ARTIST:
                setArtist("");

            case ALBUM:
                setAlbum("");

            case TITLE:
                setTitle("");

            case GENRE:
                setGenre("");

            case YEAR:
                setYear("");

            case COMMENT:
                setComment("");
        }
    }

    /**
     * @param obj
     * @return true if this and obj are equivalent
     */
    public boolean equals(Object obj)
    {
        if ((obj instanceof ID3v1Tag) == false)
        {
            return false;
        }
        ID3v1Tag object = (ID3v1Tag) obj;
        if (this.album.equals(object.album) == false)
        {
            return false;
        }
        if (this.artist.equals(object.artist) == false)
        {
            return false;
        }
        if (this.comment.equals(object.comment) == false)
        {
            return false;
        }
        if (this.genre != object.genre)
        {
            return false;
        }
        if (this.title.equals(object.title) == false)
        {
            return false;
        }
        if (this.year.equals(object.year) == false)
        {
            return false;
        }
        return super.equals(obj);
    }

    /**
     * @return an iterator to iterate through the fields of the tag
     */
    public Iterator iterator()
    {
        return new ID3v1Iterator(this);
    }


    /**
     * @param byteBuffer
     * @throws TagNotFoundException
     */
    public void read(ByteBuffer byteBuffer)
            throws TagNotFoundException
    {
        if (seek(byteBuffer) == false)
        {
            throw new TagNotFoundException(getLoggingFilename() + ":" + "ID3v1 tag not found");
        }
        logger.finer(getLoggingFilename() + ":" + "Reading v1 tag");
        //Do single file read of data to cut down on file reads
        byte[] dataBuffer = new byte[TAG_LENGTH];
        byteBuffer.position(0);
        byteBuffer.get(dataBuffer, 0, TAG_LENGTH);
        title = Utils.getString(dataBuffer, FIELD_TITLE_POS, this.FIELD_TITLE_LENGTH,"ISO-8859-1").trim();
        Matcher m = endofStringPattern.matcher(title);
        if (m.find() == true)
        {
            title = title.substring(0, m.start());
        }
        artist = Utils.getString(dataBuffer, FIELD_ARTIST_POS, this.FIELD_ARTIST_LENGTH,"ISO-8859-1").trim();
        m = endofStringPattern.matcher(artist);
        if (m.find() == true)
        {
            artist = artist.substring(0, m.start());
        }
        album = Utils.getString(dataBuffer, FIELD_ALBUM_POS, this.FIELD_ALBUM_LENGTH,"ISO-8859-1").trim();
        m = endofStringPattern.matcher(album);
        logger.finest(getLoggingFilename() + ":" + "Orig Album is:" + comment + ":");
        if (m.find() == true)
        {
            album = album.substring(0, m.start());
            logger.finest(getLoggingFilename() + ":" + "Album is:" + album + ":");
        }
        year = Utils.getString(dataBuffer, FIELD_YEAR_POS, this.FIELD_YEAR_LENGTH,"ISO-8859-1").trim();
        m = endofStringPattern.matcher(year);
        if (m.find() == true)
        {
            year = year.substring(0, m.start());
        }
        comment = Utils.getString(dataBuffer, FIELD_COMMENT_POS, this.FIELD_COMMENT_LENGTH,"ISO-8859-1").trim();
        m = endofStringPattern.matcher(comment);
        logger.finest(getLoggingFilename() + ":" + "Orig Comment is:" + comment + ":");
        if (m.find() == true)
        {
            comment = comment.substring(0, m.start());
            logger.finest(getLoggingFilename() + ":" + "Comment is:" + comment + ":");
        }
        genre = dataBuffer[this.FIELD_GENRE_POS];

    }

    /**
     * Does a tag of this version exist within the byteBuffer
     *
     * @return whether tag exists within the byteBuffer
     */
    public boolean seek(ByteBuffer byteBuffer)
    {
        byte[] buffer = new byte[FIELD_TAGID_LENGTH];
        // read the TAG value
        byteBuffer.get(buffer, 0, FIELD_TAGID_LENGTH);
        return (Arrays.equals(buffer, TAG_ID));
    }

    /**
     * Write this tag to the file, replacing any tag previously existing
     *
     * @param file
     * @throws IOException
     */
    public void write(RandomAccessFile file)
            throws IOException
    {
        logger.info("Saving file");
        byte[] buffer = new byte[TAG_LENGTH];
        int i;
        String str;
        delete(file);
        file.seek(file.length());
        //Copy the TAGID into new buffer
        System.arraycopy(TAG_ID, this.FIELD_TAGID_POS, buffer, this.FIELD_TAGID_POS, TAG_ID.length);
        int offset = this.FIELD_TITLE_POS;
        if (TagOptionSingleton.getInstance().isId3v1SaveTitle())
        {
            str = ID3Tags.truncate(title, this.FIELD_TITLE_LENGTH);
            for (i = 0; i < str.length(); i++)
            {
                buffer[i + offset] = (byte) str.charAt(i);
            }
        }
        offset = this.FIELD_ARTIST_POS;
        if (TagOptionSingleton.getInstance().isId3v1SaveArtist())
        {
            str = ID3Tags.truncate(artist, this.FIELD_ARTIST_LENGTH);
            for (i = 0; i < str.length(); i++)
            {
                buffer[i + offset] = (byte) str.charAt(i);
            }
        }
        offset = this.FIELD_ALBUM_POS;
        if (TagOptionSingleton.getInstance().isId3v1SaveAlbum())
        {
            str = ID3Tags.truncate(album, this.FIELD_ALBUM_LENGTH);
            for (i = 0; i < str.length(); i++)
            {
                buffer[i + offset] = (byte) str.charAt(i);
            }
        }
        offset = this.FIELD_YEAR_POS;
        if (TagOptionSingleton.getInstance().isId3v1SaveYear())
        {
            str = ID3Tags.truncate(year, AbstractID3v1Tag.FIELD_YEAR_LENGTH);
            for (i = 0; i < str.length(); i++)
            {
                buffer[i + offset] = (byte) str.charAt(i);
            }
        }
        offset = this.FIELD_COMMENT_POS;
        if (TagOptionSingleton.getInstance().isId3v1SaveComment())
        {
            str = ID3Tags.truncate(comment, this.FIELD_COMMENT_LENGTH);
            for (i = 0; i < str.length(); i++)
            {
                buffer[i + offset] = (byte) str.charAt(i);
            }
        }
        offset = this.FIELD_GENRE_POS;
        if (TagOptionSingleton.getInstance().isId3v1SaveGenre())
        {
            buffer[offset] = genre;
        }
        file.write(buffer);
    }

    /**
     * Create strcutured representation of this item.
     */
    public void createStructure()
    {
        MP3File.getStructureFormatter().openHeadingElement(TYPE_TAG, getIdentifier());
        //Header
        MP3File.getStructureFormatter().addElement(TYPE_TITLE, this.title);
        MP3File.getStructureFormatter().addElement(TYPE_ARTIST, this.artist);
        MP3File.getStructureFormatter().addElement(TYPE_ALBUM, this.album);
        MP3File.getStructureFormatter().addElement(TYPE_YEAR, this.year);
        MP3File.getStructureFormatter().addElement(TYPE_COMMENT, this.comment);
        MP3File.getStructureFormatter().addElement(TYPE_GENRE, (int) this.genre);
        MP3File.getStructureFormatter().closeHeadingElement(TYPE_TAG);
    }
}
