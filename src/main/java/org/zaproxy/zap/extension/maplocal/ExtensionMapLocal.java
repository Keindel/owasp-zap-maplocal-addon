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
package org.zaproxy.zap.extension.maplocal;

import java.awt.CardLayout;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.db.Database;
import org.parosproxy.paros.db.DatabaseException;
import org.parosproxy.paros.db.DatabaseUnsupportedException;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionHook;
import org.parosproxy.paros.extension.SessionChangedListener;
import org.parosproxy.paros.model.Session;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.extension.httppanel.Message;
import org.zaproxy.zap.extension.maplocal.db.RecordMapLocal;
import org.zaproxy.zap.extension.maplocal.db.TableMapLocal;
import org.zaproxy.zap.extension.maplocal.view.MapLocalStatusPanel;
import org.zaproxy.zap.extension.maplocal.view.MapLocalTableEntry;
import org.zaproxy.zap.extension.maplocal.view.MapLocalTableModel;
import org.zaproxy.zap.extension.maplocal.view.MapLocalUiManagerImpl;
import org.zaproxy.zap.extension.maplocal.view.MapLocalUiManagerInterface;
import org.zaproxy.zap.extension.maplocal.view.popup.PopupMenuEditMapLocal;
import org.zaproxy.zap.extension.maplocal.view.popup.PopupMenuRemoveMapLocal;
import org.zaproxy.zap.view.ZapMenuItem;

/**
 * An extension that adds Map Local feature. It allows user to map response bodies for chosen urls
 * to local files.
 */
public class ExtensionMapLocal extends ExtensionAdaptor implements SessionChangedListener {

    // The name is public so that other extensions can access it
    public static final String NAME = "ExtensionMapLocal";

    // The i18n prefix, by default the package name - defined in one place to make it easier
    // to copy and change this example
    protected static final String PREFIX = "mapLocal";

    /**
     * Relative path (from add-on package) to load add-on resources.
     *
     * @see Class#getResource(String)
     */
    private static final String RESOURCES = "resources";

    private static final String EXAMPLE_FILE = "example/ExampleFile.txt";

    private ZapMenuItem menuExample;
    private MapLocalStatusPanel mapLocalPanel;

    private MapLocalAPI api;

    private static final Logger LOGGER = LogManager.getLogger(ExtensionMapLocal.class);

    private MapLocalUiManagerInterface mapLocalUiManager;

    private PopupMenuEditMapLocal popupMenuEditMapLocal;
    private PopupMenuRemoveMapLocal popupMenuRemoveMapLocal;

    private Control.Mode mode = Control.getSingleton().getMode();
    private MapLocalMessageHandler mapLocalMessageHandler;
    private ProxyListenerMapLocal proxyListenerMapLocal;

    private final TableMapLocal dbTableMapLocal = new TableMapLocal();

    public ExtensionMapLocal() {
        super(NAME);
        setI18nPrefix(PREFIX);
    }

    @Override
    public void hook(ExtensionHook extensionHook) {
        super.hook(extensionHook);

        this.api = new MapLocalAPI();
        extensionHook.addApiImplementor(this.api);

        mapLocalMessageHandler = new MapLocalMessageHandler();
        extensionHook.addProxyListener(getProxyListenerMapLocal());
        extensionHook.addSessionListener(this);
        // As long as we're not running as a daemon
        if (hasView()) {
            extensionHook.getHookMenu().addToolsMenuItem(getMenuExample());
            //            can be refactored as universal popup, if required
            //            extensionHook.getHookMenu().addPopupMenuItem(getPopupMsgMenuExample());
            extensionHook.getHookMenu().addPopupMenuItem(getPopupMenuEditMapLocal());
            extensionHook.getHookMenu().addPopupMenuItem(getPopupMenuDeleteMapLocal());
            extensionHook.getHookView().addStatusPanel(getMapLocalStatusPanel());

            mapLocalUiManager = new MapLocalUiManagerImpl(extensionHook.getHookMenu(), this);
            setMapLocalUiManager(mapLocalUiManager);

            mapLocalMessageHandler.setEnabledMapLocals(
                    getMapLocalTableModel().getMapLocalsEnabledList());
        } else {
            mapLocalMessageHandler.setEnabledMapLocals(new ArrayList<>());
        }
    }

