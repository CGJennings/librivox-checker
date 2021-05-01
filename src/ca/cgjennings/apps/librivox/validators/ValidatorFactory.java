package ca.cgjennings.apps.librivox.validators;

import ca.cgjennings.apps.librivox.Checker;
import ca.cgjennings.apps.librivox.LibriVoxAudioFile;
import ca.cgjennings.apps.librivox.validators.Validator.Strictness;
import ca.cgjennings.apps.librivox.validators.Validator.Validity;
import ca.cgjennings.util.Settings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Manages the instantiation of {@link Validator} instances used to validate a
 * {@link LibriVoxAudioFile}. The validators to be used are determined by
 * parsing the <tt>resources/validator_classes</tt> file.
 *
 * @author Christopher G. Jennings https://cgjennings.ca/contact/
 */
public class ValidatorFactory {

    private ValidatorFactory() {
        InputStream in = null;
        try {
            in = getClass().getResourceAsStream("/resources/validators.classlist");
            if (in == null) {
                throw new AssertionError("missing validator class list");
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "utf-8"));
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.startsWith("!") || line.length() == 0) {
                    continue;
                }
                int split = line.indexOf(':');
                String className = null;
                String rule = null;
                if (split >= 0) {
                    className = line.substring(0, split).trim();
                    rule = line.substring(split + 1).trim();
                }
                // TODO: error handling
                if (className == null) {
                    throw new AssertionError("invalid validators.classlist syntax");
                }

                if (className.indexOf('.') < 0) {
                    className = Validator.class.getPackage().getName() + "." + className;
                }

                Class c = null;
                try {
                    c = Class.forName(className);
                } catch (ClassNotFoundException e) {
                    // TODO: error
                    continue;
                }

                if (!Validator.class.isAssignableFrom(c)) {
                    // TODO: error: not a Validator
                    continue;
                }

                Strictness s = Strictness.valueOf(rule);
                // TODO: IllegalArgEx

                if (s != Strictness.IGNORE) {
                    classes.add(c);
                    rules.add(s);
                }
            }
        } catch (IOException e) {
            // TODO: error
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static ValidatorFactory getFactory() {
        if (shared == null) {
            shared = new ValidatorFactory();
        }
        return shared;
    }

    public Strictness getStrictness(Validator method) {
        for (int i = 0; i < classes.size(); ++i) {
            if (classes.get(i).equals(method.getClass())) {
                return rules.get(i);
            }
        }
        throw new IllegalArgumentException("not a registered ValidationMethod: " + method.getClass());
    }

    public Validity getModulatedValidity(Validator method, Validity validity) {
        if (validity == Validity.FAIL && getStrictness(method) != Strictness.REQUIRED) {
            validity = Validity.WARN;
        }
        return validity;
    }

    /**
     * Create an array of new instances of the registered {@link Validator}s
     * that are currently active.
     *
     * @return an array of standard validators
     */
    public Validator[] createValidators() {
        Validator[] instances = createAllValidators();
        LinkedList<Validator> acceptList = new LinkedList<Validator>();
        for (Validator v : instances) {
            if (isClassEnabled(v.getClass())) {
                acceptList.add(v);
            }
        }
        return acceptList.toArray(new Validator[acceptList.size()]);
    }

    /**
     * Create an array of new instances of the registered {@link Validator}s.
     * This includes all validators that are defined in the class list file,
     * even those that have been turned off through preferences.
     *
     * @return an array of standard validators
     */
    public Validator[] createAllValidators() {
        Validator[] instances = new Validator[classes.size()];
        for (int i = 0; i < classes.size(); ++i) {
            try {
                instances[i] = classes.get(i).newInstance();
            } catch (InstantiationException e) {
                // TODO
                throw new AssertionError("unable to create validator: " + classes.get(i));
            } catch (IllegalAccessException e) {
                throw new AssertionError("no permission to call constructor: " + classes.get(i));
            }
        }
        return instances;
    }

    /**
     * Returns the number of registered validator classes.
     *
     * @return the number of classes in the validator table
     */
    public int getRegisteredValidatorCount() {
        return classes.size();
    }

    /**
     * Returns the number of registered validator classes.
     *
     * @return the number of classes in the validator table
     */
    @SuppressWarnings("unchecked")
    public Class<? extends Validator>[] getRegisteredValidatorClasses() {
        return classes.toArray(new Class[classes.size()]);
    }

    private static ValidatorFactory shared;
    private List<Class<? extends Validator>> classes = new ArrayList<Class<? extends Validator>>();
    private List<Strictness> rules = new ArrayList<Strictness>();

    public static void setClassEnabled(Class<? extends Validator> c, boolean enable) {
        if (c == null) {
            throw new NullPointerException("class");
        }
        Settings s = Checker.getSettings();
        s.setBoolean(PREF_CLASS_PREFIX + c.getName(), enable);
    }

    public static boolean isClassEnabled(Class<? extends Validator> c) {
        if (c == null) {
            throw new NullPointerException("class");
        }
        Settings s = Checker.getSettings();
        return s.getBoolean(PREF_CLASS_PREFIX + c.getName(), isClassEnabledDefault(c));
    }

    private static boolean isClassEnabledDefault(Class<? extends Validator> c) {
        return !(c == NoiseValidator.class || c == MetadataValidator.class);
    }

    private static final String PREF_CLASS_PREFIX = "enable ";

    /**
     * Returns <code>true</code> if all validators are currently enabled.
     *
     * @return <code>true</code> if the most complete validator set is enabled
     */
    public boolean isEnabledValidatorSetExhaustive() {
        for (int i = 0; i < classes.size(); ++i) {
            if (!isClassEnabled(classes.get(i))) {
                return false;
            }
        }
        return true;
    }
}
