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
package org.jaudiotagger.audio.flac;

import org.jaudiotagger.audio.exceptions.*;
import org.jaudiotagger.audio.flac.metadatablock.*;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.flac.FlacTag;

import java.io.*;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.Logger;


/**
 * Write Flac Tag
 */
public class FlacTagWriter
{
    // Logger Object
    public static Logger logger = Logger.getLogger("org.jaudiotagger.audio.flac");

    private List<MetadataBlock>  metadataBlockPadding        = new ArrayList<MetadataBlock>(1);
    private List<MetadataBlock>  metadataBlockApplication    = new ArrayList<MetadataBlock>(1);
    private List<MetadataBlock>  metadataBlockSeekTable      = new ArrayList<MetadataBlock>(1);
    private List<MetadataBlock>  metadataBlockCueSheet       = new ArrayList<MetadataBlock>(1);

    private FlacTagCreator tc = new FlacTagCreator();
    private FlacTagReader reader = new FlacTagReader();

    /**
     * Delete Tag from file
     *
     * @param raf
     * @param tempRaf
     * @throws IOException
     * @throws CannotWriteException
     */
    public void delete(RandomAccessFile raf, RandomAccessFile tempRaf) throws IOException, CannotWriteException
    {
        //This will save the file without any Comment or PictureData blocks  
        FlacTag emptyTag = new FlacTag(null,new ArrayList<MetadataBlockDataPicture>() );
        raf.seek(0);
        tempRaf.seek(0);
        write(emptyTag, raf, tempRaf);
    }

