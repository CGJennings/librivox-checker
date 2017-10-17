/**
 *  @author : Paul Taylor
 *  @author : Eric Farng
 *
 *  Version @version:$Id: FrameBodyUnsupported.java,v 1.13 2007/08/06 16:04:34 paultaylor Exp $
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
 * Frame that is not currently suported by this application
 *
 *
 */
package org.jaudiotagger.tag.id3.framebody;

import org.jaudiotagger.tag.InvalidFrameException;
import org.jaudiotagger.tag.InvalidTagException;
import org.jaudiotagger.tag.datatype.ByteArraySizeTerminated;
import org.jaudiotagger.tag.datatype.DataTypes;

import java.nio.ByteBuffer;

/**
 * Represents a framebody for a frame identifier jaudiotagger has not implemented a framebody for.
 *
 * This is likley to be because the FrameBody is not specified in the Specification but it may just be because the code
 * has yet to be written, the library uses this framebody when it cant find an alternative. This is different to the
 * ID3v2ExtensionFrameBody Interface which should be implemented by frame bodies that are non standard such as
 * iTunes compilation frame (TCMP) but are commonly used.
 */
public class FrameBodyUnsupported extends AbstractID3v2FrameBody implements ID3v24FrameBody,ID3v23FrameBody,ID3v22FrameBody
{
    /**
     * Because used by any unknown frame identifier varies
     */
    private String identifier = "";

    /**
     * @deprecated  because no identifier set
     */
    public FrameBodyUnsupported()
    {

    }

    /**
     * Creates a new FrameBodyUnsupported
     */
    public FrameBodyUnsupported(String identifier)
    {
        this.identifier = identifier;
    }

    /**
     * Create a new FrameBodyUnsupported
     *
     * @param identifier
     * @param value
     */
    public FrameBodyUnsupported(String identifier,byte[] value)
    {
        this.identifier = identifier;
        setObjectValue(DataTypes.OBJ_DATA, value);
    }

    /**
     * Creates a new FrameBodyUnsupported datatype.
     * 
     * @deprecated because no identifier set
     * @param value 
     */
    public FrameBodyUnsupported(byte[] value)
    {
        setObjectValue(DataTypes.OBJ_DATA, value);
    }

    /**
     * Copy constructor
     *
     * @param copyObject a copy is made of this
     */
    public FrameBodyUnsupported(FrameBodyUnsupported copyObject)
    {
        super(copyObject);
        this.identifier = new String(copyObject.identifier);

    }

    /**
     * Creates a new FrameBodyUnsupported datatype.
     *
     * @throws InvalidFrameException if unable to create framebody from buffer
     */
    public FrameBodyUnsupported(ByteBuffer byteBuffer, int frameSize)
    throws InvalidTagException
    {
        super(byteBuffer, frameSize);
    }

    /**
     * Return the frame identifier
     *
     * @return the identifier
     */
    public String getIdentifier()
    {
        return identifier;
    }

    /**
     * 
     *
     * @param obj 
     * @return whether obj is equivalent to this object
     */
    public boolean equals(Object obj)
    {
        if ((obj instanceof FrameBodyUnsupported) == false)
        {
            return false;
        }

        FrameBodyUnsupported object = (FrameBodyUnsupported) obj;
        if (this.identifier.equals(object.identifier) == false)
        {
            return false;
        }
        return super.equals(obj);
    }


    /**
     * 
     * Because the contents of this frame are an array of bytes and could be large we just
     * return the identifier.
     *
     * @return  a string representation of this frame
     */
    public String toString()
    {
        return getIdentifier();
    }

    /**
     * Setup the Object List. A byte Array which will be read upto frame size
     * bytes.
     */
    protected void setupObjectList()
    {
        objectList.add(new ByteArraySizeTerminated(DataTypes.OBJ_DATA, this));
    }

}
