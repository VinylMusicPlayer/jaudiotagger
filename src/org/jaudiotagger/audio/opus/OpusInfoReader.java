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
package org.jaudiotagger.audio.opus;

import org.jaudiotagger.audio.SupportedFileFormat;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.jaudiotagger.audio.generic.Utils;
import org.jaudiotagger.audio.ogg.util.OggPageHeader;
import org.jaudiotagger.audio.opus.util.OpusVorbisIdentificationHeader;
import org.jaudiotagger.logging.ErrorMessage;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Read encoding info, only implemented for vorbis streams
 */
public class OpusInfoReader
{

    public static Logger logger = Logger.getLogger("org.jaudiotagger.audio.opus.atom");

    public GenericAudioHeader read(RandomAccessFile raf) throws CannotReadException, IOException
    {
        long start = raf.getFilePointer();
        GenericAudioHeader info = new GenericAudioHeader();
        logger.fine("Started");
        long oldPos;

        //Check start of file does it have Ogg pattern
        byte[] b = new byte[OggPageHeader.CAPTURE_PATTERN.length];
        raf.read(b);
        if (!(Arrays.equals(b, OggPageHeader.CAPTURE_PATTERN)))
        {
            raf.seek(0);
            if (AbstractID3v2Tag.isId3Tag(raf))
            {
                raf.read(b);
                if ((Arrays.equals(b, OggPageHeader.CAPTURE_PATTERN)))
                {
                    start = raf.getFilePointer();
                }
            }
            else
            {
                throw new CannotReadException(ErrorMessage.OGG_HEADER_CANNOT_BE_FOUND.getMsg(new String(b)));
            }
        }

        //Now work backwards from file looking for the last ogg page, it reads the granule position for this last page
        //which must be set.
        //TODO should do buffering to cut down the number of file reads
        raf.seek(start);
        double pcmSamplesNumber = -1;
        raf.seek(raf.length() - 2);
        while (raf.getFilePointer() >= 4)
        {
            if (raf.read() == OggPageHeader.CAPTURE_PATTERN[3])
            {
                raf.seek(raf.getFilePointer() - OggPageHeader.FIELD_CAPTURE_PATTERN_LENGTH);
                byte[] ogg = new byte[3];
                raf.readFully(ogg);
                if (ogg[0] == OggPageHeader.CAPTURE_PATTERN[0] && ogg[1] == OggPageHeader.CAPTURE_PATTERN[1] && ogg[2] == OggPageHeader.CAPTURE_PATTERN[2])
                {
                    raf.seek(raf.getFilePointer() - 3);

                    oldPos = raf.getFilePointer();
                    raf.seek(raf.getFilePointer() + OggPageHeader.FIELD_PAGE_SEGMENTS_POS);
                    int pageSegments = raf.readByte() & 0xFF; //Unsigned
                    raf.seek(oldPos);

                    b = new byte[OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + pageSegments];
                    raf.readFully(b);

                    OggPageHeader pageHeader = new OggPageHeader(b);
                    raf.seek(0);
                    pcmSamplesNumber = pageHeader.getAbsoluteGranulePosition();
                    break;
                }
            }
            raf.seek(raf.getFilePointer() - 2);
        }

        //1st page = Identification Header
        OggPageHeader pageHeader = OggPageHeader.read(raf);
        byte[] vorbisData = new byte[pageHeader.getPageLength()];

        raf.read(vorbisData);
        OpusVorbisIdentificationHeader opusIdHeader = new OpusVorbisIdentificationHeader(vorbisData);

        //Map to generic encodingInfo
        info.setPreciseLength((float) (pcmSamplesNumber / 48000));
        info.setChannelNumber(opusIdHeader.getAudioChannels());
        info.setEncodingType("Opus Vorbis 1.0");
        info.setFormat(SupportedFileFormat.OPUS.getDisplayName());

        //According to Wikipedia OPUS Page, OPUS only works at 16bits 48khz
        info.setSamplingRate(48000);
        info.setBitsPerSample(16);

        //FIXME
        info.setBitRate(0);
        info.setVariableBitRate(true);

        //TODO CBR?
        int length = info.getTrackLength();
        long size = raf.length();

        info.setBitRate(computeBitrate(length, size));
        info.setVariableBitRate(true);

        return info;
    }

    private int computeBitrate(int length, long size)
    {
        //Protect against audio less than 0.5 seconds that can be rounded to zero causing Arithmetic Exception
        if(length==0)
        {
            length=1;
        }
        return (int) ((size / Utils.KILOBYTE_MULTIPLIER) * Utils.BITS_IN_BYTE_MULTIPLIER / length);
    }
}

