/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.document;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;

/**
 * Operation that adds a permission to a given ACL for a given user.
 *
 * @since 5.7.3
 */
@Operation(id = AddPermission.ID, category = Constants.CAT_DOCUMENT, label = "Add Permission", description = "Add Permission on the input document(s). Returns the document(s).", aliases = {
        "Document.AddACL" })
public class AddPermission {

    public static final String ID = "Document.AddPermission";

    public static final String NOTIFY_KEY = "notify";

    public static final String COMMENT_KEY = "comment";

    @Context
    protected CoreSession session;

    @Param(name = "username", alias = "user", description = "ACE target user/group.")
    protected String user;

    @Param(name = "permission", description = "ACE permission.")
    String permission;

    @Param(name = "acl", required = false, values = { ACL.LOCAL_ACL }, description = "ACL name.")
    String aclName = ACL.LOCAL_ACL;

    @Param(name = "begin", required = false, description = "ACE begin date.")
    Calendar begin;

    @Param(name = "end", required = false, description = "ACE end date.")
    Calendar end;

    @Param(name = "blockInheritance", required = false, description = "Block inheritance or not.")
    boolean blockInheritance = false;

    @Param(name = "notify", required = false, description = "Notify the user or not")
    boolean notify = false;

    @Param(name = "comment", required = false, description = "Comment")
    String comment;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        addPermission(doc);
        return session.getDocument(doc.getRef());
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentRef docRef) {
        DocumentModel doc = session.getDocument(docRef);
        addPermission(doc);
        return doc;
    }

    protected void addPermission(DocumentModel doc) {
        ACP acp = doc.getACP() != null ? doc.getACP() : new ACPImpl();
        Map<String, Serializable> contextData = new HashMap<>();
        if (notify) {
            contextData.put(NOTIFY_KEY, true);
            contextData.put(COMMENT_KEY, comment);
        }

        String creator = session.getPrincipal().getName();
        ACE ace = ACE.builder(user, permission).creator(creator).begin(begin).end(end).contextData(contextData).build();
        boolean permissionChanged = false;
        if (blockInheritance) {
            permissionChanged = acp.blockInheritance(aclName, creator);
        }
        permissionChanged = acp.addACE(aclName, ace) || permissionChanged;
        if (permissionChanged) {
            doc.setACP(acp, true);
        }
    }

}
