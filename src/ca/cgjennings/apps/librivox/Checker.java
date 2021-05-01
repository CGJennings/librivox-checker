package ca.cgjennings.apps.librivox;

import ca.cgjennings.apps.librivox.LibriVoxAudioFile.Status;
import ca.cgjennings.apps.librivox.metadata.MetadataEditorLinkFactory;
import ca.cgjennings.apps.librivox.tools.ID3Editor;
import ca.cgjennings.apps.librivox.tools.ID3UpgradeTool;
import ca.cgjennings.apps.librivox.tools.WaveformViewer;
import ca.cgjennings.apps.librivox.validators.AbstractValidator;
import ca.cgjennings.apps.librivox.validators.ValidatorFactory;
import ca.cgjennings.io.StreamCopier;
import ca.cgjennings.platform.OSXAdapter;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.MenuAdapter;
import ca.cgjennings.util.Settings;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.MenuEvent;
import javax.swing.table.TableColumnModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

/**
 * Main application for the checker tool.
 *
 * @author Christopher G. Jennings https://cgjennings.ca/contact/
 */
public final class Checker extends javax.swing.JFrame {

    /**
     * Current version number.
     */
    public static final String VERSION = "1.1";

    /**
     * Creates a new Checker application instance.
     */
    public Checker() {
        try {
            HTMLEditorKit kit = new HTMLEditorKit();
            StyleSheet css = new StyleSheet();
            css.importStyleSheet(getClass().getResource("/resources/report.css"));
            css.addStyleSheet(kit.getStyleSheet());
            kit.setStyleSheet(css);
        } catch (NullPointerException e) {
            getLogger().warning("unable to read report CSS file");
        }

        model = new FileTableModel();

        initAppIcons();
        initComponents();
        localizeMenu(appMenuBar);
        installOSXMenuHandlers();
        installMenuUpdater();

        AbstractValidator.setUserStrictnessSuffix(AbstractValidator.USER_STRICTNESS_GENTLE);

        // create row sorter and init column sizes
        fileTable.setAutoCreateRowSorter(true);

        initFileChooser();
        initDropTarget();

        addURLDialog = new URLDialog(this);

        // this forces the factory to try to parse the class table, so if there
        // are any errors they will be discovered before the user starts working
        ValidatorFactory.getFactory();

        fileTable.getColumnModel().getColumn(FileTableModel.COL_PROGRESS)
                .setCellRenderer(model.getProgressRenderer());

        fileTable.getSelectionModel().addListSelectionListener(e -> {
            int sel = fileTable.getSelectedRow();
            if (sel >= 0) {
                sel = fileTable.convertRowIndexToModel(sel);
            }
            updateReportViews(sel);
        });

        model.addTableModelListener(e -> {
            int firstRow = e.getFirstRow();
            int lastRow = e.getLastRow();

            if (rowShownInReportViews >= firstRow && rowShownInReportViews <= lastRow) {
                updateReportViews(rowShownInReportViews);
            }
        });

        validate(); // make sure table size is known
        TableColumnModel tcm = fileTable.getColumnModel();
        int tw = fileTable.getWidth();
        int cw = tw / 3;
        tcm.getColumn(1).setPreferredWidth(cw);
        tcm.getColumn(0).setPreferredWidth(tw - cw);

        HyperlinkListener hll = e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                URL url = e.getURL();
                if (url != null) {
                    // handle absolute URLs, if desired
                } else {
                    String link = e.getDescription();
                    if (MetadataEditorLinkFactory.isLink(link)) {
                        // get the rectangle covered by the <a> element
                        Element el = e.getSourceElement();
                        JEditorPane pane = (JEditorPane) e.getSource();
                        Rectangle coverRect = null;
                        try {
                            coverRect = pane.modelToView(el.getStartOffset());
                            coverRect.add(pane.modelToView(el.getEndOffset()));
                            Point p = coverRect.getLocation();
                            SwingUtilities.convertPointToScreen(p, pane);
                            coverRect.setLocation(p);
                        } catch (BadLocationException ble) {
                            getLogger().log(Level.SEVERE, null, ble);
                        }
                        MetadataEditorLinkFactory.showEditor(link, coverRect);
                    } else {
                        showHelpFile(link);
                    }
                }
            }
        };
        validationEditor.addHyperlinkListener(hll);
        informationEditor.addHyperlinkListener(hll);
        // move cursor to start of the pre-loaded greeting text (for screen readers)
        validationEditor.select(0, 0);

        // set report tab backgrounds to white
        for (int i = 0; i < infoTab.getTabCount(); ++i) {
            infoTab.setBackgroundAt(i, Color.WHITE);
        }

        if (!isAlwaysOnTopSupported()) {
            onTopSeparator.setVisible(false);
            onTopItem.setVisible(false);
        }

        setSize(getPreferredSize());
        loadPreferences();
        initAnimatedEntrance();
    }

    /**
     * Displays a help file from the files located in
     * <code>/resources/help/*.html</code>.
     *
     * @param baseName the name of the file (e.g. <code>quickstart.html</code>)
     */
    public void showHelpFile(String baseName) {
        URL url = Settings.findResourceForLocale(getClass(), getPreferredLocale(), "/resources/help/" + baseName);
        if (url != null) {
            HelpViewer.showHelpPage(this, url);
        } else {
            getLogger().log(Level.SEVERE, "can't find help file", baseName);
        }
    }

    /**
     * Returns the currently selected rows in the file table as a (possibly
     * empty) array of rows in the table model.
     *
     * @return an array of the selected rows
     */
    public int[] getSelectedRows() {
        int[] rows = fileTable.getSelectedRows();
        if (rows.length == 0 && rowShownInReportViews >= 0) {
            rows = new int[]{rowShownInReportViews};
        } else {
            for (int i = 0; i < rows.length; ++i) {
                rows[i] = fileTable.convertRowIndexToModel(rows[i]);
            }
        }
        return rows;
    }

    public int getActiveRow() {
        return fileTable.convertRowIndexToModel(fileTable.getSelectedRow());
    }

    public LibriVoxAudioFile[] getSelectedFiles() {
        int[] rows = getSelectedRows();
        LibriVoxAudioFile[] files = new LibriVoxAudioFile[rows.length];
        for (int i = 0; i < rows.length; ++i) {
            files[i] = model.getRow(rows[i]);
        }
        return files;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.ButtonGroup strictnessGroup = new javax.swing.ButtonGroup();
        fileInfoSplitter = new javax.swing.JSplitPane();
        tableScrollPane = new javax.swing.JScrollPane();
        fileTable = new javax.swing.JTable();
        infoTab = new javax.swing.JTabbedPane();
        javax.swing.JScrollPane validationScroll = new javax.swing.JScrollPane();
        validationEditor = new javax.swing.JEditorPane();
        javax.swing.JScrollPane informationScroll = new javax.swing.JScrollPane();
        informationEditor = new javax.swing.JEditorPane();
        appMenuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        checkFileItem = new javax.swing.JMenuItem();
        checkURLItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator5 = new javax.swing.JPopupMenu.Separator();
        saveCopyItem = new javax.swing.JMenuItem();
        exitSeparator = new javax.swing.JPopupMenu.Separator();
        exitItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        removeItem = new javax.swing.JMenuItem();
        clearPassed = new javax.swing.JMenuItem();
        clearAll = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator4 = new javax.swing.JPopupMenu.Separator();
        reanalyzeItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator3 = new javax.swing.JPopupMenu.Separator();
        copyValidationItem = new javax.swing.JMenuItem();
        copyInformationItem = new javax.swing.JMenuItem();
        strictnessMenu = new javax.swing.JMenu();
        gentleItem = new javax.swing.JRadioButtonMenuItem();
        strictItem = new javax.swing.JRadioButtonMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        chooseValidatorsItem = new javax.swing.JMenuItem();
        toolMenu = new javax.swing.JMenu();
        viewWaveformItem = new javax.swing.JMenuItem();
        upgradeTagItem = new javax.swing.JMenuItem();
        id3EditItem = new javax.swing.JMenuItem();
        onTopSeparator = new javax.swing.JPopupMenu.Separator();
        onTopItem = new javax.swing.JCheckBoxMenuItem();
        helpMenu = new javax.swing.JMenu();
        helpItem = new javax.swing.JMenuItem();
        quickStartItem = new javax.swing.JMenuItem();
        aboutSeparator = new javax.swing.JPopupMenu.Separator();
        aboutItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Checker");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                Checker.this.windowClosing(evt);
            }
        });

        fileInfoSplitter.setDividerLocation(128);
        fileInfoSplitter.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        tableScrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        fileTable.setModel( model );
        fileTable.setGridColor(java.awt.Color.lightGray);
        fileTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fileTableMouseClicked(evt);
            }
        });
        tableScrollPane.setViewportView(fileTable);

        fileInfoSplitter.setLeftComponent(tableScrollPane);

        infoTab.setBackground(java.awt.Color.white);
        infoTab.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 0, 0, 0));
        infoTab.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                infoTabSelectionChanged(evt);
            }
        });

        validationScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        validationEditor.setEditable(false);
        validationEditor.setBorder(null);
        validationEditor.setContentType("text/html"); // NOI18N
        validationEditor.setText(string("greeting").replace("$VER", VERSION)
        );
        validationScroll.setViewportView(validationEditor);

        infoTab.addTab(string("info-tab-validate"), validationScroll); // NOI18N

        informationScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        informationEditor.setEditable(false);
        informationEditor.setBorder(null);
        informationEditor.setContentType("text/html"); // NOI18N
        informationScroll.setViewportView(informationEditor);

        infoTab.addTab(string("info-tab-information"), informationScroll); // NOI18N

        fileInfoSplitter.setRightComponent(infoTab);
        infoTab.setMnemonicAt( 0, KeyEvent.VK_D );
        infoTab.setMnemonicAt( 1, KeyEvent.VK_I );

        getContentPane().add(fileInfoSplitter, java.awt.BorderLayout.CENTER);

        fileMenu.setText("file");

        checkFileItem.setText("file-open");
        checkFileItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkFiles(evt);
            }
        });
        fileMenu.add(checkFileItem);

        checkURLItem.setText("file-open-url");
        checkURLItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkURLItemActionPerformed(evt);
            }
        });
        fileMenu.add(checkURLItem);
        fileMenu.add(jSeparator5);

        saveCopyItem.setText("file-save-copy");
        saveCopyItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveCopyItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveCopyItem);
        fileMenu.add(exitSeparator);

        exitItem.setText("file-exit");
        exitItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitItem);

        appMenuBar.add(fileMenu);

        editMenu.setText("edit");

        removeItem.setText("edit-clear");
        removeItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeItemActionPerformed(evt);
            }
        });
        editMenu.add(removeItem);

        clearPassed.setText("edit-clear-passed");
        clearPassed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearPassedActionPerformed(evt);
            }
        });
        editMenu.add(clearPassed);

        clearAll.setText("edit-clear-all");
        clearAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearAllActionPerformed(evt);
            }
        });
        editMenu.add(clearAll);
        editMenu.add(jSeparator4);

        reanalyzeItem.setText("edit-reanalyze");
        reanalyzeItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reanalyzeItemActionPerformed(evt);
            }
        });
        editMenu.add(reanalyzeItem);
        editMenu.add(jSeparator3);

        copyValidationItem.setText("copy-validation");
        copyValidationItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyValidationItemActionPerformed(evt);
            }
        });
        editMenu.add(copyValidationItem);

        copyInformationItem.setText("copy-information");
        copyInformationItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyInformationItemActionPerformed(evt);
            }
        });
        editMenu.add(copyInformationItem);

        appMenuBar.add(editMenu);

        strictnessMenu.setText("validation");

        strictnessGroup.add(gentleItem);
        gentleItem.setSelected(true);
        gentleItem.setText("validation-gentle");
        gentleItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gentleItemActionPerformed(evt);
            }
        });
        strictnessMenu.add(gentleItem);

        strictnessGroup.add(strictItem);
        strictItem.setText("validation-strict");
        strictItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                strictItemActionPerformed(evt);
            }
        });
        strictnessMenu.add(strictItem);
        strictnessMenu.add(jSeparator1);

        chooseValidatorsItem.setText("validation-choose");
        chooseValidatorsItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chooseValidatorsItemActionPerformed(evt);
            }
        });
        strictnessMenu.add(chooseValidatorsItem);

        appMenuBar.add(strictnessMenu);

        toolMenu.setText("tools");

        viewWaveformItem.setText("tools-view-waveform");
        viewWaveformItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewWaveformItemActionPerformed(evt);
            }
        });
        toolMenu.add(viewWaveformItem);

        upgradeTagItem.setText("tools-upgrade-tags");
        upgradeTagItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upgradeTagItemActionPerformed(evt);
            }
        });
        toolMenu.add(upgradeTagItem);

        id3EditItem.setText("tools-edit-tags");
        id3EditItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                id3EditItemActionPerformed(evt);
            }
        });
        toolMenu.add(id3EditItem);
        toolMenu.add(onTopSeparator);

        onTopItem.setText("tools-keep-on-top");
        onTopItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onTopItemActionPerformed(evt);
            }
        });
        toolMenu.add(onTopItem);

        appMenuBar.add(toolMenu);

        helpMenu.setText("help");

        helpItem.setText("help-contents");
        helpItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpItemActionPerformed(evt);
            }
        });
        helpMenu.add(helpItem);

        quickStartItem.setText("quickstart");
        quickStartItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quickStartItemActionPerformed(evt);
            }
        });
        helpMenu.add(quickStartItem);
        helpMenu.add(aboutSeparator);

        aboutItem.setText("about");
        aboutItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutItem);

        appMenuBar.add(helpMenu);

        setJMenuBar(appMenuBar);

        setSize(new java.awt.Dimension(564, 550));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    public void removeFile(int row) {
        int rowToShow = rowShownInReportViews;
        if (row == rowShownInReportViews && row == model.getRowCount() - 1) {
            --rowToShow;
        }
        model.removeRow(row);
        updateReportViews(rowToShow);
    }

    public void checkZIPFile(File f) {
        ZipFile zip = null;
        try {
            String baseURL = f.toURI().toURL().toExternalForm();
            zip = new ZipFile(f);
            Enumeration<? extends ZipEntry> entries = zip.entries();
            int top = model.getRowCount();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.toLowerCase(Locale.ENGLISH).endsWith(".mp3")) {
                    try {
                        URL zipEntryURL = new URL("jar:" + baseURL + "!/" + name);
                        checkURL(zipEntryURL);
                    } catch (MalformedURLException e) {
                        getLogger().log(Level.SEVERE, "unable to compose URL for {0}!{1}", new Object[]{f, name});
                    }
                }
            }
            ensureRowIsVisible(top);
        } catch (IOException e) {
            displayErrorMessage(string("error-extract", f, e.getLocalizedMessage()));
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException e) {
                    getLogger().log(Level.WARNING, "closing " + f, e);
                }
            }
        }
    }

    public void checkFile(File f) {
        if (f.isDirectory()) {
            for (File child : f.listFiles(mp3FileFilter)) {
                try {
                    checkFile(child);
                } catch (StackOverflowError e) {
                    getLogger().warning("could not add all files (stack depth)");
                }
            }
        } else if (f.getName().toLowerCase(Locale.ENGLISH).endsWith(".zip")) {
            checkZIPFile(f);
        } else {
            check(new LibriVoxAudioFile(f));
        }
    }

    public void checkURL(URL url) {
        check(new LibriVoxAudioFile(url));
    }

    private void check(LibriVoxAudioFile f) {
        try {
            setWaitCursor(true);
            int row = model.addAudioFile(f);
            fileTable.scrollRectToVisible(
                    fileTable.getCellRect(row, 0, true));
            autoselectReportView();
        } finally {
            setWaitCursor(false);
        }
    }

    // if there is only one file in the table, show it in the report view
    private void autoselectReportView() {
        if (fileTable.getSelectedRowCount() == 0) {
            int row = model.getRowCount() - 1;
            fileTable.setRowSelectionInterval(row, row);
        }
    }

