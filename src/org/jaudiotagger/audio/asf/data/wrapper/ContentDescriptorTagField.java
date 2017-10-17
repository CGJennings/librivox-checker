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
package org.jaudiotagger.audio.asf.data.wrapper;

import java.io.UnsupportedEncodingException;

import org.jaudiotagger.audio.asf.data.ContentDescriptor;
import org.jaudiotagger.tag.TagField;

/**
 * This class encapsulates a
 * {@link entagged.audioformats.asf.data.ContentDescriptor}and provides access
 * to it. <br>
 * The content descriptor used for construction is copied.
 * 
 * @author Christian Laireiter (liree)
 */
public class ContentDescriptorTagField implements TagField {

    /**
     * This descriptor is wrapped.
     */
    private ContentDescriptor toWrap;

    /**
     * Creates an instance.
     * 
     * @param source
     *                   The descriptor which should be represented as a
     *                   {@link TagField}.
     */
    public ContentDescriptorTagField(ContentDescriptor source) {
        this.toWrap = source.createCopy();
    }

    /**
     * (overridden)
     * 
     * @see entagged.audioformats.generic.TagField#copyContent(entagged.audioformats.generic.TagField)
     */
    public void copyContent(TagField field) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    /**
     * (overridden)
     * 
     * @see entagged.audioformats.generic.TagField#getId()
     */
    public String getId() {
        return toWrap.getName();
    }

    /**
     * (overridden)
     * 
     * @see entagged.audioformats.generic.TagField#getRawContent()
     */
    public byte[] getRawContent() throws UnsupportedEncodingException {
        return toWrap.getRawData();
    }

    /**
     * (overridden)
     * 
     * @see entagged.audioformats.generic.TagField#isBinary()
     */
    public boolean isBinary() {
        return toWrap.getType() == ContentDescriptor.TYPE_BINARY;
    }

    /**
     * (overridden)
     * 
     * @see entagged.audioformats.generic.TagField#isBinary(boolean)
     */
    public void isBinary(boolean b) {
        if (!b && isBinary()) {
            throw new UnsupportedOperationException("No conversion supported.");
        }
        toWrap.setBinaryValue(toWrap.getRawData());
    }

    /**
     * (overridden)
     * 
     * @see entagged.audioformats.generic.TagField#isCommon()
     */
    public boolean isCommon() {
        return toWrap.isCommon();
    }

    /**
     * (overridden)
     * 
     * @see entagged.audioformats.generic.TagField#isEmpty()
     */
    public boolean isEmpty() {
        return toWrap.isEmpty();
    }

    /**
     * (overridden)
     * 
     * @see entagged.audioformats.generic.TagField#toString()
     */
    public String toString() {
        return toWrap.getString();
    }

}