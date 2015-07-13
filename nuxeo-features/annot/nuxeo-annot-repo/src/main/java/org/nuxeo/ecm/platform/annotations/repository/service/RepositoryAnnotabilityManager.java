/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.repository.service;

import java.net.URI;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.annotations.repository.URNDocumentViewTranslator;
import org.nuxeo.ecm.platform.annotations.service.AnnotabilityManager;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.runtime.api.Framework;

public class RepositoryAnnotabilityManager implements AnnotabilityManager {

    private final URNDocumentViewTranslator translator = new URNDocumentViewTranslator();

    private AnnotationsRepositoryService service;

    public RepositoryAnnotabilityManager() {
        service = Framework.getService(AnnotationsRepositoryService.class);
    }

    public boolean isAnnotable(URI uri) {
        DocumentView view = translator.getDocumentViewFromUri(uri);
        if (view == null) { // not a nuxeo uri
            return service.isAnnotable(null);
        }
        try (CoreSession session = CoreInstance.openCoreSession(null)) {
            DocumentModel model = session.getDocument(view.getDocumentLocation().getDocRef());
            return service.isAnnotable(model);
        }
    }
}
