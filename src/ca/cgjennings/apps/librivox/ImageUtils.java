package ca.cgjennings.apps.librivox;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

/**
 * Utility class for dealing with images.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 * @since 0.91
 */
public class ImageUtils {
	private ImageUtils() {}

	/**
	 * Ensure image format is TYPE_INT_RGB, returning a converted image if
	 * necessary.
	 * @param bi the image to guarantee
	 * @return a version of the image in the TYPE_INT_RGB format
	 */
	public static BufferedImage ensureRGB( BufferedImage bi ) {
		if( bi.getType() != BufferedImage.TYPE_INT_RGB ) {
			BufferedImage c = new BufferedImage( bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB );
			Graphics2D g = c.createGraphics();
			try {
				g.drawImage( bi, 0, 0, null );
			} finally {
				g.dispose();
			}
			bi = c;
		}
		return bi;
	}

	/**
	 * Resize an image by a scaling factor.
	 *
	 * @param bi the image
	 * @param factor the factor to multiply the original size by
	 * @return the resized image
	 */
	public static BufferedImage resizeImage( BufferedImage bi, float factor ) {
		if( bi == null ) throw new NullPointerException( "bi" );

		// use multiple passes if shrinking by more than half
		if( factor < 0.5f ) {
			return resizeImage( resizeImage( bi, 0.5f ), 2f*factor );
		}

		int w = (int) (bi.getWidth() * factor + 0.5f);
		int h = (int) (bi.getHeight() * factor + 0.5f);
		if( w == bi.getWidth() && h == bi.getHeight() ) return bi;

		BufferedImage ri = new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB );
		Graphics2D g = ri.createGraphics();
		try {
			g.setRenderingHint( RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY );
			g.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC );
			g.drawImage( bi, 0, 0, w, h, null );
		} finally {
			g.dispose();
		}
		return ri;
	}

	/**
	 * Resizes an image to fit a maximum dimension.
	 * If either the width or height of the image is greater than
	 * <code>maxDimen</code>, the image will be scaled down so that its largest
	 * dimension is <code>maxDimen</code> pixels.
	 *
	 * @param bi the image to fit
	 * @param maxDimen the maximum width and/or height
	 * @return a smaller image, if required, or the original image
	 */
	public static BufferedImage fitImage( BufferedImage bi, int maxDimen ) {
		if( bi == null ) throw new NullPointerException( "bi" );
		float xs = (float) maxDimen / (float) bi.getWidth();
		float ys = (float) maxDimen / (float) bi.getHeight();
		float s = Math.min( xs, ys );
		if( s >= 1f ) return bi;
		return resizeImage( bi, s );
	}

	/**
	 * Creates a temporary file that will last until the
	 * end of the session and which contains the image data in PNG format and
	 * returns a URL for that file.
	 *
	 * @param bi the image to create a local URL for
	 * @return the local, temporary URL
	 * @throws IOException if an I/O error occurs while writing the file
	 */
	public static URL getTemporaryURL( BufferedImage bi ) throws IOException {
		if( bi == null ) throw new NullPointerException( "bi" );
		File temp = File.createTempFile( "lvimg", ".png" );
		temp.deleteOnExit();
		ImageIO.write( bi, "png", temp );
		return temp.toURI().toURL();
	}
}
