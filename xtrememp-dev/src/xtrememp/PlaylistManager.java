/**
 * Xtreme Media Player a cross-platform media player.
 * Copyright (C) 2005-2010 Besmir Beqiri
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package xtrememp;

import xtrememp.playlist.sort.DurationComparator;
import xtrememp.playlist.sort.GenreComparator;
import java.util.Comparator;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import javax.sound.sampled.AudioSystem;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.text.Document;
import org.apache.commons.io.FilenameUtils;
import org.pushingpixels.substance.api.renderers.SubstanceDefaultTableCellRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xtrememp.player.audio.AudioPlayer;
import xtrememp.playlist.Playlist;
import xtrememp.playlist.PlaylistException;
import xtrememp.playlist.PlaylistIO;
import xtrememp.playlist.PlaylistItem;
import xtrememp.playlist.filter.Predicate;
import xtrememp.playlist.filter.TruePredicate;
import xtrememp.playlist.sort.AlbumComparator;
import xtrememp.playlist.sort.ArtistComparator;
import xtrememp.playlist.sort.HeaderPopupMenu;
import xtrememp.playlist.sort.TitleComparator;
import xtrememp.playlist.sort.TrackComparator;
import xtrememp.ui.text.SearchTextField;
import xtrememp.util.AbstractSwingWorker;
import xtrememp.util.Utilities;
import xtrememp.util.file.AudioFileFilter;
import xtrememp.util.file.M3uPlaylistFileFilter;
import xtrememp.util.file.PlaylistFileFilter;
import xtrememp.util.file.XspfPlaylistFileFilter;
import static xtrememp.util.Utilities.tr;

/**
 * Playlist manager class.
 * Special thanks to rom1dep for the changes applied to this class.
 *
 * @author Besmir Beqiri
 */
