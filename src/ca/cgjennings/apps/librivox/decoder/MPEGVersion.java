package ca.cgjennings.apps.librivox.decoder;

/**
 * An enumeration of the supported MPEG Audio versions.
 *
 * @author Christopher G. Jennings (cjennings@acm.org)
 * @since 0.91
 */
public enum MPEGVersion {
	/** MPEG 1. */
	MPEG1("MPEG-1"),
	/** MPEG 2. */
	MPEG2("MPEG-2"),
	/** MPEG 2.5 (an unofficial standard). */
	MPEG2_5("MPEG-2.5");

	private MPEGVersion( String label ) {
		desc = label;
	}
	private String desc;

	@Override
	public String toString() {
		return desc;
	}
}
