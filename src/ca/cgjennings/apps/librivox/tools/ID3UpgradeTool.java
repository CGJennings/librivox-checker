package ca.cgjennings.apps.librivox.tools;

import ca.cgjennings.apps.librivox.Checker;
import ca.cgjennings.apps.librivox.LibriVoxAudioFile;
import ca.cgjennings.apps.librivox.decoder.NotAnMP3Exception;
import ca.cgjennings.apps.librivox.tools.FileProcessor.Task;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.JFrame;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.TagOptionSingleton;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v23Tag;
import org.jaudiotagger.tag.id3.ID3v24Tag;

/**
 * Upgrades old ID3 metadata to ID3v2 under user direction.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
public class ID3UpgradeTool {

    private ID3UpgradeTool() {
    }

    //
    // NOTE: Although MP3FileMetadata hides the details of the tagging
    //       library it uses, it is not complex enough to support this
    //       tool. Thus, the tool currently uses the Jaudiotagger lib
    //       directly.
    //
    /**
     * An exception used internally when there is no ID3 tag to upgrade.
     */
    private static class MissingMetadataException extends Exception {

        public MissingMetadataException() {
            super(Checker.string("upgrade-no-tag"));
        }
    }

    /**
     * Displays a dialog to the user allowing them to confirm and configure the
     * upgrade, then upgrades the provided files.
     *
     * @param parent the parent window for the configuration dialog
     * @param files the files to upgrade
     */
    public static void promptAndUpgrade(final JFrame parent, final LibriVoxAudioFile[] files) {
        if (files == null) {
            throw new NullPointerException("files");
        }
        if (files.length == 0) {
            return;
        }

        UpgradeMetadataConfigurationDialog d = new UpgradeMetadataConfigurationDialog(parent, true);
        if (!d.showDialog()) {
            return; // cancelled
        }
        final boolean toV24 = d.isUpgradeToLatestVersionSelected();
        final boolean removeV1 = d.isObsoleteTagStrippingSelected();

        FileProcessor fp = new FileProcessor(parent);
        fp.setTitle(d.getTitle());
        for (int i = 0; i < files.length; ++i) {
            fp.addTask(new Task(files[i]) {
                @Override
                public void process() throws Throwable {
                    LibriVoxAudioFile f = getFile();
                    upgradeMetadata(f, toV24, removeV1);
                    reanalyze();
                }
            });
        }
        fp.start();
    }

//		final UpgradeMetadataProgressDialog statusDialog = new UpgradeMetadataProgressDialog( parent, true );
//		statusDialog.setFileCount( files.length );
//		Thread upgradeThread = new Thread() {
//			@Override
//			public void run() {
//				List<LibriVoxAudioFile> filesToRecheck = new LinkedList<LibriVoxAudioFile>();
//				for( int i=0; i<files.length; ++i ) {
//					LibriVoxAudioFile f = files[i];
//					statusDialog.post( Checker.string( "upgrade-file", f.getFileName() ), i );
//					if( !f.isDone() ) {
//						statusDialog.post( Checker.string( "upgrade-waiting" ), i );
//					}
//					while( !f.isDone() ) {
//						try {
//							if( statusDialog.isCancelled() ) {
//								statusDialog.dispose();
//								return;
//							}
//							f.waitForReport( 2000 );
//						} catch( InterruptedException e ) {
//							// will be cancelled, so will catch on next iteration
//						}
//					}
//					try {
//						statusDialog.post( Checker.string( "upgrade-upgrading" ), i );
//						upgradeMetadata( f, toV24, removeV1 );
//						statusDialog.post( Checker.string( "upgrade-success" ), i );
//						filesToRecheck.add( f );
//					} catch( Exception e ) {
//						String message = e.getLocalizedMessage();
//						if( message == null ) message = e.toString();
//						Checker.getLogger().log( Level.SEVERE, null, e );
//						statusDialog.post( Checker.string( "upgrade-error", f.getFileName(), message ), i );
//					}
//					if( statusDialog.isCancelled() ) {
//						statusDialog.dispose();
//						return;
//					}
//				}
//				statusDialog.jobFinished( filesToRecheck );
//			}
//		};
//		upgradeThread.start();
//		statusDialog.setVisible( true );
//	}
    private static void upgradeMetadata(LibriVoxAudioFile file, boolean toV24, boolean removeV1) throws CannotWriteException, IOException, MissingMetadataException {
        MP3File mp3;
        try {
            mp3 = new MP3File(file.getLocalFile(), MP3File.LOAD_IDV1TAG | MP3File.LOAD_IDV2TAG);
        } catch (TagException e) {
            throw new MissingMetadataException();
        } catch (InvalidAudioFrameException e) {
            throw new NotAnMP3Exception();
        } catch (ReadOnlyFileException e) {
            throw new IOException(Checker.string("error-io-readonly"));
        }

        // this will be the new V2 tag; if null, no V2 upgrade is required
        AbstractID3v2Tag newTag = null;

        if (toV24) {
            // convert tag to V24 if there is not already a V24
            if (mp3.hasID3v2Tag()) {
                // convert tag if it is not V24
                if (!(mp3.getID3v2Tag() instanceof ID3v24Tag)) {
                    newTag = upgradeV2ToV24Tag(mp3);
                }
            } else if (mp3.hasID3v1Tag()) {
                newTag = upgradeV1toV24(mp3);
            } else {
                throw new MissingMetadataException();
            }
        } else {
            // convert tag to V23 if there is not already a V23
            if (mp3.hasID3v2Tag()) {
                // convert tag if it is not V23
                if (!(mp3.getID3v2Tag() instanceof ID3v23Tag)) {
                    newTag = upgradeV2ToV23Tag(mp3);
                }
            } else if (mp3.hasID3v1Tag()) {
                newTag = upgradeV1toV23(mp3);
            } else {
                throw new MissingMetadataException();
            }
        }

        replaceTag(mp3, newTag, removeV1);
    }

    private static ID3v23Tag upgradeV1toV23(MP3File mp3) {
        return new ID3v23Tag(mp3.getID3v1Tag());
    }

    private static ID3v24Tag upgradeV1toV24(MP3File mp3) {
        return new ID3v24Tag(mp3.getID3v1Tag());
    }

    private static ID3v23Tag upgradeV2ToV23Tag(MP3File mp3) {
        return new ID3v23Tag(mp3.getID3v2TagAsv24());
    }

    private static ID3v24Tag upgradeV2ToV24Tag(MP3File mp3) {
        return mp3.getID3v2TagAsv24();
    }

    private static void replaceTag(MP3File mp3, AbstractID3v2Tag tagV2, boolean removeV1) throws CannotWriteException {
        boolean mustCommit = false;

        TagOptionSingleton opt = TagOptionSingleton.getInstance();
        opt.setId3v1Save(true);
        opt.setId3v2Save(true);

        if (removeV1) {
            mp3.setID3v1Tag(null);
            mustCommit = true;
        }

        if (tagV2 != null) {
            mp3.setID3v2Tag((AbstractID3v2Tag) tagV2);
            mustCommit = true;
        }

        if (mustCommit) {
            mp3.commit();
        }
    }
}
