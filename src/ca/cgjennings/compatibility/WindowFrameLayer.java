package ca.cgjennings.compatibility;

import java.awt.Frame;
import java.awt.Image;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Compatibility layer for <code>Frame</code> objects.
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
public class WindowFrameLayer {
	private WindowFrameLayer() {
	}

	public static void setIconImages( Frame w, List<Image> images ) {
		try {
			Method setIconImages = w.getClass().getMethod( "setIconImages", List.class );
			H.invoke( setIconImages, w, images );
		} catch( NoSuchMethodException e ) {
			if( images.size() > 0 ) {
				w.setIconImage( images.get( 0 ) );
			}
		}
	}
}
