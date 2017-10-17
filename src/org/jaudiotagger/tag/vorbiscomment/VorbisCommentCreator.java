/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Rapha�l Slinckx <raphael@slinckx.net>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *  
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jaudiotagger.tag.vorbiscomment;

import org.jaudiotagger.audio.generic.AbstractTagCreator;
import org.jaudiotagger.audio.generic.Utils;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;

import java.io.UnsupportedEncodingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.*;
import java.util.*;

/**
 * Create the raw packet data for a Vorbis Comment Tag
 */
public class VorbisCommentCreator extends AbstractTagCreator
{
    /**
     * Convert tagdata to rawdata ready for writing to file
     *
     * @param tag
     * @param padding
     * @return
     * @throws UnsupportedEncodingException
     */
    //TODO padding parameter currently ignored
    public ByteBuffer convert(Tag tag, int padding) throws UnsupportedEncodingException
    {
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            //Vendor
            String vendorString = ((VorbisCommentTag) tag).getVendor();
            int vendorLength = Utils.getUTF8Bytes(vendorString).length;
            baos.write(Utils.getSizeLittleEndian(vendorLength));
            baos.write(Utils.getUTF8Bytes(vendorString));

            //User Comment List
            int listLength = tag.getFieldCount() - 1; //Remove Vendor from count         
            baos.write(Utils.getSizeLittleEndian(listLength));

            //Add metadata raw content
            Iterator it = tag.getFields();
            while (it.hasNext())
            {
                TagField frame = (TagField) it.next();
                if(frame.getId().equals(VorbisCommentFieldKey.VENDOR.name()))
                {
                    //this is always stored above so ignore                    
                }
                else
                {
                    baos.write(frame.getRawContent());
                }
            }

            //Put into ByteBuffer
            ByteBuffer buf = ByteBuffer.wrap(baos.toByteArray());
            buf.rewind();
            return buf;
        }
        catch (IOException ioe)
        {
            //Should never happen as not writing to file at this point
            throw new RuntimeException(ioe);
        }
    }
}
