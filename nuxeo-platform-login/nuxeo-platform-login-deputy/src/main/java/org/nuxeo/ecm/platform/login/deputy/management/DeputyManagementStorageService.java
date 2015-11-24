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
 * $Id$
 */

package org.nuxeo.ecm.platform.login.deputy.management;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

public class DeputyManagementStorageService implements DeputyManager {

    private static final String DIR_NAME = "deputies";

    private static final String DIR_COL_ID = "id";

    private static final String DIR_COL_USERID = "userid";

    private static final String DIR_COL_DEPUTY = "deputy";

    private static final String DIR_COL_VALIDATE_DATE = "validateDate";

    private static final String DIR_COL_START_VALIDITY = "validityStartDate";

    private static final String DIR_COL_END_VALIDITY = "validityEndDate";

    private DirectoryService directoryService;

    private Session dirSession;

    private String directorySchema;

    public String getDeputySchemaName() {
        return directorySchema;
    }

    protected void initPersistentService() throws ClientException {

        if (directoryService == null) {
            try {
                directoryService = Framework.getService(DirectoryService.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }

        dirSession = directoryService.open(DIR_NAME);
        directorySchema = directoryService.getDirectorySchema(DIR_NAME);
    }

    private void releasePersistenceService() {
        // for now directory sessions are lost during passivation of the
        // DirectoryFacade
        // this can't be tested on the client side
        // => release directorySession after each call ...

        if (directoryService == null) {
            dirSession = null;
            return;
        }
        if (dirSession != null) {
            try {
                dirSession.close();
            } catch (Exception e) {
                // do nothing
            }
        }
        dirSession = null;
    }

    public void resetDeputies() throws ClientException {
        initPersistentService();

        try {
            DocumentModelList allEntries = dirSession.getEntries();
            List<String> ids = new ArrayList<String>();
            for (DocumentModel entry : allEntries) {
                ids.add(entry.getId());
            }
            for (String id : ids) {
                dirSession.deleteEntry(id);
            }

            dirSession.commit();

        } finally {
            releasePersistenceService();
        }

    }

    public List<String> getPossiblesAlternateLogins(String userName)
            throws ClientException {
        List<String> users = new ArrayList<String>();
        List<String> outdatedEntriesId = new ArrayList<String>();

        initPersistentService();

        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put(DIR_COL_DEPUTY, userName);

            DocumentModelList entries = null;

            entries = dirSession.query(filter);

            long currentTime = System.currentTimeMillis();
            for (DocumentModel entry : entries) {

                String alternateId = (String) entry.getProperty(
                        directorySchema, DIR_COL_USERID);
                Calendar startDate = (Calendar) entry.getProperty(
                        directorySchema, DIR_COL_START_VALIDITY);
                Calendar endDate = (Calendar) entry.getProperty(
                        directorySchema, DIR_COL_END_VALIDITY);

                boolean validateDate = (Boolean)entry.getProperty(directorySchema, DIR_COL_VALIDATE_DATE);
                boolean valid = true;
                if (validateDate && (startDate != null)
                        && (startDate.getTimeInMillis() > currentTime)) {
                    valid = false;
                }
                if (validateDate && (endDate != null)
                        && (endDate.getTimeInMillis() < currentTime)) {
                    valid = false;
                }

                if (valid) {
                    users.add(alternateId);
                } else {
                    outdatedEntriesId.add(entry.getId());
                }
            }

            for (String outDatedId : outdatedEntriesId) {
                dirSession.deleteEntry(outDatedId);
            }

            dirSession.commit();
            return users;
        } finally {

            releasePersistenceService();
        }
    }

    public List<String> getAvalaibleDeputyIds(String userName)
            throws ClientException {
        List<String> deputies = new ArrayList<String>();

        for (DocumentModel entry : getAvalaibleMandates(userName)) {
            String alternateId = (String) entry.getProperty(directorySchema,
                    DIR_COL_DEPUTY);
            deputies.add(alternateId);
        }

        return deputies;
    }

    public List<DocumentModel> getAvalaibleMandates(String userName)
            throws ClientException {
        List<DocumentModel> deputies = new ArrayList<DocumentModel>();

        initPersistentService();

        try {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put(DIR_COL_USERID, userName);
            return dirSession.query(filter);
        } catch (DirectoryException e) {
            return deputies;
        } catch (ClientException e) {
            return deputies;
        } finally {
            releasePersistenceService();
        }

    }

    public DocumentModel newMandate(String username, String deputy)
            throws ClientException {

        initPersistentService();

        try {
            DocumentModel entry = newEntry(username, deputy);
            Calendar cal = Calendar.getInstance();
            entry.setProperty(directorySchema, DIR_COL_VALIDATE_DATE,  new Boolean(false));
            entry.setProperty(directorySchema, DIR_COL_START_VALIDITY, cal);
            entry.setProperty(directorySchema, DIR_COL_END_VALIDITY, cal);
            return entry;
        } finally {
            releasePersistenceService();
        }
    }

    protected DocumentModel newEntry(String username, String deputy)
            throws ClientException {
        DataModel data = new DataModelImpl(directorySchema,
                new HashMap<String, Object>());
        DocumentModelImpl entry = new DocumentModelImpl(null, directorySchema,
                "0", null, null, null, null, new String[] { directorySchema },
                null, null, null);
        entry.addDataModel(data);
        entry.setProperty(directorySchema, DIR_COL_ID, id(username, deputy));
        entry.setProperty(directorySchema, DIR_COL_USERID, username);
        entry.setProperty(directorySchema, DIR_COL_DEPUTY, deputy);
        return entry;
    }

    public DocumentModel newMandate(String username, String deputy,
            Calendar start, Calendar end) throws ClientException {

        initPersistentService();

        try {
            DocumentModel entry = newEntry(username, deputy);
            entry.setProperty(directorySchema, DIR_COL_VALIDATE_DATE, new Boolean(true));
            entry.setProperty(directorySchema, DIR_COL_START_VALIDITY, start);
            entry.setProperty(directorySchema, DIR_COL_END_VALIDITY, end);
            return entry;
        } finally {
            releasePersistenceService();
        }
    }

    public void addMandate(DocumentModel entry) throws ClientException {

        initPersistentService();

        try {
            String id = id(entry);

            if (dirSession.getEntry(id) != null) {
                // first remove entry
                dirSession.deleteEntry(id);
                dirSession.commit();
            }

            entry.setProperty(directorySchema, DIR_COL_ID, id);

            dirSession.createEntry(entry);

            dirSession.commit();
        } catch (Throwable e) {
            System.out.println(e);
        }
            finally {
            releasePersistenceService();
        }
        return;
    }

    public void removeMandate(String username, String deputy)
            throws ClientException {

        initPersistentService();

        try {
            String id = id(username, deputy);
            dirSession.deleteEntry(id);
            dirSession.commit();
        } finally {
            releasePersistenceService();
        }
    }

    protected String id(DocumentModel entry) throws ClientException {
        return id((String) entry.getProperty(directorySchema, "userid"),
                (String) entry.getProperty(directorySchema, "deputy"));
    }

    protected String id(String username, String deputy) {
        return username + ":" + deputy;
    }

}
