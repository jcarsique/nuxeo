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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Repository descriptor.
 *
 * @author Florent Guillaume
 */
@XObject(value = "repository")
public class RepositoryDescriptor {

    private static final Log log = LogFactory.getLog(RepositoryDescriptor.class);

    public RepositoryDescriptor() {
        super();
    }

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
        public Set<String> fields;

        @XNodeList(value = "excludeField", type = HashSet.class, componentType = String.class)
        public Set<String> excludeFields;
    }

    @XObject(value = "field")
    public static class FieldDescriptor {
        // empty constructor needed by XMap
        public FieldDescriptor() {
        }

        @XNode("@type")
        public String type;

        @XNode("@name")
        public String field;

        public void setName(String name) {
            if (!StringUtils.isBlank(name) && field == null) {
                this.field = name;
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
            return this.getClass().getSimpleName() + '(' + field + ",type="
                    + type + ",table=" + table + ",column=" + column + ")";
        }
    }

    @XObject(value = "server")
    public static class ServerDescriptor {
        @XNode("@disabled")
        public boolean disabled;

        @XNode("host")
        public String host = "localhost";

        @XNode("port")
        public int port = 8181;

        @XNode("path")
        public String path = "/nuxeo";

        public String getUrl() {
            return "http://" + host + ":" + port
                    + (path.startsWith("/") ? "" : "/") + path;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + '(' + getUrl() + ')';
        }
    }

    @XNode("@name")
    public String name;

    @XNode("listen")
    public ServerDescriptor listen;

    @XNodeList(value = "connect", type = ArrayList.class, componentType = ServerDescriptor.class)
    public List<ServerDescriptor> connect = Collections.emptyList();

    @XNode("backendClass")
    public Class<? extends RepositoryBackend> backendClass;

    @XNode("cachingMapper@class")
    public Class<? extends CachingMapper> cachingMapperClass;

    @XNode("cachingMapper@enabled")
    public boolean cachingMapperEnabled = true;

    @XNodeMap(value = "cachingMapper/property", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> cachingMapperProperties = new HashMap<String, String>();

    @XNode("noDDL")
    public boolean noDDL = false;

    @XNodeList(value = "sqlInitFile", type = ArrayList.class, componentType = String.class)
    public List<String> sqlInitFiles;

    @XNode("softDelete@enabled")
    public boolean softDeleteEnabled;

    @XNode("proxies@enabled")
    public boolean proxiesEnabled = true;

    @XNode("idType")
    public String idType; // "varchar", "uuid", "sequence"

    @XNode("clustering@enabled")
    public boolean clusteringEnabled;

    @XNode("clustering@delay")
    public long clusteringDelay;

    @XNodeList(value = "schema/field", type = ArrayList.class, componentType = FieldDescriptor.class)
    public List<FieldDescriptor> schemaFields = Collections.emptyList();

    @XNode("schema/arrayColumns")
    public boolean arrayColumns;

    @XNode("indexing/fulltext@disabled")
    public boolean fulltextDisabled;

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
    public List<FulltextIndexDescriptor> fulltextIndexes;

    @XNodeList(value = "indexing/excludedTypes/type", type =  HashSet.class, componentType = String.class)
    public Set<String> fulltextExcludedTypes;

    @XNodeList(value = "indexing/includedTypes/type", type =  HashSet.class, componentType = String.class)
    public Set<String> fulltextIncludedTypes;

    @XNodeList(value = "indexing/neverPerDocumentFacets/facet", type = HashSet.class, componentType = String.class)
    public Set<String> neverPerInstanceMixins;

    @XNode("pathOptimizations@enabled")
    public boolean pathOptimizationsEnabled = true;

    /* @since 5.7 */
    @XNode("pathOptimizations@version")
    public int pathOptimizationsVersion = 2;

    @XNode("aclOptimizations@enabled")
    public boolean aclOptimizationsEnabled = true;

    /* @since 5.7 */
    @XNode("aclOptimizations@concurrentUpdate")
    public boolean aclOptimizationsConcurrentUpdate = true;

    /* @since 5.4.2 */
    @XNode("aclOptimizations@readAclMaxSize")
    public int readAclMaxSize = 4096;

    @XNode("binaryManager@class")
    public Class<? extends BinaryManager> binaryManagerClass;

    @XNode("binaryManager@key")
    public String binaryManagerKey;

    @XNode("binaryManager@listen")
    public boolean binaryManagerListen;

    @XNode("binaryManager@connect")
    public boolean binaryManagerConnect;

    @XNode("binaryStore@path")
    public String binaryStorePath;

    @XNode("@sendInvalidationEvents")
    public boolean sendInvalidationEvents;

    /** Merges only non-JCA properties. */
    public void mergeFrom(RepositoryDescriptor other) {
        listen = other.listen;
        connect = other.connect;
        backendClass = other.backendClass;
        cachingMapperClass = other.cachingMapperClass;
        cachingMapperEnabled = other.cachingMapperEnabled;
        clusteringEnabled = other.clusteringEnabled;
        clusteringDelay = other.clusteringDelay;
        noDDL = other.noDDL;
        sqlInitFiles = other.sqlInitFiles;
        softDeleteEnabled = other.softDeleteEnabled;
        proxiesEnabled = other.proxiesEnabled;
        schemaFields = other.schemaFields;
        arrayColumns = other.arrayColumns;
        fulltextDisabled = other.fulltextDisabled;
        fulltextAnalyzer = other.fulltextAnalyzer;
        fulltextCatalog = other.fulltextCatalog;
        fulltextIndexes = other.fulltextIndexes;
        neverPerInstanceMixins = other.neverPerInstanceMixins;
        pathOptimizationsEnabled = other.pathOptimizationsEnabled;
        pathOptimizationsVersion = other.pathOptimizationsVersion;
        aclOptimizationsEnabled = other.aclOptimizationsEnabled;
        aclOptimizationsConcurrentUpdate = other.aclOptimizationsConcurrentUpdate;
        readAclMaxSize = other.readAclMaxSize;
        binaryStorePath = other.binaryStorePath;
        binaryManagerClass = other.binaryManagerClass;
        binaryManagerKey = other.binaryManagerKey;
        binaryManagerListen = other.binaryManagerListen;
        binaryManagerConnect = other.binaryManagerConnect;
        sendInvalidationEvents = other.sendInvalidationEvents;
        usersSeparatorKey = other.usersSeparatorKey;
    }

    @XNode("xa-datasource")
    public String xaDataSourceName;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> properties;

    @XNode("usersSeparator@key")
    public String usersSeparatorKey;

}
