/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephane Lacoin
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.core.convert", //
        "org.nuxeo.ecm.core.convert.plugins" })
public class TestSQLBinariesIndexing {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    protected String docId;

    protected DocumentRef docRef;

    protected BlockingWork blockingWork;

    protected void waitForFulltextIndexing() {
        nextTransaction();
        coreFeature.getStorageConfiguration().waitForFulltextIndexing();
    }

    protected void nextTransaction() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    /** Creates doc, doesn't do a session save. */
    protected void createDocument() {
        DocumentModel doc = session.createDocumentModel("/", "source", "File");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        holder.setBlob(Blobs.createBlob("test"));
        doc = session.createDocument(doc);
        docId = doc.getId();
        docRef = new IdRef(docId);
    }

    /**
     * Work that waits in the fulltext updater queue, blocking other indexing work, until the main thread tells it to go
     * ahead.
     */
    public static class BlockingWork extends AbstractWork {

        private static final long serialVersionUID = 1L;

        protected transient CountDownLatch readyLatch = new CountDownLatch(1);

        protected transient CountDownLatch startLatch = new CountDownLatch(1);

        @Override
        public String getCategory() {
            return "fulltextUpdater";
        }

        @Override
        public String getTitle() {
            return "Blocking Work";
        }

        @Override
        public void work() {
            setStatus("Blocking");
            readyLatch.countDown();
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                // restore interrupted status
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            setStatus("Released");
        }
    }

    protected void blockFulltextUpdating() throws InterruptedException {
        blockingWork = new BlockingWork();
        Framework.getLocalService(WorkManager.class).schedule(blockingWork);
        blockingWork.readyLatch.await();
    }

    protected void allowFulltextUpdating() {
        blockingWork.startLatch.countDown();
        blockingWork = null;
        waitForFulltextIndexing();
    }

    protected int indexedDocs() {
        DocumentModelList res = session.query("SELECT * FROM Document WHERE ecm:fulltext = 'test'");
        return res.size();
    }

    protected int jobDocs() {
        String request = String.format("SELECT * from Document where ecm:fulltextJobId = '%s'", docId);
        return session.query(request).size();
    }

    @Test
    public void testBinariesAreIndexed() throws Exception {
        createDocument();
        blockFulltextUpdating();
        try {
            session.save();
            assertEquals(1, jobDocs());
            assertEquals(0, indexedDocs());
        } finally {
            allowFulltextUpdating();
        }

        waitForFulltextIndexing();
        assertEquals(0, jobDocs());
        assertEquals(1, indexedDocs());
    }

    @Test
    public void testCopiesAreIndexed() throws Exception {
        createDocument();
        blockFulltextUpdating();
        try {
            session.save();
            assertEquals(1, jobDocs());
            assertEquals(0, indexedDocs());

            session.copy(docRef, session.getRootDocument().getRef(), "copy").getRef();

            // check copy is part of requested
            session.save();
            assertEquals(2, jobDocs());
        } finally {
            allowFulltextUpdating();
        }

        // check copy is indexed also
        waitForFulltextIndexing();
        assertEquals(0, jobDocs());
        assertEquals(2, indexedDocs());

        // check copy doesn't stay linked to doc
        DocumentModel doc = session.getDocument(docRef);
        doc.getAdapter(BlobHolder.class).setBlob(Blobs.createBlob("other"));
        session.saveDocument(doc);

        waitForFulltextIndexing();

        assertEquals(1, indexedDocs());
    }

    @Test
    public void testVersionsAreIndexed() throws Exception {
        createDocument();
        blockFulltextUpdating();
        try {
            session.save();
            assertEquals(1, jobDocs());
            assertEquals(0, indexedDocs());

            session.checkIn(docRef, null, null);
            session.save();
        } finally {
            allowFulltextUpdating();
        }

        waitForFulltextIndexing();
        assertEquals(2, indexedDocs());
    }

}
