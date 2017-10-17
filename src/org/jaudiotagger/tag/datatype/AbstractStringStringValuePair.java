/**
 * @author : Paul Taylor
 * <p/>
 * Version @version:$Id: AbstractStringStringValuePair.java,v 1.2 2006/08/25 15:35:15 paultaylor Exp $
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
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 * you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * <p/>
 * Description:
 */
package org.jaudiotagger.tag.datatype;

import java.util.Collections;

public class AbstractStringStringValuePair extends AbstractValuePair
{
    protected String lkey = null;

    /**
     * Get Id for Value
     */
    public String getIdForValue(String value)
    {
        return (String) valueToId.get(value);
    }

    /**
     * Get value for Id
     */
    public String getValueForId(String id)
    {
        return (String) idToValue.get(id);
    }

    protected void createMaps()
    {
        iterator = idToValue.keySet().iterator();
        while (iterator.hasNext())
        {
            lkey = (String) iterator.next();
            value = (String) idToValue.get(lkey);
            valueToId.put(value, lkey);
        }

        //Value List
        iterator = idToValue.keySet().iterator();
        while (iterator.hasNext())
        {
            valueList.add(idToValue.get((String) iterator.next()));
        }
        //Sort alphabetically
        Collections.sort(valueList);
    }
}
