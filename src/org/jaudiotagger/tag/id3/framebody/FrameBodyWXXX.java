/**
 *  @author : Paul Taylor
 *  @author : Eric Farng
 *
 *  Version @version:$Id: FrameBodyWXXX.java,v 1.13 2007/11/13 14:24:35 paultaylor Exp $
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
 * Represents a user defined URL,must also privide a description
 *
 */
package org.jaudiotagger.tag.id3.framebody;

import org.jaudiotagger.tag.InvalidTagException;
import org.jaudiotagger.tag.datatype.*;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.valuepair.TextEncoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/** Represents a user defined url
 *
 */
public class FrameBodyWXXX
    extends AbstractFrameBodyUrlLink  implements ID3v24FrameBody,ID3v23FrameBody
{

    /**
     * Creates a new FrameBodyWXXX datatype.
     */
    public FrameBodyWXXX()
    {
        this.setObjectValue(DataTypes.OBJ_TEXT_ENCODING, TextEncoding.ISO_8859_1);
        this.setObjectValue(DataTypes.OBJ_DESCRIPTION, "");
        this.setObjectValue(DataTypes.OBJ_URLLINK, "");
    }

    public FrameBodyWXXX(FrameBodyWXXX body)
    {
        super(body);
    }

    /**
     * Creates a new FrameBodyWXXX datatype.
     *
     * @param textEncoding 
     * @param description  
     * @param urlLink      
     */
    public FrameBodyWXXX(byte textEncoding, String description, String urlLink)
    {
        this.setObjectValue(DataTypes.OBJ_TEXT_ENCODING, textEncoding);
        this.setObjectValue(DataTypes.OBJ_DESCRIPTION, description);
        this.setObjectValue(DataTypes.OBJ_URLLINK, urlLink);
    }

    /**
     * Creates a new FrameBodyWXXX datatype by reading from file.
     *
     * @throws InvalidTagException 
     */
    public FrameBodyWXXX(ByteBuffer byteBuffer, int frameSize)
        throws InvalidTagException
    {
        super(byteBuffer, frameSize);
    }

    /**
     *  Set a description of the hyperlink
     *
     * @param description 
     */
    public void setDescription(String description)
    {
        setObjectValue(DataTypes.OBJ_DESCRIPTION, description);
    }

    /**
     * 
     *
     * @return a description of the hyperlink
     */
    public String getDescription()
    {
        return (String) getObjectValue(DataTypes.OBJ_DESCRIPTION);
    }

    /**
      * The ID3v2 frame identifier
      *
      * @return the ID3v2 frame identifier  for this frame type
     */
    public String getIdentifier()
    {
        return ID3v24Frames.FRAME_ID_USER_DEFINED_URL;
    }

     /**
     * If the description cannot be encoded using the current encoding change the encoder
     */
    public void write(ByteArrayOutputStream tagBuffer)    
    {
        if (((AbstractString) getObject(DataTypes.OBJ_DESCRIPTION)).canBeEncoded() == false)
        {
            this.setTextEncoding(TextEncoding.UTF_16);
        }
        super.write(tagBuffer);
    }

    /**
     * This is different ot other URL Links
     */
    protected void setupObjectList()
    {
        objectList.add(new NumberHashMap(DataTypes.OBJ_TEXT_ENCODING, this, TextEncoding.TEXT_ENCODING_FIELD_SIZE));
        objectList.add(new TextEncodedStringNullTerminated(DataTypes.OBJ_DESCRIPTION, this));
        objectList.add(new StringSizeTerminated(DataTypes.OBJ_URLLINK, this));
    }
}
