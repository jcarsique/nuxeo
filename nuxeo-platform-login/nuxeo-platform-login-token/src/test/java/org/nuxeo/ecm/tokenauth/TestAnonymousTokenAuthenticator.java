/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.tokenauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.client.RemoteException;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.ui.web.auth.token.TokenAuthenticator;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

/**
 * Tests the {@link TokenAuthenticator} in the case of an anonymous user.
 * 
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features({ TokenAuthenticationServiceFeature.class, EmbeddedAutomationServerFeature.class })
@Deploy("org.nuxeo.ecm.platform.login.token.test:OSGI-INF/test-token-authentication-anonymous-contrib.xml")
@Jetty(port = 18080)
@RepositoryConfig(init = TokenAuthenticationRepositoryInit.class, cleanup = Granularity.METHOD)
public class TestAnonymousTokenAuthenticator {

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected CoreSession session;

    @Inject
    protected HttpAutomationClient automationClient;

    @Test
    public void testAuthenticatorAsAnonymous() throws Exception {

        // Mock token authentication callback and acquire token for anonymous user directly from token authentication
        // service
        TokenAuthenticationCallback cb = new TokenAuthenticationCallback("Guest", "myFavoriteApp",
                "Ubuntu box 64 bits", "This is my personal Linux box", "rw");
        String token = cb.getRemoteToken(cb.getTokenParams());
        assertNotNull(token);

        // Check automation call with anonymous user not allowed
        try {
            automationClient.getSession(token);
            fail("Getting an Automation client session with a token as anonymous user should throw a RemoteException with HTTP 401 status code");
        } catch (RemoteException e) {
            assertEquals(HttpStatus.SC_UNAUTHORIZED, e.getStatus());
        }

        // Check automation call with anonymous user allowed
        harness.deployContrib("org.nuxeo.ecm.platform.login.token.test",
                "OSGI-INF/test-token-authentication-allow-anonymous-token-contrib.xml");

        Session clientSession = automationClient.getSession(token);
        assertEquals("Guest", clientSession.getLogin().getUsername());

        harness.undeployContrib("org.nuxeo.ecm.platform.login.token.test",
                "OSGI-INF/test-token-authentication-allow-anonymous-token-contrib.xml");
    }

    protected void setPermission(DocumentModel doc, ACE ace) throws ClientException {
        ACP acp = session.getACP(doc.getRef());
        ACL localACL = acp.getOrCreateACL(ACL.LOCAL_ACL);
        localACL.add(ace);
        session.setACP(doc.getRef(), acp, true);
        session.save();
    }

}
