package org.jaudiotagger.tag.id3;

import org.jaudiotagger.tag.id3.framebody.FrameBodyTXXX;
import org.jaudiotagger.tag.id3.framebody.FrameBodyUFID;

/**
 * List of known id3v23 metadata fields
 *
 * <p>These provide a mapping from the generic key to the underlying ID3v23frames. For example most of the Musicbrainz
 * fields are implemnted using a User Defined Text Info Frame, but with a different description key, so this
 * enum provides the link between the two.
 */
public enum ID3v23FieldKey
{
    ARTIST(ID3v23Frames.FRAME_ID_V3_ARTIST,Id3FieldType.TEXT),
    ALBUM(ID3v23Frames.FRAME_ID_V3_ALBUM,Id3FieldType.TEXT),
    TITLE(ID3v23Frames.FRAME_ID_V3_TITLE,Id3FieldType.TEXT),
    TRACK(ID3v23Frames.FRAME_ID_V3_TRACK,Id3FieldType.TEXT),
    YEAR(ID3v23Frames.FRAME_ID_V3_TYER,Id3FieldType.TEXT),
    GENRE(ID3v23Frames.FRAME_ID_V3_GENRE,Id3FieldType.TEXT),
    COMMENT(ID3v23Frames.FRAME_ID_V3_COMMENT,Id3FieldType.TEXT),
    ALBUM_ARTIST(ID3v23Frames.FRAME_ID_V3_ACCOMPANIMENT,Id3FieldType.TEXT),
    COMPOSER(ID3v23Frames.FRAME_ID_V3_COMPOSER,Id3FieldType.TEXT),
    GROUPING(ID3v23Frames.FRAME_ID_V3_CONTENT_GROUP_DESC,Id3FieldType.TEXT),
    DISC_NO(ID3v23Frames.FRAME_ID_V3_SET,Id3FieldType.TEXT),
    BPM(ID3v23Frames.FRAME_ID_V3_BPM,Id3FieldType.TEXT),
    ENCODER(ID3v23Frames.FRAME_ID_V3_ENCODEDBY,Id3FieldType.TEXT),
    MUSICBRAINZ_ARTISTID(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MUSIC_BRAINZ_ARTISTID,Id3FieldType.TEXT),
    MUSICBRAINZ_RELEASEID(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MUSIC_BRAINZ_ALBUMID,Id3FieldType.TEXT),
    MUSICBRAINZ_RELEASEARTISTID(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MUSIC_BRAINZ_ALBUM_ARTISTID,Id3FieldType.TEXT),
    MUSICBRAINZ_TRACK_ID(ID3v23Frames.FRAME_ID_V3_UNIQUE_FILE_ID, FrameBodyUFID.UFID_MUSICBRAINZ,Id3FieldType.TEXT),
    MUSICBRAINZ_DISC_ID(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MUSIC_BRAINZ_DISCID,Id3FieldType.TEXT),
    MUSICIP_ID(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MUSICIP_ID,Id3FieldType.TEXT),
    AMAZON_ID(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.AMAZON_ASIN,Id3FieldType.TEXT),
    MUSICBRAINZ_RELEASE_STATUS(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MUSICBRAINZ_ALBUM_STATUS,Id3FieldType.TEXT),
    MUSICBRAINZ_RELEASE_TYPE(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MUSICBRAINZ_ALBUM_TYPE,Id3FieldType.TEXT),
    MUSICBRAINZ_RELEASE_COUNTRY(ID3v23Frames.FRAME_ID_V3_USER_DEFINED_INFO, FrameBodyTXXX.MUSICBRAINZ_ALBUM_COUNTRY,Id3FieldType.TEXT),
    LYRICS(ID3v23Frames.FRAME_ID_V3_UNSYNC_LYRICS,Id3FieldType.TEXT),
    IS_COMPILATION(ID3v23Frames.FRAME_ID_V3_IS_COMPILATION,Id3FieldType.TEXT),
    ARTIST_SORT(ID3v23Frames.FRAME_ID_V3_ARTIST_SORT_ORDER_ITUNES,Id3FieldType.TEXT),
    ALBUM_ARTIST_SORT(ID3v23Frames.FRAME_ID_V3_ALBUM_ARTIST_SORT_ORDER_ITUNES,Id3FieldType.TEXT),
    ALBUM_SORT(ID3v23Frames.FRAME_ID_V3_ALBUM_SORT_ORDER_ITUNES,Id3FieldType.TEXT),
    TITLE_SORT(ID3v23Frames. FRAME_ID_V3_TITLE_SORT_ORDER_ITUNES,Id3FieldType.TEXT),
    COMPOSER_SORT(ID3v23Frames.FRAME_ID_V3_COMPOSER_SORT_ORDER_ITUNES,Id3FieldType.TEXT),
    COVER_ART(ID3v23Frames.FRAME_ID_V3_ATTACHED_PICTURE,Id3FieldType.BINARY),
    ;

    private String fieldName;

    private String frameId;
    private String subId;
    private Id3FieldType fieldType;

    /**
     * For usual metadata fields that use a data field
     *
     * @param frameId the frame that will be used
     * @param fieldType of data atom
     */
    ID3v23FieldKey(String frameId,Id3FieldType fieldType)
    {
        this.frameId = frameId;
        this.fieldType = fieldType;

        this.fieldName = frameId;
    }

    /**
     *
     * @param frameId the frame that will be used
     * @param subId the additioanl key reuirted within the frame touniquely identify this key
     * @param fieldType
     */
    ID3v23FieldKey(String frameId,String subId,Id3FieldType fieldType)
    {
        this.frameId = frameId;
        this.subId   = subId;
        this.fieldType = fieldType;

        this.fieldName = frameId + ":" + subId;
    }

    /**
     *
     * @return fieldtype
     */
    public Id3FieldType getFieldType()
    {
        return fieldType;
    }

    /**
     * This is the frame identifier used to write the field
     *
     * @return
     */
    public String getFrameId()
    {
        return frameId;
    }

    /**
     * This is the subfield used within the frame for this type of field
     *
     * @return subId
     */
    public String getSubId()
    {
        return subId;
    }

     /**
     * This is the value of the key that can uniquely identifer a key type
     *
     * @return
     */
    public String getFieldName()
    {
        return fieldName;
    }
}
