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
package org.zaproxy.zap.extension.maplocal.db;

public class RecordMapLocal {

    private int mapLocalId;

    private String urlString;
    private String match;
    private boolean ignoreCase;
    private String localPath;

    public RecordMapLocal(
            int mapLocalId, String urlString, String match, boolean ignoreCase, String localPath) {
        this.mapLocalId = mapLocalId;
        this.urlString = urlString;
        this.match = match;
        this.ignoreCase = ignoreCase;
        this.localPath = localPath;
    }

    public int getMapLocalId() {
        return mapLocalId;
    }

    public void setMapLocalId(int mapLocalId) {
        this.mapLocalId = mapLocalId;
    }

    public String getUrlString() {
        return urlString;
    }

    public void setUrlString(String urlString) {
        this.urlString = urlString;
    }

    public String getMatch() {
        return match;
    }

    public void setMatch(String match) {
        this.match = match;
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }
}