private void checkFiles(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkFiles
    try {
        setWaitCursor(true);
        initFileChooser();
        if (JFileChooser.APPROVE_OPTION == fileChooser.showDialog(this, string("file-ok"))) {
            File[] files = fileChooser.getSelectedFiles();
            int top = model.getRowCount();
            for (File f : files) {
                checkFile(f);
            }
            ensureRowIsVisible(top);
        }
    } finally {
        setWaitCursor(false);
    }
}//GEN-LAST:event_checkFiles

    private void ensureRowIsVisible(int modelRow) {
        if (modelRow >= model.getRowCount()) {
            modelRow = model.getRowCount() - 1;
        }
        if (modelRow < 0) {
            return;
        }
        int viewRow = fileTable.convertRowIndexToView(modelRow);
        fileTable.scrollRectToVisible(fileTable.getCellRect(viewRow, 0, true));
    }

private void checkURLItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkURLItemActionPerformed
    addURLDialog.setLocationRelativeTo(this);
    addURLDialog.setVisible(true);
    addURLDialog.toFront();
    addURLDialog.requestFocusInWindow();
}//GEN-LAST:event_checkURLItemActionPerformed

private void windowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_windowClosing
    exit();
}//GEN-LAST:event_windowClosing

private void removeItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeItemActionPerformed
    int[] rows = getSelectedRows();
    Arrays.sort(rows);
    for (int i = rows.length - 1; i >= 0; --i) {
        removeFile(rows[i]);
    }
}//GEN-LAST:event_removeItemActionPerformed

