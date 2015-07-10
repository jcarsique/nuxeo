/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.repository;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.Callable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * A high-level repository descriptor, from which you get a {@link CoreSession} when calling {@link #open}.
 * <p>
 * This is obsolete as an extension point, use org.nuxeo.ecm.core.storage.sql.RepositoryService instead. Descriptor kept
 * for backward-compatibility.
 * <p>
 * Note that this is still use as an object returned by the core api RepositoryManager.
 */
@XObject("repository")
public class Repository {

    @XNode("@name")
    private String name;

    @XNode("@label")
    private String label;

    @XNode("@isDefault")
    private Boolean isDefault;

    /**
     * Factory to used to create the low-level repository.
     */
    private Callable<Object> repositoryFactory;

    public Repository() {
    }

    public Repository(String name, String label, Boolean isDefault, Callable<Object> repositoryFactory) {
        this.name = name;
        this.label = label;
        this.isDefault = isDefault;
        this.repositoryFactory = repositoryFactory;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public Boolean getDefault() {
        return isDefault;
    }

    public boolean isDefault() {
        return Boolean.TRUE.equals(isDefault);
    }

    public Callable<Object> getRepositoryFactory() {
        return repositoryFactory;
    }

    /**
     * @deprecated since 5.9.3, use {@link CoreInstance#openCoreSession} instead.
     */
    @Deprecated
    public CoreSession open() {
        return CoreInstance.openCoreSession(name);
    }

    /**
     * @deprecated since 5.9.3, use {@link CoreInstance#openCoreSession} instead.
     */
    @Deprecated
    public CoreSession open(Map<String, Serializable> context) {
        return CoreInstance.openCoreSession(name, context);
    }

    /**
     * @deprecated since 5.9.3, use {@link CoreSession#close} instead.
     */
    @Deprecated
    public static void close(CoreSession session) {
        session.close();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " {name=" + name + ", label=" + label + '}';
    }

}
