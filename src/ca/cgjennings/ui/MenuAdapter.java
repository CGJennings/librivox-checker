package ca.cgjennings.ui;

import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 * An adapter class for <code>MenuListener</code>s that provides default no-op
 * implementations of all methods.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
public class MenuAdapter implements MenuListener {

    @Override
    public void menuSelected(MenuEvent e) {
    }

    @Override
    public void menuDeselected(MenuEvent e) {
    }

    @Override
    public void menuCanceled(MenuEvent e) {
    }
}
