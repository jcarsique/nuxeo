/*
 * (C) Copyright 2011-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.ecm.core.management.statuses;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;

/**
 * Servlet for retrieving Nuxeo services running status
 */
public class StatusServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(StatusServlet.class);

    public static final String PARAM = "info";

    public static final String PARAM_STARTED = "started";

    public static final String PARAM_SUMMARY = "summary";

    public static final String PARAM_SUMMARY_KEY = "key";

    public static final String PARAM_RELOAD = "reload";

    // duplicated from org.nuxeo.launcher.config.ConfigurationGenerator to avoid
    // dependencies on nuxeo-launcher-commons
    public static final String PARAM_STATUS_KEY = "server.status.key";

    private OSGiRuntimeService runtimeService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String param = req.getParameter(PARAM);
        if (param != null) {
            doPost(req, resp);
        } else {
            sendResponse(resp, "Ok");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StringBuilder response = new StringBuilder();
        String requestedInfo = req.getParameter(PARAM);
        if (requestedInfo.equals(PARAM_STARTED)) {
            getStartedInfo(response);
        } else if (requestedInfo.equals(PARAM_SUMMARY)) {
            String givenKey = req.getParameter(PARAM_SUMMARY_KEY);
            if (getRuntimeService().getProperty(PARAM_STATUS_KEY).equals(givenKey)) {
                getSummaryInfo(response);
            } else {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            }
        } else if (requestedInfo.equals(PARAM_RELOAD)) {
            if (isStarted()) {
                response.append("reload();");
            } else {
                resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
        }
        sendResponse(resp, response.toString());
    }

    protected void sendResponse(HttpServletResponse resp, String response) throws IOException {
        resp.setContentType("text/plain");
        resp.setContentLength(response.getBytes().length);
        OutputStream out = resp.getOutputStream();
        out.write(response.getBytes());
        out.close();
    }

    private RuntimeService getRuntimeService() {
        if (runtimeService == null) {
            try {
                runtimeService = (OSGiRuntimeService) Framework.getRuntime();
            } catch (Exception e) {
                log.error(e);
            }
        }
        return runtimeService;
    }

    protected void getSummaryInfo(StringBuilder response) {
        if (isStarted()) {
            StringBuilder msg = new StringBuilder();
            boolean isFine = runtimeService.getStatusMessage(msg);
            response.append(isFine).append("\n");
            response.append(msg);
        } else {
            response.append(false).append("\n");
            response.append("Runtime failed to start");
        }
    }

    protected void getStartedInfo(StringBuilder response) {
        response.append(isStarted()).toString();
    }

    private boolean isStarted() {
        return getRuntimeService() != null && runtimeService.isStarted();
    }

    @Override
    public void init() throws ServletException {
        log.debug("Ready.");
    }

}
