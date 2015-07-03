/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.coremodel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;

/**
 * This is the {@link Session} factory when the repository is used outside of a datasource.
 * <p>
 * (When repositories are looked up through JNDI, the class org.nuxeo.ecm.core.storage.sql.ra.ConnectionFactoryImpl is
 * used instead of this one.) [suppressed link for solving cycle dependencies in eclipse]
 * <p>
 * This class is constructed by {@link SQLRepositoryFactory}.
 *
 * @author Florent Guillaume
 */
public class SQLRepository implements Repository {

    private static final Log log = LogFactory.getLog(SQLRepository.class);

    public final RepositoryImpl repository;

    private final String name;

    public SQLRepository(RepositoryDescriptor descriptor) {
        repository = new RepositoryImpl(descriptor);
        name = descriptor.name;
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Repository -----
     */

    @Override
    public String getName() {
        return name;
    }

    /*
     * Called by LocalSession.createSession
     */
    @Override
    public Session getSession(String sessionId) throws DocumentException {
        return new SQLSession(repository.getConnection(), this, sessionId);
    }

    @Override
    public void shutdown() {
        try {
            repository.close();
        } catch (NuxeoException e) {
            log.error("Cannot close repository", e);
        }
    }

    @Override
    public int getActiveSessionsCount() {
        return repository.getActiveSessionsCount();
    }

}
