/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2023 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.zap.extension.maplocal.view;

import java.awt.CardLayout;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.extension.AbstractPanel;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.extension.maplocal.ExtensionMapLocal;

@SuppressWarnings("serial")
public class MapLocalStatusPanel extends AbstractPanel {

    private static final long serialVersionUID = 1L;

    public static final String PANEL_NAME = "mapLocal";

    private ExtensionMapLocal extension;
    private javax.swing.JPanel panelCommand = null;
    private javax.swing.JLabel jLabel = null;
    private JScrollPane jScrollPane = null;
    private JXTable mapLocalTable = null;
    private MapLocalTableModel model = new MapLocalTableModel();

    private static final String MAP_LOCAL_TABLE = "mapLocal.table";
    private static final String PREF_COLUMN_WIDTH = "column.width";
    private final Preferences preferences;
    private final String prefnzPrefix = this.getClass().getSimpleName() + ".";

    private static final Logger LOGGER = LogManager.getLogger(MapLocalStatusPanel.class);

    public MapLocalStatusPanel(ExtensionMapLocal extension) {
        super();
        this.extension = extension;
        this.preferences = Preferences.userNodeForPackage(getClass());

        initialize();
    }

    private void initialize() {
        this.setLayout(new CardLayout());
        this.setSize(474, 251);
        this.setName(Constant.messages.getString("mapLocal.panel.title"));
        this.setIcon(
                new ImageIcon(
                        MapLocalStatusPanel.class.getResource(
                                "/org/zaproxy/zap/extension/maplocal/resources/maplocal.png")));
        this.add(getPanelCommand(), getPanelCommand().getName());
    }

    private javax.swing.JPanel getPanelCommand() {
        if (panelCommand == null) {

            panelCommand = new javax.swing.JPanel();
            panelCommand.setLayout(new java.awt.GridBagLayout());
            panelCommand.setName(Constant.messages.getString("mapLocal.panel.title"));

            jLabel = getJLabel();
            GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints2 = new GridBagConstraints();

            gridBagConstraints1.gridx = 0;
            gridBagConstraints1.gridy = 0;
            gridBagConstraints1.insets = new java.awt.Insets(2, 2, 2, 2);
            gridBagConstraints1.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints1.weightx = 1.0D;
            gridBagConstraints2.gridx = 0;
            gridBagConstraints2.gridy = 1;
            gridBagConstraints2.weightx = 1.0;
            gridBagConstraints2.weighty = 1.0;
            gridBagConstraints2.fill = GridBagConstraints.BOTH;
            gridBagConstraints2.insets = new java.awt.Insets(0, 0, 0, 0);
            gridBagConstraints2.anchor = GridBagConstraints.NORTHWEST;

            panelCommand.add(getJScrollPane(), gridBagConstraints2);
        }
        return panelCommand;
    }

    private javax.swing.JLabel getJLabel() {
        if (jLabel == null) {
            jLabel = new javax.swing.JLabel();
            jLabel.setText(" ");
        }
        return jLabel;
    }

    private JScrollPane getJScrollPane() {
        if (jScrollPane == null) {
            jScrollPane = new JScrollPane();
            jScrollPane.setViewportView(getMapLocals());
        }
        return jScrollPane;
    }

