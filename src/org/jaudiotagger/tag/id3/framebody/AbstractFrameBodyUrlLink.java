/**
 *  @author : Paul Taylor
 *  @author : Eric Farng
 *
 *  Version @version:$Id: AbstractFrameBodyUrlLink.java,v 1.8 2007/08/06 16:04:33 paultaylor Exp $
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

 *
 */
package org.jaudiotagger.tag.id3.framebody;

import org.jaudiotagger.tag.InvalidTagException;
import org.jaudiotagger.tag.datatype.DataTypes;
import org.jaudiotagger.tag.datatype.StringSizeTerminated;

import java.nio.ByteBuffer;

/**
 *  Abstract superclass of all URL Frames
*/
public abstract class AbstractFrameBodyUrlLink extends AbstractID3v2FrameBody
{

    /**
     * Creates a new FrameBodyUrlLink datatype.
     */
    protected AbstractFrameBodyUrlLink()
    {
        super();
    }

    /**
     * Copy Constructor
     */
    protected AbstractFrameBodyUrlLink(AbstractFrameBodyUrlLink body)
    {
        super(body);
    }

    /**
     * Creates a new FrameBodyUrlLink datatype., set up with data.
     *
     * @param urlLink 
     */
    public AbstractFrameBodyUrlLink(String urlLink)
    {
        setObjectValue(DataTypes.OBJ_URLLINK, urlLink);
    }

    /**
     * Creates a new FrameBodyUrlLink datatype.
     *
     * @throws InvalidTagException if unable to create framebody from buffer
     */
    protected AbstractFrameBodyUrlLink(ByteBuffer byteBuffer, int frameSize)
        throws InvalidTagException
    {
        super(byteBuffer, frameSize);
    }

    /**
     * Set URL Link
     *
     * @param urlLink 
     */
    public void setUrlLink(String urlLink)
    {
        setObjectValue(DataTypes.OBJ_URLLINK, urlLink);
    }

    /**
     * Get URL Link
     *
     * @return the urllink
     */
    public String getUrlLink()
    {
        return (String) getObjectValue(DataTypes.OBJ_URLLINK);
    }

    /**
     * 
     */
    protected void setupObjectList()
    {
        objectList.add(new StringSizeTerminated(DataTypes.OBJ_URLLINK, this));
    }
}
