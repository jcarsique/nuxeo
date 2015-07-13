/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: RelationActions.java 21346 2007-06-25 16:20:59Z atchertchian $
 */

package org.nuxeo.ecm.platform.relations.web.listener;

import java.util.List;

import javax.faces.event.ActionEvent;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.web.StatementInfo;

/**
 * Relation actions.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface RelationActions {

    String SEARCH_DOCUMENT_LIST = "RELATION_SEARCH_DOCUMENT_LIST";

    List<StatementInfo> getIncomingStatementsInfo();

    List<StatementInfo> getOutgoingStatementsInfo();

    void resetStatements();

    String addStatement();

    String deleteStatement(StatementInfo statementInfo);

    QNameResource getDocumentResource(DocumentModel document);

    DocumentModel getDocumentModel(Node node);

    Boolean getShowCreateForm();

    void toggleCreateForm(ActionEvent event);

    String getComment();

    void setComment(String comment);

    String getObjectDocumentTitle();

    void setObjectDocumentTitle(String objectDocumentTitle);

    String getObjectDocumentUid();

    void setObjectDocumentUid(String objectDocumentUid);

    String getObjectLiteralValue();

    void setObjectLiteralValue(String objectLiteralValue);

    String getObjectType();

    void setObjectType(String objectType);

    String getObjectUri();

    void setObjectUri(String objectUri);

    String getPredicateUri();

    void setPredicateUri(String predicateUri);

}
