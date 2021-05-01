package ca.cgjennings.apps.librivox;

import static ca.cgjennings.apps.librivox.Checker.string;
import ca.cgjennings.apps.librivox.validators.AbstractValidator;
import ca.cgjennings.apps.librivox.validators.Validator;
import ca.cgjennings.apps.librivox.validators.Validator.Category;
import ca.cgjennings.apps.librivox.validators.Validator.Validity;
import ca.cgjennings.apps.librivox.validators.ValidatorFactory;
import ca.cgjennings.util.Settings;
import java.util.Iterator;

/**
 * Used to compile reports of the validation of a {@link LibriVoxAudioFile}.
 * {@link Validator}s that subclass {@link AbstractValidator} do not generally
 * deal with the report directly, but write to it indirectly using specialized
 * methods in that class. An exception to this would be a validator that
 * validates in multiple categories.
 *
 * @author Christopher G. Jennings https://cgjennings.ca/contact/
 */
public class Report {

    public Report(LibriVoxAudioFile file) {
        isOpen = true;
        informationSegments = createStringBuilders();
        validationSegments = createStringBuilders();
        this.file = file;
    }

    /**
     * Add a statement to the information report. A statement is a
     *
     * @param cat the category the statement belongs to
     * @param text the text of the statement
     */
    public void addStatement(Category cat, Object text) {
        checkClosure();
        oneColumnEntry(INFORMATION, informationSegments[cat.ordinal()], text);
    }

    private void oneColumnEntry(int reportID, StringBuilder b, Object text) {
        emitTR(reportID, b);
        b.append("<td colspan=3>").append(format(text)).append("</td></tr>");
    }

    /**
     * Add a feature description to the report. These are information entries
     * that do not affect the file's validity. A feature description is a pair a
     * values consisting of a feature name and the value of that feature. For
     * example, "Number of Channels", "2".
     *
     * @param cat the general category that the information applies to
     * @param name the name of the feature
     * @param value the value of the feature in the file analyzed for this
     * report
     */
    public void addFeature(Category cat, Object name, Object value) {
        checkClosure();
        StringBuilder b = informationSegments[cat.ordinal()];
        keyValueEntry(INFORMATION, b, name, value);
    }

    /**
     * Adds a validation entry to the report. This is an entry that affects the
     * overall validity of the file and itself has a validity value. A file will
     * initially pass, but the final value will be the worst of all validity
     * values of all validation entries. So, a single failed validation entry
     * will cause the entire report (file) to fail.
     *
     * @param cat the general category of validation entry
     * @param method the validator that is making the entry (may be
     * <code>null</code>)
     * @param validity the validity level to associate with the entry
     * @param description a human-readable description (see
     * {@link #format(java.lang.Object)})
     * @param helpURL an optional string that provides a URL offering additional
     * information
     */
    public void addValidation(Category cat, Validator method, Validity validity, Object description, String helpURL) {
        checkClosure();

        if (method != null) {
            validity = ValidatorFactory.getFactory().getModulatedValidity(method, validity);
        }

        // switch the report's validity to this result if it is
        // "worse" than the existing result
        if (validity.ordinal() < this.validity.ordinal()) {
            this.validity = validity;
        }

        // pass results produce no output
        if (validity == Validity.PASS) {
            return;
        }

        StringBuilder b = validationSegments[cat.ordinal()];
        validationEntry(VALIDATION, b, validity, description, helpURL);
    }

    private void keyValueEntry(int reportID, StringBuilder b, Object key, Object value) {
        keyValueEntry(reportID, b, key, value, null);
    }

    private void keyValueEntry(int reportID, StringBuilder b, Object key, Object value, String help) {
        emitTR(reportID, b);
        emitTD(reportID, b);
        b.append(format(key)).append("</td>");

        b.append("<td>");
        b.append(format(value));
        if (help != null) {
            b.append(" &nbsp;<a href='").append(help).append("'>").append(string("report-help")).append("</a>");
        }

        b.append("</td></tr>");
    }

