/**
 *  Amended @author : Paul Taylor
 *  Initial @author : Eric Farng
 *
 *  Version @version:$Id$
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
package org.jaudiotagger.audio.mp3;

import org.jaudiotagger.tag.id3.*;
import org.jaudiotagger.tag.lyrics3.AbstractLyrics3;
import org.jaudiotagger.tag.lyrics3.Lyrics3v1;
import org.jaudiotagger.tag.lyrics3.Lyrics3v2;
import org.jaudiotagger.tag.*;
import org.jaudiotagger.tag.virtual.VirtualMetaDataContainer;
import org.jaudiotagger.logging.*;
import org.jaudiotagger.audio.ReadOnlyFileException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.*;

/*
 * This class represets a physical MP3 File
*/
public class MP3File extends org.jaudiotagger.audio.AbstractAudioFile
{
    /**
     * MP3 save mode lowest numbered index
     */
    public static final int MP3_FILE_SAVE_FIRST = 1;
    /**
     * MP3 save mode matching <code>write</code> method
     */
    public static final int MP3_FILE_SAVE_WRITE = 1;
    /**
     * MP3 save mode matching <code>overwrite</code> method
     */
    public static final int MP3_FILE_SAVE_OVERWRITE = 2;
    /**
     * MP3 save mode matching <code>append</code> method
     */
    public static final int MP3_FILE_SAVE_APPEND = 3;
    /**
     * MP3 save mode highest numbered index
     */
    public static final int MP3_FILE_SAVE_LAST = 3;

    protected static AbstractTagDisplayFormatter tagFormatter;

    /**
     * Logger Object
     */
    public static Logger logger = LogFormatter.getLogger();

    /**
     * the ID3v2 tag that this file contains.
     */
    private AbstractID3v2Tag id3v2tag = null;

    /**
     * Representation of the idv2 tag as a idv24 tag
     */
    private ID3v24Tag id3v2Asv24tag = null;

    /**
     * The Lyrics3 tag that this file contains.
     */
    private AbstractLyrics3 lyrics3tag = null;


    /**
     * The ID3v1 tag that this file contains.
     */
    private ID3v1Tag id3v1tag = null;

    /**
     * Creates a new empty MP3File datatype that is not associated with a
     * specific file.
     */
    public MP3File()
    {
    }

    /**
     * Create Audioheader from another AudioHeader
     */
    public MP3File(MP3File copyObject)
    {
        this.file = new File(copyObject.file.getAbsolutePath());
        this.audioHeader = new MP3AudioHeader((MP3AudioHeader)copyObject.audioHeader);
        this.id3v2tag = (AbstractID3v2Tag) ID3Tags.copyObject(copyObject.id3v2tag);
        this.lyrics3tag = (AbstractLyrics3) ID3Tags.copyObject(copyObject.lyrics3tag);
        this.id3v1tag = (ID3v1Tag) ID3Tags.copyObject(copyObject.id3v1tag);
    }

    /**
     * Creates a new MP3File datatype and parse the tag from the given filename.
     *
     * @param filename MP3 file
     * @throws IOException  on any I/O error
     * @throws TagException on any exception generated by this library.
     */
    public MP3File(String filename)
        throws IOException, TagException, ReadOnlyFileException
    {
        this(new File(filename));
    }

    /**
     * Creates a new MP3File datatype and parse the tag from the given file
     * Object.
     *
     * @param file MP3 file
     * @param ArrayList loadOptions, lists what we actually want to bother loading
     * from file.
     * @throws IOException on any I/O error
     * @throws TagException on any exception generated by this library.
     */

    public static final int LOAD_MP3TAG = 1;
    public static final int LOAD_IDV1TAG = 2;
    public static final int LOAD_IDV2TAG = 4;
    public static final int LOAD_LYRICS3 = 8;
    public static final int LOAD_FILENAMETAG = 16;
    public static final int LOAD_ALL = LOAD_MP3TAG | LOAD_IDV1TAG | LOAD_IDV2TAG | LOAD_LYRICS3 | LOAD_FILENAMETAG;

