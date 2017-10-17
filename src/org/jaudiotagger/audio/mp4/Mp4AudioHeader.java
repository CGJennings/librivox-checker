package org.jaudiotagger.audio.mp4;

import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.jaudiotagger.audio.mp4.atom.Mp4EsdsBox;

/**
 * Store some additional attributes not availble for all audio types
 */
public class Mp4AudioHeader extends GenericAudioHeader
{
    /**
     * The key for the kind field<br>
     *
     * @see #content
     */
    public final static String FIELD_KIND = "KIND";

    /**
     * The key for the kind<br>
     *
     * @see #content
     */
    public final static String FIELD_PROFILE = "PROFILE";

    public void setKind(Mp4EsdsBox.Kind kind)
    {
        content.put(FIELD_KIND,kind);
    }

    /**
     *
     * @return kind
     */
    public Mp4EsdsBox.Kind getKind()
    {
        return (Mp4EsdsBox.Kind)content.get(FIELD_KIND);
    }

    /**
     * The key for the profile
     *
     * @param profile
     */
    public void setProfile(Mp4EsdsBox.AudioProfile profile)
    {
        content.put(FIELD_PROFILE,profile);
    }

    /**
     *
     * @return audio profile
     */
    public Mp4EsdsBox.AudioProfile getProfile()
    {
        return (Mp4EsdsBox.AudioProfile)content.get(FIELD_PROFILE);
    }

}
