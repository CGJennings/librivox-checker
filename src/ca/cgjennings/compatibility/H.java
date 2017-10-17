package ca.cgjennings.compatibility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Invokes methods, converting exceptions into
 * <code>AssertionError</code>s. (The method calls should have been validated
 * by other means before calling them via this helper.)
 * 
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
class H {
	private H() {}
	public static Object invoke( Method m, Object o, Object... params ) {
		try {
			return m.invoke( o, params );
		} catch( IllegalAccessException e ) {
			throw new AssertionError( "IllegalAccessExeption: " + m.getName() );
		} catch( IllegalArgumentException e ) {
			throw new AssertionError( "IllegalArgumentException: " + m.getName() );
		} catch( InvocationTargetException e ) {
			LOGGER.log( Level.SEVERE, "compatibility layer target exception", e );
			e.getCause().printStackTrace();
			throw new AssertionError( "InvocationTargetException: " + m.getName() );
		}
	}
	
	public static Logger LOGGER = Logger.getLogger( "ca.cgjennings.compatibility" );
}
