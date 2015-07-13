/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.ecm.collections.jsf.actions;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;
import static org.nuxeo.ecm.collections.api.CollectionConstants.MAGIC_PREFIX_ID;

import java.io.Serializable;
import java.util.List;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 6.0
 */
@Name("collectionBulkEditActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class CollectionBulkEditActions implements Serializable {

    public static final String SELECTION_EDITED = "selectionEdited";

    public static final String DOCUMENTS_IMPORTED = "documentImported";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @SuppressWarnings("unchecked")
    @Observer({ SELECTION_EDITED, DOCUMENTS_IMPORTED })
    public void addCollectionsOnEvent(List<DocumentModel> documents, DocumentModel doc) {
        List<String> collectionIds = (List<String>) doc.getContextData(org.nuxeo.common.collections.ScopeType.REQUEST,
                "bulk_collections");
        if (collectionIds != null && !collectionIds.isEmpty()) {
            CollectionManager collectionManager = Framework.getService(CollectionManager.class);
            for (String collectionId : collectionIds) {
                if (collectionId.startsWith(MAGIC_PREFIX_ID)) {
                    String title = collectionId.replaceAll("^" + MAGIC_PREFIX_ID, "");
                    collectionManager.addToNewCollection(title, "", documents, documentManager);
                } else {
                    IdRef idRef = new IdRef(collectionId);
                    if (documentManager.exists(idRef)) {
                        DocumentModel collection = documentManager.getDocument(idRef);
                        collectionManager.addToCollection(collection, documents, documentManager);
                    }
                }

            }
        }
    }

}
