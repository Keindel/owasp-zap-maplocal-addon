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

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.network.HttpMessage;
import org.zaproxy.zap.extension.httppanel.Message;
import org.zaproxy.zap.model.SessionStructure;

public class MapLocalTableEntry {

    private boolean isEnabled = true;

    public enum Match {
        contains,
        regex
    }

    private static final Logger LOGGER = LogManager.getLogger(MapLocalTableEntry.class);

    private String string;
    private URL url;
    private Pattern pattern;
    private Match match;
    private boolean ignoreCase;
    private Path localPath;
    private int mapLocalId = -1;

    public MapLocalTableEntry(String string, Match match, boolean ignoreCase, Path localPath) {
        super();
        this.string = string;
        this.match = match;
        this.ignoreCase = ignoreCase;
        this.localPath = localPath;

        compilePattern();
        getUrlFromString();
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public String getString() {
        return string;
    }

    private void getUrlFromString() {
        url = null;
        try {
            url = new URL(string);
        } catch (MalformedURLException e) {
            // Ignore
        }
    }

    public String getProtocol() {
        return url == null ? "" : url.getProtocol();
    }

    public String getUrlPath() {
        return url == null ? "" : url.getPath();
    }

    public String getQuery() {
        return url == null ? "" : url.getQuery();
    }

    public String getHost() {
        return url == null ? "" : url.getHost();
    }

    public void setString(String str) {
        this.string = str;
        compilePattern();
        getUrlFromString();
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
        compilePattern();
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
        compilePattern();
    }

    public int getMapLocalId() {
        return mapLocalId;
    }

    public void setMapLocalId(int mapLocalId) {
        this.mapLocalId = mapLocalId;
    }

    public Path getLocalPath() {
        return localPath;
    }

    public void setLocalPath(Path localPath) {
        this.localPath = localPath;
    }

    public boolean match(Message aMessage, boolean isRequest, boolean onlyIfInScope) {
        if (aMessage instanceof HttpMessage && !isRequest) {
            HttpMessage message = (HttpMessage) aMessage;

            try {
                String uri = message.getRequestHeader().getURI().toString();

                if (onlyIfInScope) {
                    if (!Model.getSingleton().getSession().isInScope(uri)) {
                        return false;
                    }
                }

                boolean res;
                if (Match.contains.equals(this.match)) {
                    if (ignoreCase) {
                        res = uri.toLowerCase().contains(string.toLowerCase());
                    } else {
                        res = uri.contains(string);
                    }

                } else {
                    res = pattern.matcher(uri).find();
                }
                return res;
            } catch (Exception e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }
        return false;
    }

    private void compilePattern() {
        try {
            if (ignoreCase) {
                pattern =
                        Pattern.compile(
                                SessionStructure.regexEscape(string), Pattern.CASE_INSENSITIVE);
            } else {
                pattern = Pattern.compile(SessionStructure.regexEscape(string));
            }
        } catch (Exception e) {
            // This wont be a problem if its a 'contains' match
            LOGGER.debug("Potentially invalid regex", e);
        }
    }

    public String getDisplayMessage() {
        return Constant.messages.getString("mapLocal.match." + match.name())
                + ": "
                + (ignoreCase ? Constant.messages.getString("mapLocal.ignorecase.label") : "")
                + string;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MapLocalTableEntry)) {
            return false;
        }
        MapLocalTableEntry mapLocal = (MapLocalTableEntry) obj;
        return this.getString().equals(mapLocal.getString())
                && this.getMatch().equals(mapLocal.getMatch())
                && this.isIgnoreCase() == mapLocal.isIgnoreCase()
                && this.getLocalPath().equals(mapLocal.getLocalPath());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(349, 631)
                . // two 'randomly' chosen prime numbers
                append(string)
                .append(match)
                .append(ignoreCase)
                .append(localPath)
                .toHashCode();
    }
}
