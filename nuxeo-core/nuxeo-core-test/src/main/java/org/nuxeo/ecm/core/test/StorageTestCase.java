/*
 * Copyright (c) 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.test;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.*;

import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.datasource.ConnectionHelper;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Base test case to test all possible repository implementations (VCS on each SQL database, DBS on MongoDB or
 * in-memory).
 *
 * @since 7.3
 */
public abstract class StorageTestCase extends NXRuntimeTestCase {

    private static final Log log = LogFactory.getLog(StorageTestCase.class);

    /** The CoreSession available to tests. */
    public CoreSession session;

    /**
     * Initial number of registered session at setup, to be compared with the number of sessions at tear down.
     */
    protected int initialOpenSessions;

    protected int initialSingleConnections;

    public StorageConfiguration database = new StorageConfiguration();

    @Override
    @Before
    public void setUp() throws Exception {
        initialOpenSessions = CoreInstance.getInstance().getNumberOfSessions();
        initialSingleConnections = ConnectionHelper.countConnectionReferences();
        super.setUp();
        deployBundle("org.nuxeo.runtime.management");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.core.storage");
        deployBundle("org.nuxeo.ecm.core.storage.sql");
        DatabaseHelper.DATABASE.setUp();
        deployRepositoryContrib();
    }

    protected void deployRepositoryContrib() throws Exception {
        deployContrib("org.nuxeo.ecm.core.storage.sql.test", DatabaseHelper.DATABASE.getDeploymentContrib());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        waitForAsyncCompletion();
        super.tearDown();
        final CoreInstance core = CoreInstance.getInstance();
        int finalOpenSessions = core.getNumberOfSessions();
        int leakedOpenSessions = finalOpenSessions - initialOpenSessions;
        if (leakedOpenSessions != 0) {
            log.error(String.format("There are %s open session(s) at tear down; it seems "
                    + "the test leaked %s session(s).", Integer.valueOf(finalOpenSessions),
                    Integer.valueOf(leakedOpenSessions)));
            for (CoreInstance.RegistrationInfo info : core.getRegistrationInfos()) {
                log.warn("Leaking session", info);
            }
        }
        int finalSingleConnections = ConnectionHelper.countConnectionReferences();
        int leakedSingleConnections = finalSingleConnections - initialSingleConnections;
        if (leakedSingleConnections > 0) {
            log.error(String.format("There are %s single datasource connection(s) open at tear down; "
                    + "the test leaked %s connection(s).", Integer.valueOf(finalSingleConnections),
                    Integer.valueOf(leakedSingleConnections)));
        }
        ConnectionHelper.clearConnectionReferences();
    }

    public void waitForAsyncCompletion() {
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
    }

    public void waitForFulltextIndexing() {
        database.waitForFulltextIndexing();
    }

    public void openSession() {
        if (session != null) {
            log.warn("Closing session for you");
            closeSession();
        }
        session = openSessionAs(ADMINISTRATOR);
        assertNotNull(session);
    }

    public CoreSession openSessionAs(String username) {
        return CoreInstance.openCoreSession(database.getRepositoryName(), username);
    }

    public CoreSession openSessionAs(NuxeoPrincipal principal) {
        return CoreInstance.openCoreSession(database.getRepositoryName(), principal);
    }

    public void closeSession() {
        closeSession(session);
        session = null;
    }

    public void closeSession(CoreSession session) {
        if (session != null) {
            session.close();
        }
    }

}
