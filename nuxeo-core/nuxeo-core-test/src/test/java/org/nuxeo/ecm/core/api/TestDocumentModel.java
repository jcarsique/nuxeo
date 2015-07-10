/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestDocumentModel {

    @Inject
    protected CoreSession session;

    /**
     * Tests on a DocumentModel that hasn't been created in the session yet.
     */
    @Test
    public void testDocumentModelNotYetCreated() {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        assertTrue(doc.isCheckedOut());
        assertEquals("0.0", doc.getVersionLabel());
        doc.refresh();
    }

    @Test
    public void testContextDataOfCreatedDocument() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc.putContextData("key", "value");
        doc = session.createDocument(doc);
        assertEquals(doc.getContextData("key"), "value");
    }

    @Test
    public void testDetachAttach() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        String sid = doc.getSessionId();
        assertNotNull(sid);
        assertEquals("project", doc.getCurrentLifeCycleState());
        assertEquals("0.0", doc.getVersionLabel());

        doc.detach(false);
        doc.prefetchCurrentLifecycleState(null);
        assertNull(doc.getSessionId());
        assertNull(doc.getCurrentLifeCycleState());
        assertNull(doc.getVersionLabel());

        doc.attach(sid);
        session.saveDocument(doc);
        assertEquals("project", doc.getCurrentLifeCycleState());
        assertEquals("0.0", doc.getVersionLabel());

        try {
            doc.attach("fakesid");
            fail("Should not allow attach");
        } catch (NuxeoException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Cannot attach a document that is already attached"));
        }
    }

    /**
     * Verifies that checked out state, lifecycle state and lock info are stored on a detached document.
     */
    @Test
    public void testDetachedSystemInfo() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        doc.setLock();

        // refetch to clear lock info
        doc = session.getDocument(new IdRef(doc.getId()));
        // check in
        doc.checkIn(VersioningOption.MAJOR, null);
        // clear lifecycle info
        doc.prefetchCurrentLifecycleState(null);

        doc.detach(true);
        assertFalse(doc.isCheckedOut());
        assertEquals("project", doc.getCurrentLifeCycleState());
        assertNotNull(doc.getLockInfo());

        // refetch to clear lock info
        doc = session.getDocument(new IdRef(doc.getId()));
        // checkout
        doc.checkOut();
        // clear lifecycle info
        doc.prefetchCurrentLifecycleState(null);

        doc.detach(true);
        assertTrue(doc.isCheckedOut());
        assertEquals("project", doc.getCurrentLifeCycleState());
        assertNotNull(doc.getLockInfo());
    }

    @Test(expected = IllegalArgumentException.class)
    public void forbidSlashOnCreate() throws Exception {
        session.createDocumentModel("/", "doc/doc", "File");
    }

    @Test(expected = IllegalArgumentException.class)
    public void forbidSlashOnMove() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        session.move(doc.getRef(), new PathRef("/"), "toto/tata");
    }

    @Test(expected = IllegalArgumentException.class)
    public void forbidSlashOnCopy() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        doc = session.createDocument(doc);
        session.copy(doc.getRef(), new PathRef("/"), "toto/tata");
    }

}
