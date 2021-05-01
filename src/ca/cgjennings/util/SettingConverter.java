package ca.cgjennings.util;

/**
 * Implemented by classes that can convert a type to and from a string that can
 * be written in a settings file.
 * <p>
 * The general contract of the conversion performed by classes that implement
 * this interface is that the following test must be true for all instances of
 * <tt>O</tt>:
 * <pre>
 *     O.equals( fromSetting( toSetting( O ) ) )
 * </pre>
 *
 * @author Christopher G. Jennings https://cgjennings.ca/contact/
 */
public interface SettingConverter<T> {

    /**
     * Creates a string representation of the state of this object that can be
     * parsed by {@link #fromSetting(java.lang.String)} in order to create an
     * object that is equivalent to <code>object</code>.
     *
     * @return an invertible string representation of this object
     */
    public String toSetting(T object);

    /**
     * Returns an instance of <code>T</code> by parsing
     * <code>settingString</code>. If the parse attempt fails, this method must
     * return <code>null</code>.
     *
     * @param settingValue the string to parse
     * @return an instance of <code>V</code> created from
     * <code>settingValue</code>
     */
    public T fromSetting(String settingValue);
}
