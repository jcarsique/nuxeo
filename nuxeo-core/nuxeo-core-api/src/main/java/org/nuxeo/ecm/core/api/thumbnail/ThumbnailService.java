/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 *
 */
package org.nuxeo.ecm.core.api.thumbnail;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 5.7
 */
public interface ThumbnailService {

    /**
     * Get the document thumbnail (related to the doc type/facet)
     */
    public Blob getThumbnail(DocumentModel doc, CoreSession session);

    /**
     * Compute the thumbnail (related to the document type/facet)
     */
    public Blob computeThumbnail(DocumentModel doc, CoreSession session);

}
