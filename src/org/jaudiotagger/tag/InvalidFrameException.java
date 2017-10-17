/*
 *  @author : Paul Taylor
 *  @author : Eric Farng
 *
 *  Version @version:$Id: InvalidFrameException.java,v 1.4 2007/11/06 20:19:16 paultaylor Exp $
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
 */
package org.jaudiotagger.tag;

/**
 * Thrown if frame cannot be read correctly.
 * 
 * @author Eric Farng
 * @version $Revision: 1.4 $
 */
public class InvalidFrameException extends InvalidTagException
{
    /**
     * Creates a new InvalidFrameException datatype.
     */
    public InvalidFrameException()
    {
    }

    /**
     * Creates a new InvalidFrameException datatype.
     *
     * @param ex the cause.
     */
    public InvalidFrameException(Throwable ex)
    {
        super(ex);
    }

    /**
     * Creates a new InvalidFrameException datatype.
     *
     * @param msg the detail message.
     */
    public InvalidFrameException(String msg)
    {
        super(msg);
    }

    /**
     * Creates a new InvalidFrameException datatype.
     *
     * @param msg the detail message.
     * @param ex the cause.
     */
    public InvalidFrameException(String msg, Throwable ex)
    {
        super(msg, ex);
    }
}
