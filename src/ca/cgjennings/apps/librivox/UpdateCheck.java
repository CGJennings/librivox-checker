package ca.cgjennings.apps.librivox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main application for the checker tool.
 *
 * @author Christopher G. Jennings (https://cgjennings.ca/contact/)
 */
public class UpdateCheck {

    private UpdateCheck() {
    }

    private static String RELEASE_INFO_URL
            = "https://api.github.com/repos/cgjennings/librivox_checker/releases/latest";

    /**
     * Checks if an update is available.
     *
     * @return +1 if an update is available; 0 if not; -1 if there was an error
     */
    public static int checkForUpdate() {
        try {
            String newVersion = getLatestAvailableVersion();
            return isNewer(Checker.VERSION, newVersion) ? 1 : 0;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Returns the version tag for the most recent release on GitHub. The
     * release must use a tag like "vX.Y.Z" to be detected as a possible update.
     *
     * @return the version tag, or null if none was available (e.g., network
     * down)
     */
    public static String getLatestAvailableVersion() {
        try {
            String json = urlToString(new URL(RELEASE_INFO_URL));
            String version = gitHubReleaseInfoToVersion(json);
            return version;
        } catch (IOException ex) {
            Logger.getGlobal().log(Level.WARNING, "unable to check for update", ex);
        }
        return null;
    }

    /**
     * Returns whether a version described by a string such as "1.0.1" is newer
     * than this app's version.
     *
     * @param thisVersion the version of this app
     * @param newVersion the version that may be newer than this version
     * @return true if {@code newVersion} is actually newer
     */
    public static boolean isNewer(String thisVersion, String newVersion) {
        return toComparable(newVersion) - toComparable(thisVersion) > 0L;
    }

    private static long toComparable(String version) {
        String[] tokens = version.split("\\.", 4);
        long n = 0L;
        if (tokens.length >= 1) {
            n += 100_000_000_000_000_000L * parse(tokens[0]);
        }
        if (tokens.length >= 2) {
            n += 100_000_000_000L * parse(tokens[1]);
        }
        if (tokens.length >= 3) {
            n += parse(tokens[2]);
        }
        return n;
    }

    private static long parse(String chunk) {
        try {
            return Long.parseLong(chunk);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("invalid version string chunk " + chunk);
        }
    }

    private static String urlToString(URL url) throws IOException {
        StringBuilder sb = new StringBuilder(2048);
        String line;

        try (InputStream in = url.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        }

        return sb.toString();
    }

    private static String gitHubReleaseInfoToVersion(String json) {
        Pattern p = Pattern.compile("\"tag_name\":\\s*\"v([\\d.]+)\"", Pattern.MULTILINE);
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        Logger.getGlobal().log(Level.WARNING, "unable to find version info", json);
        return null;
    }
}
