package org.nuxeo.ecm.platform.publisher.task;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

class LookupStateByACL implements LookupState {

    @Override
    public boolean isPublished(DocumentModel doc, CoreSession session) {
        return session.getACP(doc.getRef()).getACL(CoreProxyWithWorkflowFactory.ACL_NAME) == null;
    }

}
