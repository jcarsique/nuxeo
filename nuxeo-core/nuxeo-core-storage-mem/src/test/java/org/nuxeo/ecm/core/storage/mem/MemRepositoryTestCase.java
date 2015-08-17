/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.mem;

import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.ADMINISTRATOR;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManagerComponent;
import org.nuxeo.ecm.core.blob.BlobProviderDescriptor;
import org.nuxeo.ecm.core.blob.binary.DefaultBinaryManager;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.datasource.ConnectionHelper;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class MemRepositoryTestCase extends NXRuntimeTestCase {

    private static final Log log = LogFactory.getLog(MemRepositoryTestCase.class);

    protected String repositoryName = "test";

    protected CoreSession session;

    protected int initialOpenSessions;

    protected int initialSingleConnections;

    protected BlobProviderDescriptor blobProviderDescriptor;

    @Override
    @Before
    public void setUp() throws Exception {
        initCheckLeaks();
        super.setUp();
        deployBundle("org.nuxeo.runtime.jtajca");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.core.storage");
        deployContrib("org.nuxeo.ecm.core.storage.mem.tests", "OSGI-INF/test-repo-types.xml");
        initRepository();
        TransactionHelper.startTransaction();
        openSession();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        if (session != null) {
            session.save();
        }
        waitForAsyncCompletion();
        closeSession();
        TransactionHelper.commitOrRollbackTransaction();
        closeRepository();
        super.tearDown();
        checkLeaks();
    }

    protected void initRepository() throws Exception {
        RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
        RepositoryFactory repositoryFactory = new MemRepositoryFactory();
        repositoryFactory.init(repositoryName);
        Repository repository = new Repository(repositoryName, repositoryName, null, repositoryFactory);
        repositoryManager.addRepository(repository);
        blobProviderDescriptor = new BlobProviderDescriptor();
        blobProviderDescriptor.name = repositoryName;
        blobProviderDescriptor.klass = DefaultBinaryManager.class;
        BlobManagerComponent blobManager = (BlobManagerComponent) Framework.getService(BlobManager.class);
        blobManager.registerBlobProvider(blobProviderDescriptor);
    }

    protected void closeRepository() {
        BlobManagerComponent blobManager = (BlobManagerComponent) Framework.getService(BlobManager.class);
        blobManager.unregisterBlobProvider(blobProviderDescriptor);
        RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
        repositoryManager.removeRepository(repositoryName);
    }

    protected void initCheckLeaks() {
        initialOpenSessions = CoreInstance.getInstance().getNumberOfSessions();
        initialSingleConnections = ConnectionHelper.countConnectionReferences();
    }

    protected void checkLeaks() {
        int finalOpenSessions = CoreInstance.getInstance().getNumberOfSessions();
        int leakedOpenSessions = finalOpenSessions - initialOpenSessions;
        if (leakedOpenSessions != 0) {
            log.error(String.format("There are %s open session(s) at tear down; it seems "
                    + "the test leaked %s session(s).", Integer.valueOf(finalOpenSessions),
                    Integer.valueOf(leakedOpenSessions)));
            for (CoreInstance.RegistrationInfo info : CoreInstance.getInstance().getRegistrationInfos()) {
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
        nextTransaction();
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
    }

    protected void nextTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    public void waitForFulltextIndexing() {
        waitForAsyncCompletion();
        // DatabaseHelper.DATABASE.sleepForFulltext();
    }

    public void maybeSleepToNextSecond() {
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
        return CoreInstance.openCoreSession(repositoryName, username);
    }

    public CoreSession openSessionAs(NuxeoPrincipal principal) {
        return CoreInstance.openCoreSession(repositoryName, principal);
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

    public void reopenSession() {
        closeSession();
        openSession();
    }

}
