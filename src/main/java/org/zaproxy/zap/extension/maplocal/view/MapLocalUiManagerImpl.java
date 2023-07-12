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

import java.awt.Dimension;
import java.nio.file.Path;
import org.parosproxy.paros.db.DatabaseException;
import org.parosproxy.paros.extension.ExtensionHookMenu;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.extension.httppanel.Message;
import org.zaproxy.zap.extension.maplocal.ExtensionMapLocal;
import org.zaproxy.zap.extension.maplocal.view.popup.PopupMenuAddMapLocalHistory;
import org.zaproxy.zap.extension.maplocal.view.popup.PopupMenuAddMapLocalSites;
import org.zaproxy.zap.model.StructuralSiteNode;

public class MapLocalUiManagerImpl implements MapLocalUiManagerInterface {

    private MapLocalAddEditDialog mapLocalDialog = null;

    private ExtensionMapLocal extensionMapLocal;
    private PopupMenuAddMapLocalSites popupMenuAddMapLocalSites = null;
    private PopupMenuAddMapLocalHistory popupMenuAddMapLocalHistory = null;

    public MapLocalUiManagerImpl(ExtensionHookMenu hookMenu, ExtensionMapLocal extensionMapLocal) {
        this.extensionMapLocal = extensionMapLocal;
        hookMenu.addPopupMenuItem(getPopupMenuAddMapLocalSites());
        hookMenu.addPopupMenuItem(getPopupMenuAddMapLocalHistory());
    }

    @Override
    public Class<HttpMessage> getMessageClass() {
        return HttpMessage.class;
    }

    @Override
    public Class<MapLocalTableEntry> getMapLocalClass() {
        return MapLocalTableEntry.class;
    }

    @Override
    public String getType() {
        return "HTTP";
    }

    @Override
    public void handleAddMapLocal(Message aMessage) {
        showAddDialog(aMessage);
    }

    public void handleAddMapLocal(String url) {
        showAddDialog(url, MapLocalTableEntry.Match.regex);
    }

    void addMapLocal(MapLocalTableEntry mapLocal) {
        extensionMapLocal.addMapLocal(mapLocal);
    }

    @Override
    public void handleEditMapLocal(MapLocalTableEntry mapLocal) {
        showEditDialog(mapLocal);
    }

    void editMapLocal(MapLocalTableEntry oldMapLocal, MapLocalTableEntry newMapLocal) {
        extensionMapLocal.editMapLocal(oldMapLocal, newMapLocal);
    }

    @Override
    public void handleRemoveMapLocal(MapLocalTableEntry mapLocal) {
        extensionMapLocal.removeMapLocal(mapLocal);
    }

    @Override
    public void reset() {}

    private void populateAddDialogAndSetVisible(String url, MapLocalTableEntry.Match match) {
        mapLocalDialog.init(new MapLocalTableEntry(url, match, false, Path.of("")), true);
        mapLocalDialog.setVisible(true);
    }

    private void showAddDialog(Message aMessage) {
        MapLocalTableEntry.Match match = MapLocalTableEntry.Match.regex;
        HttpMessage msg = (HttpMessage) aMessage;
        String regex = "";

        if (msg.getHistoryRef() != null && msg.getHistoryRef().getSiteNode() != null) {
            try {
                regex =
                        new StructuralSiteNode(msg.getHistoryRef().getSiteNode())
                                .getRegexPattern(false);
            } catch (DatabaseException e) {
                // Ignore
            }
        }
        if (regex.length() == 0 && msg.getRequestHeader().getURI() != null) {
            // Just use the escaped url
            regex = msg.getRequestHeader().getURI().toString();
            match = MapLocalTableEntry.Match.contains;
        }
        this.showAddDialog(regex, match);
    }

    private void showAddDialog(String url, MapLocalTableEntry.Match match) {
        if (mapLocalDialog == null) {
            mapLocalDialog =
                    new MapLocalAddEditDialog(
                            this, View.getSingleton().getMainFrame(), new Dimension(407, 350));
        }
        populateAddDialogAndSetVisible(url, match);
    }

    private void populateEditDialogAndSetVisible(MapLocalTableEntry mapLocal) {
        mapLocalDialog.init(mapLocal, false);
        mapLocalDialog.setVisible(true);
    }

    private void showEditDialog(MapLocalTableEntry mapLocal) {
        if (mapLocalDialog == null) {
            mapLocalDialog =
                    new MapLocalAddEditDialog(
                            this, View.getSingleton().getMainFrame(), new Dimension(407, 350));
        }
        populateEditDialogAndSetVisible(mapLocal);
    }

    private PopupMenuAddMapLocalSites getPopupMenuAddMapLocalSites() {
        if (popupMenuAddMapLocalSites == null) {
            popupMenuAddMapLocalSites = new PopupMenuAddMapLocalSites(this);
        }
        return popupMenuAddMapLocalSites;
    }

    private PopupMenuAddMapLocalHistory getPopupMenuAddMapLocalHistory() {
        if (popupMenuAddMapLocalHistory == null) {
            popupMenuAddMapLocalHistory = new PopupMenuAddMapLocalHistory(extensionMapLocal);
        }
        return popupMenuAddMapLocalHistory;
    }
}
