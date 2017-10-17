/*
 * Entagged Audio Tag library
 * Copyright (c) 2004-2005 Christian Laireiter <liree@web.de>
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
package org.jaudiotagger.audio.asf.io;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;

import org.jaudiotagger.audio.asf.data.Chunk;
import org.jaudiotagger.audio.asf.data.EncodingChunk;
import org.jaudiotagger.audio.asf.data.GUID;
import org.jaudiotagger.audio.asf.util.Utils;

/**
 * This class reads the chunk containing encoding data <br>
 * <b>Warning:<b><br>
 * Implementation is not completed. More analysis of this chunk is needed.
 * 
 * @author Christian Laireiter
 */
public class EncodingChunkReader {

	/**
	 * This reads the current data and interprets it as an encoding chunk. <br>
	 * <b>Warning:<b><br>
	 * Implementation is not completed. More analysis of this chunk is needed.
	 * 
	 * @param raf
	 *                  Input source
	 * @param candidate
	 *                  Chunk which possibly contains encoding data.
	 * @return Encoding info. <code>null</code> if its not a valid encoding
	 *              chunk. <br>
	 * @throws IOException
	 *                   read errors.
	 */
	public static EncodingChunk read(RandomAccessFile raf, Chunk candidate)
			throws IOException {
		if (raf == null || candidate == null) {
			throw new IllegalArgumentException("Arguments must not be null.");
		}
		if (GUID.GUID_ENCODING.equals(candidate.getGuid())) {
			raf.seek(candidate.getPosition());
			return new EncodingChunkReader().parseData(raf);
		}
		return null;
	}

	/**
	 * Should not be used for now.
	 *  
	 */
	protected EncodingChunkReader() {
		// NOTHING toDo
	}

	/**
	 * see {@link #read(RandomAccessFile, Chunk)}
	 * 
	 * @param raf
	 *                  input source.
	 * @return Enconding info. <code>null</code> if its not a valid encoding
	 *              chunk. <br>
	 * @throws IOException
	 *                   read errors.
	 */
	private EncodingChunk parseData(RandomAccessFile raf) throws IOException {
		EncodingChunk result = null;
		long chunkStart = raf.getFilePointer();
		GUID guid = Utils.readGUID(raf);
		if (GUID.GUID_ENCODING.equals(guid)) {
			BigInteger chunkLen = Utils.readBig64(raf);
			result = new EncodingChunk(chunkStart, chunkLen);

			// Can't be interpreted
			/*
			 * What do I think of this data, well it seems to be another GUID.
			 * Then followed by a UINT16 indicating a length of data following
			 * (by half). My test files just had the length of one and a two
			 * bytes zero.
			 */
			raf.skipBytes(20);

			/*
			 * Read the number of strings which will follow
			 */
			int stringCount = Utils.readUINT16(raf);

			/*
			 * Now reading the specified amount of strings.
			 */
			for (int i = 0; i < stringCount; i++) {
				result.addString(Utils.readCharacterSizedString(raf));
			}
		}
		return result;
	}

}