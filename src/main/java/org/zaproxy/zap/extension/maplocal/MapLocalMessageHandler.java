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

import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.parosproxy.paros.network.HttpMessage;
import org.zaproxy.zap.extension.httppanel.Message;
import org.zaproxy.zap.extension.maplocal.view.MapLocalTableEntry;

public class MapLocalMessageHandler {

    private static final Logger LOGGER = LogManager.getLogger(MapLocalMessageHandler.class);
    protected static final Object SEMAPHORE = new Object();

    protected List<MapLocalTableEntry> enabledMapLocals;

    public void setEnabledMapLocals(List<MapLocalTableEntry> enabledMapLocals) {
        this.enabledMapLocals = enabledMapLocals;
    }

    protected MapLocalTableEntry findEnabledMapLocal(
            Message aMessage, boolean isRequest, boolean onlyIfInScope) {
        if (enabledMapLocals.isEmpty()) return null;

        synchronized (enabledMapLocals) {
            Iterator<MapLocalTableEntry> it = enabledMapLocals.iterator();

            while (it.hasNext()) {
                MapLocalTableEntry mapLocal = it.next();

                if (mapLocal.match(aMessage, isRequest, onlyIfInScope)) {
                    return mapLocal;
                }
            }
        }
        return null;
    }

    public boolean handleMessageReceivedFromServer(HttpMessage msg, boolean onlyIfInScope) {
        MapLocalTableEntry mapLocal = findEnabledMapLocal(msg, false, onlyIfInScope);
        if (mapLocal != null) {
            try {
                synchronized (SEMAPHORE) {
                    msg.setResponseBody(Files.readAllBytes(mapLocal.getLocalPath()));
                    msg.getResponseHeader().setContentLength(msg.getResponseBody().length());
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return true;
    }
}
