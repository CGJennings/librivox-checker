package ca.cgjennings.util;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * A container for application settings.
 *
 * @author Christopher G. Jennings https://cgjennings.ca/contact/
 */
public class Settings {

    private SortedProperties properties;

    public Settings() {
        properties = new SortedProperties();
    }

    public Settings(Settings defaults) {
        properties = new SortedProperties(defaults.properties);
    }

    /**
     * Return a setting value, or <code>null</code> if it is not defined.
     * <p>
     * All other <code>get</code> methods delegate to this method.
     *
     * @param key the key to fetch the value of
     * @return the value of key, or <code>null</code>
     */
    public String get(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            value = lookUpKey(key);
        }
        return value;
    }

    public String get(String key, String defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        } else {
            return value;
        }
    }

    /**
     * This method may be overridden to provide a customized lookup mechanism
     * for providing default values. The search order for settings is as
     * follows:
     * <ol>
     * <li>the current settings instance
     * <li>the (possibly empty) chain of parent settings instances created using
     * the constructor that takes a <code>defaults</code> argument
     * <li>the value returned by this method, which is <code>null</code> in this
     * base class
     * </ol>
     *
     * @param key the key with no value set in this settings or its parent chain
     * @return the value to return for this key
     */
    protected String lookUpKey(String key) {
        return null;
    }

    /**
     * Change the value of a setting.
     * <p>
     * All other <code>set</code> methods delegate to this method.
     *
     * @param key the key to change the value of
     * @param value the new setting value
     */
    public void set(String key, String value) {
        if (key == null) {
            throw new NullPointerException("null key");
        }
        if (value == null) {
            throw new NullPointerException("null value");
        }
        properties.setProperty(key, value);
    }

    /**
     * Set the value of <code>key</code> to the setting string representation of
     * <code>value</code>.
     *
     * @param key the key to set the value of
     * @param converter the value conversion method to use
     * @param value the object whose string representation will become the the
     * value of <code>key</code>
     */
    public <T> void set(String key, SettingConverter<T> converter, T value) {
        set(key, converter.toSetting(value));
    }

    /**
     * Get the value of <code>key</code> as an instance of type <code>T</code>
     * by converting the string value of the key using <code>parser</code>. This
     * method returns <code>defaultValue</code> if the conversion fails or if
     * <code>key</code> is not defined.
     *
     * @param key
     * @param parser
     */
    public <T> T get(String key, SettingConverter<T> converter, T defaultValue) {
        if (converter == null) {
            throw new NullPointerException("converter");
        }
        T value = null;
        String v = get(key);
        if (v != null) {
            value = converter.fromSetting(v);
        }
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Remove a setting from this container, returning the value to its default.
     *
     * @param key the key to reset
     */
    public void remove(String key) {
        if (key == null) {
            throw new NullPointerException("null key");
        }
        properties.remove(key);
    }

    /**
     * Parse a <code>Color</code> in hexadecimal RRGGBB format from key. The
     * value may be prefixed with a '#'character (which follows the syntax of
     * colours in HTML). The value may optionally include a leading alpha
     * component (AARRGGBB) to specify a translucent colour. If the value is not
     * defined or cannot be parsed, the default value is returned.
     */
    public Color getColor(String key, Color defaultValue) {
        Color c = defaultValue;
        String s = get(key);
        if (s != null) {
            int x = 0;
            try {
                if (s.length() > 1 && s.charAt(0) == '#') {
                    s = s.substring(1);
                    x = Integer.parseInt(s, 16);
                } else {
                    x = Integer.parseInt(s, 16);
                }
                c = new Color(x, s.length() > 6);
            } catch (NumberFormatException e) {
                warning("unable to parse colour " + key + ": " + s);
            }
        }
        return c;
    }

    /**
     * Compose a resource URL for <code>clazz</code> using the value of
     * <code>key</code>. This is similar to calling
     * <code>clazz.getResource</code> using the setting value for
     * <code>key</code> as the resource name. The value of <code>clazz</code>
     * may be <code>null</code>, in which case the resource will be fetched
     * using <code>ClassLoader.getSystemResource</code>.
     * <p>
     * If the resource does not exist, or if the key is not defined and
     * <code>defaultValue</code> is <code>null</code>, this method will return
     * <code>null</code>. Otherwise, it returns the URL of the resource.
     *
     * @param clazz a class to use as the base for a relative URL (may be
     * <code>null</code>)
     * @param key the settings key which holds the resource name
     * @param defaultValue the default resource name to use if none is defined
     * in the settings (may be <code>null</code>)
     * @return the resource URL, or <code>null</code> if the resource or
     * resource name does not exist
     */
    public URL getResource(Class clazz, String key, String defaultValue) {
        String value = get(key, defaultValue);

        if (value == null) {
            return null;
        }
        URL url = null;
        if (clazz == null) {
            url = ClassLoader.getSystemResource(value);
        } else {
            url = clazz.getResource(value);
        }

        return url;
    }

    /**
     * A convenience method for <code>getResource( null, key, null )</code>.
     *
     * @param key the settings key which holds the resource name
     * @return the resource URL, or <code>null</code> if the resource or
     * resource name does not exist
     * @see
     * {@link #getResource(java.lang.Class, java.lang.String, java.lang.String)}
     */
    public URL getResource(String key) {
        return getResource(null, key, null);
    }

    /**
     * Return the requested resource as a stream, or return <code>null</code> if
     * the resource does not exist, if <code>key</code> is not defined and
     * <code>defaultValue</code> is <code>null</code>, or if the stream cannot
     * be opened.
     *
     * @param clazz a class to use as the base for a relative URL (may be
     * <code>null</code>)
     * @param key the settings key which holds the resource name
     * @param defaultValue the default resource name to use if none is defined
     * in the settings (may be <code>null</code>)
     * @return a stream for the resource, or <code>null</code> if the resource
     * or resource name does not exist
     */
    public InputStream getResourceAsStream(Class clazz, String key, String defaultValue) {
        URL url = getResource(clazz, key, defaultValue);

        if (url != null) {
            try {
                return url.openStream();
            } catch (IOException e) {
                // will return null
                warning("can't open resource stream", e);
            }
        }
        return null;
    }

    /**
     * A convenience method for
     * <code>getResourceAsStream( null, key, null )</code>.
     *
     * @param key the settings key which holds the resource name
     * @return a stream for the resource, or <code>null</code> if the resource
     * or resource name does not exist
     * @see
     * {@link #getResourceAsStream(java.lang.Class, java.lang.String, java.lang.String)}
     */
    public InputStream getResourceAsStream(String key) {
        return getResourceAsStream(null, key, null);
    }

    /**
     * Returns an image resource. The URL of the image is determined using
     * {@link #getResource(java.lang.Class, java.lang.String, java.lang.String)}.
     * If the URL resolves to <code>null</code> or the image cannot be read,
     * this method returns <code>null</code>.
     *
     * @param clazz a class to use as the base for a relative URL (may be
     * <code>null</code>)
     * @param key the settings key which holds the resource name
     * @param defaultValue the default resource name to use if none is defined
     * in the settings (may be <code>null</code>)
     * @return the image pointed to by the value of the setting, or
     * <code>null</code>
     */
    public BufferedImage getImage(Class clazz, String key, String defaultValue) {
        URL url = getResource(clazz, key, defaultValue);

        BufferedImage image = null;
        if (url != null) {
            try {
                image = ImageIO.read(url);
                if (image == null) {
                    warning("not a valid image: " + url);
                }
            } catch (IOException e) {
                // image is null
                warning("IOException while reading image");
            }
        }
        return image;
    }

    /**
     * A convenience method for <code>getImage( null, key, null )</code>.
     *
     * @param key the settings key which holds the resource name
     * @return the image pointed to by the value of the setting, or
     * <code>null</code>
     * @see
     * {@link #getImage(java.lang.Class, java.lang.String, java.lang.String)}
     */
    public BufferedImage getImage(String key) {
        return getImage(null, key, null);
    }

    /**
     * Look up a boolean value; any of the following values will be interpreted
     * as <code>true</code>: yes, true, 1, on, allow, enable. All other values
     * will return <code>false</code>. Case is ignored.
     *
     * @param key the settings key with the desired value
     * @param defaultValue the value to use if the key does not exist
     * @return a <code>boolean</code> value derived from the value of
     * <tt>key</tt>
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        boolean value = defaultValue;
        String s = get(key);
        if (s != null) {
            s = s.toLowerCase(Locale.US);
            if (s.equals("yes") || s.equals("true") || s.equals("1") || s.equals("on") || s.equals("allow") || s.equals("enable")) {
                value = true;
            } else {
                value = false;
            }
        }
        return value;
    }

    /**
     * Set the value of a <tt>boolean</tt> setting key.
     *
     * @param key the name of the key to set
     * @param value the value to set for <code>key</code>
     */
    public void setBoolean(String key, boolean value) {
        set(key, value ? "true" : "false");
    }

    /**
     * Parse an integer from the value of <code>key</code>. If the setting is
     * undefined, <code>defaultValue</code> is returned. If the value of the key
     * is prefixed by any of <tt>#</tt>, character,
     * <tt>0x</tt>, or <tt>0X</tt>, it will be parsed as a hexadecimal number.
     *
     * @param key the settings key with the desired value
     * @param defaultValue the default to use if <code>key</code> is undefined
     * @return an integer derived from the value of <code>key</code>, or
     * <code>defaultValue</code>
     */
    public int getInt(String key, int defaultValue) {
        int value = defaultValue;
        String v = get(key);
        if (v != null) {
            try {
                if (v.length() > 0 && v.charAt(0) == '#') {
                    value = Integer.parseInt(v.substring(1), 16);
                } else if (v.length() > 1 && v.charAt(0) == '0' && (v.charAt(1) == 'x' || v.charAt(1) == 'X')) {
                    value = Integer.parseInt(v.substring(2), 16);
                } else {
                    if (v.length() > 1 && v.charAt(0) == '+') {
                        value = Integer.parseInt(v.substring(1));
                    } else {
                        value = Integer.parseInt(v);
                    }
                }
            } catch (NumberFormatException e) {
                warning("unable to parse: " + v);
                // will return default
            }
        }
        return value;
    }

    /**
     * Set the value of an <tt>integer</tt> setting key.
     *
     * @param key the name of the key to set
     * @param value the value to set for <code>key</code>
     */
    public void setInt(String key, int value) {
        set(key, String.valueOf(value));
    }

    /**
     * Parse a float from the value of <code>key</code>. If the setting is
     * undefined, <code>defaultValue</code> is returned.
     *
     * @param key the settings key with the desired value
     * @param defaultValue the default to use if <code>key</code> is undefined
     * @return a float derived from the value of <code>key</code>, or
     * <code>defaultValue</code>
     */
    public float getFloat(String key, float defaultValue) {
        float value = defaultValue;
        String v = get(key);
        if (v != null) {
            try {
                if (v.length() > 1 && v.charAt(0) == '+') {
                    value = Float.parseFloat(v.substring(1));
                } else {
                    value = Float.parseFloat(v);
                }
            } catch (NumberFormatException e) {
                warning("unable to parse: " + v);
                // will return default
            }
        }
        return value;
    }

    /**
     * Set the value of a <tt>float</tt> setting key.
     *
     * @param key the name of the key to set
     * @param value the value to set for <code>key</code>
     */
    public void setFloat(String key, float value) {
        set(key, String.valueOf(value));
    }

    /**
     * Parse a double from the value of <code>key</code>. If the setting is
     * undefined, <code>defaultValue</code> is returned.
     *
     * @param key the settings key with the desired value
     * @param defaultValue the default to use if <code>key</code> is undefined
     * @return a double derived from the value of <code>key</code>, or
     * <code>defaultValue</code>
     */
    public double getDouble(String key, double defaultValue) {
        double value = defaultValue;
        String v = get(key);
        if (v != null) {
            try {
                if (v.length() > 1 && v.charAt(0) == '+') {
                    value = Double.parseDouble(v.substring(1));
                } else {
                    value = Double.parseDouble(v);
                }
            } catch (NumberFormatException e) {
                warning("unable to parse: " + v);
                // will return default
            }
        }
        return value;
    }

    /**
     * Set the value of a <tt>double</tt> setting key.
     *
     * @param key the name of the key to set
     * @param value the value to set for <code>key</code>
     */
    public void setDouble(String key, double value) {
        set(key, String.valueOf(value));
    }

    /**
     * Return a rectangle by parsing a comma-separated list of integers in x, y,
     * width, height order.
     *
     * @param key the key of the setting to parse
     * @param defaultValue the default to use if the key is missing
     * @return a rectangle with coordinates taken from <code>key</code>, or
     * <code>defaultValue</code>
     */
    public Rectangle getRectangle(String key, Rectangle defaultValue) {
        Rectangle rect = defaultValue;
        String v = get(key);
        if (v != null) {
            int[] n = parseIntList(v, 4, 4);
            if (n != null) {
                rect = new Rectangle(n[0], n[1], n[2], n[3]);
            }
        }
        return rect;
    }

    /**
     * Set the value of a <tt>Rectangle</tt> setting key.
     *
     * @param key the name of the key to set
     * @param value the value to set for <code>key</code>
     */
    public void setRectangle(String key, Rectangle value) {
        set(key,
                "" + value.x + "," + value.y + "," + value.width + "," + value.height);
    }

    /**
     * Return a rectangle by parsing a comma-separated list of numbers in x, y,
     * width, height order.
     *
     * @param key the key of the setting to parse
     * @param defaultValue the default to use if the key is missing
     * @return a rectangle with coordinates taken from <code>key</code>, or
     * <code>defaultValue</code>
     */
    public Rectangle2D getRectangle2D(String key, Rectangle2D defaultValue) {
        Rectangle2D rect = defaultValue;
        String v = get(key);
        if (v != null) {
            double[] n = parseDoubleList(v, 4, 4);
            if (n != null) {
                rect = new Rectangle2D.Double(n[0], n[1], n[2], n[3]);
            }
        }
        return rect;
    }

    /**
     * Set the value of a <tt>Rectangle2D</tt> setting key.
     *
     * @param key the name of the key to set
     * @param value the value to set for <code>key</code>
     */
    public void setRectangle2D(String key, Rectangle2D value) {
        set(key,
                "" + value.getX() + "," + value.getY() + "," + value.getWidth() + "," + value.getHeight());
    }

    /**
     * Return a <code>Point</code> by parsing a comma-separated list of integers
     * in x, y order.
     *
     * @param key the key of the setting to parse
     * @param defaultValue the default to use if the key is missing
     * @return a <code>Point</code> with coordinates taken from
     * <code>key</code>, or <code>defaultValue</code>
     */
    public Point getPoint(String key, Point defaultValue) {
        Point point = defaultValue;
        String v = get(key);
        if (v != null) {
            int[] n = parseIntList(v, 2, 2);
            if (n != null) {
                point = new Point(n[0], n[1]);
            }
        }
        return point;
    }

    /**
     * Set the value of a <tt>Point</tt> setting key.
     *
     * @param key the name of the key to set
     * @param value the value to set for <code>key</code>
     */
    public void setPoint(String key, Point value) {
        set(key,
                "" + value.x + "," + value.y);
    }

    /**
     * Return a <code>Point2D</code> by parsing a comma-separated list of
     * numbers in x, y order.
     *
     * @param key the key of the setting to parse
     * @param defaultValue the default to use if the key is missing
     * @return a <code>Point</code> with coordinates taken from
     * <code>key</code>, or <code>defaultValue</code>
     */
    public Point2D getPoint2D(String key, Point2D defaultValue) {
        Point2D point = defaultValue;
        String v = get(key);
        if (v != null) {
            double[] n = parseDoubleList(v, 2, 2);
            if (n != null) {
                point = new Point2D.Double(n[0], n[1]);
            }
        }
        return point;
    }

    /**
     * Set the value of a <tt>Point</tt> setting key.
     *
     * @param key the name of the key to set
     * @param value the value to set for <code>key</code>
     */
    public void setPoint2D(String key, Point2D value) {
        set(key,
                "" + value.getX() + "," + value.getY());
    }

    public void read(URL url) throws IOException {
        // this simplifies error handling when reading settings
        // using <class>.getResource()
        if (url == null) {
            throw new FileNotFoundException("null URL");
        }
        InputStream in = null;
        try {
            in = url.openStream();
            read(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     *
     * JAVA 5 only offers properties.load(InputStream)
     *
     *
     * public void read( InputStream in ) throws IOException { try { read( new
     * InputStreamReader( in, "utf-8" ) ); } catch( UnsupportedEncodingException
     * e ) { severe( "no UTF-8", e ); throw new IOException( "no UTF-8" ); } }
     *
     * public void read( Reader r ) throws IOException { properties.load( r ); }
     */
    public void read(InputStream in) throws IOException {
        properties.load(in);
    }

    /*
	 * JAVA 5 only offers properties.store( OutputStream, String );
	 * 
	 * 
	public void write( OutputStream out, String comments ) throws IOException {
		try {
			write( new OutputStreamWriter( out, "utf-8" ), comments );
		} catch( UnsupportedEncodingException e ) {
			severe( "no UTF-8", e );
			throw new IOException( "no UTF-8" );
		}
	}


	public void write( Writer out, String comments ) throws IOException {
		properties.store( out, comments );
	}
     */
    public void write(OutputStream out, String comments) throws IOException {
        properties.store(out, comments);
    }

    /**
     * Stores these settings in a platform- and user-specific storage facility.
     * The stored settings can be retrieved during a future session by calling
     * {@link #readFromLocalStorage(java.lang.Class)} with the same class value.
     *
     * @param target the class to associate the settings with
     * @since 1.0
     */
    public void writeToLocalStorage(Class<?> target) {
        Preferences prefs = Preferences.userNodeForPackage(target);
        try {
            for (String key : properties.stringPropertyNames()) {
                String value = get(key);
                if (value != null) {
                    prefs.put(key, value);
                }
            }
            prefs.flush();
        } catch (Exception ex) {
            severe("unable to write preference store " + target, ex);
        }
    }

    /**
     * Reads these settings in a platform- and user-specific storage facility.
     * Settings defined in the store will be merged with the existing settings
     * (settings not defined in the store will not be modified).
     *
     * @param target the class to associate the settings with
     * @since 1.0
     */
    public void readFromLocalStorage(Class<?> target) {
        Preferences prefs = Preferences.userNodeForPackage(target);
        try {
            for (String key : prefs.keys()) {
                String value = prefs.get(key, null);
                if (value != null) {
                    set(key, value);
                }
            }
        } catch (BackingStoreException ex) {
            severe("unable to read preference store " + target, ex);
        }
    }

    /**
     * This is a utility method that attempts to locate a
     * <code>Locale</code>-specific version of any resource. It returns a result
     * similar to calling
     * <pre>
     * thisClass.getResource( namePrefix + nameSuffix )
     * </pre> except that it will search for locale-specific versions of the
     * resource using a similar naming convention as that used for
     * <code>ResourceBundle</code>s. That is, names will be generated by
     * inserting a string between <code>namePrefix</code> and
     * <code>nameSuffix</code> that consists of an underscore and one or more
     * underscore-separated components of the locale name in language, country,
     * variant order.
     * <p>
     * Localized versions of the resource are searched for using
     * <code>thisClass.getResource</code> starting with the most specific locale
     * available (language, country, and variant) and working backwards.
     * <p>
     * For example, calling
     * <pre>
     * findResourceForLocale( thisClass, Locale.CANADA_FRENCH, "image", ".png" )
     * </pre> would search first for a resource with the name
     * <code>image_fr_CA.png</code>, then <code>image_fr.png</code>, and finally
     * <code>image.png</code>. As soon as <code>getResource</code> returned a
     * non-<code>null</code> value for one of these resources, that URL would be
     * returned. If <code>null</code> was returned for all of these calls, then
     * <code>null</code> is returned.
     *
     * @param thisClass the class to use as a base for the search
     * @param locale the locale to find the best-matching localized version for
     * @param namePrefix the prefix of the name to search for
     * @param nameSuffix the suffix of the name to search for
     * @return the URL of the best available version of the resource, or
     * <code>null</code> if the resource does not exist
     */
    public static URL findResourceForLocale(Class thisClass, Locale locale, String namePrefix, String nameSuffix) {
        URL url = null;

        String lng = "_" + locale.getLanguage();
        String cty = "_" + locale.getCountry();
        String var = "_" + locale.getCountry();

        if (var.length() > 1) {
            url = thisClass.getResource(namePrefix + lng + cty + var + nameSuffix);
        }
        if (url == null && cty.length() > 1) {
            url = thisClass.getResource(namePrefix + lng + cty + nameSuffix);
        }
        if (url == null && lng.length() > 1) {
            url = thisClass.getResource(namePrefix + lng + nameSuffix);
        }
        if (url == null) {
            url = thisClass.getResource(namePrefix + nameSuffix);
        }
        return url;
    }

    /**
     * This is a convenience for
     * {@link #findResourceForLocale(java.lang.Class, java.util.Locale, java.lang.String, java.lang.String)}
     * that contructs the name prefix and suffix automatically by splitting the
     * resource name at the last '.', if it exists and appears before the last
     * '/'. If a '.' does not appear in the last path segment of the resource
     * name, then the suffix is empty. In other words, given the name of the
     * default version of a resource that uses the standard
     * <code>file.extension</code> convention for file names, this method finds
     * the best available localized version.
     *
     * @param thisClass the class to use as a base for the search
     * @param locale the locale to find the best-matching localized version for
     * @param name the resource name to find a localized version of
     * @return the URL of the best available version of the resource, or
     * <code>null</code> if the resource does not exist
     */
    public static URL findResourceForLocale(Class thisClass, Locale locale, String name) {
        String prefix = name;
        String suffix = "";
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            int slash = name.lastIndexOf('/');
            if (dot > slash) {
                prefix = name.substring(0, dot);
                suffix = name.substring(dot);
            }
        }
        return findResourceForLocale(thisClass, locale, prefix, suffix);
    }

    /**
     * Parse a list of int values. If any number can't be parsed or the actual
     * list has fewer than <code>minimumLength</code> entries, or more than
     * <code>maximumLength</code> entries, this method returns
     * <code>null</code>.
     *
     * @param s the string to parse
     * @param minimumLength the minimum number of entries in the list
     * @param maximumLength the maximum number of entries in the list
     * @return an array of the parsed values, or <code>null</code> if
     * <code>s</code> is not valid
     */
    private static int[] parseIntList(String s, int minimumLength, int maximumLength) {
        String[] tokens = listSplitter.split(s.trim());
        if (tokens.length < minimumLength) {
            warning("list has too few entries: " + s);
            return null;
        }
        if (tokens.length > maximumLength) {
            warning("list has too many entries: " + s);
            return null;
        }

        int[] list = new int[tokens.length];
        for (int i = 0; i < tokens.length; ++i) {
            try {
                list[i] = Integer.valueOf(tokens[i]);
            } catch (NumberFormatException e) {
                warning("unable to parse number: " + list[i]);
                return null;
            }
        }
        return list;
    }

    /**
     * Parse a list of double values. If any number can't be parsed or the
     * actual list has fewer than <code>minimumLength</code> entries, or more
     * than <code>maximumLength</code> entries, this method returns
     * <code>null</code>.
     *
     * @param s the string to parse
     * @param minimumLength the minimum number of entries in the list
     * @param maximumLength the maximum number of entries in the list
     * @return an array of the parsed values, or <code>null</code> if
     * <code>s</code> is not valid
     */
    private static double[] parseDoubleList(String s, int minimumLength, int maximumLength) {
        String[] tokens = listSplitter.split(s.trim());
        if (tokens.length < minimumLength) {
            warning("list has too few entries: " + s);
            return null;
        }
        if (tokens.length > maximumLength) {
            warning("list has too many entries: " + s);
            return null;
        }

        double[] list = new double[tokens.length];
        for (int i = 0; i < tokens.length; ++i) {
            try {
                list[i] = Double.valueOf(tokens[i]);
            } catch (NumberFormatException e) {
                warning("unable to parse number: " + list[i]);
                return null;
            }
        }
        return list;
    }
    private static Pattern listSplitter = Pattern.compile("\\s*,\\s*");

    private static void warning(String s) {
        Logger.getLogger(LOGGER).warning(s);
    }

    private static void warning(String s, Throwable t) {
        Logger.getLogger(LOGGER).log(Level.WARNING, s, t);
    }

    private static void severe(String s) {
        Logger.getLogger(LOGGER).log(Level.SEVERE, s);
    }

    private static void severe(String s, Throwable t) {
        Logger.getLogger(LOGGER).log(Level.SEVERE, s, t);
    }
    private static final String LOGGER = "ca.cgjennings.util";
}