private void clearPassedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearPassedActionPerformed
    for (int i = model.getRowCount() - 1; i >= 0; --i) {
        if (model.getRow(i).getStatus() == Status.PASSED) {
            removeFile(i);
        }
    }
}//GEN-LAST:event_clearPassedActionPerformed

private void viewWaveformItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewWaveformItemActionPerformed
    int[] rows = getSelectedRows();
    for (int i = 0; i < rows.length; ++i) {
        new WaveformViewer(this, model.getRow(rows[i]));
    }
}//GEN-LAST:event_viewWaveformItemActionPerformed

private void saveCopyItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveCopyItemActionPerformed

    int sel = fileTable.getSelectedRow();
    if (sel < 0) {
        getToolkit().beep();
        return;
    }

    File fromFile = model.getRow(fileTable.convertRowIndexToModel(sel)).getLocalFile();
    File toFile = null;

    while (toFile == null) {
        if (JFileChooser.APPROVE_OPTION == fileChooser.showSaveDialog(this)) {
            toFile = fileChooser.getSelectedFile();
            if (toFile.exists()) {
                int yesNoCancel = JOptionPane.showConfirmDialog(
                        this, string("save-confirm-replace", toFile.getName()),
                        "", JOptionPane.YES_NO_CANCEL_OPTION);
                if (yesNoCancel == JOptionPane.CANCEL_OPTION) {
                    return;
                }
                if (yesNoCancel == JOptionPane.NO_OPTION) {
                    toFile = null;
                }
            }
        } else {
            return;
        }
    }

    try {
        StreamCopier.copyFile(fromFile, toFile);
    } catch (IOException e) {
        String message = e.getLocalizedMessage();
        if (message != null && message.length() > 0) {
            message = "\n" + message;
        } else {
            message = "";
        }
        JOptionPane.showMessageDialog(
                this, string("error-io-save-copy") + message, "", JOptionPane.ERROR_MESSAGE);
    }
}//GEN-LAST:event_saveCopyItemActionPerformed

