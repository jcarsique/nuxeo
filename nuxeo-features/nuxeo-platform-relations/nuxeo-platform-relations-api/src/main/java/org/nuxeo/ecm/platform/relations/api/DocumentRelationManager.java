/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Remi Cattiau
 */
package org.nuxeo.ecm.platform.relations.api;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;

/**
 * Create relations between documents
 *
 * @since 5.9.2
 */
public interface DocumentRelationManager {

    /**
     * Add link between two document
     *
     * @param from the document to link from
     * @param to the document to link to
     * @param predicate is the type of link
     * @param inverse if to is related to from ( the event will still be generated with from document )
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, DocumentModel to, String predicate, boolean inverse);

    /**
     * Add link between two document
     *
     * @param from the document to link from
     * @param to the node to link to
     * @param predicate is the type of link
     * @param inverse if to is related to from ( the event will still be generated with from document )
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, Node to, String predicate, boolean inverse);

    /**
     * Add link between two document
     *
     * @param from the document to link from
     * @param to the node to link to
     * @param predicate is the type of link
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, Node to, String predicate);

    /**
     * Add link between two document
     *
     * @param from the document to link from
     * @param to the node to link to
     * @param predicate is the type of link
     * @param inverse if to is related to from ( the event will still be generated with from document )
     * @param includeStatementsInEvents will add the statement to the events RelationEvents.BEFORE_RELATION_CREATION and
     *            RelationEvents.AFTER_RELATION_CREATION
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, Node to, String predicate, boolean inverse,
            boolean includeStatementsInEvents);

    /**
     * Add link between two document
     *
     * @param from the document to link from
     * @param to the node to link to
     * @param predicate is the type of link
     * @param inverse if to is related to from ( the event will still be generated with from document )
     * @param includeStatementsInEvents will add the statement to the events RelationEvents.BEFORE_RELATION_CREATION and
     *            RelationEvents.AFTER_RELATION_CREATION
     * @param comment of the relation
     * @throws RelationAlreadyExistsException
     */
    void addRelation(CoreSession session, DocumentModel from, Node to, String predicate, boolean inverse,
            boolean includeStatementsInEvents, String comment);

    /**
     * @param from document
     * @param to document
     * @param predicate relation type
     */
    void deleteRelation(CoreSession session, DocumentModel from, DocumentModel to, String predicate);

    /**
     * @param statement to delete
     */
    void deleteRelation(CoreSession session, Statement statement);

    /**
     * @param stmt to delete
     * @param includeStatementsInEvents add the current statement in event RelationEvents.BEFORE_RELATION_REMOVAL and
     *            RelationEvents.AFTER_RELATION_REMOVAL
     */
    void deleteRelation(CoreSession session, Statement stmt, boolean includeStatementsInEvents);

    /**
     * @param from document
     * @param to document
     * @param predicate relation type
     * @param includeStatementsInEvents add the current statement in event RelationEvents.BEFORE_RELATION_REMOVAL and
     *            RelationEvents.AFTER_RELATION_REMOVAL
     */
    void deleteRelation(CoreSession session, DocumentModel from, DocumentModel to, String predicate,
            boolean includeStatementsInEvents);

}
