/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.api;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.helper.RootSectionFinder;

import java.util.List;
import java.util.Map;

/**
 * Main publication Service.
 *
 * @author tiry
 */
public interface PublisherService {

    String DOMAIN_NAME_KEY = "DomainName";

    List<String> getAvailablePublicationTree();

    /**
     * Returns a {@code Map} with tree name as key and tree title as value.
     */
    Map<String, String> getAvailablePublicationTrees();

    PublicationTree getPublicationTree(String treeName, CoreSession coreSession, Map<String, String> params)
            throws PublicationTreeNotAvailable;

    PublicationTree getPublicationTree(String treeName, CoreSession coreSession, Map<String, String> params,
            DocumentModel currentDocument) throws PublicationTreeNotAvailable;

    PublishedDocument publish(DocumentModel doc, PublicationNode targetNode);

    PublishedDocument publish(DocumentModel doc, PublicationNode targetNode, Map<String, String> params);

    void unpublish(DocumentModel doc, PublicationNode targetNode);

    boolean isPublishedDocument(DocumentModel documentModel);

    PublicationTree getPublicationTreeFor(DocumentModel doc, CoreSession coreSession);

    PublicationNode wrapToPublicationNode(DocumentModel documentModel, CoreSession coreSession) throws
            PublicationTreeNotAvailable;

    Map<String, String> getParametersFor(String treeConfigName);

    void releaseAllTrees(String sessionId);

    RootSectionFinder getRootSectionFinder(CoreSession session);

}
