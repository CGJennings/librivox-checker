package ca.cgjennings.compatibility;

import ca.cgjennings.platform.PlatformSupport;
import java.net.URI;

/**
 * Compatibility layer that provides a simplified version of the
 * <code>java.awt.Desktop</code> API with backwards compatibility.
 * The methods are essentially the same as those in <code>Desktop</code>,
 * except that actions are specified with integer codes rather than an
 * enum, and the action methods return a <code>boolean</code> value
 * to indicate success or failure, rather than throwing an exception.
 * Furthermore, if an action is attempted that is not supported,
 * the action method simply returns <code>false</code> instead of
 * throwing an <code>UnsupportedOperationException</code>.
 * 
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
public abstract class DesktopLayer {
	protected DesktopLayer() {}
	
	public static DesktopLayer getDesktop() {
		if( layer != null )
			return layer;
		
		// try to use java.awt.Desktop if it exists
		// otherwise, see if we know of a workaround		
		layer = Java6Desktop.createIfAvailable();
		
		if( layer == null ) {
			if( PlatformSupport.PLATFORM_IS_OSX ) {
				layer = new ForkingDesktop( "open" );
			} else if( PlatformSupport.PLATFORM_IS_WINDOWS ) {
				layer = new ForkingDesktop( "cmd", "/c", "start" );
			} else {
				layer = new NullDesktop();
			}
		}
		return layer;
	}
	
	public abstract boolean browse( URI uri );
	
	public abstract boolean open( URI uri );
	
	public abstract boolean edit( URI uri );
	
	public abstract boolean print( URI uri );
	
	public abstract boolean email( URI uri );
	
	public abstract boolean isSupported( int action );
	
	/** Action code for determining if browsing is supported. */
	public static final int BROWSE = 1;
	/** Action code for determining if opening is supported. */
	public static final int OPEN = 2;
	/** Action code for determining if editing is supported. */
	public static final int EDIT = 3;
	/** Action code for determining if printing is supported. */
	public static final int PRINT = 4;
	/** Action code for determining if emailing is supported. */
	public static final int MAIL = 5;
	
	
	/** Actions must be in this range to be valid. */
	protected static final int ACTION_MIN = 1, ACTION_MAX = 5;
	
	private static DesktopLayer layer;
	
//	public static void main( String[] args ) {
//		try{
//			System.err.println( DesktopLayer.getDesktop().browse(  new URI("http://cgjennings.ca") ) );
//
//		} catch( Exception e ) {
//			e.printStackTrace();
//		}
//	}
}
