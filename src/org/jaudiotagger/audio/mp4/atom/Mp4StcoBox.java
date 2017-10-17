package org.jaudiotagger.audio.mp4.atom;

import org.jaudiotagger.audio.generic.Utils;
import org.jaudiotagger.audio.mp4.Mp4AudioHeader;
import org.jaudiotagger.audio.mp4.Mp4NotMetaFieldKey;
import org.jaudiotagger.audio.exceptions.CannotReadException;

import java.nio.ByteBuffer;
import java.io.RandomAccessFile;
import java.io.IOException;

/**
 * StcoBox ( media (stream) header), holds offsets into the Audio data
 */
public class Mp4StcoBox extends AbstractMp4Box
{
    public static final int VERSION_FLAG_POS         = 0;
    public static final int OTHER_FLAG_POS           = 1;
    public static final int NO_OF_OFFSETS_POS        = 4;


    public static final int VERSION_FLAG_LENGTH         = 1;
    public static final int OTHER_FLAG_LENGTH           = 3;
    public static final int NO_OF_OFFSETS_LENGTH        = 4;
    public static final int OFFSET_LENGTH               = 4;
    private int noOfOffSets = 0;
    private int firstOffSet;
    /**
     * Construct box from data and show contents
     *
     * @param header header info
     * @param originalDataBuffer data of box (doesnt include header data)
     */
    public Mp4StcoBox(Mp4BoxHeader header, ByteBuffer originalDataBuffer)
    {
        this.header  = header;

        //Make a slice of databuffer then we can work with relative or absolute methods safetly
        this.dataBuffer =  originalDataBuffer.slice();

        //Skip the flags
        dataBuffer.position(dataBuffer.position() + VERSION_FLAG_LENGTH  +  OTHER_FLAG_LENGTH);

        //No of offsets
        this.noOfOffSets = Utils.getNumberBigEndian(dataBuffer,
            dataBuffer.position(),
            (dataBuffer.position()  + NO_OF_OFFSETS_LENGTH  - 1));
        dataBuffer.position(dataBuffer.position() + NO_OF_OFFSETS_LENGTH );

        //First Offset, useful for sanity checks
        firstOffSet = Utils.getNumberBigEndian(dataBuffer,
                         dataBuffer.position(),
                        (dataBuffer.position()  + OFFSET_LENGTH  - 1));
    }

    /**
     * Show All offsets, uyseful for debugging
     */
    public void printAlloffsets()
    {
        dataBuffer.rewind();
        dataBuffer.position(VERSION_FLAG_LENGTH  +  OTHER_FLAG_LENGTH + NO_OF_OFFSETS_LENGTH);
        for(int i=0;i<noOfOffSets -1;i++)
        {
            int offset = Utils.getNumberBigEndian(dataBuffer,
                         dataBuffer.position(),
                        (dataBuffer.position()  + OFFSET_LENGTH    - 1));
            System.out.println("offset into audio data is:"+offset);
            dataBuffer.position(dataBuffer.position() + OFFSET_LENGTH);
        }
        int offset = Utils.getNumberBigEndian(dataBuffer,
                         dataBuffer.position(),
                        (dataBuffer.position()  + OFFSET_LENGTH    - 1));
    }

     /**
     * Construct box from data and adjust offets accordingly
     *
     * @param header header info
     * @param originalDataBuffer data of box (doesnt include header data)
     */
    public Mp4StcoBox(Mp4BoxHeader header, ByteBuffer originalDataBuffer,int adjustment)
    {
        this.header  = header;

        //Make a slice of databuffer then we can work with relative or absolute methods safetly
        this.dataBuffer =  originalDataBuffer.slice();

        //Skip the flags
        dataBuffer.position(dataBuffer.position() + VERSION_FLAG_LENGTH  +  OTHER_FLAG_LENGTH);

        //No of offsets
        this.noOfOffSets = Utils.getNumberBigEndian(dataBuffer,
            dataBuffer.position(),
            (dataBuffer.position()  + NO_OF_OFFSETS_LENGTH  - 1));
        dataBuffer.position(dataBuffer.position() + NO_OF_OFFSETS_LENGTH );
              
        for(int i=0;i<noOfOffSets;i++)
        {
            int offset = Utils.getNumberBigEndian(dataBuffer,
                         dataBuffer.position(),
                        (dataBuffer.position()  + NO_OF_OFFSETS_LENGTH  - 1));

            //Calculate new offset and update buffer
            offset = offset + adjustment;
            dataBuffer.put(Utils.getSizeBigEndian(offset));
        }
    }

