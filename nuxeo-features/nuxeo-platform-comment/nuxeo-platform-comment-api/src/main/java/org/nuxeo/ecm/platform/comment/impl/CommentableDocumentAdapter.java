/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.comment.impl;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class CommentableDocumentAdapter implements CommentableDocument {

    private static final long serialVersionUID = 2996381735762615450L;

    final DocumentModel docModel;

    public CommentableDocumentAdapter(DocumentModel docModel) {
        this.docModel = docModel;
    }

    public DocumentModel addComment(DocumentModel comment) {
        CommentManager commentManager = Framework.getService(CommentManager.class);
        return commentManager.createComment(docModel, comment);
    }

    @Deprecated
    public DocumentModel addComment(String comment) {
        CommentManager commentManager = Framework.getService(CommentManager.class);
        return commentManager.createComment(docModel, comment);
    }

    public DocumentModel addComment(DocumentModel parent, DocumentModel comment) {
        CommentManager commentManager = Framework.getService(CommentManager.class);
        return commentManager.createComment(docModel, parent, comment);
    }

    public void removeComment(DocumentModel comment) {
        CommentManager commentManager = Framework.getService(CommentManager.class);
        commentManager.deleteComment(docModel, comment);
    }

    public List<DocumentModel> getComments() {
        CommentManager commentManager = Framework.getService(CommentManager.class);
        return commentManager.getComments(docModel);
    }

    public List<DocumentModel> getComments(DocumentModel parent) {
        CommentManager commentManager = Framework.getService(CommentManager.class);
        return commentManager.getComments(docModel, parent);
    }

}
