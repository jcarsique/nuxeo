/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.tokenauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.storage.sql.DatabaseMySQL;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * Tests the {@link TokenAuthenticationService}.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(TokenAuthenticationServiceFeature.class)
public class TestTokenAuthenticationService {

    private static final Log log = LogFactory.getLog(TestTokenAuthenticationService.class);

    @Inject
    protected TokenAuthenticationService tokenAuthenticationService;

    @Inject
    protected DirectoryService directoryService;

    @After
    public void cleanDirectories() throws Exception {
        Session tokenDirSession = directoryService.open("authTokens");
        try {
            DocumentModelList entries = tokenDirSession.getEntries();
            for (DocumentModel entry : entries) {
                tokenDirSession.deleteEntry(entry);
            }
        } finally {
            tokenDirSession.close();
        }
    }

    @Test
    public void testAcquireToken() throws ClientException {

        // Test omitting required parameters
        try {
            tokenAuthenticationService.acquireToken("joe", "myFavoriteApp",
                    null, null, null);
            fail("Getting token should have failed since required parameters are missing.");
        } catch (TokenAuthenticationException e) {
            assertEquals(
                    "The following parameters are mandatory to get an authentication token: userName, applicationName, deviceId.",
                    e.getMessage());
        }

        // Test token generation
        String token = tokenAuthenticationService.acquireToken("joe",
                "myFavoriteApp", "Ubuntu box 64 bits",
                "This is my personal box", "rw");
        assertNotNull(token);

        // Test token binding persistence
        Session directorySession = directoryService.open("authTokens");
        try {
            DocumentModel tokenModel = directorySession.getEntry(token);
            assertNotNull(tokenModel);
            assertEquals(token, tokenModel.getPropertyValue("authtoken:token"));
            assertEquals("joe",
                    tokenModel.getPropertyValue("authtoken:userName"));
            assertEquals("myFavoriteApp",
                    tokenModel.getPropertyValue("authtoken:applicationName"));
            assertEquals("Ubuntu box 64 bits",
                    tokenModel.getPropertyValue("authtoken:deviceId"));
            assertEquals("This is my personal box",
                    tokenModel.getPropertyValue("authtoken:deviceDescription"));
            assertEquals("rw",
                    tokenModel.getPropertyValue("authtoken:permission"));
            assertNotNull(tokenModel.getPropertyValue("authtoken:creationDate"));
        } finally {
            directorySession.close();
        }

        // Test existing token acquisition
        String sameToken = tokenAuthenticationService.acquireToken("joe",
                "myFavoriteApp", "Ubuntu box 64 bits",
                "This is my personal box", "rw");
        assertEquals(token, sameToken);

        // Test token uniqueness
        String otherToken = tokenAuthenticationService.acquireToken("jack",
                "myFavoriteApp", "Ubuntu box 64 bits",
                "This is my personal box", "rw");
        assertTrue(!otherToken.equals(token));
    }

    @Test
    public void testGetToken() throws TokenAuthenticationException {

        // Test non existing token retrieval
        assertNull(tokenAuthenticationService.getToken("john", "myFavoriteApp",
                "Ubuntu box 64 bits"));

        // Test existing token retrieval
        tokenAuthenticationService.acquireToken("joe", "myFavoriteApp",
                "Ubuntu box 64 bits", "This is my personal box", "rw");
        assertNotNull(tokenAuthenticationService.getToken("joe",
                "myFavoriteApp", "Ubuntu box 64 bits"));
    }

    @Test
    public void testGetUserName() throws TokenAuthenticationException {

        // Test invalid token
        String token = "invalidToken";
        String userName = tokenAuthenticationService.getUserName(token);
        assertNull(userName);

        // Test valid token
        token = tokenAuthenticationService.acquireToken("joe", "myFavoriteApp",
                "Ubuntu box 64 bits", "This is my personal box", "rw");
        userName = tokenAuthenticationService.getUserName(token);
        assertEquals("joe", userName);
    }

