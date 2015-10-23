/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.io.services.enricher;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonDocumentListWriter;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.BreadcrumbJsonEnricher;

/**
 * @since 5.7.3
 * @deprecated This enricher was migrated to {@link BreadcrumbJsonEnricher}. The content enricher service doesn't work
 *             anymore.
 */
@Deprecated
public class BreadcrumbEnricher extends AbstractContentEnricher {

    @Override
    public void enrich(JsonGenerator jg, RestEvaluationContext ec) throws IOException {
        DocumentModel doc = ec.getDocumentModel();
        CoreSession session = doc.getCoreSession();
        List<DocumentModel> parentDocuments = session.getParentDocuments(doc.getRef());
        JsonDocumentListWriter.writeDocuments(jg, new DocumentModelListImpl(parentDocuments), new String[] {},
                ec.getRequest());
    }

}
