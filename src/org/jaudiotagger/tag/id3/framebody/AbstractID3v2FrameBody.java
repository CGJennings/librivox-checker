/**
 *  @author : Paul Taylor
 *  @author : Eric Farng
 *
 *  Version @version:$Id: AbstractID3v2FrameBody.java,v 1.19 2007/11/13 14:24:33 paultaylor Exp $
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
 * Abstract Superclass of all Frame Bodys
 *
 */
package org.jaudiotagger.tag.id3.framebody;

import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.*;
import org.jaudiotagger.tag.id3.AbstractTagFrameBody;
import org.jaudiotagger.tag.datatype.AbstractDataType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Contains the content for an ID3v2 frame, (the header is held directly within the frame
 */
public abstract class AbstractID3v2FrameBody
    extends AbstractTagFrameBody
{
    protected static final String TYPE_BODY = "body";


    /**
     * Frame Body Size, originally this is size as indicated in frame header
     * when we come to writing data we recalculate it.
     */
    private int size;



    /**
     * Create Empty Body. Super Constructor sets up Object list
     */
    protected AbstractID3v2FrameBody()
    {
    }

    /**
     * Create Body based on another body
     */
    protected AbstractID3v2FrameBody(AbstractID3v2FrameBody copyObject)
    {
        super(copyObject);
    }

    /**
     * Creates a new FrameBody datatype from file. The super
     * Constructor sets up the Object list for the frame.
     *
     * @param byteBuffer from where to read the frame body from
     */
    protected AbstractID3v2FrameBody(ByteBuffer byteBuffer, int frameSize)
        throws InvalidTagException
    {
        super();
        setSize(frameSize);
        this.read(byteBuffer);

    }

    /**
     * Return the ID3v2 Frame Identifier, must be implemented by concrete subclasses
     *
     * @return the frame identifier
     */
    public abstract String getIdentifier();


    /**
     * Return size of frame body,if framebody already exist will take this value from the frame header
     * but it is always recalculated before writing any changes back to disk.
     *
     * @return  size in bytes of this frame body
     */
    public int getSize()
    {
        return size;
    }

    /**
     * Set size based on size passed as parameter from frame header,
     * done before read
     */
    public void setSize(int size)
    {
        this.size = size;
    }

    /**
     * Set size based on size of the DataTypes making up the body,done after write
     */
    public void setSize()
    {
        size = 0;
        for (AbstractDataType object:objectList)
        {
            size += object.getSize();
        }
        ;
    }

    /**
     * Are two bodies equal
     *
     * @param obj
     */
    public boolean equals(Object obj)
    {
        if ((obj instanceof AbstractID3v2FrameBody) == false)
        {
            return false;
        }
        return super.equals(obj);
    }

    /**
     * This reads a frame body from a ByteBuffer into the appropriate FrameBody class and update the position of the
     * buffer to be just after the end of this framebody
     *
     * The ByteBuffer represents the tag and its position should be at the start of this framebody. The size as
     * indicated in the header is passed to the frame constructor when reading from file.
     *
     * @param byteBuffer file to read
     *
     * @throws InvalidFrameException if unable to construct a framebody from the ByteBuffer
     *
     *
     */
    public void read(ByteBuffer byteBuffer)
        throws InvalidTagException
    {
        int size = getSize();
        logger.info("Reading body for" + this.getIdentifier() + ":" + size);

        //Allocate a buffer to the size of the Frame Body and read from file
        byte[] buffer = new byte[size];
        byteBuffer.get(buffer);

        //Offset into buffer, incremented by length of previous Datatype
        //this offset is only used internally to decide where to look for the next
        //datatype within a framebody, it does not decide where to look for the next frame body
        int offset = 0;

        //Go through the ObjectList of the Frame reading the data into the
        //correct datatype.
        for (AbstractDataType object: objectList)
        {
            logger.finest("offset:"+offset);

            //The read has extended further than the defined frame size (ok to extend upto
            //size because the next datatype may be of length 0.)
            if (offset > (size))
            {
                logger.warning("Invalid Size for FrameBody");
                throw new InvalidFrameException("Invalid size for Frame Body");
            }

            //Try and load it with data from the Buffer
            //if it fails frame is invalid
            try
            {
                object.readByteArray(buffer, offset);
            }
            catch (InvalidDataTypeException e)
            {
                 logger.warning("Invalid DataType for Frame Body:"+e.getMessage());
                 throw new InvalidFrameException("Invalid DataType for Frame Body:"+e.getMessage());
            }
            //Increment Offset to start of next datatype.
            offset += object.getSize();
        }
    }

    /**
     * Write the contents of this datatype to the byte array
     *
     * @throws IOException on any I/O error
     */
    public void write(ByteArrayOutputStream tagBuffer)

    {
        logger.info("Writing frame body for" + this.getIdentifier() + ":Est Size:" + size);
        //Write the various fields to file in order
        for (AbstractDataType object:objectList)
        {
            byte[] objectData = object.writeByteArray();
            if (objectData != null)
            {
                try
                {
                    tagBuffer.write(objectData);
                }
                catch(IOException ioe)
                {
                    //This could never happen coz not writing to file, so convert to RuntimeException
                    throw new RuntimeException(ioe);
                }
            }
        }
        setSize();
        logger.info("Written frame body for" + this.getIdentifier() + ":Real Size:" + size);

    }

    /**
     * Return String Representation of Datatype     *
     */
    public void createStructure()
    {
        MP3File.getStructureFormatter().openHeadingElement(TYPE_BODY, "");
        for (AbstractDataType nextObject:objectList)
        {
            nextObject.createStructure();
        }
        MP3File.getStructureFormatter().closeHeadingElement(TYPE_BODY);
    }
}
