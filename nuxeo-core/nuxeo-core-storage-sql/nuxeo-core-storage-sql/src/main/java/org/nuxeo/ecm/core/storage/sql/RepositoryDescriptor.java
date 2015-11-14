/*
 * Copyright (c) 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.runtime.jtajca.NuxeoConnectionManagerConfiguration;

/**
 * Low-level VCS Repository Descriptor.
 */
@XObject(value = "repository")
public class RepositoryDescriptor {

    private static final Log log = LogFactory.getLog(RepositoryDescriptor.class);

    public static final int DEFAULT_READ_ACL_MAX_SIZE = 4096;

    public static final int DEFAULT_PATH_OPTIM_VERSION = 2;

    @XObject(value = "index")
    public static class FulltextIndexDescriptor {

        @XNode("@name")
        public String name;

        @XNode("@analyzer")
        public String analyzer;

        @XNode("@catalog")
        public String catalog;

        /** string or blob */
        @XNode("fieldType")
        public String fieldType;

        @XNodeList(value = "field", type = HashSet.class, componentType = String.class)
        public Set<String> fields = new HashSet<>(0);

        @XNodeList(value = "excludeField", type = HashSet.class, componentType = String.class)
        public Set<String> excludeFields = new HashSet<>(0);

        public FulltextIndexDescriptor() {
        }

        /** Copy constructor. */
        public FulltextIndexDescriptor(FulltextIndexDescriptor other) {
            name = other.name;
            analyzer = other.analyzer;
            catalog = other.catalog;
            fieldType = other.fieldType;
            fields = new HashSet<>(other.fields);
            excludeFields = new HashSet<>(other.excludeFields);
        }

        public static List<FulltextIndexDescriptor> copyList(List<FulltextIndexDescriptor> other) {
            List<FulltextIndexDescriptor> copy = new ArrayList<>(other.size());
            for (FulltextIndexDescriptor fid : other) {
                copy.add(new FulltextIndexDescriptor(fid));
            }
            return copy;
        }

        public void merge(FulltextIndexDescriptor other) {
            if (other.name != null) {
                name = other.name;
            }
            if (other.analyzer != null) {
                analyzer = other.analyzer;
            }
            if (other.catalog != null) {
                catalog = other.catalog;
            }
            if (other.fieldType != null) {
                fieldType = other.fieldType;
            }
            fields.addAll(other.fields);
            excludeFields.addAll(other.excludeFields);
        }
    }

    @XObject(value = "field")
    public static class FieldDescriptor {

        // empty constructor needed by XMap
        public FieldDescriptor() {
        }

        /** Copy constructor. */
        public FieldDescriptor(FieldDescriptor other) {
            type = other.type;
            field = other.field;
            table = other.table;
            column = other.column;
        }

        public static List<FieldDescriptor> copyList(List<FieldDescriptor> other) {
            List<FieldDescriptor> copy = new ArrayList<>(other.size());
            for (FieldDescriptor fd : other) {
                copy.add(new FieldDescriptor(fd));
            }
            return copy;
        }

        public void merge(FieldDescriptor other) {
            if (other.field != null) {
                field = other.field;
            }
            if (other.type != null) {
                type = other.type;
            }
            if (other.table != null) {
                table = other.table;
            }
            if (other.column != null) {
                column = other.column;
            }
        }

        @XNode("@type")
        public String type;

        public String field;

        @XNode("@name")
        public void setName(String name) {
            if (!StringUtils.isBlank(name) && field == null) {
                field = name;
            }
        }

        // compat with older syntax
        @XNode
        public void setXNodeContent(String name) {
            setName(name);
        }

        @XNode("@table")
        public String table;

