package org.jaudiotagger.tag.mp4.atom;

import org.jaudiotagger.audio.mp4.atom.Mp4BoxHeader;
import org.jaudiotagger.audio.mp4.atom.AbstractMp4Box;
import org.jaudiotagger.audio.generic.Utils;
import org.jaudiotagger.tag.mp4.field.Mp4FieldType;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.List;

/**
 * This box is used within both normal metadat boxes and ---- boxes to hold the actual data.
 *
 * <p>Format is as follows:
 * :length          (4 bytes)
 * :name 'Data'     (4 bytes)
 * :atom version    (1 byte)
 * :atom type flags (3 bytes)
 * :null field      (4 bytes)
 * :data
 */
public class Mp4DataBox extends AbstractMp4Box
{
    public static final String IDENTIFIER = "data";

    public static final int VERSION_LENGTH = 1;
    public static final int TYPE_LENGTH = 3;
    public static final int NULL_LENGTH = 4;
    public static final int PRE_DATA_LENGTH = VERSION_LENGTH + TYPE_LENGTH + NULL_LENGTH;
    public static final int DATA_HEADER_LENGTH = Mp4BoxHeader.HEADER_LENGTH + PRE_DATA_LENGTH;

    public static final int TYPE_POS = VERSION_LENGTH;

    //For use externally
    public static final int TYPE_POS_INCLUDING_HEADER = Mp4BoxHeader.HEADER_LENGTH + TYPE_POS;

    private int type;
    private String content;

    public static final int NUMBER_LENGTH = 2;

    //Holds the numbers decoded
    private List<Short> numbers;

    //Holds bytedata for byte fields as is not clear for multibyte fields exactly these should be wrttien
    private byte[] bytedata;

    /**
     * @param header     header info
     * @param dataBuffer data of box (doesnt include header data)
     */
    public Mp4DataBox(Mp4BoxHeader header, ByteBuffer dataBuffer)
    {
        this.header = header;
        //Double check
        if (!header.getId().equals(IDENTIFIER))
        {
            throw new RuntimeException("Unable to process data");
        }

        //Make slice so operations here don't effect position of main buffer
        this.dataBuffer = dataBuffer.slice();

        //Type
        type = Utils.getNumberBigEndian(this.dataBuffer,
                Mp4DataBox.TYPE_POS,
                Mp4DataBox.TYPE_POS + Mp4DataBox.TYPE_LENGTH - 1);

        if (type == Mp4FieldType.TEXT.getFileClassId())
        {
            content = Utils.getString(this.dataBuffer,
                    PRE_DATA_LENGTH,
                    header.getDataLength() - PRE_DATA_LENGTH,
                    header.getEncoding());
        }
        else if (type == Mp4FieldType.NUMERIC.getFileClassId())
        {
            numbers = new ArrayList<Short>();
                        
            for (int i = 0; i < ((header.getDataLength() - PRE_DATA_LENGTH) / NUMBER_LENGTH); i++)
            {
                short number = Utils.getShortNumberBigEndian(this.dataBuffer,
                        PRE_DATA_LENGTH + (i * NUMBER_LENGTH),
                        PRE_DATA_LENGTH + (i * NUMBER_LENGTH) + (NUMBER_LENGTH - 1));
                numbers.add(number);
            }

            //Make String representation  (separate values with slash)
            StringBuffer sb = new StringBuffer();
            ListIterator iterator = numbers.listIterator();
            while (iterator.hasNext())
            {
                sb.append(iterator.next());
                if (iterator.hasNext())
                {
                    sb.append("/");
                }
            }
            content = sb.toString();            
        }
        else if (type == Mp4FieldType.BYTE.getFileClassId())
        {
            //TODO byte data length seems to be 1 for pgap and cpil but 2 for tmpo ?
            //Create String representation for display
            content = Utils.getNumberBigEndian(this.dataBuffer,
                    PRE_DATA_LENGTH,
                    header.getDataLength() - 1) + "";

            //But store data for safer writng back to file
            bytedata = new byte[header.getDataLength() - PRE_DATA_LENGTH];
            int pos = dataBuffer.position();
            dataBuffer.position(pos + PRE_DATA_LENGTH);
            dataBuffer.get(bytedata);
            dataBuffer.position(pos);
        }
        else if (type == Mp4FieldType.COVERART_JPEG.getFileClassId())
        {
            content = Utils.getString(this.dataBuffer,
                    PRE_DATA_LENGTH,
                    header.getDataLength() - PRE_DATA_LENGTH,
                    header.getEncoding());
        }
    }

    public String getContent()
    {
        return content;
    }

    public int getType()
    {

        return type;
    }

    /**
     * Return numbers, only valid for numeric fields
     * @return numbers
     */
    //TODO this is only applicable for numeric databoxes, should we subclass dont know type until start
    //constructing and we also have Mp4tagTextNumericField class as well
    public List<Short> getNumbers()
    {
        return numbers;
    }

    /**

     * Return raw byte data only vaid for byte fields
     *
     * @return byte data
     */
    public byte[] getByteData()
    {
        return bytedata;
    }
}