    private MapLocalTableModel getMapLocalTableModel() {
        return (MapLocalTableModel) this.getMapLocalStatusPanel().getMapLocals().getModel();
    }

    @Override
    public boolean canUnload() {
        // The extension can be dynamically unloaded, all resources used/added can be freed/removed
        // from core.
        return true;
    }

    @Override
    public void unload() {
        super.unload();

        // In this example it's not necessary to override the method, as there's nothing to unload
        // manually, the components added through the class ExtensionHook (in hook(ExtensionHook))
        // are automatically removed by the base unload() method.
        // If you use/add other components through other methods you might need to free/remove them
        // here (if the extension declares that can be unloaded, see above method).
    }

    private MapLocalStatusPanel getMapLocalStatusPanel() {
        if (mapLocalPanel == null) {
            mapLocalPanel = new MapLocalStatusPanel(this);
            mapLocalPanel.setLayout(new CardLayout());
            mapLocalPanel.setName(Constant.messages.getString(PREFIX + ".panel.title"));
            mapLocalPanel.setIcon(
                    new ImageIcon(getClass().getResource(RESOURCES + "/maplocal.png")));
        }
        return mapLocalPanel;
    }

    private ZapMenuItem getMenuExample() {
        if (menuExample == null) {
            menuExample = new ZapMenuItem(PREFIX + ".topmenu.tools.title");

            menuExample.addActionListener(
                    e -> {
                        // This is where you do what you want to do.
                        // In this case we'll just show a popup message.
                        View.getSingleton()
                                .showMessageDialog(
                                        Constant.messages.getString(PREFIX + ".topmenu.tools.msg"));
                        // And display a file included with the add-on in the Output tab
                        displayFile(EXAMPLE_FILE);
                    });
        }
        return menuExample;
    }

