package ca.cgjennings.apps.librivox;

import ca.cgjennings.apps.librivox.decoder.AudioFrame;
import ca.cgjennings.apps.librivox.decoder.DecoderFactory;
import ca.cgjennings.apps.librivox.decoder.NotAnMP3Exception;
import ca.cgjennings.apps.librivox.decoder.StreamDecoder;
import ca.cgjennings.apps.librivox.metadata.MP3FileMetadata;
import ca.cgjennings.apps.librivox.metadata.MetadataEditorLinkFactory;
import ca.cgjennings.apps.librivox.validators.ValidatorFactory;
import ca.cgjennings.apps.librivox.validators.Validator;
import java.awt.EventQueue;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.logging.Level;
import static ca.cgjennings.apps.librivox.Checker.string;
import static ca.cgjennings.apps.librivox.Checker.getLogger;

/**
 * Represents an .mp3 file to be validated, and controls the stages of the
 * validation process.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
public class LibriVoxAudioFile {

	/**
	 * The different states that this object can be in as validation proceeds.
	 */
	public enum Status {
		/**
		 * State indicating that the file is queued and will be analyzed when
		 * a free worker thread is available.
		 */
		QUEUED( true ),
		/** State indicating that the audio data is being downloaded from a server. */
		DOWNLOADING( true ),
		/** State indicating that the audio data is being analyzed. */
		ANALYZING( true ),
		/** State indicating that the audio file passed its analysis. */
		PASSED( false ),
		/** State indicating that the audio file passed with warnings. */
		WARNINGS( false ),
		/** State indicating that the audio file failed its analysis. */
		FAILED( false ),
		/** State indicating that analysis could not be completed due to an error. */
		ERROR( false ),
		/**
		 * Status indicating that this object has been disposed to free underlying resources.
		 * Methods such as {@link #getInformationReport} will not return a meaningful result.
		 */
		DISPOSED( false );

		Status( boolean progresses ) {
			this.progresses = progresses;
		}

		/**
		 * Returns true if this status is a stage in the validation, that is,
		 * if the file's status will change over time. When the current status
		 * of a file has this property, the file is still being processed.
		 *
		 * @return <code>true</code> if the status indicates that the file is
		 *     being processed
		 */
		public boolean isProgressive() {
			return progresses;
		}

		/**
		 * Returns a localized string that describes this status.
		 * @return a localized description
		 */
		public String getLocalizedName() {
			if( localizedName == null ) {
				localizedName = string( "status-" + toString().toLowerCase( Locale.US ) );
			}
			return localizedName;
		}

		private boolean progresses;
		private String localizedName;
	};

	/** Create a new <code>LibriVoxAudioFile</code> */
	public LibriVoxAudioFile( File sourceFile ) {
		localFile = sourceFile;
		try {
			source = sourceFile.toURI().toURL();
		} catch( MalformedURLException e ) {
			throw new AssertionError( "File URL conversion failed: " + e );
		}
		queueForAnalysis( WORKER_TASK_ANALYZE );
	}

	public LibriVoxAudioFile( URI uri ) throws MalformedURLException {
		this( uri.toURL() );
	}

	public LibriVoxAudioFile( URL url ) {
		source = url;
		localFileIsTemporary = true;

		queueForAnalysis( WORKER_TASK_DOWNLOAD|WORKER_TASK_ANALYZE );
	}

	private synchronized void queueForAnalysis( final int taskFlags ) {
		if( !EventQueue.isDispatchThread() ) {
			throw new IllegalStateException( "must be called from dispatch thread" );
		}
		cancelAnalysis( true );
		Runnable job = new Runnable() {
			@Override
			public void run() {
				boolean ok;
				if( (taskFlags & WORKER_TASK_DOWNLOAD) != 0 ) {
					setStatus( Status.DOWNLOADING );
					ok = download();
					if( !ok || Thread.interrupted() ) return;
				}
				if( (taskFlags & WORKER_TASK_ANALYZE) != 0 ) {
					ok = analyze();
					if( !ok || Thread.interrupted() ) return;
				}
				// ... additional tasks
			}
		};
		report = new Report( this );
		jobToken = JobManager.analyzeInFuture( this, job );
	}
	// NOTE that these are *bit flags* if you add additional tasks
	private int WORKER_TASK_DOWNLOAD = 1<<1;
	private int WORKER_TASK_ANALYZE = 1<<2;

	/**
	 * Download a file from a URL to a temporary local file for processing.
	 * Assumes that it is called from within a worker thread.
	 */
	private boolean download() {
		// flip to downloading status with dummy progress info
		synchronized( this ) {
			setMaximumProgress( 100 );
			setCurrentProgress( 0 );
			setStatus( Status.DOWNLOADING );
		}

		InputStream in = null;
		OutputStream out = null;
		try {
			URLConnection connection = source.openConnection();
			connection.setUseCaches( false );
			connection.setConnectTimeout( CONNECT_TIMEOUT );
			synchronized( this ) {
				setMaximumProgress( connection.getContentLength() );
				setCurrentProgress( 0 );
			}
			in = connection.getInputStream();

			localFile = File.createTempFile( "dltmp", ".mp3" );
			localFile.deleteOnExit();

			out = new FileOutputStream( localFile );

			byte[] buff = new byte[ IO_BUFFER_SIZE ];
			int read;
			long totalRead = 0;
			while( (read = in.read( buff )) >= 0 ) {
				out.write( buff, 0, read );
				totalRead += read;
				setCurrentProgress( totalRead );
				if( Thread.interrupted() ) return false;
			}
		} catch( IOException e ) {
			setErrorMessage( string( "error-download", e.getLocalizedMessage() ) );
			return false;
		} finally {
			if( in != null ) {
				try {
					in.close();
				} catch( IOException e ) {
					getLogger().log( Level.WARNING, "exception on closing input stream", e );
				}
			}
			if( out != null ) {
				try {
					out.close();
				} catch( IOException e ) {
					getLogger().log( Level.WARNING, "exception on closing temporary file", e );
				}
			}
		}
		return true;
	}

	private void validatorFailure( Throwable t ) {
		setStatus( Status.ERROR );
		StringBuilder b = new StringBuilder();
		b.append( "<pre>" ).append( t.toString() ).append( '\n' );
		StackTraceElement els[] = t.getStackTrace();
		for( StackTraceElement el : els )
			b.append( el ).append( '\n' );
		b.append( "</pre>" );
		report.setErrorMessage( string( "error-validator", b.toString() ) );
		getLogger().log( Level.SEVERE, null, t );
		report.close();
	}

	/**
	 * Analyze the audio file using the copy stored in {@link #getLocalFile()}.
	 * Assumes that it is running from inside a worker thread.
	 */
	private boolean analyze() {
		// flip to analyzing status with dummy progress info
		synchronized( this ) {
			setMaximumProgress( 100 );
			setCurrentProgress( 0 );
			setStatus( Status.ANALYZING );
		}


		File f = getLocalFile();
		if( f == null ) {
			throw new AssertionError( "exptected localFile to be non-null at start of analyze()" );
		}


		getLogger().log( Level.INFO, "Reading metadata for {0}", f.getName() );
		try {
			if( metadata == null ) {
				metadata = new MP3FileMetadata( f );
			} else {
				metadata.update();
			}
		} catch( NotAnMP3Exception e ) {
			badFileType();
			return false;
		} catch( IOException e ) {
			setErrorMessage( string("error-io-read", e.getLocalizedMessage()) );
			return false;
		} catch( Exception e ) {
			getLogger().log( Level.SEVERE, null, e );
			throw new AssertionError();
		}

		InputStream in = null;
		try {
			in = new FileInputStream( f );
			in = new BufferedInputStream( in, 64*1024 );

			final int skipBytes = metadata.getStartOfAudio();
			if( skipBytes > 0 ) {
				getLogger().log( Level.INFO, "Skipping {0} metadata tag bytes in {1}", new Object[] {skipBytes, f.getName()} );
				in.skip( skipBytes );
			}

			StreamDecoder decoder = DecoderFactory.createDecoder( in, report );
			long frameNumber = 0;

			synchronized( this ) {
				setMaximumProgress( metadata.getFrameCount() );
				setCurrentProgress( 0L );
			}

			Validator[] validators = ValidatorFactory.getFactory().createValidators();
			boolean needsStreamDecoder = false;

			for( int i=0; i<validators.length; ++i ) {
				Validator v = validators[i];
				needsStreamDecoder |= v.isAudioProcessor();
				try {
					v.initialize( this, report );

					Validator[] predecessors = new Validator[i];
					System.arraycopy( validators, 0, predecessors, 0, i );

					v.beginAnalysis( decoder.getAudioHeader(), predecessors );
				} catch( Throwable t ) {
					validatorFailure( t );
					return false;
				}
				if( Thread.interrupted() )
					return false;
			}

			// TODO: should not do badFileType if we have decoded a reasonable
			// amount of audio---it must be corrupt instead
			// TODO: do something useful with the decoder error system and
			//       put errors in the report

			if( needsStreamDecoder ) try {
				AudioFrame frame = decoder.getNextFrame();
				while( frame != null ) {
					for( Validator v : validators ) {
						if( v.isAudioProcessor() ) {
							v.analyzeFrame( frame );
							if( Thread.interrupted() )
								return false;
						}
					}

					++frameNumber;
					if( frameNumber % FRAME_UPDATE_RATE == 0 ) {
						setCurrentProgress( frameNumber );
					}

					frame = decoder.getNextFrame();
				}
			} catch( NotAnMP3Exception e ) {
				badFileType();
				return false;
			} catch( IOException e ) {
				setStatus( Status.ERROR );
				// TODO: improve error message
				report.setErrorMessage( e.toString() );
				getLogger().log( Level.WARNING, null, e );
				report.close();
				return false;
			} catch( Throwable t ) {
				validatorFailure( t );
				return false;
			}

			for( Validator v : validators ) {
				try {
					v.endAnalysis();
				} catch( Throwable t ) {
					validatorFailure( t );
					return false;
				}
				if( Thread.interrupted() )
					return false;
			}

			report.close();

			Status finalStatus = Status.ERROR;
			switch( report.getValidity() ) {
				case PASS: finalStatus = Status.PASSED; break;
				case WARN: finalStatus = Status.WARNINGS; break;
				case FAIL: finalStatus = Status.FAILED; break;
			}
			setStatus( finalStatus );

		} catch( IOException e ) {
			getLogger().log( Level.SEVERE, null, e );
			setStatus( Status.ERROR );
			// TODO: improve error message
			report.setErrorMessage( e.toString() );
			getLogger().log( Level.WARNING, null, e );
			report.close();
			return false;
		} finally {
			if( in != null ) try { in.close(); } catch( IOException e ) {}
		}
		return true;
	}

	/**
	 * When a file is not thought to be an MP3 file, this method is called
	 * to try to guess the file type and create an appropriate message.
	 */
	private void badFileType() {
		String guess = null;
		try {
			guess = FileTypeDeducer.deduceFileType( localFile );
		} catch( IOException e ) {
			// treat as if we couldn't identify the file
		}
		String message = string( "error-invalid" );
		if( guess != null ) {
			message += "<br>" + string( "error-invalid-guess-type", guess );
		}
		setErrorMessage( message );
	}


	/**
	 * Sets the model that has control of this file.
	 * @param owner the table that this file belongs in
	 */
	synchronized void setOwner( FileTableModel owner ) {
		this.owner = owner;
	}

	/**
	 * Adds this file back to the analysis queue. This has no effect if the file
	 * is still downloading or is already in the queue.
	 * 
	 * @throws IllegalStateException if the file has been disposed
	 */
	public void reanalyze() {
		Status s = getStatus();
		// we are still downloading, so the file already has an analysis pending
		if( s == Status.DOWNLOADING || s == Status.QUEUED )
			return;
		if( s == Status.DISPOSED )
			throw new IllegalStateException( "file has been released using dispose()" );
		queueForAnalysis( WORKER_TASK_ANALYZE );
	}


	/**
	 * Returns <code>true</code> if this <code>LibriVoxAudioFile</code> is currently busy being
	 * downloaded or analyzed.
	 * @return <code>true</code> if the file has not either been completely
	 *     analyzed or encountered an error
	 */
	public boolean isBusy() {
		Status s = getStatus();
		return s == Status.ANALYZING || s == Status.DOWNLOADING;
	}

	/**
	 * Returns <code>true</code> if this file has finished processing; this is
	 * similar to <code>!isBusy()</code>, but it also returns <code>false</code> if the
	 * file is still in the queue and hasn't started analysis yet.
	 * @return
	 */
	public boolean isDone() {
		Status s = getStatus();
		return s != Status.QUEUED && s != Status.ANALYZING && s != Status.DOWNLOADING;
	}

	/**
	 * Return <code>true</code> if this <code>LibriVoxAudioFile</code> is a local
	 * file (rather than one obtained over the network a via a URL).
	 * @return <code>true</code> if this is a local file
	 */
	public boolean isLocal() {
		return !localFileIsTemporary;
	}

	/**
	 * Rename the file
	 * @param newName
	 * @return
	 */
	public boolean rename( String newName ) {
		if( !isLocal() )
			throw new UnsupportedOperationException( "can't rename URLs" );
		File dest = new File( localFile.getParentFile(), newName );
		return localFile.renameTo( dest );
	}

	/**
	 * Returns the name (without any path information) of the file represented
	 * by this <code>LibriVoxAudioFile</code>. This is the intended name
	 * for the file as it might appear in a catalog.
	 * This might not be the same as <code>getLocalFile().getName()</code>,
	 * because files that are obtained from URL sources will return
	 * a randomly-named temporary file.
	 *
	 * @return this <code>LibriVoxAudioFile</code>'s file name
	 */
	public String getFileName() {
		String path = source.getPath();
		try {
			path = URLDecoder.decode( path, "utf-8" );
		} catch( UnsupportedEncodingException e ) {
			throw new AssertionError();
		}
		int nameStart = path.lastIndexOf( '/' ) + 1;
		return path.substring( nameStart );
	}

	public File getLocalFile() {
		return localFile;
	}

	public URL getSource() {
		return source;
	}

	public Status getStatus() {
		return status;
	}

	public MP3FileMetadata getMetadata() {
		return metadata;
	}

	private synchronized void setStatus( Status status ) {
		this.status = status;
		if( !isBusy() ) {
			setCurrentProgress( -1L );
			// fireProgressUpdate() will be called from setCurrentProgress()
		} else {
			fireProgressUpdate();
		}
	}

	public synchronized String getErrorMessage() {
		return errorMessage;
	}

	protected synchronized void setErrorMessage( String errorMessage ) {
		report.setErrorMessage( errorMessage );
		report.close();
		setStatus( Status.ERROR );
		this.errorMessage = errorMessage;
	}

	/**
	 * Cancel the analysis of this file, if any. If the file is being downloaded
	 * or analyzed, this process will be interrupted.
	 * If <code>waitForJobToCancel</code> is <code>true</code>, then this method
	 * will not return until the thread performing the analysis stops.
	 * (If in doubt, set this to <code>true</code>.)
	 *
	 * @param waitForJobToCancel if <code>true</code>, wait for the worker thread
	 *     to stop before returning
	 */
	private synchronized void cancelAnalysis( boolean waitForJobToCancel ) {
		if( jobToken != null ) {
			jobToken.cancel();
			if( waitForJobToCancel ) {
				jobToken.waitUntilDone();
			}
			jobToken = null;
		}
		setStatus( Status.QUEUED );
	}

	public synchronized void dispose() {
		MetadataEditorLinkFactory.unlink( this );
		setOwner( null );
		cancelAnalysis( false );
		if( localFileIsTemporary && localFile != null ) {
			localFile.delete();
		}
		setStatus( Status.DISPOSED );
	}

	public String getInformationReport() {
		String doc = report.getInformationReport();
		if( doc == null ) {
			doc = Report.getDefaultDocument();
		}
		return doc;
	}

	public String getValidationReport() {
		String doc = report.getValidationReport();
		if( doc == null ) {
			doc = Report.getDefaultDocument();
		}
		return doc;
	}

	private boolean localFileIsTemporary = false;
	private File localFile;
	private URL source;
	private Report report;
	private volatile Status status = Status.QUEUED;
	private String errorMessage;

	private JobManager.JobToken jobToken;

	private volatile long progress = -1L, progressMax = 100L;
	private volatile FileTableModel owner;
	private volatile MP3FileMetadata metadata;

	/**
	 * <b>Important:</b> assumes that setCurrentProgress() will be called shortly;
	 * the call to setCurrentProgress will fire the progress update---
	 * both this and setCurrentProgress should be called from within
	 * a synchronized block (synching on this file).
	 * @param max
	 */
	private void setMaximumProgress( long max ) {
		progressMax = max;
	}

	private synchronized void setCurrentProgress( long value ) {
		progress = value;
		fireProgressUpdate();
	}

	/**
	 * Informs the owning component that the status or progress have changed,
	 * and the display should be updated accordingly. It is safe to call this
	 * from any thread.
	 */
	private void fireProgressUpdate() {
		if( owner != null ) {
			EventQueue.invokeLater( new Runnable() {
				@Override
				public void run() {
					if( owner != null ) {
						owner.progressUpdate( LibriVoxAudioFile.this );
					}
				}
			});
		}
	}

	/**
	 * Returns the current progress value as a floating point value between
	 * 0 and 1, or -1 if the notion of "progress" does not currently apply.
	 * @return a progress value between 0 and 1, or -1
	 */
	public float getCurrentProgress() {
		synchronized( this ) {
			float progress = this.progress;
			float progressMax = this.progressMax;
		}

		if( progress < 0 ) return -1f;
		float p = (float) progress / (float) progressMax;
		if( p > 1f ) p = 1f;
		return p;
	}

	/**
	 * Puts the calling thread to sleep until this file has been analyzed.
	 *
	 * @param timeout the approximate maximum time to wait, in seconds
	 * @return <code>true</code> if the file has been analyzed
	 * @throws InterruptedException if the thread is interrupted while waiting
	 */
	public synchronized boolean waitForReport( int timeout ) throws InterruptedException {
		if( jobToken != null ) {
			if( !jobToken.isDone() ) {
				jobToken.waitFor( timeout );
			}
		}
		return isDone();
	}

	private final int IO_BUFFER_SIZE = 32*1024;
	private final int CONNECT_TIMEOUT = 30*1000;
	private final long FRAME_UPDATE_RATE = 500;
}
