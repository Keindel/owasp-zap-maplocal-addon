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
import java.awt.Frame;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import org.parosproxy.paros.Constant;
import org.zaproxy.zap.extension.maplocal.view.MapLocalTableEntry.Match;
import org.zaproxy.zap.view.LayoutHelper;
import org.zaproxy.zap.view.StandardFieldsDialog;

@SuppressWarnings("serial")
public class MapLocalAddEditDialog extends StandardFieldsDialog {
    private static final String FIELD_MATCH = "mapLocal.match.label";
    private static final String FIELD_PROTOCOL = "mapLocal.protocol.label";
    private static final String FIELD_HOST = "mapLocal.host.label";
    private static final String FIELD_PATH = "mapLocal.path.label";
    private static final String FIELD_QUERY = "mapLocal.query.label";
    private static final String FIELD_IGNORECASE = "mapLocal.ignorecase.label";
    private static final String FIELD_LOCAL_PATH = "mapLocal.localpath.label";

    private JButton browseButton = null;

    private static final long serialVersionUID = 1L;

    private MapLocalUiManagerImpl mapLocalUiManager;
    private boolean add = false;
    private MapLocalTableEntry mapLocal;

    public MapLocalAddEditDialog(
            MapLocalUiManagerImpl mapLocalUiManager, Frame owner, Dimension dim) {
        super(owner, "mapLocal.add.title", dim, true);
        this.mapLocalUiManager = mapLocalUiManager;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public void init(MapLocalTableEntry mapLocal, boolean add) {
        this.add = add;
        this.mapLocal = mapLocal;

        this.removeAllFields();

        if (add) {
            this.setTitle(Constant.messages.getString("mapLocal.add.title"));
        } else {
            this.setTitle(Constant.messages.getString("mapLocal.edit.title"));
        }

        this.addComboField(FIELD_MATCH, getMatches(), this.matchToStr(mapLocal.getMatch()));
        this.addTextField(FIELD_PROTOCOL, mapLocal.getProtocol());
        this.addTextField(FIELD_HOST, mapLocal.getHost());
        this.addTextField(FIELD_PATH, mapLocal.getUrlPath());
        this.addTextField(FIELD_QUERY, mapLocal.getQuery());
        this.addCheckBoxField(FIELD_IGNORECASE, mapLocal.isIgnoreCase());
        this.addTextField(FIELD_LOCAL_PATH, mapLocal.getLocalPath().toString());

        this.addPadding();
        this.addBrowseButtonToPane();
    }

    private static List<String> getMatches() {
        ArrayList<String> list = new ArrayList<>();
        for (Match match : MapLocalTableEntry.Match.values()) {
            list.add(matchToStr(match));
        }
        return list;
    }

    private static String matchToStr(Match match) {
        return Constant.messages.getString("mapLocal.match." + match.name());
    }

    private static Match strToMatch(String str) {
        for (Match match : MapLocalTableEntry.Match.values()) {
            if (matchToStr(match).equals(str)) {
                return match;
            }
        }
        return null;
    }

    private static Path strToPath(String str) {
        return Paths.get(str);
    }

    private String getPathStrFromField() {
        return this.getStringValue(FIELD_LOCAL_PATH);
    }

    @Override
    public void save() {
        MapLocalTableEntry mapLocalFromFields =
                new MapLocalTableEntry(
                        getUrlStrFromFields(),
                        this.strToMatch(this.getStringValue(FIELD_MATCH)),
                        this.getBoolValue(FIELD_IGNORECASE),
                        this.strToPath(this.getStringValue(FIELD_LOCAL_PATH)));

        if (add) {
            mapLocalUiManager.addMapLocal(mapLocalFromFields);
            dispose();
        } else {
            mapLocalUiManager.editMapLocal(this.mapLocal, mapLocalFromFields);
            this.mapLocal = null;
            dispose();
        }
    }

    private String getUrlStrFromFields() {
        StringBuilder sbFromFields =
                new StringBuilder(this.getStringValue(FIELD_PROTOCOL))
                        .append("://")
                        .append(this.getStringValue(FIELD_HOST))
                        .append(this.getStringValue(FIELD_PATH));
        String query = this.getStringValue(FIELD_QUERY);
        if (!query.isBlank()) {
            sbFromFields.append("?").append(query);
        }
        return sbFromFields.toString();
    }

    @Override
    public String validateFields() {
        if (this.isEmptyField(FIELD_PROTOCOL)) {
            return Constant.messages.getString("mapLocal.error.noprotocol");
        }
        if (this.isEmptyField(FIELD_HOST)) {
            return Constant.messages.getString("mapLocal.error.nohost");
        }

        String urlString = getUrlStrFromFields();
        if (MapLocalTableEntry.Match.regex.equals(
                this.strToMatch(this.getStringValue(FIELD_MATCH)))) {
            try {
                Pattern.compile(urlString);
            } catch (Exception e) {
                return Constant.messages.getString("mapLocal.error.regex");
            }
        }
        if (urlString.contains("#")
                && MapLocalTableEntry.Match.contains.equals(
                        this.strToMatch(this.getStringValue(FIELD_MATCH)))) {
            return Constant.messages.getString("mapLocal.warn.urlfragment");
        }

        return validateLocalPath();
    }

    private String validateLocalPath() {
        if (this.isEmptyField(FIELD_LOCAL_PATH)) {
            return Constant.messages.getString("mapLocal.error.nolocalpath");
        } else {
            try {
                Path path = Paths.get(getPathStrFromField());
                if (!Files.isReadable(path)) {
                    return Constant.messages.getString("mapLocal.error.notreadable");
                }
            } catch (InvalidPathException ex) {
                return Constant.messages.getString("mapLocal.error.invalidlocalpath");
            } catch (SecurityException ex) {
                return Constant.messages.getString("mapLocal.error.readrestriction");
            }
        }
        return null;
    }

    @Override
    public void cancelPressed() {
        dispose();
    }

    public void browsePressed() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("choose file");
        int result = fileChooser.showOpenDialog(MapLocalAddEditDialog.this);
        if (result == JFileChooser.APPROVE_OPTION) {
            this.setFieldValue(FIELD_LOCAL_PATH, fileChooser.getSelectedFile().getPath());
        }
    }

    @Override
    public String getHelpIndex() {
        return null;
    }

    public String getBrowseButtonText() {
        return Constant.messages.getString("mapLocal.button.browse");
    }

    private JButton getBrowseButton() {
        if (browseButton == null) {
            browseButton = new JButton();
            browseButton.setText(this.getBrowseButtonText());

            browseButton.addActionListener(
                    new java.awt.event.ActionListener() {

                        @Override
                        public void actionPerformed(java.awt.event.ActionEvent e) {
                            browsePressed();
                        }
                    });
        }
        return browseButton;
    }

    private void addBrowseButtonToPane() {
        getContentPane().add(getBrowseButton(), LayoutHelper.getGBC(0, 1, 1, 0.0D));
    }
}
