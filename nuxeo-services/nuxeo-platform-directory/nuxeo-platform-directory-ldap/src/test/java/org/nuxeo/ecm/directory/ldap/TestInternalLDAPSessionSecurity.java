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
 *     mhilaire
 *
 */

package org.nuxeo.ecm.directory.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.security.auth.login.LoginException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.sql.SQLDirectoryFeature;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Test class on security based on LDAP embedded server. Only read based test can be perform because the embedded server
 * does not allow to write
 */
/* Ignored due to NXP-15777, this feature causes failure in the next test */
@Ignore
@RunWith(FeaturesRunner.class)
@Features(InternalLDAPDirectoryFeature.class)
@LocalDeploy("org.nuxeo.ecm.directory.ldap.tests:ldap-directories-internal-security.xml")
public class TestInternalLDAPSessionSecurity {

    public static final String READER_USER = "readerUser";

    @Inject
    ClientLoginFeature dummyLogin;

    @Inject
    DirectoryService dirService;

    Session userDirSession;

    Session groupDirSession;

    @Inject
    InternalLDAPDirectoryFeature ldapFeature;

    @Inject
    MockLdapServer embeddedLDAPserver;

    @Inject
    @Named(SQLDirectoryFeature.USER_DIRECTORY_NAME)
    Directory userDir;

    @Inject
    @Named(SQLDirectoryFeature.GROUP_DIRECTORY_NAME)
    Directory groupDir;

    @Before
    public void setUp() {
        ((LDAPDirectory) userDir).setTestServer(embeddedLDAPserver);
        ((LDAPDirectory) groupDir).setTestServer(embeddedLDAPserver);
        try (LDAPSession session = (LDAPSession) ((LDAPDirectory) userDir).getSession()) {
            DirContext ctx = session.getContext();
            for (String ldifFile : ldapFeature.getLdifFiles()) {
                ldapFeature.loadDataFromLdif(ldifFile, ctx);
            }
        }

        userDirSession = userDir.getSession();
        groupDirSession = groupDir.getSession();
    }

    @After
    public void tearDown() throws NamingException {
        userDirSession.close();
        groupDirSession.close();
        if (embeddedLDAPserver != null) {
            embeddedLDAPserver.shutdownLdapServer();
            embeddedLDAPserver = null;
        }
    }

    @Test
    public void readerUserCanGetEntry() throws Exception {
        dummyLogin.loginAs(READER_USER);
        DocumentModel entry = userDirSession.getEntry("Administrator");
        assertNotNull(entry);
        assertEquals("Administrator", entry.getId());
        dummyLogin.logout();
    }

    @Test
    public void readerUserCanQuery() throws LoginException {
        dummyLogin.loginAs(READER_USER);
        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        filter.put("lastName", "Manager");
        DocumentModelList entries = userDirSession.query(filter);
        assertEquals(1, entries.size());
        dummyLogin.logout();
    }

    @Test
    public void unauthorizedUserCantGetEntry() throws Exception {
        dummyLogin.loginAs("unauthorizedUser");
        DocumentModel entry = userDirSession.getEntry("Administrator");
        Assert.assertNull(entry);
        dummyLogin.logout();
    }

    @Test
    public void everyoneGroupCanGetEntry() throws Exception {
        dummyLogin.loginAs("anEveryoneUser");
        DocumentModel entry = groupDirSession.getEntry("members");
        assertNotNull(entry);
        assertEquals("members", entry.getId());
        dummyLogin.logout();
    }

}
