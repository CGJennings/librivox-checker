package ca.cgjennings.compatibility;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.logging.Level;

/**
 * An implementation of {@link DesktopLayer} that uses Java 6's
 * <code>java.awt.Desktop</code> class via reflection.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
class Java6Desktop extends DesktopLayer {

	/**
	 * If Java 6 or newer is available and <code>java.awt.Desktop.isSupported()</code>
	 * returns <code>true</code>, this method returns a <code>Java6Desktop</code>
	 * instance. Otherwise, it returns <code>null</code>.
	 * @return a <code>Java6Desktop</code> instance, if supported on this platform
	 */
	public static Java6Desktop createIfAvailable() {
		try {
			Java6Desktop desktop = new Java6Desktop();
			Class desktopClass = Class.forName( "java.awt.Desktop" );
			Method isDesktopSupported = desktopClass.getMethod( "isDesktopSupported" );
			boolean supported = (Boolean) H.invoke( isDesktopSupported, null );
			if( supported ) {
				Method getDesktop = desktopClass.getMethod( "getDesktop" );
				j6Desktop = H.invoke( getDesktop, null );
				isSupported = desktopClass.getMethod( "isSupported", Class.forName( "java.awt.Desktop$Action" ) );
				browse = desktopClass.getMethod( "browse", URI.class );
				open = desktopClass.getMethod( "open", URI.class );
				edit = desktopClass.getMethod( "edit", URI.class );
				print = desktopClass.getMethod( "print", URI.class );
				mail = desktopClass.getMethod( "mail", URI.class );
				return desktop;
			}
		} catch( AssertionError e ) {
			H.LOGGER.log( Level.SEVERE, "unexptected invoke failure", e );
		} catch( ClassNotFoundException e ) {
		} catch( NoSuchMethodException e ) {
		}
		return null;
	}

	@Override
	public boolean browse( URI uri ) {
		try {
			H.invoke( browse, j6Desktop, uri );
		} catch( AssertionError e ) {
			return false;
		}
		return true;
	}

	@Override
	public boolean open( URI uri ) {
		try {
			H.invoke( open, j6Desktop, uri );
		} catch( AssertionError e ) {
			return false;
		}
		return true;
	}

	@Override
	public boolean edit( URI uri ) {
		try {
			H.invoke( edit, j6Desktop, uri );
		} catch( AssertionError e ) {
			return false;
		}
		return true;
	}

	@Override
	public boolean print( URI uri ) {
		try {
			H.invoke( print, j6Desktop, uri );
		} catch( AssertionError e ) {
			return false;
		}
		return true;
	}

	@Override
	public boolean email( URI uri ) {
		try {
			H.invoke( mail, j6Desktop, uri );
		} catch( AssertionError e ) {
			return false;
		}
		return true;
	}

	private Object getActionObject( int action ) {
		try {
			if( A_OPEN == null ) {
				Class Action = Class.forName( "java.awt.Desktop$Action" );
				A_OPEN = Action.getField( "OPEN" ).get( null );
				A_EDIT = Action.getField( "EDIT" ).get( null );
				A_PRINT = Action.getField( "PRINT" ).get( null );
				A_MAIL = Action.getField( "MAIL" ).get( null );
				A_BROWSE = Action.getField( "BROWSE" ).get( null );
			}
			Object actionInstance;
			switch( action ) {
				case DesktopLayer.BROWSE:
					actionInstance = A_BROWSE;
					break;
				case DesktopLayer.OPEN:
					actionInstance = A_OPEN;
					break;
				case DesktopLayer.EDIT:
					actionInstance = A_EDIT;
					break;
				case DesktopLayer.PRINT:
					actionInstance = A_PRINT;
					break;
				case DesktopLayer.MAIL:
					actionInstance = A_MAIL;
					break;
				default:
					throw new IllegalArgumentException( "unknown action code " + action );
			}
			return actionInstance;
		} catch( ClassNotFoundException e ) {
			H.LOGGER.log( Level.SEVERE, "finding Desktop.Action", e );
			throw new AssertionError();
		} catch( NoSuchFieldException e ) {
			H.LOGGER.log( Level.SEVERE, "finding Desktop.Action field", e );
			throw new AssertionError();
		} catch( IllegalAccessException e ) {
			throw new AssertionError();
		}
	}

	@Override
	public boolean isSupported( int action ) {
		return (Boolean) H.invoke( isSupported, j6Desktop, getActionObject( action ) );
	}
	private static Object A_OPEN;
	private static Object A_EDIT;
	private static Object A_PRINT;
	private static Object A_MAIL;
	private static Object A_BROWSE;
	private static Object j6Desktop;
	private static Method isSupported;
	private static Method browse;
	private static Method open;
	private static Method edit;
	private static Method print;
	private static Method mail;
}
