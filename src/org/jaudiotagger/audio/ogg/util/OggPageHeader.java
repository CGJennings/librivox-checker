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
package org.jaudiotagger.audio.ogg.util;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.generic.Utils;

import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;


/**
 * $Id: OggPageHeader.java,v 1.11 2007/10/11 21:36:08 paultaylor Exp $
 *
 * reference:http://xiph.org/ogg/doc/framing.html
 *
 * @author Raphael Slinckx (KiKiDonK)
 * @version 16 d�cembre 2003
 */
public class OggPageHeader
{
    // Logger Object
       public static Logger logger = Logger.getLogger("org.jaudiotagger.audio.ogg.atom");

    //Capture pattern at start of header
    public static final byte[] CAPTURE_PATTERN = {'O', 'g', 'g','S'};

    //Ogg Page header is always 27 bytes plus the size of the segment table which is variable
    public static final int OGG_PAGE_HEADER_FIXED_LENGTH = 27;

    //Can have upto 255 segments in a page
    public static final int MAXIMUM_NO_OF_SEGMENT_SIZE = 255;

    //Each segmets can be upto 255 bytes
    public static final int MAXIMUM_SEGMENT_SIZE = 255;

    //Maximum size of pageheader (27 + 255 = 282)
    public static final int MAXIMUM_PAGE_HEADER_SIZE = OGG_PAGE_HEADER_FIXED_LENGTH + MAXIMUM_NO_OF_SEGMENT_SIZE;

    //Maximum size of page data following the page header (255 * 255 = 65025)
    public static final int MAXIMUM_PAGE_DATA_SIZE = MAXIMUM_NO_OF_SEGMENT_SIZE * MAXIMUM_SEGMENT_SIZE;

    //Maximum size of page includes header and data (282 + 65025 = 65307 bytes)
    public static final int MAXIMUM_PAGE_SIZE = MAXIMUM_PAGE_HEADER_SIZE + MAXIMUM_PAGE_DATA_SIZE;

    //Starting positions of the various attributes
    public static final int FIELD_CAPTURE_PATTERN_POS  = 0;
    public static final int FIELD_STREAM_STRUCTURE_VERSION_POS  = 4;
    public static final int FIELD_HEADER_TYPE_FLAG_POS  = 5;
    public static final int FIELD_ABSOLUTE_GRANULE_POS  = 6;
    public static final int FIELD_STREAM_SERIAL_NO_POS  = 14;
    public static final int FIELD_PAGE_SEQUENCE_NO_POS  = 18;
    public static final int FIELD_PAGE_CHECKSUM_POS     = 22;
    public static final int FIELD_PAGE_SEGMENTS_POS     = 26;
    public static final int FIELD_SEGMENT_TABLE_POS     = 27;

    //Length of various attributes
    public static final int FIELD_CAPTURE_PATTERN_LENGTH  = 4;
    public static final int FIELD_STREAM_STRUCTURE_VERSION_LENGTH  = 1;
    public static final int FIELD_HEADER_TYPE_FLAG_LENGTH  = 1;
    public static final int FIELD_ABSOLUTE_GRANULE_LENGTH  = 8;
    public static final int FIELD_STREAM_SERIAL_NO_LENGTH  = 4;
    public static final int FIELD_PAGE_SEQUENCE_NO_LENGTH  = 4;
    public static final int FIELD_PAGE_CHECKSUM_LENGTH     = 4;
    public static final int FIELD_PAGE_SEGMENTS_LENGTH     = 1;

    private byte[] rawHeaderData;
    private double absoluteGranulePosition;
    private int checksum;
    private byte headerTypeFlag;

    private boolean isValid = false;
    private int pageLength = 0;
    private int pageSequenceNumber, streamSerialNumber;
    private byte[] segmentTable;

    private List<PacketStartAndLength> packetList = new ArrayList<PacketStartAndLength>();
    private boolean lastPacketIncomplete = false;


    public static OggPageHeader read (RandomAccessFile raf) throws IOException,CannotReadException
    {
        long start = raf.getFilePointer();
        logger.info("Trying to read OggPage at:"+start);

        byte[] b = new byte[OggPageHeader.CAPTURE_PATTERN.length];
        raf.read(b);
        if (!(Arrays.equals(b, OggPageHeader.CAPTURE_PATTERN)))
        {
            throw new CannotReadException("OggS Header could not be found, not an ogg stream:"+new String(b));
        }

        raf.seek(start + OggPageHeader.FIELD_PAGE_SEGMENTS_POS);
        int pageSegments = raf.readByte() & 0xFF; //unsigned
        raf.seek(start);

        b = new byte[OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + pageSegments];
        raf.read(b);


        OggPageHeader pageHeader = new OggPageHeader(b);

        //Now just after PageHeader, ready for Packet Data
        return pageHeader;
    }

