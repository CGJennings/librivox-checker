package ca.cgjennings.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A subclass of <code>java.util.Properties</code> that stores its entries in
 * sorted order.
 *
 * @author Christopher G. Jennings https://cgjennings.ca/contact/
 */
public class SortedProperties extends Properties {

    public SortedProperties(Properties defaults) {
        super(defaults);
    }

    public SortedProperties() {
        super();
    }

    @Override
    public synchronized Enumeration<Object> keys() {
        SortedSet<Object> sorted = new TreeSet<Object>(getComparator());
        sorted.addAll(super.keySet());
        return Collections.enumeration(sorted);
    }

    /**
     * Return a comparator that will be used to sort the keys. If
     * <code>null</code> is returned, keys will be sorted according to their
     * natural order.
     *
     * @return the comparator implementing the sort order, or <code>null</code>
     */
    public synchronized Comparator<Object> getComparator() {
        return comparator;
    }

    /**
     * Set the comparator that will be used to sort the keys. If
     * <code>null</code>, keys will be sorted according to their natural order.
     *
     * @param comparator the comparator implementing the sort order, or
     * <code>null</code>
     */
    public synchronized void setComparator(Comparator<Object> comparator) {
        this.comparator = comparator;
    }
    private Comparator<Object> comparator = null;
}
