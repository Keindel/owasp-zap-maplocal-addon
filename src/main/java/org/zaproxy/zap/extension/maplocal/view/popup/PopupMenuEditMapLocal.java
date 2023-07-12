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
package org.zaproxy.zap.extension.maplocal.view.popup;

import java.awt.Component;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.extension.ExtensionPopupMenuItem;
import org.zaproxy.zap.extension.maplocal.ExtensionMapLocal;
import org.zaproxy.zap.extension.maplocal.view.MapLocalStatusPanel;

@SuppressWarnings("serial")
public class PopupMenuEditMapLocal extends ExtensionPopupMenuItem {
    private static final long serialVersionUID = 1L;

    private ExtensionMapLocal extension;

    public PopupMenuEditMapLocal() {
        super(Constant.messages.getString("mapLocal.edit.popup"));
        this.addActionListener(
                new java.awt.event.ActionListener() {

                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        extension.editUiSelectedMapLocal();
                    }
                });
    }

    public void setExtension(ExtensionMapLocal extension) {
        this.extension = extension;
    }

    @Override
    public boolean isEnableForComponent(Component invoker) {
        return invoker.getName() != null
                && invoker.getName().equals(MapLocalStatusPanel.PANEL_NAME);
    }
}
