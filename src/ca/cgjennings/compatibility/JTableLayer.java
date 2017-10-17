package ca.cgjennings.compatibility;

import java.lang.reflect.Method;
import javax.swing.JTable;

/**
 * Compatibility layer between <code>JTableLayer</code>s in Java 5 and 6.
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
public class JTableLayer {
	public static void setAutoCreateRowSorter( JTable table, boolean createRowSorter ) {
		init();
		if( version == JAVA6 ) {
			H.invoke( autoCreateRowSorter, table, createRowSorter );
		}
	}
	
	public static int convertRowIndexToModel( JTable table, int row ) {
		init();
		if( version == JAVA6 ) {
			return (Integer) H.invoke( convertRowIndexToModel, table, row );
		} else {
			return row;
		}
	}

	public static int convertRowIndexToView( JTable table, int row ) {
		init();
		if( version == JAVA6 ) {
			return (Integer) H.invoke( convertRowIndexToView, table, row );
		} else {
			return row;
		}
	}
		
	private static void init() {
		if( version >= 0 ) return;
		try {
			autoCreateRowSorter = JTable.class.getMethod( "setAutoCreateRowSorter", boolean.class );
			convertRowIndexToModel = JTable.class.getMethod( "convertRowIndexToModel", int.class );
			convertRowIndexToView = JTable.class.getMethod( "convertRowIndexToView", int.class );
			version = JAVA6;
		} catch( NoSuchMethodException e ){
			version = JAVA5;
		}
	}
	
	private static Method autoCreateRowSorter;
	private static Method convertRowIndexToModel;
	private static Method convertRowIndexToView;
			
	private static final int JAVA5 = 5, JAVA6 = 6;
	private static int version = -1;
}
