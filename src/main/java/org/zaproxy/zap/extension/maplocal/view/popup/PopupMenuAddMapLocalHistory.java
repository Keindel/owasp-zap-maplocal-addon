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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.db.DatabaseException;
import org.parosproxy.paros.model.HistoryReference;
import org.parosproxy.paros.network.HttpMalformedHeaderException;
import org.zaproxy.zap.extension.maplocal.ExtensionMapLocal;
import org.zaproxy.zap.view.messagecontainer.http.HttpMessageContainer;
import org.zaproxy.zap.view.popup.PopupMenuItemHistoryReferenceContainer;

@SuppressWarnings("serial")
public class PopupMenuAddMapLocalHistory extends PopupMenuItemHistoryReferenceContainer {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger(PopupMenuAddMapLocalHistory.class);

    private final ExtensionMapLocal extensionMapLocal;

    public PopupMenuAddMapLocalHistory(ExtensionMapLocal extension) {
        super(Constant.messages.getString("mapLocal.add.popup"));

        this.extensionMapLocal = extension;
    }

    @Override
    public boolean isEnableForInvoker(Invoker invoker, HttpMessageContainer httpMessageContainer) {
        return (invoker == Invoker.HISTORY_PANEL);
    }

    @Override
    public void performAction(HistoryReference href) {
        try {
            extensionMapLocal.addUiMapLocal(href.getHttpMessage());
        } catch (HttpMalformedHeaderException | DatabaseException e) {
            LOGGER.warn(e.getMessage(), e);
            extensionMapLocal
                    .getView()
                    .showWarningDialog(Constant.messages.getString("mapLocal.add.error.history"));
        }
    }
}