    @Test
    public void testRevokeToken() throws TokenAuthenticationException {

        // Test revoking an unexisting token, should not fail
        tokenAuthenticationService.revokeToken("unexistingToken");

        // Test revoking an existing token
        String token = tokenAuthenticationService.acquireToken("joe",
                "myFavoriteApp", "Ubuntu box 64 bits",
                "This is my personal box", "rw");
        assertEquals("joe", tokenAuthenticationService.getUserName(token));

        tokenAuthenticationService.revokeToken(token);
        assertNull(tokenAuthenticationService.getUserName(token));
    }

    @Test
    public void testGetTokenBindings() throws ClientException {

        // Test empty token bindings
        assertEquals(0,
                tokenAuthenticationService.getTokenBindings("john").size());

        // Test existing token bindings
        String token1 = tokenAuthenticationService.acquireToken("joe",
                "myFavoriteApp", "Ubuntu box 64 bits",
                "This is my personal Linux box", "rw");
        log.debug("token1 = " + token1);
        String token2 = tokenAuthenticationService.acquireToken("joe",
                "myFavoriteApp", "Windows box 32 bits",
                "This is my personal Windows box", "rw");
        log.debug("token2 = " + token2);
        String token3 = tokenAuthenticationService.acquireToken("joe",
                "nuxeoDrive", "Mac OSX VM", "This is my personal Mac box", "rw");
        log.debug("token3 = " + token3);

        DocumentModelList tokenBindings = tokenAuthenticationService.getTokenBindings("joe");
        assertEquals(3,
                tokenAuthenticationService.getTokenBindings("joe").size());

        // Bindings should be sorted by descendant creation date
        if (!(DatabaseHelper.DATABASE instanceof DatabaseMySQL)) {
            DocumentModel tokenBinding = tokenBindings.get(0);
            String binding1Token = (String) tokenBinding.getPropertyValue("authtoken:token");
            log.debug("binding1Token = " + binding1Token);
            assertEquals(token3, binding1Token);
            assertEquals("joe",
                    tokenBinding.getPropertyValue("authtoken:userName"));
            assertEquals("nuxeoDrive",
                    tokenBinding.getPropertyValue("authtoken:applicationName"));
            assertEquals("Mac OSX VM",
                    tokenBinding.getPropertyValue("authtoken:deviceId"));
            assertEquals(
                    "This is my personal Mac box",
                    tokenBinding.getPropertyValue("authtoken:deviceDescription"));
            assertEquals("rw",
                    tokenBinding.getPropertyValue("authtoken:permission"));
            assertNotNull(tokenBinding.getPropertyValue("authtoken:creationDate"));

            tokenBinding = tokenBindings.get(1);
            String binding2Token = (String) tokenBinding.getPropertyValue("authtoken:token");
            log.debug("binding2Token = " + binding2Token);
            assertEquals(token2, binding2Token);
            assertEquals("joe",
                    tokenBinding.getPropertyValue("authtoken:userName"));
            assertEquals("myFavoriteApp",
                    tokenBinding.getPropertyValue("authtoken:applicationName"));
            assertEquals("Windows box 32 bits",
                    tokenBinding.getPropertyValue("authtoken:deviceId"));
            assertEquals(
                    "This is my personal Windows box",
                    tokenBinding.getPropertyValue("authtoken:deviceDescription"));
            assertEquals("rw",
                    tokenBinding.getPropertyValue("authtoken:permission"));
            assertNotNull(tokenBinding.getPropertyValue("authtoken:creationDate"));

            tokenBinding = tokenBindings.get(2);
            String binding3Token = (String) tokenBinding.getPropertyValue("authtoken:token");
            log.debug("binding3Token = " + binding3Token);
            assertEquals(token1, binding3Token);
            assertEquals("joe",
                    tokenBinding.getPropertyValue("authtoken:userName"));
            assertEquals("myFavoriteApp",
                    tokenBinding.getPropertyValue("authtoken:applicationName"));
            assertEquals("Ubuntu box 64 bits",
                    tokenBinding.getPropertyValue("authtoken:deviceId"));
            assertEquals(
                    "This is my personal Linux box",
                    tokenBinding.getPropertyValue("authtoken:deviceDescription"));
            assertEquals("rw",
                    tokenBinding.getPropertyValue("authtoken:permission"));
            assertNotNull(tokenBinding.getPropertyValue("authtoken:creationDate"));
        } else {
            log.debug("Not testing token bindings order since running on MySQL that does not support milliseconds in dates.");
        }
    }

}
