/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.query.sql;

/**
 * This defines the constants for NXQL queries.
 *
 * @author Florent Guillaume
 */
public class NXQL {

    // constant utility class
    private NXQL() {
    }

    /** The NXQL query type. */
    public static final String NXQL = "NXQL";

    public static final String ECM_PREFIX = "ecm:";

    public static final String ECM_UUID = "ecm:uuid";

    public static final String ECM_PATH = "ecm:path";

    public static final String ECM_NAME = "ecm:name";

    public static final String ECM_POS = "ecm:pos";

    public static final String ECM_PARENTID = "ecm:parentId";

    public static final String ECM_MIXINTYPE = "ecm:mixinType";

    public static final String ECM_PRIMARYTYPE = "ecm:primaryType";

    public static final String ECM_ISPROXY = "ecm:isProxy";

    public static final String ECM_ISVERSION = "ecm:isVersion";

    /** @since 5.7.3 */
    public static final String ECM_ISVERSION_OLD = "ecm:isCheckedInVersion";

    public static final String ECM_LIFECYCLESTATE = "ecm:currentLifeCycleState";

    public static final String ECM_VERSIONLABEL = "ecm:versionLabel";

    public static final String ECM_FULLTEXT = "ecm:fulltext";

    public static final String ECM_FULLTEXT_JOBID = "ecm:fulltextJobId";

    /** @since 6.0 */
    public static final String ECM_FULLTEXT_SCORE = "ecm:fulltextScore";

    /** @deprecated since 5.4.2, use {@link #ECM_LOCK_OWNER} instead */
    @Deprecated
    public static final String ECM_LOCK = "ecm:lock";

    /** @since 5.4.2 */
    public static final String ECM_LOCK_OWNER = "ecm:lockOwner";

    /** @since 5.4.2 */
    public static final String ECM_LOCK_CREATED = "ecm:lockCreated";

    /** @since 5.7 */
    public static final String ECM_TAG = "ecm:tag";

    /** @since 5.7 */
    public static final String ECM_PROXY_TARGETID = "ecm:proxyTargetId";

    /** @since 5.7 */
    public static final String ECM_PROXY_VERSIONABLEID = "ecm:proxyVersionableId";

    /** @since 5.7.3 */
    public static final String ECM_ISCHECKEDIN = "ecm:isCheckedIn";

    /** @since 5.7.3 */
    public static final String ECM_ISLATESTVERSION = "ecm:isLatestVersion";

    /** @since 5.7.3 */
    public static final String ECM_ISLATESTMAJORVERSION = "ecm:isLatestMajorVersion";

    /** @since 5.7.3 */
    public static final String ECM_VERSIONCREATED = "ecm:versionCreated";

    /** @since 5.7.3 */
    public static final String ECM_VERSIONDESCRIPTION = "ecm:versionDescription";

    /** @since 5.7.3 */
    public static final String ECM_VERSION_VERSIONABLEID = "ecm:versionVersionableId";

    /** @since 6.0 */
    public static final String ECM_ANCESTORID = "ecm:ancestorId";

    /** @since 6.0-HF06, 7.2 */
    public static final String ECM_ACL = "ecm:acl";

    /**
     * Suffix for ecm:acl, like in {@code ecm:acl/}{@code *}{@code /principal}
     *
     * @since 6.0-HF06, 7.2
     */
    public static final String ECM_ACL_PRINCIPAL = "principal";

    /**
     * Suffix for ecm:acl, like in {@code ecm:acl/}{@code *}{@code /permission}
     *
     * @since 6.0-HF06, 7.2
     */
    public static final String ECM_ACL_PERMISSION = "permission";

    /**
     * Suffix for ecm:acl, like in {@code ecm:acl/}{@code *}{@code /grant}
     *
     * @since 6.0-HF06, 7.2
     */
    public static final String ECM_ACL_GRANT = "grant";

    /**
     * Suffix for ecm:acl, like in {@code ecm:acl/}{@code *}{@code /name}
     *
     * @since 6.0-HF06, 7.2
     */
    public static final String ECM_ACL_NAME = "name";

    /**
     * Suffix for ecm:acl, like in {@code ecm:acl/}{@code *}{@code /pos}
     *
     * @since 6.0-HF06, 7.2
     */
    public static final String ECM_ACL_POS = "pos";

    /**
     * Suffix for ecm:acl, like in {@code ecm:acl/}{@code *}{@code /creator}
     *
     * @since 7.4
     */
    public static final String ECM_ACL_CREATOR = "creator";

    /**
     * Suffix for ecm:acl, like in {@code ecm:acl/}{@code *}{@code /begin}
     *
     * @since 7.4
     */
    public static final String ECM_ACL_BEGIN = "begin";

    /**
     * Suffix for ecm:acl, like in {@code ecm:acl/}{@code *}{@code /end}
     *
     * @since 7.4
     */
    public static final String ECM_ACL_END = "end";

    /**
     * Escapes a string into a single-quoted string for NXQL.
     * <p>
     * Any single quote or backslash characters are escaped with a backslash.
     *
     * @param s the string to escape
     * @return the escaped string
     * @since 5.7, 5.6.0-HF08
     */
    public static String escapeString(String s) {
        return "'" + escapeStringInner(s) + "'";
    }

    /**
     * Escapes a string (assumed to be single quoted) for NXQL.
     * <p>
     * Any single quote or backslash characters are escaped with a backslash.
     *
     * @param s the string to escape
     * @return the escaped string, without external quotes
     * @since 5.7, 5.6.0-HF08
     */
    public static String escapeStringInner(String s) {
        // backslash -> backslash backslash
        // quote -> backslash quote
        return s.replaceAll("\\\\", "\\\\\\\\").replaceAll("'", "\\\\'");
    }

}
