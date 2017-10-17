/**
 *  @author : Paul Taylor
 *  @author : Eric Farng
 *
 *  Version @version:$Id: StringHashMap.java,v 1.9 2007/08/07 14:36:15 paultaylor Exp $
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
 *
 */
package org.jaudiotagger.tag.datatype;

import org.jaudiotagger.tag.id3.AbstractTagFrameBody;
import org.jaudiotagger.tag.id3.valuepair.Languages;
import org.jaudiotagger.tag.id3.valuepair.TextEncoding;

import java.util.*;


/**
 * Represents a String thats acts as a key into an enumeration of values. The String will be encoded
 * using the default encoding regardless of what encoding may be specified in the framebody
 */
public class StringHashMap extends StringFixedLength implements HashMapInterface
{

    /**
     * 
     */
    Map keyToValue = null;

    /**
     * 
     */
    Map valueToKey = null;

    /**
     * 
     */
    boolean hasEmptyValue = false;

    /**
     * Creates a new ObjectStringHashMap datatype.
     *
     * @param identifier
     * @param size
     * @throws IllegalArgumentException
     */
    public StringHashMap(String identifier, AbstractTagFrameBody frameBody, int size)
    {
        super(identifier, frameBody, size);

        if (identifier.equals(DataTypes.OBJ_LANGUAGE))
        {
            valueToKey = Languages.getInstanceOf().getValueToIdMap();
            keyToValue = Languages.getInstanceOf().getIdToValueMap();
        }
        else
        {
            throw new IllegalArgumentException("Hashmap identifier not defined in this class: " + identifier);
        }
    }

    public StringHashMap(StringHashMap copyObject)
    {
        super(copyObject);

        this.hasEmptyValue = copyObject.hasEmptyValue;
        this.keyToValue = copyObject.keyToValue;
        this.valueToKey = copyObject.valueToKey;
    }

    /**
     * 
     *
     * @return
     */
    public Map getKeyToValue()
    {
        return keyToValue;
    }

    /**
     * 
     *
     * @return
     */
    public Map getValueToKey()
    {
        return valueToKey;
    }

    /**
     * 
     *
     * @param value
     */
    public void setValue(Object value)
    {
        if (value instanceof String)
        {
            this.value = ((String) value).toLowerCase();
        }
        else
        {
            this.value = value;
        }
    }

    /**
     * 
     *
     * @param obj
     * @return
     */
    public boolean equals(Object obj)
    {
        if ((obj instanceof StringHashMap) == false)
        {
            return false;
        }

        StringHashMap object = (StringHashMap) obj;

        if (this.hasEmptyValue != object.hasEmptyValue)
        {
            return false;
        }

        if (this.keyToValue == null)
        {
            if (object.keyToValue != null)
            {
                return false;
            }
        }
        else
        {
            if (this.keyToValue.equals(object.keyToValue) == false)
            {
                return false;
            }
        }

        if (this.keyToValue == null)
        {
            if (object.keyToValue != null)
            {
                return false;
            }
        }
        else
        {
            if (this.valueToKey.equals(object.valueToKey) == false)
            {
                return false;
            }
        }

        return super.equals(obj);
    }

    /**
     * 
     *
     * @return
     */
    public Iterator iterator()
    {
        if (keyToValue == null)
        {
            return null;
        }
        else
        {
            // put them in a treeset first to sort them
            TreeSet treeSet = new TreeSet(keyToValue.values());

            if (hasEmptyValue)
            {
                treeSet.add("");
            }

            return treeSet.iterator();
        }
    }

    /**
     * 
     *
     * @return
     */
    public String toString()
    {
        if (value == null)
        {
            return "";
        }
        else if (keyToValue.get(value) == null)
        {
            return "";
        }
        else
        {
            return keyToValue.get(value).toString();
        }
    }

    /**
     *
     * @return the ISO_8859 encoding for Datatypes of this type
     */
     protected String  getTextEncodingCharSet()
    {
        return TextEncoding.CHARSET_ISO_8859_1;
    }
}