public class PlaylistManager extends JPanel implements ActionListener,
        DropTargetListener, ListSelectionListener {

    private final Logger logger = LoggerFactory.getLogger(PlaylistManager.class);
    private final AudioFileFilter audioFileFilter = new AudioFileFilter();
    private final PlaylistFileFilter playlistFileFilter = new PlaylistFileFilter();
    private final String upArrowChar = "\u25B2";
    private final String downArrowChar = "\u25BC";
    private JTable playlistTable;
    private JButton openPlaylistButton;
    private JButton savePlaylistButton;
    private JButton addToPlaylistButton;
    private JButton remFromPlaylistButton;
    private JButton clearPlaylistButton;
    private JButton moveUpButton;
    private JButton moveDownButton;
    private JButton mediaInfoButton;
    private ControlListener controlListener;
    private Playlist playlist;
    private PlaylistTableModel playlistTableModel;
    private HeaderPopupMenu playlistHeaderPopupMenu;
    private TableColumnModel tableColumnModel;
    private SearchTextField searchTextField;
    private Predicate<PlaylistItem> searchFilter;
    private String searchString;
    private int doubleSelectedRow = -1;
    private boolean firstPlaylistLoad = true;

    public PlaylistManager(ControlListener controlListener) {
        super(new BorderLayout());
        this.controlListener = controlListener;
        playlist = new Playlist();
        initModel();
        initComponents();
        initFiltering();
    }

    private void initModel() {
        playlistTableModel = new PlaylistTableModel(playlist);
    }

    private void initComponents() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        openPlaylistButton = new JButton(Utilities.DOCUMENT_OPEN_ICON);
        openPlaylistButton.setToolTipText(tr("MainFrame.PlaylistManager.OpenPlaylist"));
        openPlaylistButton.addActionListener(this);
        toolBar.add(openPlaylistButton);
        savePlaylistButton = new JButton(Utilities.DOCUMENT_SAVE_ICON);
        savePlaylistButton.setToolTipText(tr("MainFrame.PlaylistManager.SavePlaylist"));
        savePlaylistButton.addActionListener(this);
        toolBar.add(savePlaylistButton);
        toolBar.addSeparator();
        addToPlaylistButton = new JButton(Utilities.LIST_ADD_ICON);
        addToPlaylistButton.setToolTipText(tr("MainFrame.PlaylistManager.AddToPlaylist"));
        addToPlaylistButton.addActionListener(this);
        toolBar.add(addToPlaylistButton);
        remFromPlaylistButton = new JButton(Utilities.LIST_REMOVE_ICON);
        remFromPlaylistButton.setToolTipText(tr("MainFrame.PlaylistManager.RemoveFromPlaylist"));
        remFromPlaylistButton.addActionListener(this);
        remFromPlaylistButton.setEnabled(false);
        toolBar.add(remFromPlaylistButton);
        clearPlaylistButton = new JButton(Utilities.EDIT_CLEAR_ICON);
        clearPlaylistButton.setToolTipText(tr("MainFrame.PlaylistManager.ClearPlaylist"));
        clearPlaylistButton.addActionListener(this);
        clearPlaylistButton.setEnabled(false);
        toolBar.add(clearPlaylistButton);
        toolBar.addSeparator();
        moveUpButton = new JButton(Utilities.GO_UP_ICON);
        moveUpButton.setToolTipText(tr("MainFrame.PlaylistManager.MoveUp"));
        moveUpButton.addActionListener(this);
        moveUpButton.setEnabled(false);
        toolBar.add(moveUpButton);
        moveDownButton = new JButton(Utilities.GO_DOWN_ICON);
        moveDownButton.setToolTipText(tr("MainFrame.PlaylistManager.MoveDown"));
        moveDownButton.addActionListener(this);
        moveDownButton.setEnabled(false);
        toolBar.add(moveDownButton);
        toolBar.addSeparator();
        mediaInfoButton = new JButton(Utilities.MEDIA_INFO_ICON);
        mediaInfoButton.setToolTipText(tr("MainFrame.PlaylistManager.MediaInfo"));
        mediaInfoButton.addActionListener(this);
        mediaInfoButton.setEnabled(false);
        toolBar.add(mediaInfoButton);
        toolBar.add(Box.createHorizontalGlue());
        searchTextField = new SearchTextField(15);
        searchTextField.setMaximumSize(new Dimension(120, searchTextField.getPreferredSize().height));
        searchTextField.getTextField().getDocument().addDocumentListener(new SearchFilterListener());
        toolBar.add(searchTextField);
        toolBar.add(Box.createHorizontalStrut(6));
        this.add(toolBar, BorderLayout.NORTH);

        playlistTable = new JTable(playlistTableModel);
        playlistTable.setDefaultRenderer(String.class, new PlaylistCellRenderer());
        playlistTable.setActionMap(null);
        tableColumnModel = playlistTable.getColumnModel();

        playlistHeaderPopupMenu = new HeaderPopupMenu();

        playlistTable.getTableHeader().addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent ev) {
                if (SwingUtilities.isRightMouseButton(ev) || (MouseInfo.getNumberOfButtons() == 1 && ev.isControlDown())) {
                    playlistHeaderPopupMenu.show(playlistTable.getTableHeader(), ev.getX(), ev.getY());
                    return;
                }

                int clickedColumn = tableColumnModel.getColumnIndexAtX(ev.getX());
                TableColumn tableColumn = tableColumnModel.getColumn(clickedColumn);
                String columnName = String.valueOf(tableColumn.getHeaderValue());
                //String headerValue = String.valueOf(tableColumn.getHeaderValue());

                Comparator<PlaylistItem> comparator = null;
                if (columnName.contains(PlaylistTableModel.COLUMN_NAMES[0])) {
                    comparator = new TrackComparator();
                } else if (columnName.contains(PlaylistTableModel.COLUMN_NAMES[1])) {
                    comparator = new TitleComparator();
                } else if (columnName.contains(PlaylistTableModel.COLUMN_NAMES[2])) {
                    comparator = new DurationComparator();
                } else if (columnName.contains(PlaylistTableModel.COLUMN_NAMES[3])) {
                    comparator = new ArtistComparator();
                } else if (columnName.contains(PlaylistTableModel.COLUMN_NAMES[4])) {
                    comparator = new AlbumComparator();
                } else if (columnName.contains(PlaylistTableModel.COLUMN_NAMES[5])) {
                    comparator = new GenreComparator();
                }
                playlistTableModel.sort(comparator);

                /*
                switch (clickedColumn) {
                case PlaylistTableModel.TRACK_COLUMN:
                comparator = new TrackComparator();
                break;
                case PlaylistTableModel.TITLE_COLUMN:
                comparator = new TitleComparator();
                break;
                case PlaylistTableModel.TIME_COLUMN:
                comparator = new DurationComparator();
                break;
                case PlaylistTableModel.ARTIST_COLUMN:
                comparator = new ArtistComparator();
                break;
                case PlaylistTableModel.ALBUM_COLUMN:
                comparator = new AlbumComparator();
                break;
                case PlaylistTableModel.GENRE_COLUMN:
                comparator = new GenreComparator();
                break;
                }

                resetTableHeaderValues();
                clearSelection();

                StringBuilder sb = new StringBuilder(columnName);
                if (headerValue.equals(columnName) || headerValue.contains(upArrowChar)) {
                playlistTableModel.sort(comparator);
                sb.append(" ").append(downArrowChar);
                tableColumn.setHeaderValue(sb.toString());
                } else {
                playlistTableModel.sort(Collections.reverseOrder(comparator));
                sb.append(" ").append(upArrowChar);
                tableColumn.setHeaderValue(sb.toString());
                }
                 */

                colorizeRow();
            }
        });
        playlistTable.setFillsViewportHeight(true);
        playlistTable.setShowGrid(false);
        playlistTable.setRowSelectionAllowed(true);
        playlistTable.setColumnSelectionAllowed(false);
        playlistTable.setDragEnabled(false);
        playlistTable.setFont(playlistTable.getFont().deriveFont(Font.BOLD));
        playlistTable.setIntercellSpacing(new Dimension(0, 0));
        playlistTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        playlistTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent ev) {
                int selectedRow = playlistTable.rowAtPoint(ev.getPoint());
                if (SwingUtilities.isLeftMouseButton(ev) && ev.getClickCount() == 2) {
                    if (selectedRow != -1) {
                        playlist.setCursorPosition(selectedRow);
                        controlListener.acOpenAndPlay();
                    }
                }
            }
        });
        playlistTable.getSelectionModel().addListSelectionListener(this);
        playlistTable.getColumnModel().getSelectionModel().addListSelectionListener(this);
        playlistTable.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                // View Media Info
                if (e.getKeyCode() == KeyEvent.VK_I && e.getModifiers() == KeyEvent.CTRL_MASK) {
                    viewMediaInfo();
                } // Select all
                else if (e.getKeyCode() == KeyEvent.VK_A && e.getModifiers() == KeyEvent.CTRL_MASK) {
                    playlistTable.selectAll();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    // Move selected track(s) up
                    if (e.getModifiers() == KeyEvent.ALT_MASK) {
                        moveUp();
                    } // Select previous track
                    else {
                        if (playlistTable.getSelectedRow() > 0) {
                            int previousRowIndex = playlistTable.getSelectedRow() - 1;
                            playlistTable.clearSelection();
                            playlistTable.addRowSelectionInterval(previousRowIndex, previousRowIndex);
                            makeRowVisible(previousRowIndex);
                        }
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    // Move selected track(s) down
                    if (e.getModifiers() == KeyEvent.ALT_MASK) {
                        moveDown();
                    } // Select next track
                    else {
                        if (playlistTable.getSelectedRow() < playlistTable.getRowCount() - 1) {
                            int nextRowIndex = playlistTable.getSelectedRow() + 1;
                            playlistTable.clearSelection();
                            playlistTable.addRowSelectionInterval(nextRowIndex, nextRowIndex);
                            makeRowVisible(nextRowIndex);
                        }
                    }
                }// Play selected track
                else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    int selectedRow = playlistTable.getSelectedRow();
                    if (selectedRow != -1) {
                        playlist.setCursorPosition(selectedRow);
                        controlListener.acOpenAndPlay();
                    }
                } // Add new tracks
                else if (e.getKeyCode() == KeyEvent.VK_INSERT) {
                    addFilesDialog();
                } // Delete selected tracks
                else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    remove();
                }
            }
        });
        XtremeMP.getInstance().getMainFrame().setDropTarget(new DropTarget(playlistTable, this));
        JScrollPane ptScrollPane = new JScrollPane(playlistTable);
        ptScrollPane.setActionMap(null);
        this.add(ptScrollPane, BorderLayout.CENTER);
    }

    private void initFiltering() {
        searchFilter = new Predicate<PlaylistItem>() {

            @Override
            public boolean evaluate(PlaylistItem pli) {
                boolean matches = false;
                String formattedName = pli.getFormattedName();
                if (formattedName != null) {
                    matches = formattedName.toLowerCase().contains(
                            PlaylistManager.this.searchString.toLowerCase());
                }
                return matches;
            }
        };
    }

    /**
     * Reset header values (column names) to default.
     */
    private void resetTableHeaderValues() {
        for (int i = 0; i < tableColumnModel.getColumnCount(); i++) {
            tableColumnModel.getColumn(i).setHeaderValue(PlaylistTableModel.COLUMN_NAMES[i]);
        }
    }

    protected void addFiles(List<File> newFiles) {
        AddFilesWorker addFilesWorker = new AddFilesWorker(newFiles);
        addFilesWorker.execute();
    }

    public void add(PlaylistItem newPli) {
        playlistTableModel.add(newPli);
    }

    public void add(List<PlaylistItem> newItems) {
        playlistTableModel.add(newItems);
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public void randomizePlaylist() {
        if (!playlist.isEmpty()) {
            playlistTableModel.randomize();
            colorizeRow();
        }
    }

    public void loadPlaylist(String location) {
        PlaylistLoaderWorker playlistLoader = new PlaylistLoaderWorker(location);
        playlistLoader.execute();
    }

    public void refreshRow(int index) {
        playlistTableModel.fireTableRowsUpdated(index, index);
    }

    public void openPlaylist() {
        JFileChooser fileChooser = new JFileChooser(Settings.getLastDir());
        fileChooser.addChoosableFileFilter(playlistFileFilter);
        fileChooser.setMultiSelectionEnabled(false);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            Settings.setLastDir(file.getPath());
            clearPlaylist();
            loadPlaylist(file.getPath());
        }
    }

    public boolean savePlaylistDialog() {
        JFileChooser fileChooser = new JFileChooser(Settings.getLastDir());
        M3uPlaylistFileFilter m3uFileFilter = new M3uPlaylistFileFilter();
        XspfPlaylistFileFilter xspfFileFilter = new XspfPlaylistFileFilter();
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(m3uFileFilter);
        fileChooser.addChoosableFileFilter(xspfFileFilter);
        fileChooser.setMultiSelectionEnabled(false);
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            FileFilter fileFilter = fileChooser.getFileFilter();
            String fileName = file.getName().toLowerCase();
            if (fileFilter == m3uFileFilter) {
                if (!fileName.endsWith(".m3u")) {
                    fileName = fileName.concat(".m3u");
                }
                try {
                    return PlaylistIO.saveM3U(playlist, file.getParent() + File.separator + fileName);
                } catch (PlaylistException ex) {
                    logger.error("Can't save playlist in M3U format", ex);
                }
            }
            if (fileFilter == xspfFileFilter) {
                if (!fileName.endsWith(".xspf")) {
                    fileName = fileName.concat(".xspf");
                }
                try {
                    return PlaylistIO.saveXSPF(playlist, file.getParent() + File.separator + fileName);
                } catch (PlaylistException ex) {
                    logger.error("Can't save playlist in XSPF format", ex);
                }
            }
            Settings.setLastDir(file.getParent());
        }
        return false;
    }

    public void addFilesDialog() {
        JFileChooser fileChooser = new JFileChooser(Settings.getLastDir());
        fileChooser.setDialogTitle("Add Files or Directories");
        fileChooser.addChoosableFileFilter(audioFileFilter);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Settings.setLastDir(fileChooser.getSelectedFiles()[0].getParent());
            addFiles(Arrays.asList(fileChooser.getSelectedFiles()));
        }
    }

    public void moveUp() {
        if (playlistTable.getSelectedRowCount() > 0) {
            int[] selectedRows = playlistTable.getSelectedRows();
            int minSelectedIndex = selectedRows[0];
            if (minSelectedIndex > 0) {
                playlistTable.clearSelection();
                for (int i = 0, len = selectedRows.length; i < len; i++) {
                    int selectedRow = selectedRows[i];
                    int prevRow = selectedRow - 1;
                    playlistTableModel.moveItem(selectedRow, prevRow);
                    playlistTable.addRowSelectionInterval(prevRow, prevRow);
                }
                makeRowVisible(minSelectedIndex - 1);
            }
            colorizeRow();
        }
    }

    public void moveDown() {
        if (playlistTable.getSelectedRowCount() > 0) {
            int[] selectedRows = playlistTable.getSelectedRows();
            int maxLength = selectedRows.length - 1;
            int maxSelectedIndex = selectedRows[maxLength];
            if (maxSelectedIndex < playlist.size() - 1) {
                playlistTable.clearSelection();
                for (int i = maxLength; i >= 0; i--) {
                    int selectedRow = selectedRows[i];
                    int nextRow = selectedRow + 1;
                    playlistTableModel.moveItem(selectedRow, nextRow);
                    playlistTable.addRowSelectionInterval(nextRow, nextRow);
                }
                makeRowVisible(maxSelectedIndex + 1);
            }
            colorizeRow();
        }
    }

    public void remove() {
        int selectedRowCount = playlistTable.getSelectedRowCount();
        if (selectedRowCount == playlist.size()) {
            clearPlaylist();
        } else if (selectedRowCount > 0) {
            List<PlaylistItem> items = new ArrayList<PlaylistItem>();
            int[] selectedRows = playlistTable.getSelectedRows();
            for (int i = 0, len = selectedRows.length; i < len; i++) {
                items.add(playlist.getItemAt(selectedRows[i]));
            }
            playlistTableModel.removeAll(items);
            clearSelection();
            colorizeRow();
        }
    }

    protected void clearSelection() {
        playlistTable.clearSelection();
        remFromPlaylistButton.setEnabled(false);
        mediaInfoButton.setEnabled(false);
        moveUpButton.setEnabled(false);
        moveDownButton.setEnabled(false);
    }

    public void clearPlaylist() {
        if (!playlist.isEmpty()) {
            playlistTableModel.clear();
            doubleSelectedRow = -1;
            remFromPlaylistButton.setEnabled(false);
            mediaInfoButton.setEnabled(false);
            moveUpButton.setEnabled(false);
            moveDownButton.setEnabled(false);
            clearPlaylistButton.setEnabled(false);
            resetTableHeaderValues();
            Settings.setPlaylistPosition(-1);
            playlistTable.requestFocusInWindow();
        }
    }

    public void colorizeRow() {
        if (!playlist.isEmpty()) {
            int cursorPos = playlist.getCursorPosition();
            doubleSelectedRow = cursorPos;
            playlistTable.repaint();
            makeRowVisible(cursorPos);
        }
    }

    public void makeRowVisible(int rowIndex) {
        if (!(playlistTable.getParent() instanceof JViewport)) {
            return;
        }
        JViewport viewport = (JViewport) playlistTable.getParent();
        Rectangle contentRect = (Rectangle) playlistTable.getCellRect(rowIndex, playlistTable.getSelectedColumn(), true).clone();
        Point pt = viewport.getViewPosition();
        contentRect.setLocation(contentRect.x - pt.x, contentRect.y - pt.y);
        viewport.scrollRectToVisible(contentRect);
    }

    private void viewMediaInfo() {
        int selectedRow = playlistTable.getSelectedRow();
        if (selectedRow != -1) {
            PlaylistItem pli = playlist.getItemAt(selectedRow);
            MediaInfoWorker mediaInfoWorker = new MediaInfoWorker(pli);
            mediaInfoWorker.execute();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source.equals(openPlaylistButton)) {
            openPlaylist();
        } else if (source.equals(savePlaylistButton)) {
            savePlaylistDialog();
        } else if (source.equals(addToPlaylistButton)) {
            addFilesDialog();
        } else if (source.equals(remFromPlaylistButton)) {
            remove();
        } else if (source.equals(clearPlaylistButton)) {
            clearPlaylist();
        } else if (source.equals(moveUpButton)) {
            moveUp();
        } else if (source.equals(moveDownButton)) {
            moveDown();
        } else if (source.equals(mediaInfoButton)) {
            viewMediaInfo();
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource() == playlistTable.getSelectionModel()) {
            if (playlistTable.getSelectedRowCount() > 0) {
                remFromPlaylistButton.setEnabled(true);
                mediaInfoButton.setEnabled(true);
            }
            ListSelectionModel lsm = playlistTable.getSelectionModel();
            if (lsm.getMinSelectionIndex() == 0) {
                moveUpButton.setEnabled(false);
            } else {
                moveUpButton.setEnabled(true);
            }
            if (lsm.getMaxSelectionIndex() == (playlistTable.getRowCount() - 1)) {
                moveDownButton.setEnabled(false);
            } else {
                moveDownButton.setEnabled(true);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void drop(DropTargetDropEvent ev) {
        DropTargetContext targetContext = ev.getDropTargetContext();
        Transferable t = ev.getTransferable();
        try {
            // Windows
            if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                ev.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                addFiles((List<File>) t.getTransferData(DataFlavor.javaFileListFlavor));
                targetContext.dropComplete(true);
                // Linux
            } else if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                ev.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                String urls = (String) t.getTransferData(DataFlavor.stringFlavor);
                List<File> fileList = new ArrayList<File>();
                StringTokenizer st = new StringTokenizer(urls);
                while (st.hasMoreTokens()) {
                    URI uri = new URI(st.nextToken());
                    fileList.add(new File(uri));
                }
                addFiles(fileList);
                targetContext.dropComplete(true);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent ev) {
    }

    @Override
    public void dragOver(DropTargetDragEvent ev) {
    }

    @Override
    public void dragEnter(DropTargetDragEvent ev) {
    }

    @Override
    public void dragExit(DropTargetEvent ev) {
    }

    protected class PlaylistCellRenderer extends SubstanceDefaultTableCellRenderer {

        private Border emptyBorder = BorderFactory.createEmptyBorder();
        private Color dsColor = Color.red;

        public PlaylistCellRenderer() {
            super();

            int[] columnsWidth = {50, 850, 150, 500, 300, 200};
            for (int i = 0; i < playlistTable.getColumnCount(); i++) {
                DefaultTableColumnModel colModel = (DefaultTableColumnModel) playlistTable.getColumnModel();
                TableColumn col = colModel.getColumn(i);
                col.setPreferredWidth(columnsWidth[i]);
            }

            TableColumn column = playlistTable.getColumn(playlistTable.getColumnName(PlaylistTableModel.TIME_COLUMN));
            JLabel label = new JLabel("XXX0:00:00");
            int labelWidth = label.getPreferredSize().width;
            column.setMinWidth(labelWidth);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
//            if (!SubstanceLookAndFeel.isCurrentLookAndFeel()) {
//                return super.getTableCellRendererComponent(table, value,
//                        isSelected, hasFocus, row, column);
//            }

            super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);

            this.setBorder(emptyBorder);

            if (column == PlaylistTableModel.TIME_COLUMN) {
                this.setHorizontalAlignment(SwingConstants.RIGHT);
            } else {
                this.setHorizontalAlignment(SwingConstants.LEFT);
            }

            if (row == doubleSelectedRow) {
                this.setForeground(dsColor);
            }

            return this;
        }
    }

    protected class SearchFilterListener implements DocumentListener {

        public void changeFilter(DocumentEvent event) {
            Document document = event.getDocument();
            try {
                clearSelection();
                searchString = document.getText(0, document.getLength());
                if (searchString != null && !searchString.isEmpty()) {
                    playlistTableModel.filter(searchFilter);
                    moveUpButton.setEnabled(false);
                    moveDownButton.setEnabled(false);
                } else {
                    playlist.filter(TruePredicate.<PlaylistItem>getInstance());
                }
                colorizeRow();
            } catch (Exception ex) {
            }
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            changeFilter(e);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            changeFilter(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            changeFilter(e);
        }
    }

    protected class PlaylistLoaderWorker extends AbstractSwingWorker<Void, PlaylistItem> {

        private final String location;

        public PlaylistLoaderWorker(String location) {
            this.location = location;
        }

        @Override
        protected Void doInBackground() throws Exception {
            List<PlaylistItem> pliList = PlaylistIO.load(location);
            int count = 0;
            int size = pliList.size();
            for (PlaylistItem pli : pliList) {
                if (pli.isFile()) {
                    pli.getTagInfo();
                }
                publish(pli);
                count++;
                setProgress(100 * count / size);
            }
            return null;
        }

        @Override
        protected void process(List<PlaylistItem> moreItems) {
            playlistTableModel.add(moreItems);
        }

        @Override
        protected void done() {
            setProgress(100);
            if (!playlist.isEmpty()) {
                clearPlaylistButton.setEnabled(true);
                AudioPlayer audioPlayer = XtremeMP.getInstance().getAudioPlayer();
                if (audioPlayer.getState() == AudioSystem.NOT_SPECIFIED || audioPlayer.getState() == AudioPlayer.STOP) {
                    int index = Settings.getPlaylistPosition();
                    if (firstPlaylistLoad && index >= 0 && index <= (playlist.size() - 1)) {
                        playlist.setCursorPosition(index);
                    } else {
                        playlist.begin();
                    }
                    if (firstPlaylistLoad) {
                        firstPlaylistLoad = false;
                        controlListener.acOpen();
                    } else {
                        controlListener.acOpenAndPlay();
                    }
                }
            }
        }
    }

    protected class AddFilesWorker extends AbstractSwingWorker<Void, PlaylistItem> {

        private final List<File> fileList;

        public AddFilesWorker(List<File> fileList) {
            this.fileList = fileList;
        }

        @Override
        protected Void doInBackground() {
            List<File> tempFileList = new ArrayList<File>();
            for (File file : fileList) {
                if (file.isDirectory()) {
                    scanDir(file, tempFileList);
                } else if (audioFileFilter.accept(file)) {
                    tempFileList.add(file);
                }
            }
            int count = 0;
            int size = tempFileList.size();
            for (File file : tempFileList) {
                String baseName = FilenameUtils.getBaseName(file.getName());
                PlaylistItem pli = new PlaylistItem(baseName, file.getAbsolutePath(), -1, true);
                pli.getTagInfo();
                publish(pli);
                count++;
                setProgress(100 * count / size);
            }
            return null;
        }

        @Override
        protected void process(List<PlaylistItem> moreItems) {
            playlistTableModel.add(moreItems);
        }

        @Override
        protected void done() {
            setProgress(100);
            if (!playlist.isEmpty()) {
                clearPlaylistButton.setEnabled(true);
            }
        }

        protected void scanDir(File dir, List<File> fileList) {
            for (File file : dir.listFiles((FilenameFilter) audioFileFilter)) {
                if (file.isFile()) {
                    fileList.add(file);
                } else {
                    scanDir(file, fileList);
                }
            }
        }
    }

    protected class MediaInfoWorker extends AbstractSwingWorker<Void, PlaylistItem> {

        private final PlaylistItem pli;

        public MediaInfoWorker(PlaylistItem pli) {
            this.pli = pli;
        }

        @Override
        protected Void doInBackground() throws Exception {
            if (pli != null) {
                pli.getTagInfo();
            }
            return null;
        }

        @Override
        protected void process(List<PlaylistItem> moreItems) {
            playlistTableModel.add(moreItems);
        }

        @Override
        protected void done() {
            setProgress(100);
            if (pli != null) {
                MediaInfoDialog mediaInfoDialog = new MediaInfoDialog(pli);
                mediaInfoDialog.setVisible(true);
            }
        }
    }
}
