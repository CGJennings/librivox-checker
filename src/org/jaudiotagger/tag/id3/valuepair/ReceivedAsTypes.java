/**
 * @author : Paul Taylor
 * <p/>
 * Version @version:$Id: ReceivedAsTypes.java,v 1.3 2007/08/06 16:04:36 paultaylor Exp $
 * <p/>
 * Jaudiotagger Copyright (C)2004,2005
 * <p/>
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 * or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public License ainteger with this library; if not,
 * you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * <p/>
 * Description:
 * Used by Commercial Frame (COMR)
 */
package org.jaudiotagger.tag.id3.valuepair;

import org.jaudiotagger.tag.datatype.AbstractIntStringValuePair;

public class ReceivedAsTypes extends AbstractIntStringValuePair
{
    private static ReceivedAsTypes receivedAsTypes;

    public static ReceivedAsTypes getInstanceOf()
    {
        if (receivedAsTypes == null)
        {
            receivedAsTypes = new ReceivedAsTypes();
        }
        return receivedAsTypes;
    }

    private ReceivedAsTypes()
    {
        idToValue.put(0x00, "Other");
        idToValue.put(0x01, "Standard CD album with other songs");
        idToValue.put(0x02, "Compressed audio on CD");
        idToValue.put(0x03, "File over the Internet");
        idToValue.put(0x04, "Stream over the Internet");
        idToValue.put(0x05, "As note sheets");
        idToValue.put(0x06, "As note sheets in a book with other sheets");
        idToValue.put(0x07, "Music on other media");
        idToValue.put(0x08, "Non-musical merchandise");
        createMaps();
    }
}