    public OggPageHeader(byte[] b)
    {
        this.rawHeaderData = b;
        int streamStructureRevision = b[FIELD_STREAM_STRUCTURE_VERSION_POS];
        headerTypeFlag = b[FIELD_HEADER_TYPE_FLAG_POS];
        if (streamStructureRevision == 0)
        {
            this.absoluteGranulePosition = 0;
            for (int i = 0; i < FIELD_ABSOLUTE_GRANULE_LENGTH; i++)
            {
                this.absoluteGranulePosition += u(b[i + FIELD_ABSOLUTE_GRANULE_POS]) * Math.pow(2, 8 * i);
            }

            streamSerialNumber = Utils.getNumberLittleEndian(b,FIELD_STREAM_SERIAL_NO_POS,17);
            pageSequenceNumber = Utils.getNumberLittleEndian(b,FIELD_PAGE_SEQUENCE_NO_POS,21);
            checksum =Utils.getNumberLittleEndian(b,FIELD_PAGE_CHECKSUM_POS,25);
            int pageSegments = u( b[FIELD_PAGE_SEGMENTS_POS  ] );

            this.segmentTable = new byte[b.length - OGG_PAGE_HEADER_FIXED_LENGTH];
            int packetLength        = 0;
            Integer segmentLength   = null;
            for (int i = 0; i < segmentTable.length; i++)
            {
                segmentTable[i] = b[OGG_PAGE_HEADER_FIXED_LENGTH + i];
                segmentLength = u(segmentTable[i]);
                this.pageLength += segmentLength;
                packetLength+=segmentLength;

                if(segmentLength<MAXIMUM_SEGMENT_SIZE)
                {
                    packetList.add(new PacketStartAndLength(pageLength-packetLength,packetLength));
                    packetLength=0;
                }
            }

            //If last segment value is 255 this packet continues onto next page
            //and wont have been added to the packetStartAndEnd list yet
            if(segmentLength== MAXIMUM_SEGMENT_SIZE)
            {
                packetList.add(new PacketStartAndLength(pageLength-packetLength,packetLength));
                lastPacketIncomplete=true;
            }
            isValid = true;
        }

        logger.info("Constructed OggPage:"+this.toString());
    }

    private int u(int i)
    {
        return i & 0xFF;
    }

    /**
     *
     * @return true if the last packet on this page extends to the next page
     */
    public boolean isLastPacketIncomplete()
    {
        return lastPacketIncomplete;
    }

    public double getAbsoluteGranulePosition()
    {
        logger.fine("Number Of Samples: "+absoluteGranulePosition);
        return this.absoluteGranulePosition;
    }


    public int getCheckSum()
    {
        return checksum;
    }


    public byte getHeaderType()
    {
        return headerTypeFlag;
    }


    public int getPageLength()
    {
        logger.fine("This page length: "+pageLength);
        return this.pageLength;
    }

    public int getPageSequence()
    {
        return pageSequenceNumber;
    }

    public int getSerialNumber()
    {
        return streamSerialNumber;
    }

    public byte[] getSegmentTable()
    {
        return this.segmentTable;
    }

    public boolean isValid()
    {
        return isValid;
    }

    /**
     *
     * @return a list of packet start position and size within this page.
     */
    public List<PacketStartAndLength> getPacketList()
    {
        return packetList;
    }

    /**
     *
     * @return the raw header data that this pageheader is derived from
     */
    public byte[] getRawHeaderData()
    {
        return rawHeaderData;
    }

    public String toString()
    {
        String out = "Ogg Page Header:isvalid:"+isValid
            +":type:"+headerTypeFlag
            +":oggpageheaderlength:"+rawHeaderData.length
            +":length:"+pageLength
            +":seqno:"+getPageSequence()
            +":packetincomplete:"+isLastPacketIncomplete()
            +":sernum:"+this.getSerialNumber();

        for(PacketStartAndLength packet:getPacketList())
        {
            out+=packet.toString();
        }
        return out;
    }

    /**
     * Within the page specifies the start and length of each packet
     * in the page offset from the end of the pageheader (after the segment table)
     */
    public static class PacketStartAndLength
    {
        private Integer startPosition = 0;
        private Integer length   = 0;

        public PacketStartAndLength(int startPosition,int length)
        {
            this.startPosition=startPosition;
            this.length =length;
        }

        public int getStartPosition()
        {
            return startPosition;
        }

        public void setStartPosition(int startPosition)
        {
            this.startPosition = startPosition;
        }

        public int getLength()
        {
            return length;
        }

        public void setLength(int length)
        {
            this.length = length;
        }

        public String toString()
        {
            return "start:"+startPosition+":length:"+length;
        }
    }

    /** This represents all the flags that can be set in the headerType field.
     *  Note these values can be ORED together. For example the last packet in
     *  a file would normally have a value of 0x5 because both the CONTINUED_PACKET
     *  bit and the END_OF_BITSTREAM bit would be set.
     */
    public static enum HeaderTypeFlag
    {
        FRESH_PACKET((byte)0x0),
        CONTINUED_PACKET((byte)0x1),
        START_OF_BITSTREAM((byte)0x2),
        END_OF_BITSTREAM((byte)0x4);

        byte fileValue;
        HeaderTypeFlag(byte fileValue)
        {
            this.fileValue=fileValue;
        }

        /**
         *
         * @return the value that should be written to file to enable this flag
         */
        public byte getFileValue()
        {
            return fileValue;
        }
    }
}

