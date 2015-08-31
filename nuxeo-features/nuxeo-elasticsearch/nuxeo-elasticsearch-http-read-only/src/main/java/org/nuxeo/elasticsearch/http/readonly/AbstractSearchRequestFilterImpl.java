package org.nuxeo.elasticsearch.http.readonly;/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benoit Delbosc
 */

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.json.JSONException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.elasticsearch.http.readonly.filter.RequestValidator;
import org.nuxeo.elasticsearch.http.readonly.filter.SearchRequestFilter;

/**
 * Rewrite an Elsaticsearch search request to add security filter.
 *
 * URI Search are turned into Request body search.
 *
 * @since 7.3
 */
public abstract class AbstractSearchRequestFilterImpl implements SearchRequestFilter {

    protected static final String MATCH_ALL = "{\"query\": {\"match_all\": {}}}";
    protected static final String QUERY_STRING = "{\"query\":{\"query_string\":{\"query\":\"%s\",\"default_field\":\"%s\",\"default_operator\":\"%s\"}}}";
    protected static final String BACKSLASH_MARKER = "_@@_";

    protected String payload;
    protected String rawQuery;
    protected String types;
    protected String indices;
    protected NuxeoPrincipal principal;
    protected String url;
    protected String filteredPayload;

    public AbstractSearchRequestFilterImpl() {

    }

    public void init(CoreSession session, String indices, String types, String rawQuery, String payload) {
        RequestValidator validator = new RequestValidator();
        this.indices = validator.getIndices(indices);
        this.types = validator.getTypes(indices, types);
        this.principal = (NuxeoPrincipal) session.getPrincipal();
        this.rawQuery = rawQuery;
        this.payload = payload;
        if (payload == null && !principal.isAdministrator()) {
            // here we turn the UriSearch query_string into a body search
            extractPayloadFromQuery();
        }
    }

    public String getTypes() {
        return types;
    }

    public String getIndices() {
        return indices;
    }

    @Override
    public String toString() {
        if (payload == null || payload.isEmpty()) {
            return "Uri Search: " + getUrl() + " user: " + principal;
        }
        try {
            return "Body Search: " + getUrl() + " user: " + principal + " payload: " + getPayload();
        } catch (JSONException e) {
            return "Body Search: " + getUrl() + " user: " + principal + " invalid JSON payload: " + e.getMessage();
        }
    }

    public @NotNull String getUrl() {
        if (url == null) {
            url = "/" + indices + "/" + types + "/_search";
            if (rawQuery != null) {
                url += "?" + rawQuery;
            }
        }
        return url;
    }

    public abstract String getPayload() throws JSONException;

    protected Map<String, String> getQueryMap() {
        String[] params = rawQuery.split("&");
        Map<String, String> map = new HashMap<>();
        for (String param : params) {
            String name = param.split("=")[0];
            if (param.contains("=")) {
                map.put(name, param.split("=")[1]);
            } else {
                map.put(name, "");
            }
        }
        return map;
    }

    protected void setRawQuery(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            if (entry.getValue().isEmpty()) {
                sb.append(entry.getKey());
            } else {
                sb.append(String.format("%s=%s", entry.getKey(), entry.getValue()));
            }
        }
        rawQuery = sb.toString();
    }

    protected void extractPayloadFromQuery() {
        Map<String, String> qm = getQueryMap();
        String queryString = qm.remove("q");
        if (queryString == null) {
            payload = MATCH_ALL;
            return;
        }
        try {
            queryString = URLDecoder.decode(queryString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Invalid URI Search query_string encoding: " + e.getMessage());
        }
        String defaultField = qm.remove("df");
        if (defaultField == null) {
            defaultField = "_all";
        }
        String defaultOperator = qm.remove("default_operator");
        if (defaultOperator == null) {
            defaultOperator = "OR";
        }
        payload = String.format(QUERY_STRING, queryString, defaultField, defaultOperator);
        setRawQuery(qm);
    }
}
