package ca.cgjennings.compatibility;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Compatibility layer for the Java 5 <code>File</code> class.
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
public class FileLayer {
	private FileLayer() {}
	
	public static void setWritable( File f, boolean writable ) {
		try {
			Method setWritable = File.class.getMethod( "setWritable", boolean.class );
			H.invoke( setWritable, f, writable );
		} catch( NoSuchMethodException e ) {			
		}		
	}
}
