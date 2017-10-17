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
package org.jaudiotagger.audio.asf.util;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Iterator;

import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.audio.asf.data.AsfHeader;
import org.jaudiotagger.audio.asf.data.ContentDescription;
import org.jaudiotagger.audio.asf.data.ContentDescriptor;
import org.jaudiotagger.audio.asf.data.ExtendedContentDescription;
import org.jaudiotagger.audio.asf.data.wrapper.ContentDescriptorTagField;
import org.jaudiotagger.audio.generic.GenericTag;
import org.jaudiotagger.tag.TagTextField;

/**
 * This class provides functionality to convert
 * {@link entagged.audioformats.asf.data.AsfHeader}objects into
 * {@link entagged.audioformats.Tag}objects (More extract information and
 * create a {@link entagged.audioformats.generic.GenericTag}).<br>
 * 
 * 
 * @author Christian Laireiter (liree)
 */
public class TagConverter {

	/**
	 * This method assigns those tags of <code>tag</code> which are defined to
	 * be common by entagged. <br>
	 * 
	 * @param tag
	 *            The tag from which the values are gathered. <br>
	 *            Assigned values are: <br>
	 * @see Tag#getAlbum() <br>
	 * @see Tag#getTrack() <br>
	 * @see Tag#getYear() <br>
	 * @see Tag#getGenre() <br>
	 * @param description
	 *            The extended content description which should recieve the
	 *            values. <br>
	 *            <b>Warning: </b> the common values will be replaced.
	 */
	public static void assignCommonTagValues(Tag tag,
			ExtendedContentDescription description) {
		ContentDescriptor tmp = null;
		if (tag.getFirstAlbum() != null && tag.getFirstAlbum().length() > 0) {
			tmp = new ContentDescriptor(ContentDescriptor.ID_ALBUM,
					ContentDescriptor.TYPE_STRING);
			tmp.setStringValue(tag.getFirstAlbum());
			description.addOrReplace(tmp);
		} else {
			description.remove(ContentDescriptor.ID_ALBUM);
		}
		if (tag.getFirstTrack() != null && tag.getFirstTrack().length() > 0) {
			tmp = new ContentDescriptor(ContentDescriptor.ID_TRACKNUMBER,
					ContentDescriptor.TYPE_STRING);
			tmp.setStringValue(tag.getFirstTrack());
			description.addOrReplace(tmp);
		} else {
			description.remove(ContentDescriptor.ID_TRACKNUMBER);
		}
		if (tag.getFirstYear() != null && tag.getFirstYear().length() > 0) {
			tmp = new ContentDescriptor(ContentDescriptor.ID_YEAR,
					ContentDescriptor.TYPE_STRING);
			tmp.setStringValue(tag.getFirstYear());
			description.addOrReplace(tmp);
		} else {
			description.remove(ContentDescriptor.ID_YEAR);
		}
		if (tag.getFirstGenre() != null && tag.getFirstGenre().length() > 0) {
			tmp = new ContentDescriptor(ContentDescriptor.ID_GENRE,
					ContentDescriptor.TYPE_STRING);
			tmp.setStringValue(tag.getFirstGenre());
			description.addOrReplace(tmp);
			int index = Arrays.asList(ExtendedContentDescription.DEFAULT_GENRES).indexOf(
					tag.getFirstGenre());
			if (index != -1) {
				tmp = new ContentDescriptor(ContentDescriptor.ID_GENREID,
						ContentDescriptor.TYPE_STRING);
				tmp.setStringValue("(" + index + ")");
				description.addOrReplace(tmp);
			} else {
				description.remove(ContentDescriptor.ID_GENREID);
			}
		} else {
			description.remove(ContentDescriptor.ID_GENRE);
			description.remove(ContentDescriptor.ID_GENREID);
		}
	}

