/**
 *  @author : Paul Taylor
 *  @author : Eric Farng
 *
 *  Version @version:$Id: FrameBodyTMOO.java,v 1.7 2006/08/25 15:35:25 paultaylor Exp $
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
package org.jaudiotagger.tag.id3.framebody;

import org.jaudiotagger.tag.InvalidTagException;
import org.jaudiotagger.tag.id3.ID3v24Frames;

import java.nio.ByteBuffer;


public class FrameBodyTMOO extends AbstractFrameBodyTextInfo  implements ID3v24FrameBody
{
    /**
     * Creates a new FrameBodyTMOO datatype.
     */
    public FrameBodyTMOO()
    {
    }

    public FrameBodyTMOO(FrameBodyTMOO body)
    {
        super(body);
    }

    /**
     * Creates a new FrameBodyTMOO datatype.
     *
     * @param textEncoding 
     * @param text         
     */
    public FrameBodyTMOO(byte textEncoding, String text)
    {
        super(textEncoding, text);
    }

    /**
     * Creates a new FrameBodyTMOO datatype.
     *
     * @throws java.io.IOException 
     * @throws InvalidTagException 
     */
    public FrameBodyTMOO(ByteBuffer byteBuffer, int frameSize)
        throws InvalidTagException
    {
        super(byteBuffer, frameSize);
    }

      /**
      * The ID3v2 frame identifier
      *
      * @return the ID3v2 frame identifier  for this frame type
     */
    public String getIdentifier()
    {
        return  ID3v24Frames.FRAME_ID_MOOD;
    }

}