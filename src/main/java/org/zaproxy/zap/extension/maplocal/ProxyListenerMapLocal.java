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

import org.parosproxy.paros.core.proxy.ProxyListener;
import org.parosproxy.paros.extension.history.ProxyListenerLog;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.network.HttpMessage;

public class ProxyListenerMapLocal implements ProxyListener {

    // Should be before the breaks listener
    public static final int PROXY_LISTENER_ORDER = ProxyListenerLog.PROXY_LISTENER_ORDER - 2;

    private Model model;
    private ExtensionMapLocal extension;

    public ProxyListenerMapLocal(Model model, ExtensionMapLocal extension) {
        this.model = model;
        this.extension = extension;
    }

    @Override
    public boolean onHttpRequestSend(HttpMessage msg) {
        return true;
    }

    @Override
    public boolean onHttpResponseReceive(HttpMessage msg) {
        return extension.messageReceivedFromServer(msg);
    }

    @Override
    public int getArrangeableListenerOrder() {
        return PROXY_LISTENER_ORDER;
    }
}
