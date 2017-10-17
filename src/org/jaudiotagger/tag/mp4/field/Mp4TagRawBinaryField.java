package org.jaudiotagger.tag.mp4.field;

import org.jaudiotagger.tag.mp4.Mp4TagField;
import org.jaudiotagger.tag.mp4.atom.Mp4DataBox;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.audio.mp4.atom.Mp4BoxHeader;
import org.jaudiotagger.audio.generic.Utils;

import java.nio.ByteBuffer;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Represents raw binary data
 *
 * <p>We use this when we find an atom under the ilst atom that we do not recognise , that does not
 * follow standard conventions in order to save the data without modification so it can be safetly
 * written back to file
 */
public class Mp4TagRawBinaryField extends Mp4TagField
{
    protected int    dataSize;
    protected byte[] dataBytes;


    /**
     * Construct binary field from rawdata of audio file
     *
     * @param header
     * @param raw
     * @throws java.io.UnsupportedEncodingException
     */
    public Mp4TagRawBinaryField(Mp4BoxHeader header, ByteBuffer raw) throws UnsupportedEncodingException
    {
        super(header.getId());
        dataSize=header.getDataLength();
        build(raw);
    }

    public Mp4FieldType getFieldType()
    {
        return Mp4FieldType.NUMERIC;
    }

    /**
     * Used when creating raw content
     *
     * @return
     * @throws java.io.UnsupportedEncodingException
     */
    protected byte[] getDataBytes() throws UnsupportedEncodingException
    {
        return dataBytes;
    }

    
    /**
     * Build from data
     *
     * <p>After returning buffers position will be after the end of this atom
     * @param raw
     */
    protected void build(ByteBuffer raw)
    {
        //Read the raw data into byte array
        this.dataBytes = new byte[dataSize];
        for (int i = 0; i < dataBytes.length; i++)
        {
            this.dataBytes[i] = raw.get();
       }      
    }

    public boolean isBinary()
    {
        return true;
    }

    public boolean isEmpty()
    {
        return this.dataBytes.length == 0;
    }

    public int getDataSize()
    {
        return dataSize;

    }
    public byte[] getData()
    {
        return this.dataBytes;
    }

    public void setData(byte[] d)
    {
        this.dataBytes = d;
    }

    public void copyContent(TagField field)
    {
        throw new UnsupportedOperationException("not done");
    }

    public byte[] getRawContent() throws UnsupportedEncodingException
      {
          logger.fine("Getting Raw data for:"+getId());
          try
          {
              ByteArrayOutputStream outerbaos = new ByteArrayOutputStream();
              outerbaos.write(Utils.getSizeBigEndian(Mp4BoxHeader.HEADER_LENGTH + dataSize));
              outerbaos.write(Utils.getDefaultBytes(getId(),"ISO-8859-1"));
              outerbaos.write(dataBytes);
              System.out.println("SIZE"+outerbaos.size());
              return outerbaos.toByteArray();
          }
          catch (IOException ioe)
          {
              //This should never happen as were not actually writing to/from a file
              throw new RuntimeException(ioe);
          }
      }

}
