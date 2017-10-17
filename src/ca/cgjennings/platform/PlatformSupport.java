package ca.cgjennings.platform;

import java.awt.Event;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

/**
 * This utility class provides methods that help integrate an application
 * into the native operating system more cleanly. It is pure Java code.
 *
 * <p><b>Note:</b> This version hacked up to remove code not needed for Checker.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
public class PlatformSupport {

	/** This class is not instantiable. */
	private PlatformSupport() {
	}

	/**
	 * Call this method before opening any Swing windows to install the native
	 * look and feel. If they are available in the classpath, various OS-specific
	 * optimizations and patches may also be installed.
	 */
	public static void installNativeLookAndFeel() {
		// set custom system properties
		String[] newProperties = {
			"apple.laf.useScreenMenuBar", "true",
		};

		Properties p = System.getProperties();
		for( int i = 0; i < newProperties.length; i += 2 ) {
			p.setProperty( newProperties[i], newProperties[i + 1] );
		}
		System.setProperties( p );

		// set look & feel
		java.awt.Toolkit.getDefaultToolkit().setDynamicLayout( true );
		JFrame.setDefaultLookAndFeelDecorated( true );
		try {
			// install native l&f
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
			// if this is found in the classpath, it will install fixes to the Windows l&f implementation
			UIManager.setLookAndFeel( "net.java.plaf.windows.WindowsLookAndFeel" );
		} catch( Exception e ) {
		}
	}

	/**
	 * Parse a string to create a <code>KeyStroke</code> appropriate as an
	 * accelerator for the native OS. The string must have the following syntax:
	 * <pre>
	 *    &lt;modifiers&gt;* (&lt;typedID&gt; | &lt;pressedReleasedID&gt;)
	 *
	 *    modifiers := menu | shift | control | ctrl | meta | alt | altGraph
	 *    typedID := typed &lt;typedKey&gt;
	 *    typedKey := string of length 1 giving Unicode character.
	 *    pressedReleasedID := (pressed | released) key
	 *    key := KeyEvent key code name, without the "VK_" prefix.
	 * </pre>
	 * If typed, pressed or released is not specified, pressed is assumed.
	 * <p>
	 * The special pseudo-modifier "menu" will be converted into the correct
	 * menu accelerator key for the native platform. For example, "menu X" will
	 * be treated as "ctrl X" on the Windows platform, but as "meta X" (which
	 * is Command key + X) on Max OS X platform. Note that there is no way
	 * to determine from the returned <code>KeyStroke</code> instance whether
	 * the "menu" modifier was used or not.
	 *
	 * @param stroke a string formatted as above
	 * @return a <code>KeyStroke</code> object representing the specified key event
	 */
	public static KeyStroke getKeyStroke( String stroke ) {
		if( menuKeyReplacement == null ) {
			int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
			switch( mask ) {
				case Event.ALT_MASK:
					menuKeyReplacement = "alt ";
					break;
				case Event.META_MASK:
					menuKeyReplacement = "meta ";
					break;
				case Event.SHIFT_MASK:
					menuKeyReplacement = "shift ";
					break;
				default:
					menuKeyReplacement = "ctrl ";
					if( mask != Event.CTRL_MASK ) {
						System.err.println( "Warning: unknown menu accelerator mask; using CTRL instead" );
					}
			}
		}
		Matcher menuPatternMatcher = menuKeyPattern.matcher( stroke );
		stroke = menuPatternMatcher.replaceAll( menuKeyReplacement );
		return KeyStroke.getKeyStroke( stroke );
	}
	private static final Pattern menuKeyPattern = Pattern.compile( "menu\\s", Pattern.CASE_INSENSITIVE );
	private static String menuKeyReplacement;

	/** True if and only if the JVM is running on an Apple OS X-based operating system. */
	public static final boolean PLATFORM_IS_OSX = System.getProperty( "os.name" ).toLowerCase().startsWith( "mac os x" );
	/** True if and only if the JVM is running on a Windows-based operating system. */
	public static final boolean PLATFORM_IS_WINDOWS = System.getProperty( "os.name" ).toLowerCase().startsWith( "windows" );

	/**
	 * A variant look and feel for OS X that is popular with developers. It needs
	 * to be handled differently from Apple's Aqua L&F in some cases.
	 * @return <code>true</code> if Quaqua is the current L&F
	 */
	public static boolean isUsingQuaquaLookAndFeel() {
		return UIManager.getLookAndFeel().getClass().getName()
				.equals( "ch.randelshofer.quaqua.QuaquaLookAndFeel" );
	}

	/**
	 * Swaps the buttons identified as <code>ok</code> and <code>cancel</code>
	 * when running under OS X. This assumes that the buttons are in the usual
	 * Windows and X order, where Cancel comes last and OK comes just to its left.
	 * What is swapped: default button status, text, enabled state,
	 * and action listeners. (It does not currently swap icons because no
	 * Checker dialogs use icons on their OK or Cancel buttons.)
	 *
	 * @param ok the button that was designed to represent OK
	 * @param cancel the button that was designed to represent Cancel
	 */
	public static void correctOKCancelOrder( JButton ok, JButton cancel ) {
		if( !PLATFORM_IS_OSX ) return;

		if( ok == null ) throw new NullPointerException( "ok" );
		if( cancel == null ) throw new NullPointerException( "cancel" );

		// swap text
		String t = ok.getText();
		ok.setText( cancel.getText() );
		cancel.setText( t );

		// swap enable state
		boolean e = ok.isEnabled();
		ok.setEnabled( cancel.isEnabled() );
		cancel.setEnabled( e );

		// swap action listeners
		ActionListener[] okLi = ok.getActionListeners();
		ActionListener[] caLi = cancel.getActionListeners();
		removeActionListeners( ok, okLi );
		removeActionListeners( cancel, caLi );
		addActionListeners( ok, caLi );
		addActionListeners( cancel, okLi );

		// swap default button
		if( ok.isDefaultButton() ) {
			ok.getRootPane().setDefaultButton( cancel );
		} else if( cancel.isDefaultButton() ) {
			cancel.getRootPane().setDefaultButton( ok );
		}
	}
	private static void removeActionListeners( JButton b, ActionListener[] subtract ) {
		for( ActionListener al : subtract ) {
			b.removeActionListener( al );
		}
	}
	private static void addActionListeners( JButton b, ActionListener[] add ) {
		for( ActionListener al : add ) {
			b.addActionListener( al );
		}
	}


	/**
	 * Given the OK and Cancel buttons for a dialog, this will return
	 * whichever button represents OK after calling {@link #correctOKCancelOrder}.
	 * This is useful if you want to, for example, disable the OK button in
	 * response to user input.
	 *
	 * @param ok the button that was designed to represent OK
	 * @param cancel the button that was designed to represent Cancel
	 * @return the button that represents OK after order correction
	 */
	public static JButton getOK( JButton ok, JButton cancel ) {
		return PLATFORM_IS_OSX ? cancel : ok;
	}

	/**
	 * Given the OK and Cancel buttons for a dialog, this will return
	 * whichever button represents Cancel after calling {@link #correctOKCancelOrder}.
	 *
	 * @param ok the button that was designed to represent OK
	 * @param cancel the button that was designed to represent Cancel
	 * @return the button that represents Cancel after order correction
	 */
	public static JButton getCancel( JButton ok, JButton cancel ) {
		return PLATFORM_IS_OSX ? ok : cancel;
	}
}
