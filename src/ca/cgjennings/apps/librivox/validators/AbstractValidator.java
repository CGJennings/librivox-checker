package ca.cgjennings.apps.librivox.validators;

import ca.cgjennings.apps.librivox.decoder.AudioFrame;
import ca.cgjennings.apps.librivox.Checker;
import ca.cgjennings.apps.librivox.LibriVoxAudioFile;
import ca.cgjennings.apps.librivox.Report;
import ca.cgjennings.apps.librivox.decoder.AudioHeader;
import ca.cgjennings.util.SettingConverter;
import ca.cgjennings.util.Settings;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;

/**
 * An abstract base class for {@link Validator}s. This abstract base class
 * simplifies the task of creating a validator and obviates the need to deal
 * directly with the validation {@link Report}.
 * <p>
 * This class provides facilities for a <code>Validator</code> to obtain
 * configuration parameters at runtime from settings files. The base settings
 * file for a given validator is located in the <code>resources</code> folder
 * and has the same name as the validator's class name, with the
 * <code>.properties</code> file extension. Alternate versions of the settings
 * file can be defined based on the current validation strictness setting.
 * Currently, the only valid strictness levels are strict (no suffix) and gentle
 * (settings defined in a file that adds "_g" to the class name).
 * <p>
 * This base class uses the protected versus public attribute of methods to
 * suggest how they are intended to be used. Protected methods are not normally
 * used directly, but are accessed indirectly by higher-level methods designed
 * to simplify report generation.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
public abstract class AbstractValidator implements Validator {

    /**
     * You must override this method to return the general category of testing
     * performed by this validator.
     *
     * @return the {@link Category} of this validator's tests
     */
    public abstract Category getCategory();

    /**
     * You must override this method to return <code>true</code> if you wish to
     * process the file's audio data. {@inheritdoc }
     * <p>
     * This base class implementation returns <code>false</code>.
     */
    @Override
    public boolean isAudioProcessor() {
        return false;
    }

    /**
     * The base class implementation does nothing.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void beginAnalysis(AudioHeader header, Validator[] predecessors) {
    }

    /**
     * The base class implementation does nothing.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void endAnalysis() {
    }

    /**
     * This method is overridden by the abstract base class in order to
     * initialize the services the base class provides. It is recommended that
     * validator writers not override this method, but instead do their
     * initialization in {@link Validator#beginAnalysis}. If you do override
     * this method, then you <b>must</b> call the super implementation from the
     * subclass.
     * <p>
     * {@inheritDoc}
     *
     * @param file the audio file to be processed
     * @param report the report that results should be written to
     */
    @Override
    public void initialize(LibriVoxAudioFile file, Report report) {
        this.file = file;
        this.report = report;
    }

    private LibriVoxAudioFile file;
    private Report report;

    /**
     * Returns the audio file that this validator is analyzing.
     *
     * @return the file this validator will analyze
     */
    protected final LibriVoxAudioFile getLibriVoxFile() {
        if (file == null) {
            throw new AssertionError("subclass overrode initialize() incorrectly");
        }
        return file;
    }

    /**
     * Returns the report that this validator should write results to. Note that
     * validators should not normally use this method directly, but should
     * instead use the report-writing features provided by the base class.
     *
     * @return the report for this validator's results
     */
    protected final Report getReport() {
        if (report == null) {
            throw new AssertionError("subclass overrode initialize() incorrectly");
        }
        return report;
    }

    /**
     * Record the result of a test with rule name <code>rule</code> as a
     * failure, using <code>value</code> to describe the result to the user.
     * This will add an entry to the report's validation section describing the
     * outcome.
     * <p>
     * Depending on the value of <code>ruleKey</code> and the class's strictness
     * setting (as set in <code>validators.classlist</code>), the failure result
     * may be downgraded to a warning.
     *
     * @param ruleKey the name of the rule's key in the validator's settings
     * file
     * @param value the text to display for this result
     */
    public void fail(String ruleKey, Object value) {
        recordResult(getCategory(), getReport(), ruleKey, Validity.FAIL, value);
    }

    /**
     * Record the result of a test with rule name <code>ruleKey</code> as a
     * warning, using <code>value</code> to describe the result to the user.
     * This will add an entry to the report's validation section describing the
     * outcome.
     *
     * @param ruleKey the name of the rule's key in the validator's settings
     * file
     * @param value the text to display for this result
     */
    public void warn(String ruleKey, Object value) {
        recordResult(getCategory(), getReport(), ruleKey, Validity.WARN, value);
    }

    /**
     * Adds a key, value pair to the information part of the report. For
     * example, the key might be <code>"Sampling Rate"</code> and the value
     * might be <code>"44.1 kHz"</code>.
     *
     * @param key the name of the feature to describe
     * @param value the value of that feature
     */
    public void addFeature(Object key, Object value) {
        getReport().addFeature(getCategory(), key, value);
    }

    /**
     * This is a convenience method for
     * <code>addFeature( this.string( nameKey ), value )</code>.
     *
     * @param nameKey the localization key for the feature name
     * @param value the value for this name
     */
    public final void feature(String nameKey, Object value) {
        addFeature(string(nameKey), value);
    }

    /**
     * Adds a general statement to the information part of the report.
     */
    public void addStatement(Object statement) {
        getReport().addStatement(getCategory(), statement);
    }

    /**
     * {@inheritdoc }
     * <p>
     * This base class implementation throws an
     * <code>UnsupportedOperationException</code>. The exception will be thrown
     * if {@link #isAudioProcessor} returns <code>true</code> and this method
     * has not been overridden.
     */
    @Override
    public void analyzeFrame(AudioFrame frame) {
        throw new UnsupportedOperationException(
                "you must override analyzeFrame() if isAudioProcessor() returns true"
        );
    }

    /**
     * {@inheritDoc }
     * <p>
     * The initial validity of a file is set to <code>PASS</code>, so that
     * {@link #updateValidity} can correctly downgrade the validity of a file as
     * it fails tests.
     *
     * @return the validity determination for the validated audio file
     */
    @Override
    public Validity getValidity() {
        return validity;
    }

    public static void setUserStrictnessSuffix(String suffix) {
        preferredStrictness = suffix;
    }

    public static String getUserStrictnessSuffix() {
        return preferredStrictness;
    }

    public static final String USER_STRICTNESS_STRICT = "";
    public static final String USER_STRICTNESS_GENTLE = "_g";
    private volatile static String preferredStrictness = USER_STRICTNESS_GENTLE;

    /**
     * Returns a {@link Settings} instance suitable for this validator.
     * <p>
     * This base class implementation shares instances on a per-class basis.
     * When a class instance first calls this method, a new {@link Settings}
     * instance is created and initialized with settings from the file
     * <tt>resources/<i>classname</i>.properties</tt>, if it exists, or a
     * locale-specific version of the file if that is available.
     *
     * @return a shared <code>Settings</code> instance with settings for this
     * validator
     */
    public Settings getSettings() {
        String name = getClass().getSimpleName();
        String suffix = getUserStrictnessSuffix();

        Settings settings;
        synchronized (sharedSettings) {
            settings = sharedSettings.get(name + suffix);
            if (settings == null) {
                settings = new Settings();
                // add defaults
                if (suffix.length() > 0) {
                    addSettingsGroup(settings, name, "");
                }
                // add custom settings for Validation level
                addSettingsGroup(settings, name, suffix);
                sharedSettings.put(name + suffix, settings);
            }
        }
        return settings;
    }

    /**
     * Reads a settings file, overwriting any existing settings with the same
     * name. This is used to allow strictness levels other than the default to
     * override the default settings by calling it once with an empty suffix and
     * once with the suffix of the desired strictness.
     */
    private void addSettingsGroup(Settings settings, String name, String suffix) {
        URL url = Settings.findResourceForLocale(
                this.getClass(), getLocale(), "/resources/" + name + suffix, ".properties");
        if (url != null) {
            try {
                settings.read(url);
            } catch (IOException e) {
                System.err.println("failure while reading settings: " + getClass().getSimpleName());
                e.printStackTrace();
            }
        }
    }

    /**
     * Return a localized (and possibly formatted) text string. The value of
     * <code>key</code> is first looked for in the localized settings for this
     * class. If no value is found there, then the value is looked for in the
     * application's
     * <tt>text_<i>XX</i>.properties</tt> file. If no value for the key is
     * found, a default string equal to
     * <code>"[MISSING STRING " + key + "]"</code> is used.
     * <p>
     * If the optional <code>arguments</code> are provided, they will be used to
     * format the fetched string as if by<br>
     * <code>
     * String.format( getLocale(), valueOfKey, arguments )
     * </code>
     *
     * @param key the key for the desired text
     * @param arguments parameters to use when formatting the localized text
     * @return the localized, formatted value for <code>key</code>
     */
    public final String string(String key, Object... arguments) {
        String value = getSettings().get(key);
        if (value == null) {
            return Checker.string(key, arguments);
        }
        if (arguments != null && arguments.length > 0) {
            value = String.format(getLocale(), value, arguments);
        }
        return value;
    }

    /**
     * Returns the preferred locale for this validation method.
     *
     * @return the locale that localized content should be presented in
     */
    public final Locale getLocale() {
        return Checker.getPreferredLocale();
    }

    /**
     * Set the validity for this file to <tt>v</tt> without any checking.
     * <p>
     * Subclasses should not normally call this directly, but should allow it to
     * be set for them through calls to {@link #fail} and {@link #warn}.
     *
     * @param v the new validity for this method
     */
    protected void setValidity(Validity v) {
        validity = v;
    }

    /**
     * Update the validity value with a new result. This method's validity will
     * only be changed if <code>v</code> is "worse" than the current validity.
     * The validity values from best to worst are:<br>
     * <code>PASS &gt; WARN &gt; FAIL &gt; INCOMPLETE</code>
     * <p>
     * Subclasses should not normally call this directly, but should allow it to
     * be set for them through calls to {@link #fail} and {@link #warn}.
     *
     * @param v the new validity level that may override the current level
     */
    protected void updateValidity(Validity v) {
        Validity current = getValidity();
        if (v.ordinal() < current.ordinal() || v == Validity.INCOMPLETE) {
            setValidity(v);
        }
    }

    /**
     * Returns a {@link Strictness} that represents an enforcement level for a
     * validation test. The result is determined from the value of the setting
     * returned by {@link #getSettings}<code>( key )</code>. If <code>key</code>
     * is not defined, it will default to <code>REQUIRED</code>.
     *
     * @param key the setting key for the test
     * @return a strictness level for the test
     */
    protected Strictness getTestStrictness(String key) {
        return getSettings().get(key, RULE_CONVERTER, Strictness.REQUIRED);
    }

    /**
     * Applies a {@link Strictness} setting to a test {@link Validity},
     * downgrading <code>validity</code> if it is a stronger result than
     * <code>strictness</code> allows.
     *
     * @param strictness the strictness rule to apply to <code>validity</code>
     * @param validity the <code>validity</code> to modify
     * @return the modulated validity
     */
    protected Validity modulateValidity(Strictness strictness, Validity validity) {
        if (validity == Validity.FAIL && strictness != Strictness.REQUIRED) {
            validity = Validity.WARN;
        }
        if (strictness == Strictness.IGNORE) {
            validity = Validity.PASS;
        }
        return validity;
    }

    /**
     * This method updates the validity state of this validator and writes an
     * appropriate message to the validation report. This consists of the
     * following steps:
     * <ol>
     * <li> If <code>ruleKey</code> is not <code>null</code>, then an
     * enforcement rule will be used to modify the validity level. The value of
     * <code>ruleKey</code> will be looked up in the settings for this method
     * using {@link #getTestStrictness(java.lang.String)}.
     * <li> The validity level of this validator will be updated using
     * {@link #updateValidity}.
     * <li> The <code>message</code> will be written to the <code>report</code>.
     * </ol>
     * <p>
     * If the validator's settings defines a key equal to the rule name plus the
     * suffix <code>"-help"</code>, then the report will attempt to use that as
     * a help file that the user can consult about the rule.
     * <p>
     * (Note that the final validity for the validator will be still be
     * determined by applying the validator class's <code>Strictness</code>
     * setting.)
     *
     * @param category the category of the report that this result belongs in
     * @param report the report to write this message to
     * @param ruleKey the key to use to determine the test's strictness (may be
     * <code>null</code>)
     * @param validity the validity result for the test
     * @param message the message to write to the report
     */
    protected void recordResult(Category category, Report report, String ruleKey, Validity validity, Object message) {
        Strictness s = Strictness.REQUIRED;
        if (ruleKey != null) {
            s = getTestStrictness(ruleKey);
        }
        validity = modulateValidity(s, validity);
        updateValidity(validity);
        report.addValidation(category, this, validity, message, getSettings().get(ruleKey + "-help", "nyi.html"));
    }

    private Validity validity = Validity.PASS;

    private static final HashMap<String, Settings> sharedSettings = new HashMap<String, Settings>();

    static final SettingConverter<Strictness> RULE_CONVERTER = new SettingConverter<Strictness>() {
        @Override
        public String toSetting(Strictness object) {
            return object.name().toUpperCase(Locale.US);
        }

        @Override
        public Strictness fromSetting(String settingValue) {
            try {
                return Strictness.valueOf(settingValue.toUpperCase(Locale.US));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    };

    /**
     * Returns the name of the validator as a human-friendly string. This is
     * used to name the validator in the Choose Validators dialog.
     *
     * @return the name of the validator
     */
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    /**
     * Returns a brief description of the validator as a human-friendly string.
     *
     * @return a description of what the validator tests
     */
    @Override
    public String getDescription() {
        return "";
    }
}
