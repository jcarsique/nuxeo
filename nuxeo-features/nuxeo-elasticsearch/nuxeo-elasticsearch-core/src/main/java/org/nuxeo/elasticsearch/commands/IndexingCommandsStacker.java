/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 *     Benoit Delbosc
 */

package org.nuxeo.elasticsearch.commands;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.commands.IndexingCommand.Type;
import java.util.Map;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BINARYTEXT_UPDATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDIN;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDOUT;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHILDREN_ORDER_CHANGED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED_BY_COPY;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_MOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_PROXY_UPDATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_REMOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_SECURITY_UPDATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_TAG_UPDATED;

/**
 * Contains logic to stack ElasticSearch commands depending on Document events This class is mainly here to make testing
 * easier
 */
public abstract class IndexingCommandsStacker {

    protected static final Log log = LogFactory.getLog(IndexingCommandsStacker.class);

    protected abstract Map<String, IndexingCommands> getAllCommands();

    protected abstract boolean isSyncIndexingByDefault();

    protected IndexingCommands getCommands(DocumentModel doc) {
        return getAllCommands().get(getDocKey(doc));
    }

    public void stackCommand(DocumentEventContext docCtx, String eventId) {
        DocumentModel doc = docCtx.getSourceDocument();
        if (doc == null) {
            return;
        }
        Boolean block = (Boolean) docCtx.getProperty(ElasticSearchConstants.DISABLE_AUTO_INDEXING);
        if (block != null && block) {
            if (log.isDebugEnabled()) {
                log.debug("Indexing is disable, skip indexing command for doc " + doc);
            }
            return;
        }
        boolean sync = isSynchronous(docCtx, doc);
        stackCommand(doc, eventId, sync);
    }

    protected boolean isSynchronous(DocumentEventContext docCtx, DocumentModel doc) {
        // 1. look at event context
        Boolean sync = (Boolean) docCtx.getProperty(ElasticSearchConstants.ES_SYNC_INDEXING_FLAG);
        if (sync != null) {
            return sync;
        }
        // 2. look at document context
        sync = (Boolean) doc.getContextData().get(ElasticSearchConstants.ES_SYNC_INDEXING_FLAG);
        if (sync != null) {
            return sync;
        }
        // 3. get the default
        sync = isSyncIndexingByDefault();
        return sync;
    }

    protected void stackCommand(DocumentModel doc, String eventId, boolean sync) {
        IndexingCommands cmds = getOrCreateCommands(doc);
        Type type;
        boolean recurse = false;
        switch (eventId) {
            case DOCUMENT_CREATED:
            case LifeCycleConstants.TRANSITION_EVENT:
                type = Type.INSERT;
                break;
            case DOCUMENT_CREATED_BY_COPY:
                type = Type.INSERT;
                recurse = isFolderish(doc);
                break;
            case BEFORE_DOC_UPDATE:
            case DOCUMENT_CHECKEDOUT:
            case DOCUMENT_CHECKEDIN:
            case BINARYTEXT_UPDATED:
            case DOCUMENT_TAG_UPDATED:
            case DOCUMENT_PROXY_UPDATED:
                type = Type.UPDATE;
                break;
            case DOCUMENT_MOVED:
                type = Type.UPDATE;
                recurse = isFolderish(doc);
                break;
            case DOCUMENT_REMOVED:
                type = Type.DELETE;
                recurse = isFolderish(doc);
                break;
            case DOCUMENT_SECURITY_UPDATED:
                type = Type.UPDATE_SECURITY;
                recurse = isFolderish(doc);
                break;
            case DOCUMENT_CHILDREN_ORDER_CHANGED:
                type = Type.UPDATE_DIRECT_CHILDREN;
                recurse = true;
                break;
            default:
                return;
        }
        if (sync && recurse) {
            // split into 2 commands one sync and an async recurse
            cmds.add(type, true, false);
            cmds.add(type, false, true);
        } else {
            cmds.add(type, sync, recurse);
        }
    }

    private boolean isFolderish(DocumentModel doc) {
        return doc.isFolder() && ! doc.isVersion();
    }

    protected IndexingCommands getOrCreateCommands(DocumentModel doc) {
        IndexingCommands cmds = getCommands(doc);
        if (cmds == null) {
            cmds = new IndexingCommands(doc);
            getAllCommands().put(getDocKey(doc), cmds);
        }
        return cmds;
    }

    protected String getDocKey(DocumentModel doc) {
        // Don't merge commands with different session, so we work only on attached doc
        return doc.getId() + "#" + doc.getSessionId();
    }

}