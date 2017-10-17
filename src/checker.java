/**
 * This class is simply a convenience for starting the application
 * from a JAR file. The {@link #main} method simply passes its arguments
 * on to {@link ca.cgjennings.apps.librivox.Checker#main}.
 * (As a side effect of this, under OS X the menu bar will
 * display the application name simply as "checker" rather
 * than the fully qualified application class name.)
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 * @since 0.91
 */
public final class checker {
	private checker() {}
	public static void main( String[] args ) {
		ca.cgjennings.apps.librivox.Checker.main( args );
	}
}
