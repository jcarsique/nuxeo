/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Vladimir Pasquier <vpasquier@nuxeo.com>
 * Laurent Doguin <ldoguin@nuxeo.com>
 * Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.ecm.platform.thumbnail.listener;

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailAdapter;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.event.DeletedDocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Thumbnail listener handling creation and update document event to store doc thumbnail preview (only for DocType File)
 *
 * @since 5.7
 */
public class UpdateThumbnailListener implements PostCommitEventListener {

    public static final String THUMBNAIL_UPDATED = "thumbnailUpdated";

    protected void processDoc(CoreSession session, DocumentModel doc) {
        Blob thumbnailBlob = getManagedThumbnail(doc);
        if (thumbnailBlob == null) {
            ThumbnailAdapter thumbnailAdapter = doc.getAdapter(ThumbnailAdapter.class);
            if (thumbnailAdapter == null) {
                return;
            }
            thumbnailBlob = thumbnailAdapter.computeThumbnail(session);
        }
        if (thumbnailBlob != null) {
            if (!doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET)) {
                doc.addFacet(ThumbnailConstants.THUMBNAIL_FACET);
            }
            doc.setPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME, (Serializable) thumbnailBlob);
        } else {
            if (doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET)) {
                doc.setPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME, null);
                doc.removeFacet(ThumbnailConstants.THUMBNAIL_FACET);
            }
        }
        if (doc.isDirty()) {
            doc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.NONE);
            doc.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, Boolean.TRUE);
            doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, Boolean.TRUE);
            doc.putContextData(NotificationConstants.DISABLE_NOTIFICATION_SERVICE, Boolean.TRUE);
            doc.putContextData("disableAuditLogger", Boolean.TRUE);
            if (doc.isVersion()) {
                doc.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
            }
            doc.putContextData(THUMBNAIL_UPDATED, true);
            session.saveDocument(doc);
        }
    }

    private Blob getManagedThumbnail(DocumentModel doc) {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh == null) {
            return null;
        }
        Blob blob = bh.getBlob();
        if (blob == null) {
            return null;
        }
        BlobManager blobManager = Framework.getService(BlobManager.class);
        try {
            InputStream is = blobManager.getThumbnail(blob);
            if (is == null) {
                return null;
            }
            return Blobs.createBlob(is);
        } catch (IOException e) {
            throw new NuxeoException("Failed to get managed blob thumbnail", e);
        }
    }

    @Override
    public void handleEvent(EventBundle events) {
        if (!events.containsEventName(ThumbnailConstants.EventNames.scheduleThumbnailUpdate.name())) {
            return;
        }
        Set<String> processedDocs = new HashSet<String>();
        for (Event event : events) {
            if (!ThumbnailConstants.EventNames.scheduleThumbnailUpdate.name().equals(event.getName())) {
                continue;
            }
            DocumentEventContext context = (DocumentEventContext) event.getContext();
            DocumentModel doc = context.getSourceDocument();
            if (doc instanceof DeletedDocumentModel) {
                continue;
            }
            if (doc.isProxy()) {
                continue;
            }
            if (processedDocs.contains(doc.getId())) {
                continue;
            }
            CoreSession repo = context.getCoreSession();
            processDoc(repo, doc);
            processedDocs.add(doc.getId());
        }
    }
}