private void reanalyzeItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reanalyzeItemActionPerformed
    int[] sel = getSelectedRows();
    for (int row : sel) {
        model.getRow(row).reanalyze();
    }
}//GEN-LAST:event_reanalyzeItemActionPerformed

private void exitItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitItemActionPerformed
    exit();
}//GEN-LAST:event_exitItemActionPerformed

private void fileTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fileTableMouseClicked
    if (evt.getClickCount() >= 2 && evt.getButton() == MouseEvent.BUTTON1) {
        Point p = evt.getPoint();
        int row = fileTable.convertRowIndexToModel(fileTable.rowAtPoint(p));
        if (row >= 0 && row < model.getRowCount()) {
            LibriVoxAudioFile file = model.getRow(row);
            try {
                Desktop.getDesktop().open(file.getLocalFile());
            } catch (IOException t) {
                getLogger().warning(t.toString());
            }
        }
    }
}//GEN-LAST:event_fileTableMouseClicked

private void upgradeTagItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upgradeTagItemActionPerformed
    ID3UpgradeTool.promptAndUpgrade(this, getSelectedFiles());
}//GEN-LAST:event_upgradeTagItemActionPerformed

private void clearAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearAllActionPerformed
    int rows = model.getRowCount();
    for (int i = rows - 1; i >= 0; --i) {
        removeFile(i);
    }
}//GEN-LAST:event_clearAllActionPerformed

private void gentleItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gentleItemActionPerformed
    AbstractValidator.setUserStrictnessSuffix("_g");
    warnAboutStrictnessChange();
}//GEN-LAST:event_gentleItemActionPerformed

