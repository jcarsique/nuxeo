/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 */

package org.nuxeo.ecm.platform.audit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(AuditFeature.class)
public class TestTransactedAudit {

    protected @Inject CoreSession repo;

    @Before
    public void isInjected() {
        assertThat(repo, notNullValue());
    }

    @Test
    public void canLogMultipleLifecycleTransitionsInSameTx() {
        // generate events
        DocumentModel doc = repo.createDocumentModel("/", "a-file", "File");
        doc = repo.createDocument(doc);
        String initialLifeCycle = doc.getCurrentLifeCycleState();
        doc.followTransition(LifeCycleConstants.DELETE_TRANSITION);
        String deletedLifeCycle = doc.getCurrentLifeCycleState();
        doc.followTransition(LifeCycleConstants.UNDELETE_TRANSITION);
        String undeletedLifeCycle = doc.getCurrentLifeCycleState();
        TransactionHelper.commitOrRollbackTransaction();
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();

        // test audit trail
        AuditReader reader = Framework.getLocalService(AuditReader.class);
        List<LogEntry> trail = reader.getLogEntriesFor(doc.getId());

        assertThat(trail, notNullValue());
        assertThat(trail.size(), is(3));

        boolean seenDocCreated = false;
        boolean seenDocDeleted = false;
        boolean seenDocUndeleted = false;

        for (LogEntry entry : trail) {
            String lifeCycle = entry.getDocLifeCycle();
            String id = entry.getEventId();
            if (DocumentEventTypes.DOCUMENT_CREATED.equals(id)) {
                if (initialLifeCycle.equals(lifeCycle)) {
                    seenDocCreated = true;
                }
            } else if (LifeCycleConstants.TRANSITION_EVENT.equals(id)) {
                if (undeletedLifeCycle.equals(lifeCycle)) {
                    seenDocUndeleted = true;
                } else if (deletedLifeCycle.equals(lifeCycle)) {
                    seenDocDeleted = true;
                }
            }
        }

        assertThat(seenDocUndeleted, is(true));
        assertThat(seenDocDeleted, is(true));
        assertThat(seenDocCreated, is(true));

        // needed for session cleanup
        TransactionHelper.startTransaction();
    }

    @Test
    public void testLogDate() throws InterruptedException {
        // generate doc creation events
        DocumentModel doc = repo.createDocumentModel("/", "a-file", "File");
        doc = repo.createDocument(doc);

        // simulate a long running process in the same transaction: make the
        // delay big enough to make logDate not the same as eventDate even on
        // databases that have a 1s time resolution.
        Thread.sleep(1000);

        // commit the transaction an let the audit service log the events in the
        // log
        TransactionHelper.commitOrRollbackTransaction();
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();

        // test audit trail
        AuditReader reader = Framework.getLocalService(AuditReader.class);
        List<LogEntry> trail = reader.getLogEntriesFor(doc.getId());

        assertThat(trail, notNullValue());
        assertThat(trail.size(), is(1));

        Date eventDate = null;
        Date logDate = null;
        for (LogEntry entry : trail) {
            String id = entry.getEventId();
            if (DocumentEventTypes.DOCUMENT_CREATED.equals(id)) {
                eventDate = entry.getEventDate();
                logDate = entry.getLogDate();
            }
        }
        assertNotNull(eventDate);
        assertNotNull(logDate);
        assertTrue(logDate.after(eventDate));

        // needed for session cleanup
        TransactionHelper.startTransaction();
    }
}
