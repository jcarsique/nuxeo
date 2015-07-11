/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.content.template.factories;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.content.template.listener.RepositoryInitializationListener;
import org.nuxeo.ecm.platform.content.template.service.TemplateItemDescriptor;

/**
 * Specific factory for Root. Since some other {@link RepositoryInitializationListener} have run before, root won't be
 * empty but we may still have to run this initializer.
 *
 * @author Thierry Delprat
 */
public class SimpleTemplateBasedRootFactory extends SimpleTemplateBasedFactory {

    @Override
    public void createContentStructure(DocumentModel eventDoc) {
        initSession(eventDoc);

        if (!shouldCreateContent(eventDoc)) {
            return;
        }

        for (TemplateItemDescriptor item : template) {
            String itemPath = eventDoc.getPathAsString();
            if (item.getPath() != null) {
                itemPath += "/" + item.getPath();
            }
            DocumentModel newChild = session.createDocumentModel(itemPath, item.getId(), item.getTypeName());
            newChild.setProperty("dublincore", "title", item.getTitle());
            newChild.setProperty("dublincore", "description", item.getDescription());
            setProperties(item.getProperties(), newChild);
            newChild = session.createDocument(newChild);
            setAcl(item.getAcl(), newChild.getRef());
        }
        // init root ACL if really empty
        setAcl(acl, eventDoc.getRef());
    }

    /**
     * Returns {@code false} if the type of one of the children documents matches a template item type, {@code true}
     * otherwise.
     */
    protected boolean shouldCreateContent(DocumentModel eventDoc) {
        for (TemplateItemDescriptor item : template) {
            // don't use getChildren, which can be costly
            // if the folder has a huge number of children
            String query = "SELECT ecm:uuid FROM Document WHERE ecm:parentId = '%s' AND ecm:primaryType = '%s'";
            query = String.format(query, eventDoc.getId(), item.getTypeName());
            IterableQueryResult it = session.queryAndFetch(query, NXQL.NXQL);
            try {
                if (it.iterator().hasNext()) {
                    return false;
                }
            } finally {
                it.close();
            }
        }
        return true;
    }
}
