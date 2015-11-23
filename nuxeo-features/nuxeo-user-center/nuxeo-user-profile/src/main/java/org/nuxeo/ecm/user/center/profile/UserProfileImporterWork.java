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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
package org.nuxeo.ecm.user.center.profile;

import org.nuxeo.common.annotation.Experimental;
import org.nuxeo.ecm.core.work.AbstractWork;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
@Experimental(comment="https://jira.nuxeo.com/browse/NXP-12200")
public class UserProfileImporterWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    protected transient UserProfileImporter importer;

    @Override
    public String getTitle() {
        return "Userprofile Importer";
    }

    @Override
    public Progress getProgress() {
        if (importer != null && importer.totalRecords > 0) {
            return new Progress(importer.currentRecord, importer.totalRecords);
        }
        return super.getProgress();
    }

    @Override
    public void work() {
        initSession();
        importer = new UserProfileImporter();
        importer.doImport(session);
    }

}