    private void validationEntry(int reportID, StringBuilder b, Validity validity, Object description, String helpURL) {
        String image = null;
        switch (validity) {
            case PASS:
                image = PASS_ICON;
                break;
            case WARN:
                ++warnings;
                image = WARN_ICON;
                break;
            case FAIL:
            case INCOMPLETE:
                ++errors;
                image = FAIL_ICON;
                break;
            default:
                throw new AssertionError("unknown validity " + validity);
        }
        if (image != null) {
            image = "<image src='" + image + "' width=" + ICON_WIDTH + " height=" + ICON_HEIGHT + ">";
        }

        keyValueEntry(reportID, b, format(image), format(description), helpURL);
    }

    /**
     * Write the opening TD tag for a multicolumn regular report row.
     */
    private void emitTD(int reportID, StringBuilder b) {
        b.append("<td");
        if (++TDTagCount[reportID] == 1) {
            if (reportID == VALIDATION) {
                b.append(" width=1%");
            } else {
                b.append(" width=25%");
            }
        }
        b.append('>');
    }

    /**
     * Write the help column TD tag for a multicolumn regular report row.
     */
    private void emitHelpTD(int reportID, StringBuilder b) {
        b.append("<td");
        if (++helpTDTagCount[reportID] == 1) {
            b.append(" width=1%");
        }
        b.append('>');
    }

    /**
     * Write the opening TR tag for a regular report row.
     */
    private void emitTR(int reportID, StringBuilder b) {
        b.append("<tr");
        if ((++TRTagCount[reportID] & 1) == 1) {
            b.append(" class='odd'");
        }
        b.append('>');
    }

    public void addDivider(Category cat) {
        emitDivider(informationSegments[cat.ordinal()]);
    }

    private void emitDivider(StringBuilder b) {
        b.append("<tr><th colspan=3 style='font-size: 1px'>&nbsp;</th></tr>");
    }

    /**
     * Mark the report as closed and prepare the final report text. Once a
     * report is closed, no more entries may be added to it.
     */
    public void close() {
        isOpen = false;

        informationReportText = createReport(INFORMATION, informationSegments);
        validationReportText = createReport(VALIDATION, validationSegments);

        // allow temporary buffers to be gc'd
        informationSegments = null;
        validationSegments = null;
    }

    private String createReport(int reportID, StringBuilder[] categories) {
        StringBuilder report = new StringBuilder();
        appendHeader(report);
        report.append("<tr><th colspan=3>")
                .append(file.getFileName())
                .append("</th></tr>");

        String caption = null;
        if (errorMessage == null) {
            if (reportID == VALIDATION && warnings == 0 && errors == 0) {
                String successMessage;
                if (ValidatorFactory.getFactory().isEnabledValidatorSetExhaustive()) {
                    successMessage = "report-pass";
                } else {
                    successMessage = "report-pass-subset";
                }
                validationEntry(VALIDATION, report, Validity.PASS, string(successMessage), null);
            }

            for (int i = 0; i < categories.length; ++i) {
                if (reportID == INFORMATION && i > 0 && categories[i - 1].length() > 0) {
                    emitDivider(report);
                }
                report.append(categories[i]);
            }

            if (reportID == VALIDATION) {
                if (warnings == 0 && errors == 0) {
                    caption = string("report-pass-detail");
                }
            }
        } else {
            validationEntry(reportID, report, Validity.FAIL, errorMessage, null);
            if (validationSegments[Category.ERROR.ordinal()].length() > 0) {
                emitDivider(report);
                report.append(validationSegments[Category.ERROR.ordinal()]);
            }
        }

        appendFooter(report, caption);
        return report.toString();
    }

    protected static void appendHeader(StringBuilder r) {
        r.append("<html><body><table width=100% cellpadding=4>");
    }

    protected static void appendFooter(StringBuilder r, String caption) {
        r.append("</table>");
        if (caption != null) {
            r.append("<p>&nbsp;</p><p>").append(caption).append("</p>");
        }
        r.append("</body></html>");
    }

