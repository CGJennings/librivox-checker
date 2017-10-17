/**
 *  @author : Paul Taylor
 *
 *  Version @version:$Id: TextEncoding.java,v 1.5 2007/08/06 16:04:36 paultaylor Exp $
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
 * Valid Text Encodings
 */
package org.jaudiotagger.tag.id3.valuepair;

import org.jaudiotagger.tag.datatype.AbstractIntStringValuePair;


/**
 * Text Encoding supported by ID3v24, the id is recognised by ID3
 * whereas the value maps to a java java.nio.charset.Charset, all the
 * charsets defined below are guaranteed on every Java platform.
 *
 * Note in ID3 UTF_16 can be implemented as either UTF16BE or UTF16LE with byte ordering
 * marks, in JAudioTagger we always implement it as UTF16BE (this is how the Java UTF-16 charset works).
 */
public class TextEncoding extends AbstractIntStringValuePair
{
    //Supported Java charsets
    public static final String CHARSET_ISO_8859_1 = "ISO-8859-1";
    public static final String CHARSET_UTF_16     = "UTF-16";
    public static final String CHARSET_UTF_16BE   = "UTF-16BE";
    public static final String CHARSET_UTF_8      = "UTF-8";

    //Supported ID3 charset ids
    public static final byte ISO_8859_1 = 0;
    public static final byte UTF_16     = 1;               // We use UTF-16 with LE byteordering and byte order mark
    public static final byte UTF_16BE   = 2;
    public static final byte UTF_8      = 3;

    //The number of bytes used to hold the text encoding field size
    public static final int  TEXT_ENCODING_FIELD_SIZE = 1;

    private static TextEncoding textEncodings;

    public static TextEncoding getInstanceOf()
    {
        if (textEncodings == null)
        {
            textEncodings = new TextEncoding();
        }
        return textEncodings;
    }

    private TextEncoding()
    {
        idToValue.put((int) ISO_8859_1   , CHARSET_ISO_8859_1 );
        idToValue.put((int) UTF_16       , CHARSET_UTF_16);
        idToValue.put((int) UTF_16BE     , CHARSET_UTF_16BE);
        idToValue.put((int) UTF_8        , CHARSET_UTF_8);

        createMaps();
    }
}