	/**
	 * This method will add or replace all values of tag are not defined as
	 * common by entagged.
	 * 
	 * @param tag
	 *            The tag containing the values.
	 * @param descriptor
	 *            the extended content description.
	 */
	public static void assignOptionalTagValues(Tag tag,
			ExtendedContentDescription descriptor) {
		Iterator it = tag.getFields();
		ContentDescriptor tmp = null;
		while (it.hasNext()) {
			try {
				TagField currentField = (TagField) it.next();
				if (!currentField.isCommon()) {
					tmp = new ContentDescriptor(currentField.getId(),
							ContentDescriptor.TYPE_STRING);
					if (currentField.isBinary()) {
						tmp.setBinaryValue(currentField.getRawContent());
					} else {
						tmp.setStringValue(currentField.toString());
					}
					descriptor.addOrReplace(tmp);
				}
			} catch (UnsupportedEncodingException uee) {
				uee.printStackTrace();
			}
		}
	}

	/**
	 * This method creates a new {@link ContentDescription}object, filled with
	 * the according values of the given <code>tag</code>.<br>
	 * <b>Warning </b>: <br>
	 * Only the first values can be stored in asf files, because the content
	 * description is limited.
	 * 
	 * @param tag
	 *            The tag from which the values are taken. <br>
	 * @see Tag#getFirstArtist() <br>
	 * @see Tag#getFirstTitle() <br>
	 * @see Tag#getFirstComment() <br>
	 * 
	 * @return A new content description object filled with <code>tag</code>.
	 */
	public static ContentDescription createContentDescription(Tag tag) {
		ContentDescription result = new ContentDescription();
		result.setAuthor(tag.getFirstArtist());
		result.setTitle(tag.getFirstTitle());
		result.setComment(tag.getFirstComment());
		TagTextField cpField = AsfCopyrightField.getCopyright(tag);
		if (cpField != null) {
			result.setCopyRight(cpField.getContent());
		}
		return result;
	}

	/**
	 * This method creates a new {@link ExtendedContentDescription}object
	 * filled with the values of the given <code>tag</code>.<br>
	 * Since extended content description of asf files can store name-value
	 * pairs, nearly each {@link entagged.audioformats.generic.TagField}can be
	 * stored whithin. <br>
	 * One constraint is that the strings must be convertable to "UTF-16LE"
	 * encoding and don't exceed a length of 65533 in binary representation.
	 * <br>
	 * 
	 * @param tag
	 *            The tag whose values the result will be filled with.
	 * @return A new extended content description object.
	 */
	public static ExtendedContentDescription createExtendedContentDescription(
			Tag tag) {
		ExtendedContentDescription result = new ExtendedContentDescription();
		assignCommonTagValues(tag, result);
		return result;
	}

	/**
	 * This method creates a {@link Tag}and fills it with the contents of the
	 * given {@link AsfHeader}.<br>
	 * 
	 * @param source
	 *            The asf header which contains the information. <br>
	 * @return A Tag with all its values.
	 */
	public static Tag createTagOf(AsfHeader source) {
		GenericTag result = new GenericTag();
		/*
		 * It is possible that the file contains no content description, since
		 * that some informations aren't available.
		 */
		if (source.getContentDescription() != null) {
			result.setArtist(source.getContentDescription().getAuthor());
			result.setComment(source.getContentDescription().getComment());
			result.setTitle(source.getContentDescription().getTitle());
			AsfCopyrightField cpField = new AsfCopyrightField();
			/*
			 * I know I said use the setString() method. However, the value is
			 * already a "UTF-16LE" string within is bounds. So Nothing could
			 * happen.
			 */
			cpField.setContent(source.getContentDescription().getCopyRight());
			result.set(cpField);
		}
		/*
		 * It is possible that the file contains no extended content
		 * description. In that case some informations cannot be provided.
		 */
		if (source.getExtendedContentDescription() != null) {
			//result.setTrack(source.getExtendedContentDescription().getTrack());
			result.setYear(source.getExtendedContentDescription().getYear());
			result.setGenre(source.getExtendedContentDescription().getGenre());
			result.setAlbum(source.getExtendedContentDescription().getAlbum());

			/*
			 * Now any properties, which don't belong to the common section of
			 * entagged.
			 */
			ExtendedContentDescription extDesc = source
					.getExtendedContentDescription();
			Iterator it = extDesc.getDescriptors().iterator();
			while (it.hasNext()) {
				ContentDescriptor current = (ContentDescriptor) it.next();
				// If common, it has been added to the result some lines upward.
				if (!current.isCommon()) {
					result.add(new ContentDescriptorTagField(current));
				}
			}
		}
		return result;
	}
}