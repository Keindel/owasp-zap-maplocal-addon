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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.table.AbstractTableModel;
import org.parosproxy.paros.Constant;

@SuppressWarnings("serial")
public class MapLocalTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final int COLUMN_COUNT = 3;

    private static final String[] columnNames = {
        Constant.messages.getString("mapLocal.table.header.enabled"),
        Constant.messages.getString("mapLocal.table.header.url"),
        Constant.messages.getString("mapLocal.table.header.localpath")
    };

    private List<MapLocalTableEntry> mapLocals;
    private List<MapLocalTableEntry> mapLocalsEnabled;

    private Map<MapLocalTableEntry, Integer> mapLocalToRowMapping;

    private int lastAffectedRow;

    public MapLocalTableModel() {
        super();

        mapLocals = new ArrayList<>(0);
        mapLocalsEnabled = new ArrayList<>(0);

        mapLocalToRowMapping = new HashMap<>();

        lastAffectedRow = -1;
    }

    public List<MapLocalTableEntry> getMapLocalsList() {
        return mapLocals;
    }

    public List<MapLocalTableEntry> getMapLocalsEnabledList() {
        return mapLocalsEnabled;
    }

    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    @Override
    public int getRowCount() {
        return mapLocals.size();
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public Object getValueAt(int row, int column) {
        Object obj = null;
        MapLocalTableEntry mapLocal = mapLocals.get(row);
        if (column == 0) {
            obj = mapLocal.isEnabled();
        } else if (column == 1) {
            obj = mapLocal.getDisplayMessage();
        } else {
            obj = mapLocal.getLocalPath().toString();
        }
        return obj;
    }

    public MapLocalTableEntry getMapLocalAtRow(int row) {
        return mapLocals.get(row);
    }

    public void addMapLocal(MapLocalTableEntry mapLocal) {
        mapLocals.add(mapLocal);
        this.fireTableRowsInserted(mapLocals.size() - 1, mapLocals.size() - 1);

        rebuildMapLocalToRowMapping();
        lastAffectedRow = mapLocalToRowMapping.get(mapLocal);

        if (mapLocal.isEnabled()) {
            synchronized (mapLocalsEnabled) {
                mapLocalsEnabled.add(mapLocal);
            }
        }
    }

    public void editMapLocal(MapLocalTableEntry oldMapLocal, MapLocalTableEntry newMapLocal) {
        int row = mapLocalToRowMapping.remove(oldMapLocal);
        mapLocals.remove(row);
        this.fireTableRowsDeleted(row, row);

        mapLocalToRowMapping.put(newMapLocal, 0);
        mapLocals.add(newMapLocal);
        this.fireTableRowsInserted(mapLocals.size() - 1, mapLocals.size() - 1);

        rebuildMapLocalToRowMapping();
        lastAffectedRow = mapLocalToRowMapping.get(newMapLocal);

        synchronized (mapLocalsEnabled) {
            if (oldMapLocal.isEnabled()) {
                mapLocalsEnabled.remove(oldMapLocal);
            }
            if (newMapLocal.isEnabled()) {
                mapLocalsEnabled.add(newMapLocal);
            }
        }
    }

    public void removeMapLocal(MapLocalTableEntry mapLocal) {
        Integer row = mapLocalToRowMapping.remove(mapLocal);

        if (row != null) {
            mapLocals.remove(mapLocal);
            this.fireTableRowsDeleted(row, row);

            rebuildMapLocalToRowMapping();

            synchronized (mapLocalsEnabled) {
                if (mapLocal.isEnabled()) {
                    mapLocalsEnabled.remove(mapLocal);
                }
            }
        }
    }

    public int getLastAffectedRow() {
        return lastAffectedRow;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return (column == 0);
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        if (column == 0) {
            if (value instanceof Boolean) {
                boolean isEnabled = mapLocals.get(row).isEnabled();
                mapLocals.get(row).setEnabled((Boolean) value);
                this.fireTableCellUpdated(row, column);

                if (isEnabled) {
                    synchronized (mapLocalsEnabled) {
                        mapLocalsEnabled.remove(mapLocals.get(row));
                    }
                } else {
                    synchronized (mapLocalsEnabled) {
                        mapLocalsEnabled.add(mapLocals.get(row));
                    }
                }
            }
        }
    }

    @Override
    public Class<?> getColumnClass(int column) {
        if (column == 0) {
            return Boolean.class;
        }
        return String.class;
    }

    private void rebuildMapLocalToRowMapping() {
        mapLocalToRowMapping.clear();
        int i = 0;
        for (Iterator<MapLocalTableEntry> iterator = mapLocals.iterator();
                iterator.hasNext();
                ++i) {
            mapLocalToRowMapping.put(iterator.next(), i);
        }
    }

    public void clear() {
        mapLocals.clear();
        mapLocalsEnabled.clear();
        mapLocalToRowMapping.clear();

        if (lastAffectedRow >= 0) {
            this.fireTableRowsDeleted(0, lastAffectedRow);
        }
        lastAffectedRow = -1;
    }
}
