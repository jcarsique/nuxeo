package org.nuxeo.ecm.admin.oauth;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jboss.seam.annotations.In;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

public abstract class DirectoryBasedEditor implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected DocumentModelList entries;

    protected DocumentModel editableEntry;

    protected DocumentModel creationEntry;

    protected abstract String getDirectoryName();

    protected abstract String getSchemaName();

    protected boolean showAddForm = false;

    @In(create = true)
    protected transient CoreSession documentManager;

    public boolean getShowAddForm() {
        return showAddForm;
    }

    public void toggleShowAddForm() {
        showAddForm = !showAddForm;
    }

    public DocumentModel getCreationEntry() throws PropertyException {
        if (creationEntry == null) {
            creationEntry = BaseSession.createEntryModel(null, getSchemaName(), null, null);
        }
        return creationEntry;
    }

    public void refresh() {
        entries = null;
    }

    public void createEntry() throws DirectoryException {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        try (Session session = ds.open(getDirectoryName())) {
            session.createEntry(creationEntry);
            creationEntry = null;
            showAddForm = false;
            entries = null;
        }
    }

    public void resetCreateEntry() {
        creationEntry = null;
        showAddForm = false;
    }

    public void resetEditEntry() {
        editableEntry = null;
        showAddForm = false;
    }

    public DocumentModel getEditableEntry() {
        return editableEntry;
    }

    protected Map<String, Serializable> getQueryFilter() {
        return Collections.emptyMap();
    }

    protected Set<String> getOrderSet() {
        return Collections.emptySet();
    }

    public DocumentModelList getEntries() throws DirectoryException {
        if (entries == null) {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            try (Session session = ds.open(getDirectoryName())) {
                Map<String, Serializable> emptyMap = getQueryFilter();
                Set<String> emptySet = getOrderSet();

                entries = session.query(emptyMap, emptySet, null, true);
            }
        }
        return entries;
    }

    public void editEntry(String entryId) throws DirectoryException {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        try (Session session = ds.open(getDirectoryName())) {
            editableEntry = session.getEntry(entryId);
        }
    }

    public void saveEntry() throws DirectoryException {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        try (Session directorySession = ds.open(getDirectoryName())) {
            UnrestrictedSessionRunner sessionRunner = new UnrestrictedSessionRunner(documentManager) {
                @Override
                public void run() throws ClientException {
                    directorySession.updateEntry(editableEntry);
                }
            };
            sessionRunner.runUnrestricted();
            editableEntry = null;
            entries = null;
        }
    }

    public void deleteEntry(String entryId) throws DirectoryException {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        try (Session directorySession = ds.open(getDirectoryName())) {
            UnrestrictedSessionRunner sessionRunner = new UnrestrictedSessionRunner(documentManager) {
                @Override
                public void run() throws ClientException {
                    directorySession.deleteEntry(entryId);
                }
            };
            sessionRunner.runUnrestricted();
            if (editableEntry != null && editableEntry.getId().equals(entryId)) {
                editableEntry = null;
            }
            entries = null;
        }
    }
}
