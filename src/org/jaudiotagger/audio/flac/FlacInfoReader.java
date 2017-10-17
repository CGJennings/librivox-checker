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

import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.jaudiotagger.audio.exceptions.*;
import org.jaudiotagger.audio.flac.metadatablock.MetadataBlockDataStreamInfo;
import org.jaudiotagger.audio.flac.metadatablock.MetadataBlockHeader;
import org.jaudiotagger.audio.flac.metadatablock.BlockType;

import java.io.*;
import java.util.logging.Logger;

/**
 * Read info from Flac file
 */
public class FlacInfoReader
{
    // Logger Object
    public static Logger logger = Logger.getLogger("org.jaudiotagger.audio.flac");

    private static final int NO_OF_BITS_IN_BYTE = 8;
    private static final int KILOBYTES_TO_BYTES_MULTIPLIER = 1000;

    public GenericAudioHeader read(RandomAccessFile raf) throws CannotReadException, IOException
    {
        FlacStream.findStream(raf);

        MetadataBlockDataStreamInfo mbdsi = null;
        boolean isLastBlock = false;

        //Search for StreamInfo Block, but even after we found it we still have to continue thorugh all
        //the metadata blocks so that we can find the start of the audio frames which we need to calculate
        //the bitrate
        while (!isLastBlock)
        {
            MetadataBlockHeader mbh = MetadataBlockHeader.readHeader(raf);
            if (mbh.getBlockType() == BlockType.STREAMINFO)
            {
                mbdsi = new MetadataBlockDataStreamInfo(mbh,raf);
                if (!mbdsi.isValid())
                {
                    throw new CannotReadException("FLAC StreamInfo not valid");
                }               
            }
            else
            {
                raf.seek(raf.getFilePointer() + mbh.getDataLength());
            }

            isLastBlock = mbh.isLastBlock();
            mbh = null; //Free memory
        }

        if (mbdsi==null)
        {
            throw new CannotReadException("Unable to find Flac StreamInfo");
        }

        GenericAudioHeader info = new GenericAudioHeader();
		info.setLength(mbdsi.getLength());
        info.setPreciseLength(mbdsi.getPreciseLength());
        info.setChannelNumber(mbdsi.getChannelNumber());
        info.setSamplingRate(mbdsi.getSamplingRatePerChannel());
        info.setEncodingType(mbdsi.getEncodingType());
        info.setExtraEncodingInfos("");
        info.setBitrate(computeBitrate(mbdsi.getPreciseLength(), raf.length() -raf.getFilePointer()));

        return info;
    }

    private int computeBitrate(float length, long size)
    {         
        return (int) ((size / KILOBYTES_TO_BYTES_MULTIPLIER) * NO_OF_BITS_IN_BYTE / length);
    }

    /**
     * Count the number of metadatablocks, useful for debugging
     *
     * @param f
     * @return
     * @throws CannotReadException
     * @throws IOException
     */
    public int countMetaBlocks(File f)throws CannotReadException, IOException
    {
        RandomAccessFile raf = new RandomAccessFile(f,"r");

        FlacStream.findStream(raf);

        boolean isLastBlock = false;
              
        int count = 0;
        while (!isLastBlock)
        {
            MetadataBlockHeader mbh = MetadataBlockHeader.readHeader(raf);
            logger.info("Found block:"+mbh.getBlockType());
            raf.seek(raf.getFilePointer() + mbh.getDataLength());
            isLastBlock = mbh.isLastBlock();
            mbh = null; //Free memory
            count ++;
        }
        raf.close();
        return count;
    }
}
