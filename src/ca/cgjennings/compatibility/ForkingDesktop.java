package ca.cgjennings.compatibility;

import java.io.IOException;
import java.net.URI;

/**
 * An implementation of {@link DesktopLayer} that starts a new process
 * by appending the requested URI or file to a standard command set
 * when the <code>ForkingDesktop</code> was created.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
public class ForkingDesktop extends DesktopLayer {
	/**
	 * Create a new <code>ForkingDesktop</code> that will execute commands
	 * by adding the file or URI argument to the command and arguments
	 * indicated by <code>commandBase</code>.
	 * @param commandBase an array of strings that describe a command and arguments
	 *     to use to execute desktop commands
	 */
	public ForkingDesktop( String... commandBase ) {
		this.commandBase = commandBase;
	}
	
	private boolean exec( URI uri ) {
		try {
			String[] command = new String[ commandBase.length + 1 ];
			for( int i=0; i<command.length-1; ++i ) {
				command[i] = commandBase[i];
			}
			command[ commandBase.length ] = uri.toASCIIString();
			
			ProcessBuilder pb = new ProcessBuilder( command );
			pb.start();
			return true;
		} catch( IOException e ) {
			return false;
		}
	}
	
	private String[] commandBase;

	@Override
	public boolean browse( URI uri ) {
		return exec( uri );
	}

	@Override
	public boolean open( URI uri ) {
		return exec( uri );
	}

	@Override
	public boolean edit( URI uri ) {
		return exec( uri );
	}

	@Override
	public boolean print( URI uri ) {
		return false;
	}

	@Override
	public boolean email( URI uri ) {
		return exec( uri );
	}

	@Override
	public boolean isSupported( int action ) {
		switch( action ) {
			case BROWSE: return true;
			case MAIL: return true;
			case OPEN: return true;
			case EDIT: return true;
			case PRINT: return false;
			default:
				throw new IllegalArgumentException( "invalid action code " + action );
		}
	}
}
