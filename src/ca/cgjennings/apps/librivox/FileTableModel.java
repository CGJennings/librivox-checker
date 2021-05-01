package ca.cgjennings.apps.librivox;

import static ca.cgjennings.apps.librivox.Checker.string;
import ca.cgjennings.apps.librivox.LibriVoxAudioFile.Status;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Structures a collection of {@link LibriVoxAudioFile} objects for display in a
 * <code>JTable</code>.
 *
 * @author Christopher G. Jennings https://cgjennings.ca/contact/
 */
class FileTableModel extends AbstractTableModel {

    public FileTableModel() {
        rows = new ArrayList<>();
    }

    public int addAudioFile(LibriVoxAudioFile af) {
        int pos = rows.size();
        addAudioFile(pos, af);
        return pos;
    }

    public void addAudioFile(int index, LibriVoxAudioFile af) {
        assertEDT();
        rows.add(af);
        af.setOwner(this);
        fireTableRowsInserted(index, index);
    }

    public void removeRow(int row) {
        LibriVoxAudioFile file = rows.get(row);
        rows.remove(row);
        fireTableRowsDeleted(row, row);
        file.dispose();
    }

    public LibriVoxAudioFile getRow(int row) {
        return rows.get(row);
    }

    public int findRowForFile(LibriVoxAudioFile file) {
        for (int r = 0; r < rows.size(); ++r) {
            if (rows.get(r) == file) {
                fireTableRowsUpdated(r, r);
                return r;
            }
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COL_COUNT;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public String getColumnName(int column) {
        String key;
        switch (column) {
            case COL_FILE:
                key = "col-file";
                break;
            case COL_PROGRESS:
                key = "col-progress";
                break;
            default:
                throw new AssertionError("unknown column");
        }
        return string(key);
    }

    @Override
    public Object getValueAt(int row, int col) {
        LibriVoxAudioFile f = rows.get(row);
        switch (col) {
            case COL_FILE:
                return f.getFileName();
            case COL_PROGRESS:
                // although the renderer generates the text that is displayed to
                // the user, we return a string that will allow the automatic
                // row sorter to do its thing
                String cell;
                boolean busy = f.isBusy();
                Status s = f.getStatus();
                if (f.isBusy()) {
                    cell = String.format(Locale.CANADA, "%s %06.0f", s.name(), f.getCurrentProgress() * 100000f);
                } else {
                    cell = s.name();
                }
                return cell;
        }
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Called from an {@link LibriVoxAudioFile} when that file's progress or
     * status has changed.
     *
     * @param source the {@link LibriVoxAudioFile} that has changed state
     */
    void progressUpdate(LibriVoxAudioFile source) {
        assertEDT();

        for (int r = 0; r < rows.size(); ++r) {
            if (rows.get(r) == source) {
                fireTableRowsUpdated(r, r);
                return;
            }
        }

        throw new AssertionError("file not in table: " + source);
    }

    public ProgressRenderer getProgressRenderer() {
        return sharedProgressRenderer;
    }
    private ProgressRenderer sharedProgressRenderer = new ProgressRenderer();

    public class ProgressRenderer extends DefaultTableCellRenderer {

        Font regular, small;

        public ProgressRenderer() {
            setOpaque(false);
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            row = table.convertRowIndexToModel(row);

            if (regular == null) {
                regular = getFont().deriveFont(Font.BOLD);
                small = regular.deriveFont(Font.PLAIN, regular.getSize2D() - 2f);
            }

            LibriVoxAudioFile file = rows.get(row);
            state = file.getStatus();
            progress = file.getCurrentProgress();

            // the status itself can provide the right text for the activity, but
            // we treat "downloading" from a local jar file as a special case.
            if (state == Status.DOWNLOADING && file.getSource().toExternalForm().startsWith("jar:file")) {
                setText(string("status-extracting"));
            } else {
                setText(state.getLocalizedName());
            }

            if (!isSelected) {
                if (state == LibriVoxAudioFile.Status.ERROR || state == LibriVoxAudioFile.Status.FAILED) {
                    setForeground(failColor);
                } else if (state == LibriVoxAudioFile.Status.PASSED) {
                    setForeground(passColor);
                } else if (state == LibriVoxAudioFile.Status.WARNINGS) {
                    setForeground(warnColor);
                } else {
                    setForeground(Color.BLACK);
                }
            }

            if (progress < 0) {
                setFont(regular);
            } else {
                setFont(small);
            }

            isAnalysis = state != LibriVoxAudioFile.Status.DOWNLOADING;

            return this;
        }

        @Override
        protected void paintComponent(Graphics g1) {
            g1.setColor(getBackground());
            g1.fillRect(0, 0, getWidth(), getHeight());

            if (progress >= 0) {
                int x = MARGIN;
                int y = MARGIN;
                int w = getWidth() - MARGIN * 2 - 1;
                int h = getHeight() - MARGIN * 2 - 1;
                int p = Math.round(w * progress);

                Graphics2D g = (Graphics2D) g1;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color grad1, grad2;
                if (isAnalysis) {
                    grad1 = gradient1;
                    grad2 = gradient2;
                } else {
                    grad1 = dlGradient1;
                    grad2 = dlGradient2;
                }

                GradientPaint gp = new GradientPaint(x, y, grad1, x, (y + h) * 3 / 4, grad2, true);
                g.setPaint(gp);
                g.fillRoundRect(x, y, p, h, ARC_WIDTH, ARC_HEIGHT);
                g.setPaint(outline);
                g.drawRoundRect(x, y, w, h, ARC_WIDTH, ARC_HEIGHT);
            }

            super.paintComponent(g1);
        }

        private static final int MARGIN = 2;
        private static final int ARC_WIDTH = 8;
        private static final int ARC_HEIGHT = 8;
        private float progress;
        private boolean isAnalysis;
        private LibriVoxAudioFile.Status state;
        private final Color gradient1 = Checker.getSettings().getColor("table-progress1", Color.BLUE);
        private final Color gradient2 = Checker.getSettings().getColor("table-progress2", Color.CYAN);
        private final Color dlGradient1 = Checker.getSettings().getColor("table-dl-progress1", Color.BLUE);
        private final Color dlGradient2 = Checker.getSettings().getColor("table-dl-progress2", Color.CYAN);
        private final Color outline = Checker.getSettings().getColor("table-progress-outline", Color.LIGHT_GRAY);
        private final Color passColor = Checker.getSettings().getColor("table-progress-pass", Color.GREEN);
        private final Color warnColor = Checker.getSettings().getColor("table-progress-warn", Color.YELLOW);
        private final Color failColor = Checker.getSettings().getColor("table-progress-fail", Color.RED);
    }

    private final List<LibriVoxAudioFile> rows;

    public static final int COL_FILE = 0;
    public static final int COL_PROGRESS = 1;

    public static final int COL_COUNT = 2;

    /**
     * Sprinkled here and there to verify that the LVF objects are obeying
     * threading assumptions.
     */
    private void assertEDT() {
        if (!EventQueue.isDispatchThread()) {
            throw new AssertionError("called outside of EDT");
        }
    }
}