        @XNode("@column")
        public String column;

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + '(' + field + ",type=" + type + ",table=" + table + ",column="
                    + column + ")";
        }
    }

    /** False if the boolean is null or FALSE, true otherwise. */
    private static boolean defaultFalse(Boolean bool) {
        return Boolean.TRUE.equals(bool);
    }

    /** True if the boolean is null or TRUE, false otherwise. */
    private static boolean defaultTrue(Boolean bool) {
        return !Boolean.FALSE.equals(bool);
    }

    public String name;

    @XNode("@name")
    public void setName(String name) {
        this.name = name;
        pool.setName("repository/" + name);
    }

    @XNode("@label")
    public String label;

    @XNode("@isDefault")
    private Boolean isDefault;

    public Boolean isDefault() {
        return isDefault;
    }

    // compat, when used with old-style extension point syntax
    // and nested repository
    @XNode("repository")
    public RepositoryDescriptor repositoryDescriptor;

    public NuxeoConnectionManagerConfiguration pool = new NuxeoConnectionManagerConfiguration();

    @XNode("pool")
    public void setPool(NuxeoConnectionManagerConfiguration pool) {
        pool.setName("repository/" + name);
        this.pool = pool;
    }

    @XNode("backendClass")
    public Class<? extends RepositoryBackend> backendClass;

    @XNode("clusterInvalidatorClass")
    public Class<? extends ClusterInvalidator> clusterInvalidatorClass;

    @XNode("cachingMapper@class")
    public Class<? extends CachingMapper> cachingMapperClass;

    @XNode("cachingMapper@enabled")
    private Boolean cachingMapperEnabled;

    public boolean getCachingMapperEnabled() {
        return defaultTrue(cachingMapperEnabled);
    }

    @XNodeMap(value = "cachingMapper/property", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> cachingMapperProperties = new HashMap<>();

    @XNode("noDDL")
    private Boolean noDDL;

    public boolean getNoDDL() {
        return defaultFalse(noDDL);
    }

    @XNodeList(value = "sqlInitFile", type = ArrayList.class, componentType = String.class)
    public List<String> sqlInitFiles = new ArrayList<>(0);

    @XNode("softDelete@enabled")
    private Boolean softDeleteEnabled;

    public boolean getSoftDeleteEnabled() {
        return defaultFalse(softDeleteEnabled);
    }

    protected void setSoftDeleteEnabled(boolean enabled) {
        softDeleteEnabled = Boolean.valueOf(enabled);
    }

    @XNode("proxies@enabled")
    private Boolean proxiesEnabled;

    public boolean getProxiesEnabled() {
        return defaultTrue(proxiesEnabled);
    }

    protected void setProxiesEnabled(boolean enabled) {
        proxiesEnabled = Boolean.valueOf(enabled);
    }

    @XNode("idType")
    public String idType; // "varchar", "uuid", "sequence"

    @XNode("clustering@id")
    private String clusterNodeId;

    public String getClusterNodeId() {
        return clusterNodeId;
    }

    @XNode("clustering@enabled")
    private Boolean clusteringEnabled;

    public boolean getClusteringEnabled() {
        return defaultFalse(clusteringEnabled);
    }

    protected void setClusteringEnabled(boolean enabled) {
        clusteringEnabled = Boolean.valueOf(enabled);
    }

    @XNode("clustering@delay")
    private Long clusteringDelay;

    public long getClusteringDelay() {
        return clusteringDelay == null ? 0 : clusteringDelay.longValue();
    }

    protected void setClusteringDelay(long delay) {
        clusteringDelay = Long.valueOf(delay);
    }

    @XNodeList(value = "schema/field", type = ArrayList.class, componentType = FieldDescriptor.class)
    public List<FieldDescriptor> schemaFields = new ArrayList<>(0);

    @XNode("schema/arrayColumns")
    private Boolean arrayColumns;

    public boolean getArrayColumns() {
        return defaultFalse(arrayColumns);
    }

    public void setArrayColumns(boolean enabled) {
        arrayColumns = Boolean.valueOf(enabled);
    }

    @XNode("indexing/fulltext@disabled")
    private Boolean fulltextDisabled;

    public boolean getFulltextDisabled() {
        return defaultFalse(fulltextDisabled);
    }

    public void setFulltextDisabled(boolean disabled) {
        fulltextDisabled = Boolean.valueOf(disabled);
    }

    @XNode("indexing/fulltext@searchDisabled")
    private Boolean fulltextSearchDisabled;

    public boolean getFulltextSearchDisabled() {
        if (getFulltextDisabled()) {
            return true;
        }
        return defaultFalse(fulltextSearchDisabled);
    }

    public void setFulltextSearchDisabled(boolean disabled) {
        fulltextSearchDisabled = Boolean.valueOf(disabled);
    }

    @XNode("indexing/fulltext@analyzer")
    public String fulltextAnalyzer;

    @XNode("indexing/fulltext@parser")
    public String fulltextParser;

    @XNode("indexing/fulltext@catalog")
    public String fulltextCatalog;

    @XNode("indexing/queryMaker@class")
    public void setQueryMakerDeprecated(String klass) {
        log.warn("Setting queryMaker from repository configuration is now deprecated");
    }

    @XNodeList(value = "indexing/fulltext/index", type = ArrayList.class, componentType = FulltextIndexDescriptor.class)
    public List<FulltextIndexDescriptor> fulltextIndexes = new ArrayList<>(0);

    @XNodeList(value = "indexing/excludedTypes/type", type = HashSet.class, componentType = String.class)
    public Set<String> fulltextExcludedTypes = new HashSet<>(0);

    @XNodeList(value = "indexing/includedTypes/type", type = HashSet.class, componentType = String.class)
    public Set<String> fulltextIncludedTypes = new HashSet<>(0);

    // compat
    @XNodeList(value = "indexing/neverPerDocumentFacets/facet", type = HashSet.class, componentType = String.class)
    public Set<String> neverPerInstanceMixins = new HashSet<>(0);

    @XNode("pathOptimizations@enabled")
    private Boolean pathOptimizationsEnabled;

    public boolean getPathOptimizationsEnabled() {
        return defaultTrue(pathOptimizationsEnabled);
    }

    protected void setPathOptimizationsEnabled(boolean enabled) {
        pathOptimizationsEnabled = Boolean.valueOf(enabled);
    }

    /* @since 5.7 */
    @XNode("pathOptimizations@version")
    private Integer pathOptimizationsVersion;

    public int getPathOptimizationsVersion() {
        return pathOptimizationsVersion == null ? DEFAULT_PATH_OPTIM_VERSION : pathOptimizationsVersion.intValue();
    }

    @XNode("aclOptimizations@enabled")
    private Boolean aclOptimizationsEnabled;

    public boolean getAclOptimizationsEnabled() {
        return defaultTrue(aclOptimizationsEnabled);
    }

    protected void setAclOptimizationsEnabled(boolean enabled) {
        aclOptimizationsEnabled = Boolean.valueOf(enabled);
    }

    /* @since 5.4.2 */
    @XNode("aclOptimizations@readAclMaxSize")
    private Integer readAclMaxSize;

    public int getReadAclMaxSize() {
        return readAclMaxSize == null ? DEFAULT_READ_ACL_MAX_SIZE : readAclMaxSize.intValue();
    }

    @XNode("usersSeparator@key")
    public String usersSeparatorKey;

    @XNode("xa-datasource")
    public String xaDataSourceName;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> properties = new HashMap<>();

    public RepositoryDescriptor() {
    }

    /** Copy constructor. */
    public RepositoryDescriptor(RepositoryDescriptor other) {
        name = other.name;
        label = other.label;
        isDefault = other.isDefault;
        pool = other.pool == null ? null : new NuxeoConnectionManagerConfiguration(other.pool);
        backendClass = other.backendClass;
        clusterInvalidatorClass = other.clusterInvalidatorClass;
        cachingMapperClass = other.cachingMapperClass;
        cachingMapperEnabled = other.cachingMapperEnabled;
        cachingMapperProperties = new HashMap<>(other.cachingMapperProperties);
        noDDL = other.noDDL;
        sqlInitFiles = new ArrayList<>(other.sqlInitFiles);
        softDeleteEnabled = other.softDeleteEnabled;
        proxiesEnabled = other.proxiesEnabled;
        schemaFields = FieldDescriptor.copyList(other.schemaFields);
        arrayColumns = other.arrayColumns;
        idType = other.idType;
        clusterNodeId = other.clusterNodeId;
        clusteringEnabled = other.clusteringEnabled;
        clusteringDelay = other.clusteringDelay;
        fulltextDisabled = other.fulltextDisabled;
        fulltextSearchDisabled = other.fulltextSearchDisabled;
        fulltextAnalyzer = other.fulltextAnalyzer;
        fulltextParser = other.fulltextParser;
        fulltextCatalog = other.fulltextCatalog;
        fulltextIndexes = FulltextIndexDescriptor.copyList(other.fulltextIndexes);
        fulltextExcludedTypes = new HashSet<>(other.fulltextExcludedTypes);
        fulltextIncludedTypes = new HashSet<>(other.fulltextIncludedTypes);
        neverPerInstanceMixins = other.neverPerInstanceMixins;
        pathOptimizationsEnabled = other.pathOptimizationsEnabled;
        pathOptimizationsVersion = other.pathOptimizationsVersion;
        aclOptimizationsEnabled = other.aclOptimizationsEnabled;
        readAclMaxSize = other.readAclMaxSize;
        usersSeparatorKey = other.usersSeparatorKey;
        xaDataSourceName = other.xaDataSourceName;
        properties = new HashMap<>(other.properties);
    }

    public void merge(RepositoryDescriptor other) {
        if (other.name != null) {
            name = other.name;
        }
        if (other.label != null) {
            label = other.label;
        }
        if (other.isDefault != null) {
            isDefault = other.isDefault;
        }
        if (other.pool != null) {
            pool = new NuxeoConnectionManagerConfiguration(other.pool);
        }
        if (other.backendClass != null) {
            backendClass = other.backendClass;
        }
        if (other.clusterInvalidatorClass != null) {
            clusterInvalidatorClass = other.clusterInvalidatorClass;
        }
        if (other.cachingMapperClass != null) {
            cachingMapperClass = other.cachingMapperClass;
        }
        if (other.cachingMapperEnabled != null) {
            cachingMapperEnabled = other.cachingMapperEnabled;
        }
        cachingMapperProperties.putAll(other.cachingMapperProperties);
        if (other.noDDL != null) {
            noDDL = other.noDDL;
        }
        sqlInitFiles.addAll(other.sqlInitFiles);
        if (other.softDeleteEnabled != null) {
            softDeleteEnabled = other.softDeleteEnabled;
        }
        if (other.proxiesEnabled != null) {
            proxiesEnabled = other.proxiesEnabled;
        }
        if (other.idType != null) {
            idType = other.idType;
        }
        if (other.clusterNodeId != null) {
            clusterNodeId = other.clusterNodeId;
        }
        if (other.clusteringEnabled != null) {
            clusteringEnabled = other.clusteringEnabled;
        }
        if (other.clusteringDelay != null) {
            clusteringDelay = other.clusteringDelay;
        }
        for (FieldDescriptor of : other.schemaFields) {
            boolean append = true;
            for (FieldDescriptor f : schemaFields) {
                if (f.field.equals(of.field)) {
                    f.merge(of);
                    append = false;
                    break;
                }
            }
            if (append) {
                schemaFields.add(of);
            }
        }
        if (other.arrayColumns != null) {
            arrayColumns = other.arrayColumns;
        }
        if (other.fulltextDisabled != null) {
            fulltextDisabled = other.fulltextDisabled;
        }
        if (other.fulltextSearchDisabled != null) {
            fulltextSearchDisabled = other.fulltextSearchDisabled;
        }
        if (other.fulltextAnalyzer != null) {
            fulltextAnalyzer = other.fulltextAnalyzer;
        }
        if (other.fulltextParser != null) {
            fulltextParser = other.fulltextParser;
        }
        if (other.fulltextCatalog != null) {
            fulltextCatalog = other.fulltextCatalog;
        }
        for (FulltextIndexDescriptor oi : other.fulltextIndexes) {
            boolean append = true;
            for (FulltextIndexDescriptor i : fulltextIndexes) {
                if (ObjectUtils.equals(i.name, oi.name)) {
                    i.merge(oi);
                    append = false;
                    break;
                }
            }
            if (append) {
                fulltextIndexes.add(oi);
            }
        }
        fulltextExcludedTypes.addAll(other.fulltextExcludedTypes);
        fulltextIncludedTypes.addAll(other.fulltextIncludedTypes);
        neverPerInstanceMixins.addAll(other.neverPerInstanceMixins);
        if (other.pathOptimizationsEnabled != null) {
            pathOptimizationsEnabled = other.pathOptimizationsEnabled;
        }
        if (other.pathOptimizationsVersion != null) {
            pathOptimizationsVersion = other.pathOptimizationsVersion;
        }
        if (other.aclOptimizationsEnabled != null) {
            aclOptimizationsEnabled = other.aclOptimizationsEnabled;
        }
        if (other.readAclMaxSize != null) {
            readAclMaxSize = other.readAclMaxSize;
        }
        if (other.usersSeparatorKey != null) {
            usersSeparatorKey = other.usersSeparatorKey;
        }
        if (other.xaDataSourceName != null) {
            xaDataSourceName = other.xaDataSourceName;
        }
        properties.putAll(other.properties);
    }

}