private void strictItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_strictItemActionPerformed
    AbstractValidator.setUserStrictnessSuffix("");
    warnAboutStrictnessChange();
}//GEN-LAST:event_strictItemActionPerformed

	private void quickStartItemActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_quickStartItemActionPerformed
            showHelpFile("quickstart.html");
	}//GEN-LAST:event_quickStartItemActionPerformed

	private void aboutItemActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_aboutItemActionPerformed
            about();
	}//GEN-LAST:event_aboutItemActionPerformed

    /**
     * Shows the application's About dialog.
     */
    public void about() {
        HelpViewer.showAboutViewer(this);
    }

	private void infoTabSelectionChanged( javax.swing.event.ChangeEvent evt ) {//GEN-FIRST:event_infoTabSelectionChanged
            // explicitly request focus in the selected tab
            // in order to screen readers to access the contents
            Component c = infoTab.getSelectedComponent();
            if (c != null) {
                ((JScrollPane) c).getViewport().getView().requestFocusInWindow();
            }
	}//GEN-LAST:event_infoTabSelectionChanged

	private void copyValidationItemActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_copyValidationItemActionPerformed
            copyReport(0);
	}//GEN-LAST:event_copyValidationItemActionPerformed

	private void copyInformationItemActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_copyInformationItemActionPerformed
            copyReport(1);
	}//GEN-LAST:event_copyInformationItemActionPerformed

	private void helpItemActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_helpItemActionPerformed
            showHelpFile("index.html");
	}//GEN-LAST:event_helpItemActionPerformed

	private void onTopItemActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_onTopItemActionPerformed
            setAlwaysOnTop(onTopItem.isSelected());
	}//GEN-LAST:event_onTopItemActionPerformed

	private void chooseValidatorsItemActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_chooseValidatorsItemActionPerformed
            ChooseValidatorDialog d = new ChooseValidatorDialog(this);
            d.setVisible(true);
            warnAboutStrictnessChange();
	}//GEN-LAST:event_chooseValidatorsItemActionPerformed

	private void id3EditItemActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_id3EditItemActionPerformed
            try {
                if (getActiveRow() < 0) {
                    return;
                }
                ID3Editor ed = new ID3Editor(this, getSelectedFiles(), model.getRow(getActiveRow()));
                ed.setLocationRelativeTo(this);
                ed.setVisible(true);
            } catch (CancellationException e) {
                // user didn't want to wait for file to finish processing
            }
	}//GEN-LAST:event_id3EditItemActionPerformed

    private void copyReport(int index) {
        infoTab.setSelectedIndex(index);
        JEditorPane ed = index == 0 ? validationEditor : informationEditor;
        ed.requestFocusInWindow();
        ed.selectAll();
        ed.copy();
    }

    private void warnAboutStrictnessChange() {
        if (model.getRowCount() > 0) {
            boolean anyBusy = false;
            for (int i = 0; i < model.getRowCount(); ++i) {
                if (model.getRow(i).isBusy()) {
                    anyBusy = true;
                    break;
                }
            }
            if (anyBusy) {
                JOptionPane.showMessageDialog(
                        this, string("validation-warning"), "",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void initFileChooser() {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            fileChooser.addChoosableFileFilter(mp3FileFilter);
            fileChooser.setMultiSelectionEnabled(true);
        }
    }
    private JFileChooser fileChooser;
    private MP3FileFilter mp3FileFilter = new MP3FileFilter();

    private void initDropTarget() {
        new ca.cgjennings.ui.dnd.FileDrop(getRootPane(),
                BorderFactory.createLineBorder(new Color(0x025095), 3),
                tableScrollPane, true, files -> {
                    int top = model.getRowCount();
                    for (File f : files) {
                        checkFile(f);
                    }
                    ensureRowIsVisible(top);
        });
    }

    /**
     * Set or hide the wait cursor over the application window, if this was run
     * as an application and the window is visible. This method is thread safe.
     * Calls to this method nest, so if the wait cursor is set <i>n</i>
     * times, it must be unset <i>n</i> times before the default cursor is
     * restored.
     * <p>
     * <b>Important:</b> To ensure that an exception does not lock the window
     * into wait cursor mode, always place the
     * <code>setWaitCursor( false )</code> statement in a <code>finally </code>
     * clause.
     *
     * @param waiting
     */
    public static void setWaitCursor(final boolean waiting) {
        if (EventQueue.isDispatchThread()) {
            setWaitCursorImpl(waiting);
        } else {
            EventQueue.invokeLater(() -> setWaitCursorImpl(waiting));
        }
    }

    /**
     * The non-thread safe part of {@link #setWaitCursor(boolean)}.
     */
    private static void setWaitCursorImpl(boolean waiting) {
        if (waiting) {
            ++waitNesting;
        } else {
            --waitNesting;
        }
        if (waitNesting < 2 && mainApp != null && mainApp.isShowing()) {
            Component glass = mainApp.getGlassPane();
            if (waitNesting == 1) {
                glass.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                glass.setVisible(true);
            } else {
                glass.setVisible(false);
                glass.setCursor(Cursor.getDefaultCursor());
            }
        }
        if (waitNesting < 0) {
            waitNesting = 0;
            Throwable t = new AssertionError("setWaitCursor(false) without setWaitCursor(true)");
            getLogger().log(Level.SEVERE, null, t);
        }
    }

    private void updateReportViews(int row) {
        LibriVoxAudioFile file = null;
        if (row >= model.getRowCount()) {
            return;
        }
        if (row >= 0) {
            file = model.getRow(row);
            // if the file is already showing and the
            // tool is busy analyzing or downloading, then
            // do not update (otherwise, it will update repeatedly
            // with a blank document when the progress bar updates)
            Status newStatus = file.getStatus();
            if ((row == rowShownInReportViews) && file.getStatus() == rowStatusShownInReportViews) {
                return;
            }

            rowStatusShownInReportViews = newStatus;
        } else {
            if (row == rowShownInReportViews) {
                return;
            } else {
                rowStatusShownInReportViews = Status.DOWNLOADING;
            }
        }
        rowShownInReportViews = row;

        setWaitCursor(true);
        try {
            String validReport, infoReport;
            if (row < 0) {
                String report = Report.getDefaultDocument();
                validationEditor.setText(report);
                informationEditor.setText(report);
            } else {
                validReport = file.getValidationReport();
                infoReport = file.getInformationReport();
                validationEditor.setText(validReport);
                informationEditor.setText(infoReport);
            }
            validationEditor.select(0, 0);
            informationEditor.select(0, 0);
        } finally {
            setWaitCursor(false);
        }
    }
    private int rowShownInReportViews = -1;
    private Status rowStatusShownInReportViews = Status.DOWNLOADING;
    // used to determine if and which window to use when the wait cursor is
    // set using the static methods---this means that these can be called from
    // other classes safely if we feel the need
    private static Checker mainApp = null;
    private static int waitNesting = 0;

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        initLoggerLevel(Level.WARNING);

        java.awt.EventQueue.invokeLater(() -> {
            initLookAndFeel();
            mainApp = new Checker();
            MetadataEditorLinkFactory.setEditorParentFrame(mainApp);
        });
    }

    /**
     * Called before creating the application window to install a look and feel.
     * On OS X, installs the system look and feel. On other platforms, will
     * install Nimbus if available or else fall back to the system look and feel
     * (e.g., under Java 5).
     */
    private static void initLookAndFeel() {
        if (PlatformSupport.PLATFORM_IS_OSX) {
            try {
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
                getLogger().log(Level.SEVERE, null, e);
            }
            return;
        }

        // Try to load preferred L&F, with system L&F as fallback
        try {
            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException ex2) {
                getLogger().log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * If running on OS X, this will cause the "About" and "Quit" menu items to
     * appear in the application menu rather than the "File" and "Help" menus,
     * respectively. On other platforms it has no effect.
     */
    private void installOSXMenuHandlers() {
        if (!PlatformSupport.PLATFORM_IS_OSX) {
            return;
        }
        try {
            OSXAdapter.setQuitHandler(this, Checker.class.getMethod("exit"));
            OSXAdapter.setAboutHandler(this, Checker.class.getMethod("about"));
            fileMenu.remove(exitSeparator);
            fileMenu.remove(exitItem);
            helpMenu.remove(aboutSeparator);
            helpMenu.remove(aboutItem);
        } catch (NoSuchMethodException | SecurityException ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
    }

    private static void initLoggerLevel(Level level) {
        Logger base = Logger.getGlobal();
        while (base.getParent() != null) {
            base = base.getParent();
        }
        base.setLevel(level);
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutItem;
    private javax.swing.JPopupMenu.Separator aboutSeparator;
    private javax.swing.JMenuBar appMenuBar;
    private javax.swing.JMenuItem checkFileItem;
    private javax.swing.JMenuItem checkURLItem;
    private javax.swing.JMenuItem chooseValidatorsItem;
    private javax.swing.JMenuItem clearAll;
    private javax.swing.JMenuItem clearPassed;
    private javax.swing.JMenuItem copyInformationItem;
    private javax.swing.JMenuItem copyValidationItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitItem;
    private javax.swing.JPopupMenu.Separator exitSeparator;
    private javax.swing.JSplitPane fileInfoSplitter;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JTable fileTable;
    private javax.swing.JRadioButtonMenuItem gentleItem;
    private javax.swing.JMenuItem helpItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem id3EditItem;
    private javax.swing.JTabbedPane infoTab;
    private javax.swing.JEditorPane informationEditor;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JCheckBoxMenuItem onTopItem;
    private javax.swing.JPopupMenu.Separator onTopSeparator;
    private javax.swing.JMenuItem quickStartItem;
    private javax.swing.JMenuItem reanalyzeItem;
    private javax.swing.JMenuItem removeItem;
    private javax.swing.JMenuItem saveCopyItem;
    private javax.swing.JRadioButtonMenuItem strictItem;
    private javax.swing.JMenu strictnessMenu;
    private javax.swing.JScrollPane tableScrollPane;
    private javax.swing.JMenu toolMenu;
    private javax.swing.JMenuItem upgradeTagItem;
    private javax.swing.JEditorPane validationEditor;
    private javax.swing.JMenuItem viewWaveformItem;
    // End of variables declaration//GEN-END:variables

    /**
     * Exit the application, performing all necessary cleanup.
     */
    public void exit() {
        dispose();
        savePreferences();
        System.exit(0);
    }

    /**
     * Return a localized (and possibly formatted) text string. The value of
     * <code>key</code> will be fetched from the
     * <tt>text_<i>XX</i>.properties</text> file appropriate for the default
     * language on the platform. If the optional <code>arguments</code> are
     * provided, they will be used to format the fetched string as if by<br>
     * <code>
     * String.format( Locale.getDefault(), text, arguments )
     * </code>
     *
     * <p>
     * If the requested key is not defined, the returned string take the form
     * "[MISSING STRING key]" where key is the key value that was passed in.
     *
     * @param key the key for the desired text
     * @param arguments parameters to use when formatting the localized text
     * @return the localized, formatted value for <code>key</code>
     */
    public static String string(String key, Object... arguments) {
        return String.format(locale, string(key), arguments);
    }

    /**
     * Return a localized text string. The value of <code>key</code> will be
     * fetched from the
     * <tt>text_<i>XX</i>.properties</text> file appropriate for the default
     * language on the platform.
     *
     * <p>
     * If the requested key is not defined, the returned string take the form
     * "[MISSING STRING key]" where key is the key value that was passed in.
     *
     * @param key the key for the desired text
     * @return the localized value for <code>key</code>
     */
    public static String string(String key) {
        try {
            return strings.getString(key);
        } catch (MissingResourceException e) {
            getLogger().log(Level.SEVERE, "missing definition for string: {0}", key);
            return "[MISSING STRING " + key + "]";
        }
    }

    /**
     * Returns <code>true</code> if a string is defined in the string resource
     * table.
     *
     * @param key the key to check
     * @return <code>true</code> if the key is defined
     * @see #string(java.lang.String)
     */
    public static boolean stringExists(String key) {
        // Java 5 soes not have ResourceBundle.containsKey()
        try {
            strings.getString(key);
            return true;
        } catch (MissingResourceException e) {
            return false;
        }
    }
    private static ResourceBundle strings;

    public static Locale getPreferredLocale() {
        return locale;
    }
    private static Locale locale;

    static {
        locale = Locale.getDefault();
        strings = ResourceBundle.getBundle("resources/text", locale);
    }

    /**
     * Returns application settings.
     *
     * @return the application settings instance
     */
    public static Settings getSettings() {
        if (settings == null) {
            settings = new Settings();
            try {
                settings.read(Checker.class.getResource("/resources/application.properties"));
                settings.readFromLocalStorage(Checker.class);
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Unable to read settings", e);
            }
        }
        return settings;
    }
    private static Settings settings;
    private FileTableModel model;
    private URLDialog addURLDialog;

    /**
     * Writes any pending application setting changes to local storage.
     */
    public static void flushSettings() {
        if (settings != null) {
            settings.writeToLocalStorage(Checker.class);
        }
    }

    /**
     * Returns the <code>Logger</code> used for logging errors and other
     * messages from the application.
     *
     * @return the shared logger instance for the application
     */
    public static Logger getLogger() {
        return LOGGER;
    }
    private static final Logger LOGGER = Logger.getLogger("ca.cgjennings.apps.librivox");

    /**
     * Snaps a window to the left or right edge of the application window,
     * depending on which side has more space.
     *
     * @param w the window to snap against the application window
     * @param yAlign a value describing how to align the window vertically;
     * negative to align to top, 0 to align to middle; positive to align to
     * bottom
     */
    public static void popToEdge(Window w, int yAlign) {
        Rectangle screenBounds = mainApp.getGraphicsConfiguration().getBounds();
        Rectangle windowBounds = mainApp.getBounds();

        int roomOnLeft = windowBounds.x - screenBounds.x;
        int roomOnRight = (screenBounds.x + screenBounds.width) - (windowBounds.x + windowBounds.width);

        final int GAP = 16;
        int x, y;
        if (roomOnLeft > roomOnRight) {
            x = windowBounds.x - w.getWidth() - GAP;
        } else {
            x = windowBounds.x + windowBounds.width + GAP;
        }

        if (yAlign < 0) {
            y = windowBounds.y + GAP;
        } else if (yAlign > 0) {
            y = windowBounds.y + windowBounds.height - GAP;
        } else {
            y = windowBounds.y + (windowBounds.height - w.getHeight()) / 2;
        }

        mainApp.bumpWindowOntoDisplay(w, x, y);
    }

    private void bumpWindowOntoDisplay(Window w, int x, int y) {
        Rectangle screenBounds = getGraphicsConfiguration().getBounds();
        Rectangle windowBounds = getBounds();

        int limit = screenBounds.x + screenBounds.width;
        if (x + w.getWidth() > limit) {
            x = limit - (x + w.getWidth());
        }
        if (x < screenBounds.x) {
            x = screenBounds.x;
        }

        limit = screenBounds.y + screenBounds.height;
        if (y + w.getHeight() > limit) {
            y = limit - (y + w.getHeight());
        }
        if (y < screenBounds.y) {
            y = screenBounds.y;
        }

        w.setLocation(x, y);
    }

    private void initAnimatedEntrance() {
        Rectangle screenBounds = getGraphicsConfiguration().getBounds();
        Rectangle windowBounds = getBounds();
        final int x1 = screenBounds.x + screenBounds.width;
        final int x2 = x1 - windowBounds.width - INITIAL_WINDOW_POSITION_MARGIN;
        final int y = windowBounds.y;
        setLocation(x1, y);
        ActionListener al = new ActionListener() {
            double x = x1;
            double dx = (x2 - x1) / 10;

            @Override
            public void actionPerformed(ActionEvent e) {
                setLocation((int) (x + 0.5), y);
                x += dx;
                if (x <= x2) {
                    ((Timer) e.getSource()).stop();
                }
                if (!isVisible()) {
                    setVisible(true);
                }
            }
        };
        Timer t = new Timer(1000 / 60, al); // i.e. try for 60 fps
        t.start();
    }
    private static final int INITIAL_WINDOW_POSITION_MARGIN = 32;

    private void initAppIcons() {
        final List<Image> icons = new LinkedList<>();
        final String[] iconFiles = new String[]{
            "pass@4x", "pass@2x", "pass"
        };

        for (String file : iconFiles) {
            BufferedImage image = null;
            Throwable failReason = null;
            URL imageURL = getClass().getResource("/resources/" + file + ".png");
            if (imageURL != null) {
                try {
                    image = ImageIO.read(imageURL);
                } catch (IOException ex) {
                    failReason = ex;
                }
            }
            if (image == null) {
                getLogger().log(Level.WARNING, "failed to read icon image", failReason);
            } else {
                icons.add(image);
            }
        }
        setIconImages(icons);
    }

    /**
     * Displays an error message in a dialog box.
     *
     * @param message the string to display
     */
    public void displayErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, "<html>" + message, "", JOptionPane.ERROR_MESSAGE);
    }

    private void localizeMenu(Component menu) {
        if (!(menu instanceof JMenuItem)) {
            if (menu instanceof JMenuBar) {
                JMenuBar bar = (JMenuBar) menu;
                for (int i = 0; i < bar.getMenuCount(); ++i) {
                    localizeMenu(bar.getMenu(i));
                }
            }
            return; // e.g., separators
        }

        if (menu instanceof JMenu) {
            JMenu parent = (JMenu) menu;
            int children = parent.getMenuComponentCount();
            for (int i = 0; i < children; ++i) {
                localizeMenu(parent.getMenuComponent(i));
            }
        }
        JMenuItem item = (JMenuItem) menu;
        localizeMenuItem(item, item.getText());
    }

    /**
     * Sets the text of any JMenuItem to a string from a localization key. If
     * the string contains an ampersand (&), the following character is set as
     * the item's mnemonic and the ampersand is removed. If the string contains
     * a |, the following text is parsed as a key stroke that will act as the
     * item's accelerator.
     */
    private void localizeMenuItem(final JMenuItem menu, String key) {
        String value;
        if (PlatformSupport.PLATFORM_IS_OSX && stringExists(key + "-mac")) {
            value = string(key + "-mac");
        } else {
            value = string(key);
        }

        // find accelerator
        int i = value.indexOf('|');
        if (i >= 0) {
            KeyStroke keyStroke = PlatformSupport.getKeyStroke(value.substring(i + 1).trim());
            value = value.substring(0, i).trim();
            menu.setAccelerator(keyStroke);
            Action a = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    menu.doClick(100);
                }
            };
            getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStroke, key);
            getRootPane().getActionMap().put(key, a);
        }

        // replace "..." with true ellipsis
        value = value.replace("...", "\u2026");

        // find mnemonic in the part that is left (minus any accelerator)
        i = value.indexOf('&');
        if (i >= 0 && i < value.length() - 1) {
            char mnemonic = value.charAt(i + 1);
            value = value.substring(0, i) + value.substring(i + 1);
            menu.setText(value);
            menu.setMnemonic(mnemonic);
            menu.setDisplayedMnemonicIndex(i);
        } else {
            menu.setText(value);
        }
    }

    private void installMenuUpdater() {
        final MenuAdapter enabledStateUpdater = new MenuAdapter() {
            @Override
            public void menuSelected(MenuEvent e) {
                boolean enable = getSelectedRows().length > 0;
                saveCopyItem.setEnabled(enable);
                setAllEnabled(editMenu, enable);
                setAllEnabled(toolMenu, enable);
                onTopItem.setEnabled(true);
            }

            private void setAllEnabled(JMenu m, boolean enable) {
                for (int i = 0; i < m.getMenuComponentCount(); ++i) {
                    Component c = m.getMenuComponent(i);
                    if (!(c instanceof JSeparator)) {
                        c.setEnabled(enable);
                    }
                }
            }
        };
        fileMenu.addMenuListener(enabledStateUpdater);
        editMenu.addMenuListener(enabledStateUpdater);
        toolMenu.addMenuListener(enabledStateUpdater);
    }

    private static final String PREF_STRICT_MODE = "strict mode";
    private static final String PREF_KEEP_ON_TOP = "keep on top";
    private static final String PREF_SELECTED_TAB = "selected tab";
    private static final String PREF_SPLITTER = "splitter position";

    /**
     * Load application preferences (called during startup).
     */
    private void loadPreferences() {
        Settings s = getSettings();
        s.readFromLocalStorage(Checker.class);

        final boolean strict = s.getBoolean(PREF_STRICT_MODE, false);
        if (strict) {
            strictItem.setSelected(true);
            AbstractValidator.setUserStrictnessSuffix("");
        }

        final boolean onTop = s.getBoolean(PREF_KEEP_ON_TOP, false);
        if (onTop) {
            onTopItem.setSelected(true);
            onTopItemActionPerformed(null);
        }

        int tabIndex = Math.min(Math.max(0, s.getInt(PREF_SELECTED_TAB, 0)), infoTab.getTabCount() - 1);
        infoTab.setSelectedIndex(tabIndex);

        // if negative, value from GUI editor layout is used
        int splitpos = s.getInt(PREF_SPLITTER, -1);
        if (splitpos > fileInfoSplitter.getHeight()) {
            splitpos = -1;
        }
        if (splitpos >= 0) {
            fileInfoSplitter.setDividerLocation(splitpos);
        }
    }

    /**
     * Save application preferences (typically called at shutdown, but may be
     * called at other times as well).
     */
    private void savePreferences() {
        Settings s = getSettings();

        s.setBoolean(PREF_STRICT_MODE, strictItem.isSelected());
        s.setBoolean(PREF_KEEP_ON_TOP, onTopItem.isSelected());
        s.setInt(PREF_SELECTED_TAB, infoTab.getSelectedIndex());
        s.setInt(PREF_SPLITTER, fileInfoSplitter.getDividerLocation());

        flushSettings();
    }
}
