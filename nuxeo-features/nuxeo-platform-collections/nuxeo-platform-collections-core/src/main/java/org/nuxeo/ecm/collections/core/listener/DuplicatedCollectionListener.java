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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.core.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.adapter.CollectionMember;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Event handler to duplicate the collection members of a duplicated collection. The handler is synchronous because it
 * is important to capture the collection member ids of the duplicated collection at the exact moment of duplication. We
 * don't want to duplicate a collection member that was indeed added to the duplicated collection after the duplication.
 * The handler will then launch asynchronous tasks to duplicate the collection members.
 *
 * @since 5.9.3
 */
public class DuplicatedCollectionListener implements EventListener {

    private static final Log log = LogFactory.getLog(DuplicatedCollectionListener.class);

    @Override
    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }

        final String eventId = event.getName();

        final DocumentEventContext docCxt = (DocumentEventContext) event.getContext();

        DocumentModel doc = null;
        if (eventId.equals(DocumentEventTypes.DOCUMENT_CREATED_BY_COPY)) {
            doc = docCxt.getSourceDocument();
        } else if (eventId.equals(DocumentEventTypes.DOCUMENT_CHECKEDIN)) {
            DocumentRef checkedInVersionRef = (DocumentRef) ctx.getProperties().get("checkedInVersionRef");
            doc = ctx.getCoreSession().getDocument(checkedInVersionRef);
            if (!doc.isVersion()) {
                return;
            }
        } else {
            return;
        }

        final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);

        if (collectionManager.isCollection(doc)) {

            if (eventId.equals(DocumentEventTypes.DOCUMENT_CREATED_BY_COPY)) {
                log.trace(String.format("Collection %s copied", doc.getId()));
            } else if (eventId.equals(DocumentEventTypes.DOCUMENT_CHECKEDIN)) {
                log.trace(String.format("Collection %s checked in", doc.getId()));
            }

            collectionManager.processCopiedCollection(doc);

        } else if (collectionManager.isCollected(doc)) {
            doc.getAdapter(CollectionMember.class).setCollectionIds(null);
            ctx.getCoreSession().saveDocument(doc);
            doc.removeFacet(CollectionConstants.COLLECTABLE_FACET);
        }
    }

}
