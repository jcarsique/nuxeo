/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: DocumentFileCodec.java 29556 2008-01-23 00:59:39Z jcarsique $
 */

package org.nuxeo.ecm.platform.url.codec;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.service.AbstractDocumentViewCodec;

public class DocumentFileCodec extends AbstractDocumentViewCodec {

    public static final String FILE_PROPERTY_PATH_KEY = "FILE_PROPERTY_PATH";

    /**
     * @deprecated soon will be part of the file property, passed as parameter for now
     */
    @Deprecated
    public static final String FILENAME_PROPERTY_PATH_KEY = "FILENAME_PROPERTY_PATH";

    public static final String FILENAME_KEY = "FILENAME";

    private static final Log log = LogFactory.getLog(DocumentFileCodec.class);

    // nxdoc/server/docId/property_path/filename/?requestParams
    public static final String URLPattern = "/(\\w+)/([a-zA-Z_0-9\\-]+)(/([a-zA-Z_0-9/:\\-\\.\\]\\[]*))+(/([^\\?]*))+(\\?)?(.*)?";

    public DocumentFileCodec() {
    }

    public DocumentFileCodec(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String getUrlFromDocumentView(DocumentView docView) {
        DocumentLocation docLoc = docView.getDocumentLocation();
        String filepath = docView.getParameter(FILE_PROPERTY_PATH_KEY);
        String filename = docView.getParameter(FILENAME_KEY);
        if (docLoc != null && filepath != null && filename != null) {
            StringBuilder buf = new StringBuilder();
            buf.append(getPrefix());
            buf.append("/");
            buf.append(docLoc.getServerName());
            buf.append("/");
            buf.append(docLoc.getDocRef().toString());
            buf.append("/");
            buf.append(filepath);
            buf.append("/");
            buf.append(URIUtils.quoteURIPathToken(filename));
            String uri = buf.toString();
            Map<String, String> requestParams = new HashMap<String, String>(docView.getParameters());
            requestParams.remove(FILE_PROPERTY_PATH_KEY);
            requestParams.remove(FILENAME_KEY);
            return URIUtils.addParametersToURIQuery(uri, requestParams);
        }
        return null;
    }

    /**
     * Extracts document location from a Zope-like URL ie : server/path_or_docId/view_id/tab_id .
     */
    @Override
    public DocumentView getDocumentViewFromUrl(String url) {
        final Pattern pattern = Pattern.compile(getPrefix() + URLPattern);
        Matcher m = pattern.matcher(url);
        if (m.matches()) {
            if (m.groupCount() >= 4) {

                // for debug
                // for (int i = 1; i < m.groupCount() + 1; i++) {
                // System.err.println(i + ": " + m.group(i));
                // }

                final String server = m.group(1);
                String uuid = m.group(2);
                final DocumentRef docRef = new IdRef(uuid);

                // get other parameters

                Map<String, String> params = new HashMap<String, String>();
                if (m.groupCount() >= 4) {
                    String filePropertyPath = m.group(4);
                    params.put(FILE_PROPERTY_PATH_KEY, filePropertyPath);
                }

                if (m.groupCount() >= 6) {
                    String filename = m.group(6);
                    try {
                        filename = URLDecoder.decode(filename, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        filename = StringUtils.toAscii(filename);
                    }
                    int jsessionidIndex = filename.indexOf(";jsessionid");
                    if (jsessionidIndex != -1) {
                        filename = filename.substring(0, jsessionidIndex);
                    }
                    params.put(FILENAME_KEY, filename);
                }

                if (m.groupCount() >= 8) {
                    String query = m.group(8);
                    Map<String, String> requestParams = URIUtils.getRequestParameters(query);
                    if (requestParams != null) {
                        params.putAll(requestParams);
                    }
                }

                final DocumentLocation docLoc = new DocumentLocationImpl(server, docRef);

                return new DocumentViewImpl(docLoc, null, params);
            }
        }

        return null;
    }

    public static String getFilename(DocumentModel doc, DocumentView docView) {
        String filename = docView.getParameter(FILENAME_KEY);
        if (filename == null) {
            // try to get it from document
            String propertyPath = docView.getParameter(FILENAME_PROPERTY_PATH_KEY);
            String propertyName = DocumentModelUtils.decodePropertyName(propertyPath);
            if (propertyName != null) {
                filename = (String) DocumentModelUtils.getPropertyValue(doc, propertyName);
            }
        }
        return filename;
    }

}
