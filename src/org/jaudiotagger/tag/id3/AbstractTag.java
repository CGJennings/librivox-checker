/**
 *  @author : Paul Taylor
 *  @author : Eric Farng
 *
 *  Version @version:$Id: AbstractTag.java,v 1.1 2007/08/07 16:03:56 paultaylor Exp $
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
 * Description:This class represents any type of tag in an MP3 file, including ID3 and
 * Lyrics and the file name.
 */
package org.jaudiotagger.tag.id3;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Iterator;

/**
 * A tag is term given to a container that holds audio metadata
 */
public abstract class AbstractTag extends AbstractTagItem
{
    protected static final String TYPE_TAG = "tag";


    public AbstractTag()
    {
    }

    public AbstractTag(AbstractTag copyObject)
    {
        super(copyObject);
    }

     /**
     * Looks for this tag in the buffer
     *
     * @param byteBuffer
      * @return returns true if found, false otherwise.
     */
    abstract public boolean seek(ByteBuffer byteBuffer);

    /**
     * Writes the tag to the file
     *
     * @param file
     * @throws IOException
     */
    public abstract void write(RandomAccessFile file) throws IOException;


    /**
     * Removes the specific tag from the file
     *
     * @param file MP3 file to append to.
     * @throws IOException on any I/O error
     */
    abstract public void delete(RandomAccessFile file)throws IOException;


    /**
     * Determines whether another datatype is equal to this tag. It just compares
     * if they are the same class, then calls <code>super.equals(obj)</code>.
     *
     * @param obj The object to compare
     * @return if they are equal
     */
    public boolean equals(Object obj)
    {
        if ((obj instanceof AbstractTag) == false)
        {
            return false;
        }

        return super.equals(obj);
    }

    /**
     * 
     *
     * @return 
     */
    abstract public Iterator iterator();
}