    public MP3File(File file, int loadOptions)
        throws IOException, TagException, ReadOnlyFileException
    {
        this.file = file;
        logger.info("Reading file:" + "path"+ file.getPath() + ":abs:" + file.getAbsolutePath());
        if(file.exists()==false)
        {
            logger.severe("Unable to find:"+file.getPath());
            throw new FileNotFoundException("Unable to find:"+file.getPath());
        }

        if (file.canWrite() == false)
        {
            logger.severe("Unable to write:"+file.getPath());
            throw new ReadOnlyFileException("Unable to write to:"+file.getPath());
        }

        RandomAccessFile newFile = new RandomAccessFile(file, "rw");
        if ((loadOptions & LOAD_MP3TAG) != 0)
        {
            try
            {
                audioHeader = new MP3AudioHeader(newFile);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
        if ((loadOptions & LOAD_IDV1TAG) != 0)
        {
            try
            {
                id3v1tag = new ID3v11Tag(newFile);
            }
            catch (TagNotFoundException ex)
            {
            }
            try
            {
                if (id3v1tag == null)
                {
                    id3v1tag = new ID3v1Tag(newFile);
                }
            }
            catch (TagNotFoundException ex)
            {
            }
        }
        if ((loadOptions & LOAD_IDV2TAG) != 0)
        {
            try
            {
                this.setID3v2Tag(new ID3v24Tag(newFile));
            }
            catch (TagNotFoundException ex)
            {
            }
            try
            {
                if (id3v2tag == null)
                {
                    this.setID3v2Tag(new ID3v23Tag(newFile));
                }
            }
            catch (TagNotFoundException ex)
            {
            }
            try
            {
                if (id3v2tag == null)
                {
                    this.setID3v2Tag(new ID3v22Tag(newFile));
                }
            }
            catch (TagNotFoundException ex)
            {
            }
        }
        if ((loadOptions & LOAD_LYRICS3) != 0)
        {
            try
            {
                lyrics3tag = new Lyrics3v2(newFile);
            }
            catch (TagNotFoundException ex)
            {
            }
            try
            {
                if (lyrics3tag == null)
                {
                    lyrics3tag = new Lyrics3v1(newFile);
                }
            }
            catch (TagNotFoundException ex)
            {
            }
        }

        //Create Virtual tag from the ID3v24tag
        this.metaData = new VirtualMetaDataContainer((ID3v24Tag)this.getID3v2TagAsv24());

        newFile.close();


    }

    /**
     * Used by tags when writing to calculate the location of the music file
     */
    public long getMP3StartByte(RandomAccessFile file)
    {
        try
        {
            MP3AudioHeader audioHeader = new MP3AudioHeader(file);
            return audioHeader.getMp3StartByte();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return 0;
    }


    /**
     * Returns true if this datatype contains an <code>Id3v1</code> tag
     *
     * @return true if this datatype contains an <code>Id3v1</code> tag
     */
    public boolean hasID3v1Tag()
    {
        return (id3v1tag != null);
    }

    /**
     * Returns true if this datatype contains an <code>Id3v2</code> tag
     *
     * @return true if this datatype contains an <code>Id3v2</code> tag
     */
    public boolean hasID3v2Tag()
    {
        return (id3v2tag != null);
    }

    /**
     * Returns true if this datatype contains an <code>Lyrics3</code> tag
     *
     * @return true if this datatype contains an <code>Lyrics3</code> tag
     */
    public boolean hasLyrics3Tag()
    {
        return (lyrics3tag != null);
    }

    /**
     * Creates a new MP3File datatype and parse the tag from the given file
     * Object.
     *
     * @param file MP3 file
     * @throws IOException  on any I/O error
     * @throws TagException on any exception generated by this library.
     */
    public MP3File(File file)
        throws IOException, TagException, ReadOnlyFileException
    {
        this(file, LOAD_ALL);
    }


    /**
     * Sets all four (id3v1, lyrics3, filename, id3v2) tags in this instance to
     * the <code>frame</code> argument if the tag exists. This method does not
     * use the options inside the <code>tagOptions</code> datatype.
     *
     * @param frame frame to set / replace in all four tags.
     * @todo this method is very inefficient.
     */
    public void setFrameAcrossTags(AbstractID3v2Frame frame)
    {
        ID3v24Tag id3v1 = null;
        ID3v24Tag lyrics3 = null;
        if (this.id3v1tag != null)
        {
            id3v1 = new ID3v24Tag(this.id3v1tag);
            id3v1.setFrame(frame);
            this.id3v1tag.overwrite(id3v1);
        }
        if (this.id3v2tag != null)
        {
            id3v2tag.setFrame(frame);
        }
        if (this.lyrics3tag != null)
        {
            lyrics3 = new ID3v24Tag(this.lyrics3tag);
            lyrics3.setFrame(frame);
            this.lyrics3tag = new Lyrics3v2(lyrics3);
        }
    }

    /**
     * Gets the frames from all four (id3v1, lyrics3, filename, id3v2) mp3 tags
     * in this instance for each tag that exists. This method does not use the
     * options inside the <code>tagOptions</code> datatype.
     *
     * @param identifier ID3v2.4 Tag Frame Identifier.
     * @return ArrayList of all instances of the desired frame. Each instance
     *         is returned as an <code>ID3v2_4Frame</code>. The nature of the
     *         code returns the array in a specific order, but this order is
     *         not guaranteed for future versions of this library.
     * @todo this method is very inefficient.
     */
    public ArrayList getFrameAcrossTags(String identifier)
    {
        if ((identifier != null) && (identifier.length() > 0))
        {
            ID3v24Tag id3v1 = null;
            ID3v24Tag lyrics3 = null;
            ArrayList list = new ArrayList();
            Iterator iterator;
            if (this.id3v1tag != null)
            {
                id3v1 = new ID3v24Tag(this.id3v1tag);
                if (id3v1.hasFrameOfType(identifier))
                {
                    iterator = id3v1.getFrameOfType(identifier);
                    while (iterator.hasNext())
                    {
                        list.add(iterator.next());
                    }
                }
            }
            if (this.id3v2tag != null)
            {
                if (id3v2tag.hasFrameOfType(identifier))
                {
                    iterator = id3v2tag.getFrameOfType(identifier);
                    while (iterator.hasNext())
                    {
                        list.add(iterator.next());
                    }
                }
            }
            if (this.lyrics3tag != null)
            {
                lyrics3 = new ID3v24Tag(this.lyrics3tag);
                if (lyrics3.hasFrameOfType(identifier))
                {
                    iterator = lyrics3.getFrameOfType(identifier);
                    while (iterator.hasNext())
                    {
                        list.add(iterator.next());
                    }
                }
            }

            return list;
        }
        return null;
    }

    /**
     * Sets the v1(_1)tag to the tag provided as an argument.
     *
     * @param id3v1tag DOCUMENT ME!
     */
    public void setID3v1Tag(ID3v1Tag id3v1tag)
    {
        logger.info("setting tagv1:v1 tag");
        this.id3v1tag = id3v1tag;
    }

    /**
     * Sets the <code>ID3v1</code> tag for this datatype. A new
     * <code>ID3v1_1</code> datatype is created from the argument and then used
     * here.
     *
     * @param mp3tag Any MP3Tag datatype can be used and will be converted into a
     *               new ID3v1_1 datatype.
     */
    public void setID3v1Tag(AbstractTag mp3tag)
    {
        logger.info("setting tagv1:abstract");
        id3v1tag = new ID3v11Tag(mp3tag);
    }

    /**
     * Returns the <code>ID3v1</code> tag for this datatype.
     *
     * @return the <code>ID3v1</code> tag for this datatype
     */
    public ID3v1Tag getID3v1Tag()
    {
        return id3v1tag;
    }

    /**
     * Sets the <code>ID3v2</code> tag for this datatype. A new
     * <code>ID3v2_4</code> datatype is created from the argument and then used
     * here.
     *
     * @param mp3tag Any MP3Tag datatype can be used and will be converted into a
     *               new ID3v2_4 datatype.
     */
    public void setID3v2Tag(AbstractTag mp3tag)
    {
        id3v2tag = new ID3v24Tag(mp3tag);

    }

    /**
     * Sets the v2 tag to the v2 tag provided as an argument.
     * Also store a v24 version of tag as v24 is the interface to be used
     * when talking with client applications.
     *
     * @param id3v2tag DOCUMENT ME!
     */
    public void setID3v2Tag(AbstractID3v2Tag id3v2tag)
    {
        this.id3v2tag = id3v2tag;
        if (id3v2tag instanceof ID3v24Tag)
        {
            this.id3v2Asv24tag = (ID3v24Tag) this.id3v2tag;
        }
        else
        {
            this.id3v2Asv24tag = new ID3v24Tag(id3v2tag);
        }
    }

    /**
     * Set v2 tag ,dont need to set v24 tag because saving
     *
     * @tood temp its rather messy
     */
    public void setID3v2TagOnly(AbstractID3v2Tag id3v2tag)
    {
        this.id3v2tag = id3v2tag;
        this.id3v2Asv24tag = null;
    }

    /**
     * Returns the <code>ID3v2</code> tag for this datatype.
     *
     * @return the <code>ID3v2</code> tag for this datatype
     */
    public AbstractID3v2Tag getID3v2Tag()
    {
        return id3v2tag;
    }

    /**
     * Returns a representation of tag as v24
     */
    public AbstractID3v2Tag getID3v2TagAsv24()
    {
        return id3v2Asv24tag;
    }

    /**
     * Sets the <code>Lyrics3</code> tag for this datatype. A new
     * <code>Lyrics3v2</code> datatype is created from the argument and then
     * used here.
     *
     * @param mp3tag Any MP3Tag datatype can be used and will be converted into a
     *               new Lyrics3v2 datatype.
     */
    public void setLyrics3Tag(AbstractTag mp3tag)
    {
        lyrics3tag = new Lyrics3v2(mp3tag);
    }

    /**
     * DOCUMENT ME!
     *
     * @param lyrics3tag DOCUMENT ME!
     */
    public void setLyrics3Tag(AbstractLyrics3 lyrics3tag)
    {
        this.lyrics3tag = lyrics3tag;
    }

    /**
     * Returns the <code>ID3v1</code> tag for this datatype.
     *
     * @return the <code>ID3v1</code> tag for this datatype
     */
    public AbstractLyrics3 getLyrics3Tag()
    {
        return lyrics3tag;
    }



    /**
     * Returns true if there are any unsynchronized tags in this datatype. A
     * fragment is unsynchronized if it exists in two or more tags but is not
     * equal across all of them.
     *
     * @return true of any fragments are unsynchronized.
     * @todo there might be a faster way to do this, other than calling
     * <code>getUnsynchronizedFragments</code>
     */
    public boolean isUnsynchronized()
    {
        return getUnsynchronizedFragments().size() > 0;
    }

    /**
     * Returns a HashSet of unsynchronized fragments across all tags in this
     * datatype. A fragment is unsynchronized if it exists in two or more tags
     * but is not equal across all of them.
     *
     * @return a HashSet of unsynchronized fragments
     */
    public HashSet getUnsynchronizedFragments()
    {
        ID3v24Tag id3v1 = null;
        ID3v24Tag lyrics3 = null;
        ID3v24Tag filename = null;
        AbstractID3v2Tag id3v2 = null;
        ID3v24Tag total = new ID3v24Tag(this.id3v2tag);
        AbstractID3v2Frame frame;
        String identifier;
        HashSet set = new HashSet();
        total.append(id3v1tag);
        total.append(lyrics3tag);
        total.append(id3v2tag);
        id3v1 = new ID3v24Tag(this.id3v1tag);
        lyrics3 = new ID3v24Tag(this.lyrics3tag);
        id3v2 = this.id3v2tag;
        Iterator iterator = total.iterator();
        while (iterator.hasNext())
        {
            frame = (AbstractID3v2Frame) iterator.next();
            identifier = frame.getIdentifier();
            if (id3v2 != null)
            {
                if (id3v2.hasFrame(identifier))
                {
                    if (((AbstractID3v2Frame) id3v2.getFrame(identifier)).isSubsetOf(frame) == false)
                    {
                        set.add(identifier);
                    }
                }
            }
            if (id3v1 != null)
            {
                if (id3v1.hasFrame(identifier))
                {
                    if (((AbstractID3v2Frame) id3v1.getFrame(identifier)).isSubsetOf(frame) == false)
                    {
                        set.add(identifier);
                    }
                }
            }
            if (lyrics3 != null)
            {
                if (lyrics3.hasFrame(identifier))
                {
                    if (((AbstractID3v2Frame) lyrics3.getFrame(identifier)).isSubsetOf(frame) == false)
                    {
                        set.add(identifier);
                    }
                }
            }
            if (filename != null)
            {
                if (filename.hasFrame(identifier))
                {
                    if (((AbstractID3v2Frame) filename.getFrame(identifier)).isSubsetOf(frame) == false)
                    {
                        set.add(identifier);
                    }
                }
            }
        }
        return set;
    }


    /**
     * Remove tag from file
     *
     * @param mp3tag DOCUMENT ME!
     * @throws FileNotFoundException DOCUMENT ME!
     * @throws IOException           DOCUMENT ME!
     */
    public void delete(AbstractTag mp3tag)
        throws FileNotFoundException, IOException
    {
        mp3tag.delete(new RandomAccessFile(this.file, "rw"));
    }

    /**
     * Saves the tags in this datatype to the file referred to by this datatype. It
     * will be saved as TagConstants.MP3_FILE_SAVE_WRITE
     *
     * @throws IOException  on any I/O error
     * @throws TagException on any exception generated by this library.
     */
    public void save()
        throws IOException, TagException
    {
        save(this.file, TagOptionSingleton.getInstance().getDefaultSaveMode());
    }

    /**
     * Saves the tags in this datatype to the file referred to by this datatype. It
     * will be saved as TagConstants.MP3_FILE_SAVE_WRITE
     *
     * @param saveMode write, overwrite, or append. Defined as
     *                 <code>TagConstants.MP3_FILE_SAVE_WRITE
     *                 TagConstants.MP3_FILE_SAVE_OVERWRITE
     *                 TagConstants.MP3_FILE_SAVE_APPEND </code>
     * @throws IOException  on any I/O error
     * @throws TagException on any exception generated by this library.
     */
    public void save(int saveMode)
        throws IOException, TagException
    {
        save(this.file, saveMode);
    }

    /**
     * Saves the tags in this datatype to the file argument. It will be saved as
     * TagConstants.MP3_FILE_SAVE_WRITE
     *
     * @param filename file to save the this datatype's tags to
     * @throws IOException  on any I/O error
     * @throws TagException on any exception generated by this library.
     */
    public void save(String filename)
        throws IOException, TagException
    {
        save(new File(filename), TagOptionSingleton.getInstance().getDefaultSaveMode());
    }

    /**
     * Saves the tags in this datatype to the file argument. It will be saved as
     * TagConstants.MP3_FILE_SAVE_WRITE
     *
     * @param filename file to save the this datatype's tags to
     * @param saveMode write, overwrite, or append. Defined as
     *                 <code>TagConstants.MP3_FILE_SAVE_WRITE
     *                 TagConstants.MP3_FILE_SAVE_OVERWRITE
     *                 TagConstants.MP3_FILE_SAVE_APPEND </code>
     * @throws IOException  on any I/O error
     * @throws TagException on any exception generated by this library.
     */
    public void save(String filename, int saveMode)
        throws IOException, TagException
    {
        save(new File(filename), saveMode);
    }

    /**
     * Saves the tags in this datatype to the file argument. It will be saved as
     * TagConstants.MP3_FILE_SAVE_WRITE
     *
     * @param file     file to save the this datatype's tags to
     * @param saveMode write, overwrite, or append. Defined as
     *                 <code>TagConstants.MP3_FILE_SAVE_WRITE
     *                 TagConstants.MP3_FILE_SAVE_OVERWRITE
     *                 TagConstants.MP3_FILE_SAVE_APPEND </code>
     * @throws IOException  on any I/O error
     * @throws TagException on any exception generated by this library.
     */
    public void save(File file, int saveMode)
        throws IOException, TagException
    {
        if ((saveMode < MP3_FILE_SAVE_FIRST) ||
            (saveMode > MP3_FILE_SAVE_LAST))
        {
            throw new TagException("Invalid Save Mode");
        }
        logger.info("Saving  : " + file.getAbsolutePath());
        RandomAccessFile rfile = null;
        try
        {
            //ID3v2 Tag
            if (TagOptionSingleton.getInstance().isId3v2Save())
            {
                if (id3v2tag == null)
                {
                    rfile = new RandomAccessFile(file, "rw");
                    (new ID3v24Tag()).delete(rfile);
                    rfile.close();
                }
                else
                {
                    if (saveMode == MP3_FILE_SAVE_WRITE)
                    {
                        id3v2tag.write(file, ((MP3AudioHeader)this.getAudioHeader()).getMp3StartByte());
                    }
                    else if (saveMode == MP3_FILE_SAVE_APPEND)
                    {
                        id3v2tag.append(rfile);
                    }
                    else if (saveMode == MP3_FILE_SAVE_OVERWRITE)
                    {
                        id3v2tag.overwrite(rfile);
                    }
                }
            }
            rfile = new RandomAccessFile(file, "rw");

            //Lyrics 3 Tag
            if (TagOptionSingleton.getInstance().isLyrics3Save())
            {
                if (lyrics3tag == null)
                {
                    if (saveMode == MP3_FILE_SAVE_OVERWRITE)
                    {
                        (new Lyrics3v2()).delete(rfile);
                    }
                }
                else
                {
                    if (saveMode == MP3_FILE_SAVE_WRITE)
                    {
                        lyrics3tag.write(rfile);
                    }
                    else if (saveMode == MP3_FILE_SAVE_APPEND)
                    {
                        lyrics3tag.append(rfile);
                    }
                    else if (saveMode == MP3_FILE_SAVE_OVERWRITE)
                    {
                        lyrics3tag.overwrite(rfile);
                    }
                }
            }
            //ID3v1 tag
            if (TagOptionSingleton.getInstance().isId3v1Save())
            {
                logger.info("saving v1");
                if (id3v1tag == null)
                {
                    logger.info("deleting v1");
                    (new ID3v1Tag()).delete(rfile);
                }
                else
                {
                    logger.info("saving v1 still");
                    if (saveMode == MP3_FILE_SAVE_WRITE)
                    {
                        id3v1tag.write(rfile);
                    }
                    else if (saveMode == MP3_FILE_SAVE_APPEND)
                    {
                        id3v1tag.append(rfile);
                    }
                    else if (saveMode == MP3_FILE_SAVE_OVERWRITE)
                    {
                        id3v1tag.overwrite(rfile);
                        int debug = 0; // strange bug where last line is not run??
                    }
                }
            }

        }
        catch (TagException e)
        {
            logger.severe("Problem writing tags to file,TagException:" + file.getAbsolutePath());
            e.printStackTrace();
            throw e;
        }
        catch (IOException ex)
        {
            logger.severe("Problem writing tags to file,Unexpected Exception" + file.getAbsolutePath());
            ex.printStackTrace();
            throw new TagException();
        }
        finally
        {
            if (rfile != null)
            {
                rfile.close();
            }
        }
    }

    /**
     * Displays MP3File Structure
     */
    public String displayStructureAsXML()
    {
        createXMLStructureFormatter();
        this.tagFormatter.openHeadingElement("file", this.getFile().getAbsolutePath());
        if (this.getID3v1Tag() != null)
        {
            this.getID3v1Tag().createStructure();
        }
        if (this.getID3v2Tag() != null)
        {
            this.getID3v2Tag().createStructure();
        }
        this.tagFormatter.closeHeadingElement("file");
        return tagFormatter.toString();
    }

    /**
     * Displays MP3File Structure
     */
    public String displayStructureAsPlainText()
    {
        createPlainTextStructureFormatter();
        this.tagFormatter.openHeadingElement("file", this.getFile().getAbsolutePath());
        if (this.getID3v1Tag() != null)
        {
            this.getID3v1Tag().createStructure();
        }
        if (this.getID3v2Tag() != null)
        {
            this.getID3v2Tag().createStructure();
        }
        this.tagFormatter.closeHeadingElement("file");
        return tagFormatter.toString();
    }

    private static void createXMLStructureFormatter()
    {
        tagFormatter = new XMLTagDisplayFormatter();
    }

    private static void createPlainTextStructureFormatter()
    {
        tagFormatter = new PlainTextTagDisplayFormatter();
    }

    public static AbstractTagDisplayFormatter getStructureFormatter()
    {
        return tagFormatter;
    }
    //For writing tag to screen


}

