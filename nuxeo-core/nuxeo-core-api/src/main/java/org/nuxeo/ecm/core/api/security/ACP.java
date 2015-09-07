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
 * $Id$
 */

package org.nuxeo.ecm.core.api.security;

import java.io.Serializable;
import java.util.Set;

/**
 * Access control policy (ACP) control the permissions access on a resource.
 * <p>
 * An ACP may contains several ACLs (access control list) identified by names.
 * <p>
 * The list of ACLs is ordered so that when checking permissions the ACL are consulted in an ascending order. (The ACL
 * on position 0 is consulted first).
 * <p>
 * Every ACP has at least one ACL having the reserved name "local". This is the only user editable list (through the
 * security UI).
 * <p>
 * Other ACLs are used internally and are editable only through the API.
 * <p>
 * Also an ACP may have a list named "inherited" that represents the ACLs inherited from the resource parents if any.
 * These ACLs are merged in a single list that is always read only even through the API.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface ACP extends Serializable, Cloneable {

    /**
     * Check whether this ACP grant the given permission on the given user, denies it or doesn't specify a rule.
     * <p>
     * This is checking only the ACLs on that ACP. Parents if any are not checked.
     *
     * @param principal the principal to check
     * @param permission the permission to check
     * @return Access.GRANT if granted, Access.DENY if denied or Access.UNKNOWN if no rule for that permission exists.
     *         Never returns null.
     */
    Access getAccess(String principal, String permission);

    /**
     * Checks the access on the ACLs for each set of the given permissions and principals.
     * <p>
     * This differs for an iterative check using getAccess(String principal, String permission) in the order of checks -
     * so that in this case each ACE is fully checked against the given users and permissions before passing to the next
     * ACE.
     *
     * @param principals
     * @param permissions
     * @return
     */
    Access getAccess(String[] principals, String[] permissions);

    /**
     * Replaces the modifiable user entries (associated with the currentDocument) related to the current ACP.
     * <p>
     * Considers that all the passed entries are modifiable and attempts to set them as local entries related to the
     * current document.
     *
     * @param userEntries
     */
    void setRules(UserEntry[] userEntries);

    /**
     * Replaces the modifiable user entries (associated with the currentDocument) related to the current ACP.
     * <p>
     * Considers that all the passed entries are modifiable and attempts to set them as local entries related to the
     * current document.
     * <p>
     * The current behavior reset <strong>completely</strong> the current ACL.
     *
     * @param userEntries
     * @param overwrite if true, will overwrite the whole current ACL
     */
    void setRules(UserEntry[] userEntries, boolean overwrite);

    /**
     * Replaces the modifiable user entries (associated with the currentDocument) related to the ACP.
     * <p>
     * Considers that all the passed entries are modifiable and attempts to set them as entries related to the current
     * document.
     *
     * @param aclName
     * @param userEntries
     */
    void setRules(String aclName, UserEntry[] userEntries);

    /**
     * Replaces the modifiable user entries (associated with the currentDocument) related to the ACP.
     * <p>
     * Considers that all the passed entries are modifiable and attempts to set them as entries related to the current
     * document.
     *
     * @param aclName
     * @param userEntries
     * @param overwrite if true, will overwrite the whole ACL
     */
    void setRules(String aclName, UserEntry[] userEntries, boolean overwrite);

    /**
     *
     * @param acl
     */
    void addACL(ACL acl);

    void addACL(int pos, ACL acl);

    /**
     * @deprecated since 7.4. Always use {@link #addACL(ACL)} to have correctly ordered acls. To force by-passing the
     *             order, use {@link #addACL(int, ACL)}.
     */
    @Deprecated
    void addACL(String afterMe, ACL acl);

    ACL removeACL(String name);

    ACL getACL(String name);

    ACL[] getACLs();

    ACL getMergedACLs(String name);

    ACL getOrCreateACL(String name);

    ACL getOrCreateACL();

    /**
     * Returns the usernames granted to perform an operation based on a list of permissions.
     *
     * @deprecated Use the method from UserManager service getUsersForPermission instead
     * @param perms the list of permissions.
     * @return a list of usernames
     */
    @Deprecated
    String[] listUsernamesForAnyPermission(Set<String> perms);

    /**
     * Return a recursive copy of the ACP sharing no mutable substructure with the original
     *
     * @return a copy
     */
    ACP clone();

    /**
     * Block the inheritance on the given {@code aclName}.
     *
     * @param username the user blocking the inheritance
     * @return true if the ACP was changed.
     * @since 7.4
     */
    boolean blockInheritance(String aclName, String username);

    /**
     * Unblock the inheritance on the given {@code aclName}.
     *
     * @return true if the ACP was changed.
     * @since 7.4
     */
    boolean unblockInheritance(String aclName);

    /**
     * Add an ACE to the given {@code aclName}.
     *
     * @return true if the ACP was changed.
     * @since 7.4
     */
    boolean addACE(String aclName, ACE ace);

    /**
     * Replace the {@code oldACE} with {@code newACE} on the given {@code aclName}, only if the {@code oldACE} exists.
     * <p>
     * The {@code newACE} keeps the same index as {@code oldACE}.
     *
     * @return true if the ACP was changed.
     * @since 7.4
     */
    boolean replaceACE(String aclName, ACE oldACE, ACE newACE);

    /**
     * Remove an ACE on the given {@code aclName}.
     *
     * @return true if the ACP was changed.
     * @since 7.4
     */
    boolean removeACE(String aclName, ACE ace);

    /**
     * Remove all ACEs for {@code username} on the given {@code aclName}.
     *
     * @return true if the ACP was changed.
     * @since 7.4
     */
    boolean removeACEsByUsername(String aclName, String username);

    /**
     * Remove all ACEs for {@code username} on the whole ACP.
     *
     * @return true if the ACP was changed.
     * @since 7.4
     */
    boolean removeACEsByUsername(String username);

}
