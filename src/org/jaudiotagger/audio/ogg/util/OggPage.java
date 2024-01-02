/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
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

import java.nio.ByteBuffer;

public class OggPage
{

    private final OggPageHeader header;
    private final ByteBuffer content;

    public OggPage(OggPageHeader header, ByteBuffer content)
    {
        this.header = header;
        this.content = content;
        fixCksum();
    }

    public static OggPage parse(ByteBuffer buf) throws CannotReadException
    {
        OggPageHeader header = OggPageHeader.read(buf);

        ByteBuffer content = buf.slice();
        content.limit(header.getPageLength());
        Utils.skip(buf, header.getPageLength());

        return new OggPage(header, content);
    }

    public void write(ByteBuffer buf)
    {
        header.write(buf);
        buf.put(content.duplicate());
    }

    public OggPageHeader getHeader()
    {
        return header;
    }

    public ByteBuffer getContent()
    {
        return content;
    }

    public void setSequenceNo(int seqNo)
    {
        header.setPageSequence(seqNo);
        fixCksum();
    }

    private void fixCksum()
    {
        ByteBuffer temp = ByteBuffer.allocate(size());
        write(temp);

        //CRC should be zero before calculating it
        temp.putInt(OggPageHeader.FIELD_PAGE_CHECKSUM_POS, 0);
        byte[] crc = OggCRCFactory.computeCRC(temp.array());
        int cksum = Utils.getIntLE(crc);

        header.setChecksum(cksum);
    }

    public int size()
    {
        return OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH    // header
                + header.getSegmentTable().length            // variable segment table
                + header.getPageLength();                    // content
    }
}

