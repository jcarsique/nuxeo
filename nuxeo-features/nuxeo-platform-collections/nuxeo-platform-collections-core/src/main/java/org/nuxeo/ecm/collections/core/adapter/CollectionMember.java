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
package org.nuxeo.ecm.collections.core.adapter;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 5.9.3
 */
public class CollectionMember {

    private static final Log log = LogFactory.getLog(CollectionMember.class);

    protected DocumentModel document;

    public CollectionMember(final DocumentModel doc) {
        document = doc;
    }

    public void addToCollection(final String collectionId) {
        List<String> collectionIds = getCollectionIds();
        if (!collectionIds.contains(collectionId)) {
            collectionIds.add(collectionId);
        }
        setCollectionIds(collectionIds);
    }

    public void setCollectionIds(final List<String> collectionIds) {
        document.setPropertyValue(CollectionConstants.DOCUMENT_COLLECTION_IDS_PROPERTY_NAME,
                (Serializable) collectionIds);
    }

    public List<String> getCollectionIds() {
        @SuppressWarnings("unchecked")
        List<String> collectionIds = (List<String>) document.getPropertyValue(CollectionConstants.DOCUMENT_COLLECTION_IDS_PROPERTY_NAME);
        return collectionIds;
    }

    public DocumentModel getDocument() {
        return document;
    }

    public void removeFromCollection(final String documentId) {
        List<String> collectionIds = getCollectionIds();
        if (!collectionIds.remove(documentId)) {
            log.warn(String.format("Element '%s' is not present in the specified collection.", documentId));
        }
        setCollectionIds(collectionIds);
    }

}
