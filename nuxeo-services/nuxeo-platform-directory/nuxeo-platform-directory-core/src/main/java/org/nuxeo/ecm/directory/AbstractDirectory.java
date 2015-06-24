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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.directory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelComparator;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

public abstract class AbstractDirectory implements Directory {

    protected final Log log = LogFactory.getLog(AbstractDirectory.class);

    public final String name;

    protected DirectoryFieldMapper fieldMapper;

    protected final Map<String, List<Reference>> references = new HashMap<>();

    // simple cache system for entry lookups, disabled by default
    protected final DirectoryCache cache;

    // @since 5.7
    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected final Counter sessionCount;

    protected final Counter sessionMaxCount;

    protected AbstractDirectory(String name) {
        this.name = name;
        cache = new DirectoryCache(name);
        sessionCount = registry.counter(MetricRegistry.name("nuxeo", "directories", name, "sessions", "active"));

        sessionMaxCount = registry.counter(MetricRegistry.name("nuxeo", "directories", name, "sessions", "max"));
    }

    /**
     * Invalidate my cache and the caches of linked directories by references.
     */
    public void invalidateCaches() throws DirectoryException {
        cache.invalidateAll();
        for (Reference ref : getReferences()) {
            Directory targetDir = ref.getTargetDirectory();
            if (targetDir != null) {
                targetDir.invalidateDirectoryCache();
            }
        }
    }

    public DirectoryFieldMapper getFieldMapper() {
        if (fieldMapper == null) {
            fieldMapper = new DirectoryFieldMapper();
        }
        return fieldMapper;
    }

    @Override
    public Reference getReference(String referenceFieldName) {
        List<Reference> refs = getReferences(referenceFieldName);
        if (refs == null || refs.isEmpty()) {
            return null;
        } else if (refs.size() == 1) {
            return refs.get(0);
        } else {
            throw new DirectoryException(
                    "Unexpected multiple references for " + referenceFieldName + " in directory " + getName());
        }
    }

    @Override
    public List<Reference> getReferences(String referenceFieldName) {
        return references.get(referenceFieldName);
    }

    public boolean isReference(String referenceFieldName) {
        return references.containsKey(referenceFieldName);
    }

    public void addReference(Reference reference) throws ClientException {
        reference.setSourceDirectoryName(getName());
        String fieldName = reference.getFieldName();
        List<Reference> fieldRefs;
        if (references.containsKey(fieldName)) {
            fieldRefs = references.get(fieldName);
        } else {
            references.put(fieldName, fieldRefs = new ArrayList<>(1));
        }
        fieldRefs.add(reference);
    }

    public void addReferences(Reference[] references) throws ClientException {
        for (Reference reference : references) {
            addReference(reference);
        }
    }

    @Override
    public Collection<Reference> getReferences() {
        List<Reference> allRefs = new ArrayList<>(2);
        for (List<Reference> refs : references.values()) {
            allRefs.addAll(refs);
        }
        return allRefs;
    }

    /**
     * Helper method to order entries.
     *
     * @param entries the list of entries.
     * @param orderBy an ordered map of field name -> "asc" or "desc".
     */
    public void orderEntries(List<DocumentModel> entries, Map<String, String> orderBy) throws DirectoryException {
        Collections.sort(entries, new DocumentModelComparator(getSchema(), orderBy));
    }

    @Override
    public DirectoryCache getCache() {
        return cache;
    }

    public void removeSession(Session session) {
        sessionCount.dec();
    }

    public void addSession(Session session) {
        sessionCount.inc();
        if (sessionCount.getCount() > sessionMaxCount.getCount()) {
            sessionMaxCount.inc();
        }
    }

    @Override
    public void invalidateDirectoryCache() throws DirectoryException {
        getCache().invalidateAll();
    }

    @Override
    public boolean isMultiTenant() {
        return false;
    }

    @Override
    public void shutdown() {
        sessionCount.dec(sessionCount.getCount());
        sessionMaxCount.dec(sessionMaxCount.getCount());
    }

}
