/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.http;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @author Alexandre Russel
 */
public class AnnotationsBodyServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private AnnotationServiceFacade facade;

    @Override
    public void init() {
        facade = new AnnotationServiceFacade();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String annId = req.getPathInfo().replaceFirst("/", "");
        String body = facade.getAnnotationBody(annId, (NuxeoPrincipal) req.getUserPrincipal(), req.getRequestURL() + "/");
        resp.getWriter().write(body);
    }
}
