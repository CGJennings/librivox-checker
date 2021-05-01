package ca.cgjennings.apps.librivox.decoder;

/**
 * A enumeration of the possible MPEG Audio layer types.
 *
 * @author Christopher G. Jennings https://cgjennings.ca/contact/
 * @since 0.91
 */
public enum LayerType {
    /**
     * Layer I.
     */
    LAYERI("Layer I"),
    /**
     * Layer II.
     */
    LAYERII("Layer II"),
    /**
     * Layer III.
     */
    LAYERIII("Layer III");

    private LayerType(String label) {
        desc = label;
    }
    private String desc;

    @Override
    public String toString() {
        return desc;
    }
}
