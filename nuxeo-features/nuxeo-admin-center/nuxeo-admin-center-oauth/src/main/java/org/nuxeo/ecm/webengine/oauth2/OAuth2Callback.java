/*
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
 *      Nelson Silva
 */
package org.nuxeo.ecm.webengine.oauth2;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.google.api.client.auth.oauth2.Credential;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

/**
 * WebEngine module to handle the OAuth2 callback
 */
@Path("/oauth2")
@Produces("text/html;charset=UTF-8")
@WebObject(type = "oauth2")
public class OAuth2Callback extends ModuleRoot {

    @Context
    private HttpServletRequest request;

    Credential credential;

    private static final Log log = LogFactory.getLog(OAuth2Callback.class);

    /**
     * @param serviceProviderName
     * @return the rendered page.
     */
    @GET
    @Path("{serviceProviderName}/callback")
    public Object doGet(@PathParam("serviceProviderName") String serviceProviderName)
            throws IOException {

        OAuth2ServiceProviderRegistry registry = Framework.getService(OAuth2ServiceProviderRegistry.class);
        OAuth2ServiceProvider provider = registry.getProvider(serviceProviderName);
        if (provider == null) {
            return Response.status(HttpServletResponse.SC_NOT_FOUND).entity(
                    "No service provider called: \"" + serviceProviderName + "\".").build();
        }

        Map<String, Object> args = new HashMap<>();

        new UnrestrictedSessionRunner(ctx.getCoreSession()) {
            @Override
            public void run() {
                try {
                    credential = provider.handleAuthorizationCallback(request);
                } catch (NuxeoException e) {
                    log.error("Authorization request failed", e);
                    args.put("error", "Authorization request failed");
                }
            }
        }.runUnrestricted();

        String token = (credential == null) ? "" : credential.getAccessToken();
        args.put("token", token);
        return getView("index").args(args);
    }
}
