/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.local.LocalException;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.storage.sql.listeners.DummyAsyncRetryListener;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.RepositorySettings;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.runtime.transaction.TransactionRuntimeException;

@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestSQLRepositoryJTAJCA {

    @SuppressWarnings("deprecation")
    private static final String ADMINISTRATOR = SecurityConstants.ADMINISTRATOR;

    @Inject
    protected RepositorySettings repositorySettings;

    @Inject
    protected RepositoryService repositoryService;

    @Inject
    protected EventService eventService;

    @Inject
    protected CoreSession session;

    protected void waitForAsyncCompletion() {
        nextTransaction();
        eventService.waitForAsyncCompletion();
    }

    protected void nextTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    /**
     * Test that connection sharing allows use of several sessions at the same time.
     */
    @Test
    public void testSessionSharing() throws Exception {
        String repositoryName = session.getRepositoryName();
        Repository repo = repositoryService.getRepository(repositoryName);
        assertEquals(1, repo.getActiveSessionsCount());

        try (CoreSession session2 = CoreInstance.openCoreSession(repositoryName, ADMINISTRATOR)) {
            assertEquals(1, repo.getActiveSessionsCount());
            DocumentModel doc = new DocumentModelImpl("/", "doc", "Document");
            doc = session.createDocument(doc);
            session.save();
            // check that this is immediately seen from other connection
            // (underlying ManagedConnection is the same)
            assertTrue(session2.exists(new PathRef("/doc")));
        }
        assertEquals(1, repo.getActiveSessionsCount());
    }

    /**
     * Test that a commit implicitly does a save.
     */
    @Test
    public void testSaveOnCommit() throws Exception {
        // first transaction
        DocumentModel doc = new DocumentModelImpl("/", "doc", "Document");
        doc = session.createDocument(doc);
        // let commit do an implicit save
        nextTransaction();

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    TransactionHelper.startTransaction();
                    try (CoreSession session2 = CoreInstance.openCoreSession(session.getRepositoryName(), ADMINISTRATOR)) {
                        assertTrue(session2.exists(new PathRef("/doc")));
                    } finally {
                        TransactionHelper.commitOrRollbackTransaction();
                    }
                } catch (Exception e) {
                    fail(e.toString());
                }
            }
        };
        t.start();
        t.join();
    }

    protected static final Log log = LogFactory.getLog(TestSQLRepositoryJTAJCA.class);

    protected static class TxWarnChecker extends AppenderSkeleton {

        boolean seenWarn;

        @Override
        public void close() {

        }

        @Override
        public boolean requiresLayout() {
            return false;
        }

        /*
         * (non-Javadoc)
         * @see org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi. LoggingEvent)
         */
        @Override
        protected void append(LoggingEvent event) {
            if (!Level.WARN.equals(event.getLevel())) {
                return;
            }
            Object msg = event.getMessage();
            if (msg instanceof String
                    && (((String) msg).startsWith("Session invoked in a container without a transaction active"))) {
                seenWarn = true;
            }
        }

    }

    /**
     * Cannot use session after close if no tx.
     */
    @Test
    public void testAccessWithoutTx() {
        TransactionHelper.commitOrRollbackTransaction();
        TxWarnChecker checker = new TxWarnChecker();
        Logger.getRootLogger().addAppender(checker);
        try {
            session.getRootDocument();
            fail("should throw");
        } catch (LocalException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("No transaction active, cannot reconnect"));
        }
        TransactionHelper.startTransaction();
    }

    /**
     * Testing that if 2 modifications are done at the same time on the same document on 2 separate transactions, one is
     * rejected (TransactionRuntimeException)
     */
    // not working as is
    @Ignore
    @Test
    public void testConcurrentModification() throws Exception {
        // first transaction
        DocumentModel doc = session.createDocumentModel("/", "doc", "Note");
        doc.getProperty("dc:title").setValue("initial");
        doc = session.createDocument(doc);
        // let commit do an implicit save
        nextTransaction();
        // release cx
        repositorySettings.releaseSession();

        final DocumentRef ref = new PathRef("/doc");
        TransactionHelper.startTransaction();
        // openSession();
        doc = session.getDocument(ref);
        doc.getProperty("dc:title").setValue("first");
        session.saveDocument(doc);
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    TransactionHelper.startTransaction();
                    try (CoreSession session2 = CoreInstance.openCoreSession(session.getRepositoryName(), ADMINISTRATOR)) {
                        DocumentModel doc = session2.getDocument(ref);
                        doc.getProperty("dc:title").setValue("second update");
                        session2.saveDocument(doc);
                    } catch (Exception e) {
                        log.error("Catched error while setting title", e);
                    } finally {
                        TransactionHelper.commitOrRollbackTransaction();
                    }
                } catch (Exception e) {
                    fail(e.toString());
                }
            }
        };
        t.start();
        t.join();
        try {
            TransactionHelper.commitOrRollbackTransaction(); // release cx
            fail("expected TransactionRuntimeException");
        } catch (TransactionRuntimeException e) {
            // expected
        }
    }

    @Test
    @LocalDeploy("org.nuxeo.ecm.core.storage.sql.test:OSGI-INF/test-listeners-async-retry-contrib.xml")
    public void testAsyncListenerRetry() throws Exception {
        DummyAsyncRetryListener.clear();

        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc.setProperty("dublincore", "title", "title1");
        doc = session.createDocument(doc);
        session.save();

        waitForAsyncCompletion();

        assertEquals(2, DummyAsyncRetryListener.getCountHandled());
        assertEquals(1, DummyAsyncRetryListener.getCountOk());
    }

    @Test
    public void testAcquireThroughSessionId() throws Exception {
        DocumentModel file = session.createDocumentModel("/", "file", "File");
        file = session.createDocument(file);
        session.save();

        assertNotNull(file.getCoreSession());
    }

    @Test
    public void testReconnectAfterClose() throws Exception {
        DocumentModel file = session.createDocumentModel("/", "file", "File");
        file = session.createDocument(file);
        session.save();
        CoreSession closedSession = session;

        waitForAsyncCompletion();
        repositorySettings.releaseSession();

        // use a closed session. because of tx, we can reconnect it
        assertNotNull(closedSession.getRootDocument());

        // reopen session for rest of the code
        session = repositorySettings.createSession();
    }

    @Test
    public void testReconnectAfterCommit() throws Exception {
        DocumentModel file = session.createDocumentModel("/", "file", "File");
        file = session.createDocument(file);
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        // keep existing CoreSession whose Session was implicitly closed
        // by commit
        // reconnect possible through tx
        assertNotNull(session.getRootDocument());
    }

    @Test
    public void testReconnectAfterCloseNoTx() throws Exception {
        DocumentModel file = session.createDocumentModel("/", "file", "File");
        file = session.createDocument(file);
        session.save();
        CoreSession closedSession = session;

        waitForAsyncCompletion();
        repositorySettings.releaseSession();
        TransactionHelper.commitOrRollbackTransaction();

        // no startTransaction
        // use a closed session -> exception
        try {
            closedSession.getRootDocument();
            fail("should throw");
        } catch (LocalException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("No transaction active, cannot reconnect"));
        }
    }

    /**
     * DocumentModel.getCoreSession cannot reconnect through a sid that does not exist anymore.
     */
    @Test
    public void testReconnectAfterCloseThroughSessionId() throws Exception {
        DocumentModel file = session.createDocumentModel("/", "file", "File");
        file = session.createDocument(file);
        session.save();

        waitForAsyncCompletion();
        repositorySettings.releaseSession();
        session = repositorySettings.createSession();

        assertNull(file.getCoreSession());
    }

    @Test
    public void testMultiThreaded() throws Exception {
        assertNotNull(session.getRootDocument());

        final CoreSession finalSession = session;
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    TransactionHelper.startTransaction();
                    try {
                        assertNotNull(finalSession.getRootDocument());
                    } finally {
                        TransactionHelper.commitOrRollbackTransaction();
                    }
                } catch (Exception e) {
                    fail(e.toString());
                }
            }
        };
        t.start();
        t.join();

        assertNotNull(session.getRootDocument());
    }

    @Test
    public void testMultiThreadedNeedsTx() throws Exception {
        assertNotNull(session.getRootDocument());

        final CoreSession finalSession = session;
        final Exception[] threadException = new Exception[1];
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    // no tx
                    finalSession.getRootDocument();
                } catch (Exception e) {
                    threadException[0] = e;
                }
            }
        };
        t.start();
        t.join();
        Exception e = threadException[0];
        assertNotNull(e);
        assertTrue(e.getMessage(), e instanceof LocalException);
    }

    @Test
    public void testCloseFromOtherTx() throws Exception {
        assertNotNull(session.getRootDocument());

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    repositorySettings.releaseSession();
                } catch (Exception e) {
                    fail(e.toString());
                }
            }
        };
        t.start();
        t.join();
        session = repositorySettings.createSession();
    }

}