    /**
     * Write tag to file
     *
     * @param tag
     * @param raf
     * @param rafTemp
     * @throws CannotWriteException
     * @throws IOException
     */
    public void write(Tag tag, RandomAccessFile raf, RandomAccessFile rafTemp) throws CannotWriteException, IOException
    {
        logger.info("Writing tag");

        //Clean up old data
        metadataBlockPadding.clear();
        metadataBlockApplication.clear();
        metadataBlockSeekTable.clear();
        metadataBlockCueSheet.clear();

        //Read existing data
        byte[] b = new byte[FlacStream.FLAC_STREAM_IDENTIFIER_LENGTH];
        raf.readFully(b);

        String flac = new String(b);
        if (!flac.equals(FlacStream.FLAC_STREAM_IDENTIFIER))
        {
            throw new CannotWriteException("This is not a FLAC file");
        }

        boolean isLastBlock = false;
        while (!isLastBlock)
        {
            MetadataBlockHeader mbh = MetadataBlockHeader.readHeader(raf);
            switch (mbh.getBlockType())
            {
                case VORBIS_COMMENT:
                case PADDING :
                case PICTURE:
                    {
                        //All these will be replaced by the new metadata so we just treat as padding in order
                        //to determine how much space is already allocated in the file
                        raf.seek(raf.getFilePointer() + mbh.getDataLength());
                        MetadataBlockData mbd = new MetadataBlockDataPadding(mbh.getDataLength());
                        metadataBlockPadding.add(new MetadataBlock(mbh, mbd));
                        break;
                    }
                case APPLICATION :
                    {
                        MetadataBlockData mbd = new MetadataBlockDataApplication(mbh,raf);
                        metadataBlockApplication.add(new MetadataBlock(mbh, mbd));
                        break;
                    }
                case SEEKTABLE :
                    {
                        MetadataBlockData mbd = new MetadataBlockDataSeekTable(mbh,raf);
                        metadataBlockSeekTable.add(new MetadataBlock(mbh, mbd));
                        break;
                    }
                case CUESHEET :
                    {
                        MetadataBlockData mbd = new MetadataBlockDataCueSheet(mbh,raf);
                        metadataBlockCueSheet.add(new MetadataBlock(mbh, mbd));
                        break;
                    }
                default :
                    {
                        //What are the consequences of doing this
                        raf.seek(raf.getFilePointer() + mbh.getDataLength());
                        break;
                    }
            }
            isLastBlock = mbh.isLastBlock();
        }

        //Number of bytes in the existing file available before audio data
        int availableRoom = computeAvailableRoom();

        //Minimum Size of the New tag data without padding         
        int newTagSize =  tc.convert(tag).limit();

        //Number of bytes required for new tagdata and other metadata blocks
        int neededRoom = newTagSize + computeNeededRoom();

        raf.seek(0);

        logger.info("Writing tag available bytes:"+availableRoom + ":needed bytes:"+neededRoom);

        //There is enough room to fit the tag without moving the audio just need to
        //adjust padding accordingly need to allow space for padding header if padding required
        if ((availableRoom == neededRoom)||(availableRoom > neededRoom + MetadataBlockHeader.HEADER_LENGTH))
        {
            //Jump over Flac and StreamInfoBlock
            raf.seek(FlacStream.FLAC_STREAM_IDENTIFIER_LENGTH
                    + MetadataBlockHeader.HEADER_LENGTH
                    + MetadataBlockDataStreamInfo.STREAM_INFO_DATA_LENGTH);

            //Write Application Blocks
            for (int i = 0; i < metadataBlockApplication.size(); i++)
            {
                raf.write(metadataBlockApplication.get(i).getHeader().getBytesWithoutIsLastBlockFlag());
                raf.write(metadataBlockApplication.get(i).getData().getBytes());
            }

            //Write Seek Table Blocks
            for (int i = 0; i < metadataBlockSeekTable.size(); i++)
            {
                raf.write(metadataBlockSeekTable.get(i).getHeader().getBytesWithoutIsLastBlockFlag());
                raf.write(metadataBlockSeekTable.get(i).getData().getBytes());
            }

            //Write Cue sheet Blocks
            for (int i = 0; i < metadataBlockCueSheet.size(); i++)
            {
                raf.write(metadataBlockCueSheet.get(i).getHeader().getBytesWithoutIsLastBlockFlag());
                raf.write(metadataBlockCueSheet.get(i).getData().getBytes());
            }

            //Write tag (and padding)
            raf.getChannel().write(tc.convert(tag, availableRoom - neededRoom));
        }
        else
        {
            //Read StreamInfoHeader into buffer
            FileChannel fc = raf.getChannel();
            b = new byte[FlacStream.FLAC_STREAM_IDENTIFIER_LENGTH
                    + MetadataBlockHeader.HEADER_LENGTH
                    + MetadataBlockDataStreamInfo.STREAM_INFO_DATA_LENGTH];
            raf.readFully(b);
            //Skip to start of Audio
            raf.seek(availableRoom  + FlacStream.FLAC_STREAM_IDENTIFIER_LENGTH
                                    + MetadataBlockHeader.HEADER_LENGTH
                                    + MetadataBlockDataStreamInfo.STREAM_INFO_DATA_LENGTH);

            FileChannel tempFC = rafTemp.getChannel();

            //Write FlacStream and StreamIfoMetablock to new file
            rafTemp.write(b);

            //Write all the metadatablocks
            for (int i = 0; i < metadataBlockApplication.size(); i++)
            {
                rafTemp.write(metadataBlockApplication.get(i).getHeader().getBytesWithoutIsLastBlockFlag());
                rafTemp.write(metadataBlockApplication.get(i).getData().getBytes());
            }

            for (int i = 0; i < metadataBlockSeekTable.size(); i++)
            {
                rafTemp.write(metadataBlockSeekTable.get(i).getHeader().getBytesWithoutIsLastBlockFlag());
                rafTemp.write(metadataBlockSeekTable.get(i).getData().getBytes());
            }

            for (int i = 0; i < metadataBlockCueSheet.size(); i++)
            {
                rafTemp.write(metadataBlockCueSheet.get(i).getHeader().getBytesWithoutIsLastBlockFlag());
                rafTemp.write(metadataBlockCueSheet.get(i).getData().getBytes());
            }

            //Write tag data use default padding
            rafTemp.write(tc.convert(tag, FlacTagCreator.DEFAULT_PADDING).array());

            //Write audio to new file
            tempFC.transferFrom(fc, tempFC.position(), fc.size());
        }
    }

    /**
     *
     * @return space currently availble for writing all Flac metadatablocks exceprt for StreamInfo which is fixed size
     */
    private int computeAvailableRoom()
    {
        int length = 0;

        for (int i = 0; i < metadataBlockApplication.size(); i++)
        {
            length += metadataBlockApplication.get(i).getLength();
        }

        for (int i = 0; i < metadataBlockSeekTable.size(); i++)
        {
            length += metadataBlockSeekTable.get(i).getLength();
        }

        for (int i = 0; i < metadataBlockCueSheet.size(); i++)
        {
            length += metadataBlockCueSheet.get(i).getLength();
        }

        for (int i = 0; i < metadataBlockPadding.size(); i++)
        {
            length +=  metadataBlockPadding.get(i).getLength();
        }

        return length;
    }

    /**
     *
     * @return space required to write the metadata blocks that are part of Flac but are not part of tagdata
     * in the normal sense.
     */
    private int computeNeededRoom()
    {
        int length = 0;

        for (int i = 0; i < metadataBlockApplication.size(); i++)
        {
            length +=  metadataBlockApplication.get(i).getLength();
        }

        for (int i = 0; i < metadataBlockSeekTable.size(); i++)
        {
            length +=  metadataBlockSeekTable.get(i).getLength();
        }

        for (int i = 0; i < metadataBlockCueSheet.size(); i++)
        {
            length += metadataBlockCueSheet.get(i).getLength();
        }

        return length;
    }
}

