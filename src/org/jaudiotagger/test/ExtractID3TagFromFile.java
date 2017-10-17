package org.jaudiotagger.test;

import org.jaudiotagger.audio.mp3.MP3File;

import java.io.File;

/**
 * Simple class that will attempt to recusively read all files within a directory, flags
 * errors that occur.
 */
public class ExtractID3TagFromFile
{

    public static void main(final String[] args)
    {
        ExtractID3TagFromFile test = new ExtractID3TagFromFile();

        if(args.length!=2)
        {
            System.err.println("usage ExtractID3TagFromFile Filename FilenameOut");
            System.err.println("      You must enter the file to extract the tag from and where to extract to");
            System.exit(1);
        }

        File file    = new File(args[0]);
        File outFile = new File(args[1]);
        if(!file.isFile())
        {
            System.err.println("usage ExtractID3TagFromFile Filename FilenameOut");
            System.err.println("      File "+args[0] +" could not be found");
            System.exit(1);
        }

         try
         {
            final MP3File tmpMP3 = new MP3File(file);
            tmpMP3.extractID3v2TagDataIntoFile(outFile);
         }
         catch(Exception e)
         {
              System.err.println("Unable to extract tag");
              System.exit(1);
         }
    }

    public static final String IDENT = "$Id: ExtractID3TagFromFile.java,v 1.2 2007/08/06 16:04:37 paultaylor Exp $";

}
