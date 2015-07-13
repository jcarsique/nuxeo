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
 *     matic
 */
package org.nuxeo.ecm.platform.annotations.repository.service;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author matic
 */
public class AnnotationsFulltextInjector {

    public static final String RELATED_TEXT_PROPERTY = "relatedtext";

    public static final String RELATED_TEXT_ID_PROPERTY = "relatedtextid";

    public static final String RELATED_TEXT_LIST_PROPERTY = "relatedtext:relatedtextresources";

    public static final String ANNOTATION_RESOURCE_ID_PREFIX = "annotation_";

    public boolean removeAnnotationText(DocumentModel doc, String annotationId) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> relatedResources = doc.getProperty(RELATED_TEXT_LIST_PROPERTY).getValue(List.class);
        String resourceIdToRemove = annotationId == null ? null : makeResourceId(annotationId);
        List<Map<String, String>> resourcesToRemove = new ArrayList<Map<String, String>>();
        for (Map<String, String> resource : relatedResources) {
            String resourceId = resource.get(RELATED_TEXT_ID_PROPERTY);
            if (resourceIdToRemove != null) {
                if (resourceIdToRemove.equals(resourceId)) {
                    resourcesToRemove.add(resource);
                }
            } else {
                // remove all annotations
                if (resourceId == null || resourceId.startsWith(ANNOTATION_RESOURCE_ID_PREFIX)) {
                    resourcesToRemove.add(resource);
                }
            }
        }
        if (!resourcesToRemove.isEmpty()) {
            relatedResources.removeAll(resourcesToRemove);
            doc.setPropertyValue(RELATED_TEXT_LIST_PROPERTY, (Serializable) relatedResources);
            return true;
        }
        return false;
    }

    public void setAnnotationText(DocumentModel doc, String annotationId, String annotationBody) {
        if (annotationBody == null) {
            return;
        }
        // strip HTML markup if any
        BlobHolder bh = new SimpleBlobHolder(Blobs.createBlob(annotationBody, "text/html"));
        ConversionService service = Framework.getService(ConversionService.class);
        if (service != null) {
            try {
                annotationBody = service.convert("html2text", bh, null).getBlob().getString();
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
        }
        @SuppressWarnings("unchecked")
        List<Map<String, String>> relatedResources = doc.getProperty(RELATED_TEXT_LIST_PROPERTY).getValue(List.class);
        HashMap<String, String> resource = new HashMap<String, String>();
        resource.put(RELATED_TEXT_ID_PROPERTY, makeResourceId(annotationId));
        resource.put(RELATED_TEXT_PROPERTY, annotationBody);
        relatedResources.add(resource);
        doc.setPropertyValue(RELATED_TEXT_LIST_PROPERTY, (Serializable) relatedResources);
    }

    protected static String makeResourceId(String annotationId) {
        return ANNOTATION_RESOURCE_ID_PREFIX + annotationId;
    }

}