    /**
     * Used to format descriptions that are to be included in a report. May be
     * <code>null</code>, in which case an empty string is returned. Otherwise,
     * if the object is <code>Iterable</code>, a sequence of lines is returned
     * by recursively <code>format</code>ting each element in the iterable,
     * separating them with line break codes (<tt>&lt;br&gt;</tt>). If the
     * object is not iterable, then its string representation is returned by
     * calling its <code>toString</code> method.
     *
     * @param obj the description to format
     * @return a string created from the description object
     */
    protected String format(Object obj) {
        if (obj == null) {
            obj = "";
        }
        if (!(obj instanceof String)) {
            if (obj instanceof Iterable) {
                StringBuilder b = new StringBuilder();
                Iterator it = ((Iterable) obj).iterator();
                if (it.hasNext()) {
                    b.append(format(it.next()));
                }
                while (it.hasNext()) {
                    b.append("<br>");
                    b.append(format(it.next()));
                }
                obj = b;
            }
            // ... specialize for data types ...
        }

        return obj.toString();
    }

    /**
     * Return the information report for this document, or <code>null</code> if
     * the report has not been closed.
     *
     * @return
     */
    public String getInformationReport() {
        return informationReportText;
    }

    /**
     * Return the information report for this document, or <code>null</code> if
     * the report has not been closed.
     *
     * @return
     */
    public String getValidationReport() {
        return validationReportText;
    }

    public Validity getValidity() {
        return validity;
    }

    /**
     * Throw an exception if the report has already been closed.
     */
    private void checkClosure() {
        if (!isOpen) {
            throw new IllegalStateException("report is closed");
        }
    }

    /**
     * Creates an array of <code>StringBuilders</code> (one for each
     * <code>Category</code>).
     */
    private StringBuilder[] createStringBuilders() {
        StringBuilder[] builders = new StringBuilder[Category.values().length];
        for (int i = 0; i < builders.length; ++i) {
            builders[i] = new StringBuilder();
        }
        return builders;
    }

    /**
     * Return a document that can be used as a placeholder when a valid report
     * is not selected.
     *
     * @return a placeholder document
     */
    public static String getDefaultDocument() {
        if (defaultDocument == null) {
            StringBuilder b = new StringBuilder();
            appendHeader(b);
            appendFooter(b, null);
            defaultDocument = b.toString();
        }
        return defaultDocument;
    }

    /**
     * Overrides the standard report content with an error message. If an error
     * occurs in the system building the report, it can call this method to set
     * an error message. When the report is closed, a report that consists of
     * the error message will be generate instead of a standard report.
     *
     * @param message the error message to set for the report
     */
    public void setErrorMessage(String message) {
        checkClosure();
        errorMessage = message;
    }

    private boolean isOpen;
    private StringBuilder[] informationSegments, validationSegments;

    // for each report, tracks how many rows with multiple columns have been
    // emitted and how many rows have been emitted; this tells us whether
    // we need to write column width attributes and which <TR> class to use
    private int[] TDTagCount = new int[REPORTS], helpTDTagCount = new int[REPORTS], TRTagCount = new int[REPORTS];

    private static final int VALIDATION = 0, INFORMATION = 1, REPORTS = 2;

    private LibriVoxAudioFile file;
    private volatile String informationReportText;
    private volatile String validationReportText;
    private volatile Validity validity = Validity.PASS;

    // an external error message that can be set while processing the file
    // to create the report; if set it will override the standard report
    private String errorMessage;

    private static String defaultDocument;

    private static Settings SETTINGS = Checker.getSettings();
    private static final String PASS_ICON = SETTINGS.getResource(
            Report.class, "report-pass-icon", "/resources/pass.png"
    ).toExternalForm();
    private static final String WARN_ICON = SETTINGS.getResource(
            Report.class, "report-warn-icon", "/resources/warn.png"
    ).toExternalForm();
    private static final String FAIL_ICON = SETTINGS.getResource(
            Report.class, "report-fail-icon", "/resources/fail.png"
    ).toExternalForm();
    private static final int ICON_WIDTH = SETTINGS.getInt("report-icon-width", 32);
    private static final int ICON_HEIGHT = SETTINGS.getInt("report-icon-height", 32);

    private int errors = 0, warnings = 0;
}