    private static void displayFile(String file) {
        if (!View.isInitialised()) {
            // Running in daemon mode, shouldn't have been called
            return;
        }
        try {
            File f = new File(Constant.getZapHome(), file);
            if (!f.exists()) {
                // This is something the user should know, so show a warning dialog
                View.getSingleton()
                        .showWarningDialog(
                                Constant.messages.getString(
                                        ExtensionMapLocal.PREFIX + ".error.nofile",
                                        f.getAbsolutePath()));
                return;
            }
            // Quick way to read a small text file
            String contents = new String(Files.readAllBytes(f.toPath()));
            // Write to the output panel
            View.getSingleton().getOutputPanel().append(contents);
            // Give focus to the Output tab
            View.getSingleton().getOutputPanel().setTabFocus();
        } catch (Exception e) {
            // Something unexpected went wrong, write the error to the log
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public String getDescription() {
        return Constant.messages.getString(PREFIX + ".desc");
    }

    public void setMapLocalUiManager(MapLocalUiManagerInterface uiManager) {
        if (getView() == null) {
            return;
        }
        this.mapLocalUiManager = uiManager;
    }

    private MapLocalUiManagerInterface getMapLocalUiManager() {
        return mapLocalUiManager;
    }

    public void addUiMapLocal(Message aMessage) {
        MapLocalUiManagerInterface uiManager = getMapLocalUiManager();
        if (uiManager != null) {
            uiManager.handleAddMapLocal(aMessage);
        }
    }

    public void editUiSelectedMapLocal() {
        MapLocalTableEntry mapLocal = getMapLocalStatusPanel().getSelectedMapLocal();
        if (mapLocal != null && mapLocalUiManager != null) {
            mapLocalUiManager.handleEditMapLocal(mapLocal);
        }
    }

    public void removeUiSelectedMapLocal() {
        MapLocalTableEntry mapLocal = getMapLocalStatusPanel().getSelectedMapLocal();
        if (mapLocal != null && mapLocalUiManager != null) {
            mapLocalUiManager.handleRemoveMapLocal(mapLocal);
        }
    }

    public void addMapLocal(MapLocalTableEntry mapLocal) {
        this.getMapLocalStatusPanel().addMapLocal(mapLocal);
        // Switch to the panel for some visual feedback
        this.getMapLocalStatusPanel().setTabFocus();
        writeMapLocalToDB(mapLocal);
    }

    private void writeMapLocalToDB(MapLocalTableEntry mapLocal) {
        RecordMapLocal recordMapLocal = null;
        try {
            recordMapLocal =
                    dbTableMapLocal.write(
                            mapLocal.getString(),
                            mapLocal.getMatch().toString(),
                            mapLocal.isIgnoreCase(),
                            mapLocal.getLocalPath().toString());

            int mapLocId = recordMapLocal.getMapLocalId();
            mapLocal.setMapLocalId(mapLocId);
        } catch (DatabaseException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void editMapLocal(MapLocalTableEntry oldMapLocal, MapLocalTableEntry newMapLocal) {
        this.getMapLocalStatusPanel().editMapLocal(oldMapLocal, newMapLocal);

        updateMapLocalInDB(oldMapLocal, newMapLocal);
    }

    private void updateMapLocalInDB(
            MapLocalTableEntry oldMapLocal, MapLocalTableEntry newMapLocal) {
        try {
            dbTableMapLocal.update(
                    oldMapLocal.getMapLocalId(),
                    newMapLocal.getString(),
                    newMapLocal.getMatch().toString(),
                    newMapLocal.isIgnoreCase(),
                    newMapLocal.getLocalPath().toString());

            newMapLocal.setMapLocalId(oldMapLocal.getMapLocalId());
        } catch (DatabaseException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void removeMapLocal(MapLocalTableEntry mapLocal) {
        this.getMapLocalStatusPanel().removeMapLocal(mapLocal);

        deleteMapLocalInDB(mapLocal);
    }

    private void deleteMapLocalInDB(MapLocalTableEntry mapLocal) {
        try {
            dbTableMapLocal.deleteMapLocal(mapLocal.getMapLocalId());
        } catch (DatabaseException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void sessionAboutToChange(final Session session) {}

    @Override
    public void sessionChanged(Session session) {
        if (getView() == null) {
            return;
        }

        getMapLocalStatusPanel().clearTableModel();
        readAllMapLocalsFromDB();
    }

    @Override
    public void sessionScopeChanged(Session session) {}

    @Override
    public void sessionModeChanged(Control.Mode mode) {
        this.mode = mode;
    }

    private void readAllMapLocalsFromDB() {
        try {
            List<Integer> mapLocIds = dbTableMapLocal.getMapLocalList();
            for (Integer mapLocId : mapLocIds) {
                RecordMapLocal recMapLoc = dbTableMapLocal.read(mapLocId);
                MapLocalTableEntry mapLocal =
                        new MapLocalTableEntry(
                                recMapLoc.getUrlString(),
                                MapLocalTableEntry.Match.valueOf(recMapLoc.getMatch()),
                                recMapLoc.isIgnoreCase(),
                                Path.of(recMapLoc.getLocalPath()));
                mapLocal.setMapLocalId(mapLocId);
                this.getMapLocalStatusPanel().addMapLocal(mapLocal);
            }
        } catch (DatabaseException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private PopupMenuEditMapLocal getPopupMenuEditMapLocal() {
        if (popupMenuEditMapLocal == null) {
            popupMenuEditMapLocal = new PopupMenuEditMapLocal();
            popupMenuEditMapLocal.setExtension(this);
        }
        return popupMenuEditMapLocal;
    }

    private PopupMenuRemoveMapLocal getPopupMenuDeleteMapLocal() {
        if (popupMenuRemoveMapLocal == null) {
            popupMenuRemoveMapLocal = new PopupMenuRemoveMapLocal();
            popupMenuRemoveMapLocal.setExtension(this);
        }
        return popupMenuRemoveMapLocal;
    }

    public boolean messageReceivedFromServer(HttpMessage httpMessage) {
        if (mode.equals(Control.Mode.safe)) {
            return true;
        }
        return mapLocalMessageHandler.handleMessageReceivedFromServer(
                httpMessage, mode.equals(Control.Mode.protect));
    }

    public ProxyListenerMapLocal getProxyListenerMapLocal() {
        if (proxyListenerMapLocal == null) {
            proxyListenerMapLocal = new ProxyListenerMapLocal(getModel(), this);
        }
        return proxyListenerMapLocal;
    }

    @Override
    public void databaseOpen(Database db) throws DatabaseException, DatabaseUnsupportedException {
        db.addDatabaseListener(dbTableMapLocal);
        dbTableMapLocal.databaseOpen(db.getDatabaseServer());
    }
}
