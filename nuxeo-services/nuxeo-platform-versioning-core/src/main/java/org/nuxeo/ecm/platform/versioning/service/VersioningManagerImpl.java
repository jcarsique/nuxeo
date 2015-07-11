/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Dragos Mihalache
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.versioning.service;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.versioning.api.VersionIncEditOptions;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Versions management component implementation.
 */
public class VersioningManagerImpl extends DefaultComponent implements VersioningManager {

    public static final String COMPONENT_ID = "org.nuxeo.ecm.platform.versioning.VersioningManager";

    @Override
    public VersionIncEditOptions getVersionIncEditOptions(DocumentModel doc) {
        VersionIncEditOptions options = new VersionIncEditOptions();
        VersioningService service = Framework.getService(VersioningService.class);
        for (VersioningOption option : service.getSaveOptions(doc)) {
            VersioningActions action;
            switch (option) {
            case MINOR:
                action = VersioningActions.ACTION_INCREMENT_MINOR;
                break;
            case MAJOR:
                action = VersioningActions.ACTION_INCREMENT_MAJOR;
                break;
            default:
                action = VersioningActions.ACTION_NO_INCREMENT;
            }
            if (option == service.getSaveOptions(doc).get(0)) {
                options.setDefaultVersioningAction(action);
            }
            options.addOption(action);
        }

        return options;
    }

    @Override
    public String getVersionLabel(DocumentModel doc) {
        return doc.getVersionLabel();
    }

    @Override
    @Deprecated
    public DocumentModel incrementMajor(DocumentModel doc) {
        setVersion(doc, getValidMajor(doc) + 1, 0);
        return doc;
    }

    @Override
    @Deprecated
    public DocumentModel incrementMinor(DocumentModel doc) {
        doc.setPropertyValue(VersioningService.MINOR_VERSION_PROP, Long.valueOf(getValidMinor(doc) + 1));
        return doc;
    }

    private static void setVersion(DocumentModel doc, long major, long minor) {
        doc.setPropertyValue(VersioningService.MAJOR_VERSION_PROP, Long.valueOf(major));
        doc.setPropertyValue(VersioningService.MINOR_VERSION_PROP, Long.valueOf(minor));
    }

    private static long getValidVersion(DocumentModel doc, String propName) {
        Object propVal = doc.getPropertyValue(propName);
        if (propVal == null || !(propVal instanceof Long)) {
            return 0;
        } else {
            return ((Long) propVal).longValue();
        }
    }

    private static long getValidMajor(DocumentModel doc) {
        return getValidVersion(doc, VersioningService.MAJOR_VERSION_PROP);
    }

    private static long getValidMinor(DocumentModel doc) {
        return getValidVersion(doc, VersioningService.MINOR_VERSION_PROP);
    }

    @Override
    @Deprecated
    public String getMajorVersionPropertyName(String documentType) {
        return VersioningService.MAJOR_VERSION_PROP;
    }

    @Override
    @Deprecated
    public String getMinorVersionPropertyName(String documentType) {
        return VersioningService.MINOR_VERSION_PROP;
    }

}
