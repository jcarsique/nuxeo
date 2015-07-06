/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.dublincore.service;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Service that writes Metadata.
 *
 * @author <a href="td@nuxeo.com">Thierry Delprat</a>
 */
public class DublinCoreStorageService extends DefaultComponent {

    public static Log log = LogFactory.getLog(DublinCoreStorageService.class);

    public static final String ID = "DublinCoreStorageService";

    public void setCreationDate(DocumentModel doc, Calendar creationDate, Event event) {
        doc.setProperty("dublincore", "created", creationDate);
        addContributor(doc, event);
    }

    public void setModificationDate(DocumentModel doc, Calendar modificationDate, Event event) {
        doc.setProperty("dublincore", "modified", modificationDate);
        if (doc.getProperty("dublincore", "created") == null) {
            setCreationDate(doc, modificationDate, event);
        }
    }

    public void addContributor(DocumentModel doc, Event event) {
        Principal principal = event.getContext().getPrincipal();
        if (principal == null) {
            return;
        }

        String principalName = principal.getName();
        if (principal instanceof SystemPrincipal) {
            SystemPrincipal nxp = (SystemPrincipal) principal;
            String originatingUser = nxp.getOriginatingUser();
            if ((originatingUser == null || SYSTEM_USERNAME.equals(originatingUser))
                    && !DOCUMENT_CREATED.equals(event.getName())) {
                return;
            } else {
                principalName = originatingUser;
            }
        }

        String[] contributorsArray = (String[]) doc.getProperty("dublincore", "contributors");

        List<String> contributorsList = new ArrayList<String>();

        if (contributorsArray != null && contributorsArray.length > 0) {
            contributorsList = Arrays.asList(contributorsArray);
            // make it resizable
            contributorsList = new ArrayList<String>(contributorsList);
        } else {
            // initialize creator too
            SchemaManager schemaMgr = Framework.getLocalService(SchemaManager.class);
            if (schemaMgr.getSchema("dublincore").getField("creator") != null) {
                // First time only => creator
                doc.setProperty("dublincore", "creator", principalName);
            }
        }

        if (!contributorsList.contains(principalName)) {
            contributorsList.add(principalName);
            String[] contributorListIn = new String[contributorsList.size()];
            contributorsList.toArray(contributorListIn);
            doc.setProperty("dublincore", "contributors", contributorListIn);
        }

        doc.setProperty("dublincore", "lastContributor", principalName);
    }

    public void setIssuedDate(DocumentModel doc, Calendar issuedDate) {
        doc.setPropertyValue("dc:issued", issuedDate);
    }

}