    public JXTable getMapLocals() {
        if (mapLocalTable == null) {
            mapLocalTable = new JXTable(model);

            mapLocalTable.setColumnSelectionAllowed(false);
            mapLocalTable.setCellSelectionEnabled(false);
            mapLocalTable.setRowSelectionAllowed(true);
            mapLocalTable.setColumnControlVisible(true);

            mapLocalTable
                    .getColumnModel()
                    .getColumn(0)
                    .setPreferredWidth(restoreColumnWidth(MAP_LOCAL_TABLE, 100));
            mapLocalTable
                    .getColumnModel()
                    .getColumn(0)
                    .addPropertyChangeListener(new ColumnResizedListener(MAP_LOCAL_TABLE));
            mapLocalTable.getColumnModel().getColumn(0).setMaxWidth(250);

            mapLocalTable.getTableHeader().setReorderingAllowed(false);

            mapLocalTable.setName(PANEL_NAME);
            mapLocalTable.setDoubleBuffered(true);
            mapLocalTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
            mapLocalTable.addMouseListener(
                    new java.awt.event.MouseAdapter() {
                        @Override
                        public void mousePressed(java.awt.event.MouseEvent e) {

                            showPopupMenuIfTriggered(e);
                        }

                        @Override
                        public void mouseReleased(java.awt.event.MouseEvent e) {
                            showPopupMenuIfTriggered(e);
                        }

                        private void showPopupMenuIfTriggered(java.awt.event.MouseEvent e) {
                            if (e.isPopupTrigger()) {

                                // Select table item
                                int row = mapLocalTable.rowAtPoint(e.getPoint());
                                if (row < 0
                                        || !mapLocalTable
                                                .getSelectionModel()
                                                .isSelectedIndex(row)) {
                                    mapLocalTable.getSelectionModel().clearSelection();
                                    if (row >= 0) {
                                        mapLocalTable
                                                .getSelectionModel()
                                                .setSelectionInterval(row, row);
                                    }
                                }

                                View.getSingleton()
                                        .getPopupMenu()
                                        .show(e.getComponent(), e.getX(), e.getY());
                            }
                        }

                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() > 1) {
                                extension.editUiSelectedMapLocal();
                            }
                        }
                    });
        }
        return mapLocalTable;
    }

    public MapLocalTableEntry getSelectedMapLocal() {
        int selectedRow = mapLocalTable.getSelectedRow();
        if (selectedRow != -1) {
            return model.getMapLocalAtRow(mapLocalTable.convertRowIndexToModel(selectedRow));
        }
        return null;
    }

    private void selectRowAndEnsureVisible(int row) {
        if (row != -1) {
            mapLocalTable.getSelectionModel().setSelectionInterval(row, row);
            mapLocalTable.scrollRectToVisible(mapLocalTable.getCellRect(row, 0, true));
        }
    }

    private void addMapLocalModel(MapLocalTableEntry mapLocal) {
        model.addMapLocal(mapLocal);
        selectRowAndEnsureVisible(model.getLastAffectedRow());
    }

    public void addMapLocal(final MapLocalTableEntry mapLocal) {
        if (EventQueue.isDispatchThread()) {
            addMapLocalModel(mapLocal);
            return;
        }
        try {
            EventQueue.invokeAndWait(
                    new Runnable() {
                        @Override
                        public void run() {
                            addMapLocalModel(mapLocal);
                        }
                    });
        } catch (Exception e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }

    private void editMapLocalModel(MapLocalTableEntry oldMapLocal, MapLocalTableEntry newMapLocal) {
        model.editMapLocal(oldMapLocal, newMapLocal);
        selectRowAndEnsureVisible(model.getLastAffectedRow());
    }

    public void editMapLocal(
            final MapLocalTableEntry oldMapLocal, final MapLocalTableEntry newMapLocal) {
        if (EventQueue.isDispatchThread()) {
            editMapLocalModel(oldMapLocal, newMapLocal);
            return;
        }
        try {
            EventQueue.invokeAndWait(
                    new Runnable() {
                        @Override
                        public void run() {
                            editMapLocalModel(oldMapLocal, newMapLocal);
                        }
                    });
        } catch (Exception e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }

    private void removeMapLocalModel(MapLocalTableEntry mapLocal) {
        model.removeMapLocal(mapLocal);
    }

    public void removeMapLocal(final MapLocalTableEntry mapLocal) {
        if (EventQueue.isDispatchThread()) {
            removeMapLocalModel(mapLocal);
            return;
        }
        try {
            EventQueue.invokeAndWait(
                    new Runnable() {
                        @Override
                        public void run() {
                            removeMapLocalModel(mapLocal);
                        }
                    });
        } catch (Exception e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }

    private void saveColumnWidth(String prefix, int width) {
        if (width > 0) {
            LOGGER.debug(
                    "Saving preference {}{}.{}={}", prefnzPrefix, prefix, PREF_COLUMN_WIDTH, width);
            this.preferences.put(
                    prefnzPrefix + prefix + "." + PREF_COLUMN_WIDTH, Integer.toString(width));
            // immediate flushing
            try {
                this.preferences.flush();
            } catch (final BackingStoreException e) {
                LOGGER.warn("Error while saving the preferences", e);
            }
        }
    }

    private int restoreColumnWidth(String prefix, int fallback) {
        int result = fallback;
        final String sizestr =
                preferences.get(prefnzPrefix + prefix + "." + PREF_COLUMN_WIDTH, null);
        if (sizestr != null) {
            int width = 0;
            try {
                width = Integer.parseInt(sizestr.trim());
            } catch (final Exception e) {
                // ignoring, cause is prevented by default values;
            }
            if (width > 0) {
                result = width;
                LOGGER.debug(
                        "Restoring preference {}{}.{}={}",
                        prefnzPrefix,
                        prefix,
                        PREF_COLUMN_WIDTH,
                        width);
            }
        }
        return result;
    }

    public void clearTableModel() {
        model.clear();
    }

    private final class ColumnResizedListener implements PropertyChangeListener {

        private final String prefix;

        public ColumnResizedListener(String prefix) {
            super();
            assert prefix != null;
            this.prefix = prefix;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            TableColumn column = (TableColumn) evt.getSource();
            if (column != null) {
                LOGGER.debug(
                        "{}{}.{}={}", prefnzPrefix, prefix, PREF_COLUMN_WIDTH, column.getWidth());
                saveColumnWidth(prefix, column.getWidth());
            }
        }
    }
}
