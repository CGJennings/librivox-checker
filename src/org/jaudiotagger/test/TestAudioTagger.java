/**
 * @author : Paul Taylor
 * <p/>
 * Version @version:$Id: TestAudioTagger.java,v 1.9 2007/08/06 16:04:37 paultaylor Exp $
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
package org.jaudiotagger.test;

import org.jaudiotagger.audio.mp3.MP3File;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;

/**
 * Simple class that will attempt to recusively read all files within a directory, flags
 * errors that occur.
 */
public class TestAudioTagger
{

    public static void main(final String[] args) 
    {
        TestAudioTagger test = new TestAudioTagger();

        if(args.length==0)
        {
            System.err.println("usage TestAudioTagger Dirname");
            System.err.println("      You must enter the root directory");
            System.exit(1);
        }
        else if(args.length>1)
        {
            System.err.println("usage TestAudioTagger Dirname");
            System.err.println("      Only one parameter accepted");
            System.exit(1);
        }
        File rootDir = new File(args[0]);
        if(!rootDir.isDirectory())
        {
            System.err.println("usage TestAudioTagger Dirname");
            System.err.println("      Directory "+args[0] +" could not be found");
            System.exit(1);
        }
        Date start = new Date();
        System.out.println("Started to read from:"+rootDir.getPath()+" at "
            + DateFormat.getTimeInstance().format(start));
        test.scanSingleDir(rootDir);
        Date finish = new Date();
        System.out.println("Started to read from:"+rootDir.getPath()+" at "
                    + DateFormat.getTimeInstance().format(start));
        System.out.println("Finished to read from:"+rootDir.getPath()
             + DateFormat.getTimeInstance().format(finish));
        System.out.println("Attempted  to read:"+count);
        System.out.println("Successful to read:"+(count-failed));
        System.out.println("Failed     to read:"+failed);

    }


    private static int count =0;
    private static int failed=0;
    /**
     * Recursive function to scan directory
     */
    private void scanSingleDir(final File dir)
    {

        final File[] audioFiles = dir.listFiles(new MP3FileFilter());
        if (audioFiles.length > 0)
        {
            for (File audioFile : audioFiles)
            {
                count++;
                final File mp3File = audioFile;
                try
                {
                    //System.out.println("About to read record:"+count+":"+mp3File.getPath());
                    final MP3File tmpMP3 = new MP3File(mp3File);

                }
                catch (Throwable t)
                {
                    System.err.println("Unable to read record:" + count + ":" + mp3File.getPath());
                    failed++;
                    t.printStackTrace();
                }
            }
        }

        final File[] audioFileDirs = dir.listFiles(new DirFilter());
        if (audioFileDirs.length > 0)
        {
            for (File audioFileDir : audioFileDirs)
            {
                scanSingleDir(audioFileDir);
            }
        }
    }

    final class MP3FileFilter
        extends javax.swing.filechooser.FileFilter
        implements java.io.FileFilter
    {

        /**
         * allows Directories
         */
        private final boolean allowDirectories;

        /**
         * Create a default MP3FileFilter.  The allowDirectories field will
         * default to false.
         */
        public MP3FileFilter()
        {
            this(false);
        }

        /**
         * Create an MP3FileFilter.  If allowDirectories is true, then this filter
         * will accept directories as well as mp3 files.  If it is false then
         * only mp3 files will be accepted.
         *
         * @param allowDirectories whether or not to accept directories
         */
        private MP3FileFilter(final boolean allowDirectories)
        {
            this.allowDirectories = allowDirectories;
        }

        /**
         * Determines whether or not the file is an mp3 file.  If the file is
         * a directory, whether or not is accepted depends upon the
         * allowDirectories flag passed to the constructor.
         *
         * @param file the file to test
         * @return true if this file or directory should be accepted
         */
        public final boolean accept(final java.io.File file)
        {
            return (
                ((file.getName()).toLowerCase().endsWith(".mp3"))
                    ||
                    (file.isDirectory() && (this.allowDirectories == true))
            );
        }

        /**
         * Returns the Name of the Filter for use in the Chooser Dialog
         *
         * @return The Description of the Filter
         */
        public final String getDescription()
        {
            return new String(".mp3 Files");
        }
    }

    public final class DirFilter implements java.io.FileFilter
    {
        public DirFilter()
        {

        }


        /**
         * Determines whether or not the file is an mp3 file.  If the file is
         * a directory, whether or not is accepted depends upon the
         * allowDirectories flag passed to the constructor.
         *
         * @param file the file to test
         * @return true if this file or directory should be accepted
         */
        public final boolean accept(final java.io.File file)
        {
            return file.isDirectory();
        }

        public static final String IDENT = "$Id: TestAudioTagger.java,v 1.9 2007/08/06 16:04:37 paultaylor Exp $";
    }
}