    /**
     * The number of offsets
     *
     * @return
     */
    public int getNoOfOffSets()
    {
        return noOfOffSets;
    }

    /**
     * The value of the first offset
     *
     * @return
     */
    public int getFirstOffSet()
    {
        return firstOffSet;
    }

    public static void debugShowStcoInfo(RandomAccessFile raf) throws IOException,CannotReadException
    {
        Mp4BoxHeader moovHeader = Mp4BoxHeader.seekWithinLevel(raf, Mp4NotMetaFieldKey.MOOV.getFieldName());
        if(moovHeader==null)
        {
            throw new CannotReadException("This file does not appear to be an audio file");
        }
        ByteBuffer   moovBuffer = ByteBuffer.allocate(moovHeader.getLength() - Mp4BoxHeader.HEADER_LENGTH);
        raf.getChannel().read(moovBuffer);
        moovBuffer.rewind();

        //Level 2-Searching for "mvhd" somewhere within "moov", we make a slice after finding header
        //so all get() methods will be relative to mvdh positions
        Mp4BoxHeader boxHeader = Mp4BoxHeader.seekWithinLevel(moovBuffer,Mp4NotMetaFieldKey.MVHD.getFieldName());
        if(boxHeader==null)
        {
            throw new CannotReadException("This file does not appear to be an audio file");
        }
        ByteBuffer mvhdBuffer = moovBuffer.slice();
        Mp4MvhdBox mvhd = new Mp4MvhdBox(boxHeader,mvhdBuffer);
        mvhdBuffer.position(mvhdBuffer.position()+boxHeader.getDataLength());

        //Level 2-Searching for "trak" within "moov"
        boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer,Mp4NotMetaFieldKey.TRAK.getFieldName());
        int endOfFirstTrackInBuffer = mvhdBuffer.position() + boxHeader.getDataLength();

        if(boxHeader==null)
        {
            throw new CannotReadException("This file does not appear to be an audio file");
        }
        //Level 3-Searching for "mdia" within "trak"
        boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer,Mp4NotMetaFieldKey.MDIA.getFieldName());
        if(boxHeader==null)
        {
            throw new CannotReadException("This file does not appear to be an audio file");
        }

        //Level 4-Searching for "mdhd" within "mdia"
       boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer,Mp4NotMetaFieldKey.MDHD.getFieldName());
       if(boxHeader==null)
       {
           throw new CannotReadException("This file does not appear to be an audio file");
       }

       //Level 4-Searching for "minf" within "mdia"
       mvhdBuffer.position(mvhdBuffer.position() + boxHeader.getDataLength());
       boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer,Mp4NotMetaFieldKey.MINF.getFieldName());
       if(boxHeader==null)
       {
           throw new CannotReadException("This file does not appear to be an audio file");
       }

       //Level 5-Searching for "smhd" within "minf"
       //Only an audio track would have a smhd frame
       boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer,Mp4NotMetaFieldKey.SMHD.getFieldName());
       if(boxHeader==null)
       {
           throw new CannotReadException("This file does not appear to be an audio file");
       }
       mvhdBuffer.position(mvhdBuffer.position() + boxHeader.getDataLength());

       //Level 5-Searching for "stbl within "minf"
       boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer,Mp4NotMetaFieldKey.STBL.getFieldName());
       if(boxHeader==null)
       {
           throw new CannotReadException("This file does not appear to be an audio file");
       }

       //Level 6-Searching for "stco within "stbl"
       boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer,Mp4NotMetaFieldKey.STCO.getFieldName());
       if(boxHeader==null)
       {
           throw new CannotReadException("This file does not appear to be an audio file");
       }
        Mp4StcoBox stco = new Mp4StcoBox(boxHeader,mvhdBuffer);
        stco.printAlloffsets();
    }
}
