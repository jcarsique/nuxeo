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

package org.nuxeo.ecm.core.storage.mongodb;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.local.LocalException;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.runtime.transaction.TransactionRuntimeException;

public class TestMongoDBRepositoryJTAJCA extends MongoDBRepositoryTestCase {

    private static final String NO_TX_CANNOT_RECONN = "No transaction active, cannot reconnect";

    /**
     * Test that connection sharing allows use of several sessions at the same time.
     */
    @Test
    public void testSessionSharing() throws Exception {
        RepositoryService repositoryManager = Framework.getLocalService(RepositoryService.class);
        Repository repo = repositoryManager.getRepository(repositoryName);
        // assertEquals(1, repo.getActiveSessionsCount());

        CoreSession session2 = openSessionAs(SecurityConstants.ADMINISTRATOR);
        // assertEquals(1, repo.getActiveSessionsCount());
        try {
            DocumentModel doc = new DocumentModelImpl("/", "doc", "Document");
            doc = session.createDocument(doc);
            session.save();
            // check that this is immediately seen from other connection
            // (underlying ManagedConnection is the same)
            assertTrue(session2.exists(new PathRef("/doc")));
        } finally {
            closeSession(session2);
        }
        // assertEquals(1, repo.getActiveSessionsCount());
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
        TransactionHelper.commitOrRollbackTransaction(); // release cx
        TransactionHelper.startTransaction();
        openSession(); // reopen cx and hold open

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    TransactionHelper.startTransaction();
                    CoreSession session2;
                    session2 = openSessionAs(SecurityConstants.ADMINISTRATOR);
                    try {
                        assertTrue(session2.exists(new PathRef("/doc")));
                    } finally {
                        closeSession(session2);
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

    protected static final Log log = LogFactory.getLog(TestMongoDBRepositoryJTAJCA.class);

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
        closeSession(session);
        TransactionHelper.commitOrRollbackTransaction(); // release cx

        final DocumentRef ref = new PathRef("/doc");
        TransactionHelper.startTransaction();
        openSession();
        doc = session.getDocument(ref);
        doc.getProperty("dc:title").setValue("first");
        session.saveDocument(doc);
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    TransactionHelper.startTransaction();
                    CoreSession session2;
                    session2 = openSessionAs(SecurityConstants.ADMINISTRATOR);
                    try {
                        DocumentModel doc = session2.getDocument(ref);
                        doc.getProperty("dc:title").setValue("second update");
                        session2.saveDocument(doc);
                    } catch (Exception e) {
                        log.error("Catched error while setting title", e);
                    } finally {
                        TransactionHelper.commitOrRollbackTransaction();
                        closeSession(session2);
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
        closeSession();
        TransactionHelper.commitOrRollbackTransaction();

        TransactionHelper.startTransaction();
        // use a closed session. because of tx, we can reconnect it
        assertNotNull(closedSession.getRootDocument());
        // commit will close low-level session
        TransactionHelper.commitOrRollbackTransaction();
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
        closeSession();
        TransactionHelper.commitOrRollbackTransaction();

        // no startTransaction
        // use a closed session -> exception
        try {
            closedSession.getRootDocument();
            fail("should throw");
        } catch (LocalException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(NO_TX_CANNOT_RECONN));
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

        closeSession();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        openSession();

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
        assertTrue(e.getMessage(), e.getMessage().contains(NO_TX_CANNOT_RECONN));
    }

    @Test
    public void testCloseFromOtherTx() throws Exception {
        assertNotNull(session.getRootDocument());

        final CoreSession finalSession = session;
        session = null;
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    finalSession.close();
                } catch (Exception e) {
                    fail(e.toString());
                }
            }
        };
        t.start();
        t.join();
    }

}
