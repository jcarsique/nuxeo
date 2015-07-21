/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.core.test;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.SimpleFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Binder;

/**
 * The core feature provides deployments needed to have a nuxeo core running. Several annotations can be used:
 * <ul>
 * <li>FIXME
 * <li>FIXME
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Deploy({ "org.nuxeo.runtime.management", //
        "org.nuxeo.ecm.core.schema", //
        "org.nuxeo.ecm.core.query", //
        "org.nuxeo.ecm.core.api", //
        "org.nuxeo.ecm.core.event", //
        "org.nuxeo.ecm.core", //
        "org.nuxeo.ecm.core.mimetype", //
        "org.nuxeo.ecm.core.convert", //
        "org.nuxeo.ecm.core.convert.plugins", //
        "org.nuxeo.ecm.core.storage", //
        "org.nuxeo.ecm.core.storage.sql", //
        "org.nuxeo.ecm.core.storage.sql.test" //
})
@Features({ RuntimeFeature.class, TransactionalFeature.class })
public class CoreFeature extends SimpleFeature {

    private static final Log log = LogFactory.getLog(CoreFeature.class);

    protected int initialOpenSessions;

    protected RepositorySettings repository;

    protected boolean cleaned;

    protected StorageConfiguration storageConfiguration = new StorageConfiguration();

    public StorageConfiguration getStorageConfiguration() {
        return storageConfiguration;
    }

    public RepositorySettings getRepository() {
        return repository;
    }

    public BackendType getBackendType() {
        return repository.getBackendType();
    }

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        repository = new RepositorySettings(runner);
        runner.getFeature(RuntimeFeature.class).addServiceProvider(repository);
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        repository.initialize();
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(RepositorySettings.class).toInstance(repository);
    }

    @Override
    public void beforeRun(FeaturesRunner runner) throws Exception {
        // wait for async tasks that may have been triggered by
        // RuntimeFeature (typically repo initialization)
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        final CoreInstance core = CoreInstance.getInstance();
        initialOpenSessions = core.getNumberOfSessions();
        if (initialOpenSessions != 0) {
            log.error(String.format("There are already %s open session(s) before running tests.",
                    Integer.valueOf(initialOpenSessions)));
            for (CoreInstance.RegistrationInfo info : core.getRegistrationInfos()) {
                log.warn("Leaking session", info);
            }
        }
        if (repository.getGranularity() != Granularity.METHOD) {
            initializeSession(runner);
        }
    }

    @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
        waitForAsyncCompletion(); // fulltext and various workers
        if (repository.getGranularity() != Granularity.METHOD) {
            cleanupSession(runner);
        }
        repository.shutdown();

        final CoreInstance core = CoreInstance.getInstance();
        int finalOpenSessions = core.getNumberOfSessions();
        int leakedOpenSessions = finalOpenSessions - initialOpenSessions;
        if (leakedOpenSessions > 0) {
            log.error(String.format("There are %s open session(s) at tear down; it seems "
                    + "the test leaked %s session(s).", Integer.valueOf(finalOpenSessions),
                    Integer.valueOf(leakedOpenSessions)));
        }
    }

    @Override
    public void beforeSetup(FeaturesRunner runner) throws Exception {
        if (repository.getGranularity() == Granularity.METHOD) {
            initializeSession(runner);
        }
    }

    @Override
    public void afterTeardown(FeaturesRunner runner) throws Exception {
        if (repository.getGranularity() == Granularity.METHOD) {
            cleanupSession(runner);
        }
    }

    protected void waitForAsyncCompletion() {
        boolean tx = TransactionHelper.isTransactionActive();
        boolean rb = TransactionHelper.isTransactionMarkedRollback();
        if (tx || rb) {
            // there may be afterCommit work pending, so we
            // have to commit the transaction
            TransactionHelper.commitOrRollbackTransaction();
        }
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        if (tx || rb) {
            // restore previous tx status
            TransactionHelper.startTransaction();
            if (rb) {
                TransactionHelper.setTransactionRollbackOnly();
            }
        }
    }

    protected void cleanupSession(FeaturesRunner runner) {
        waitForAsyncCompletion();
        if (TransactionHelper.isTransactionMarkedRollback()) { // ensure tx is
                                                               // active
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        CoreSession session = repository.getSession();
        if (session == null) {
            session = repository.createSession();
        }
        try {
            log.trace("remove everything except root");
            session.removeChildren(new PathRef("/"));
            log.trace("remove orphan versions as OrphanVersionRemoverListener is not triggered by CoreSession#removeChildren");
            String rootDocumentId = session.getRootDocument().getId();
            IterableQueryResult results = session.queryAndFetch("SELECT ecm:uuid FROM Document, Relation", NXQL.NXQL);
            for (Map<String, Serializable> result : results) {
                String uuid = result.get("ecm:uuid").toString();
                if (rootDocumentId != uuid) {
                    try {
                        session.removeDocument(new IdRef(uuid));
                    } catch (DocumentNotFoundException e) {
                        // could have unknown type in db, ignore
                    }
                }
            }
            results.close();
            session.save();
            waitForAsyncCompletion();
            if (!session.query("SELECT * FROM Document, Relation").isEmpty()) {
                log.error("Fail to cleanupSession, repository will not be empty for the next test.");
            }
        } catch (NuxeoException e) {
            log.error("Unable to reset repository", e);
        } finally {
            CoreScope.INSTANCE.exit();
        }
        repository.releaseSession();
        cleaned = true;
        CoreInstance.getInstance().cleanupThisThread();
    }

    protected void initializeSession(FeaturesRunner runner) throws Exception {
        if (cleaned) {
            // re-trigger application started
            RepositoryService repositoryService = Framework.getLocalService(RepositoryService.class);
            repositoryService.applicationStarted(null);
            cleaned = false;
        }
        CoreScope.INSTANCE.enter();
        CoreSession session = repository.createSession();
        if (session == null) {
            throw new AssertionError("Cannot open session");
        }
        RepositoryInit factory = repository.getInitializer();
        if (factory != null) {
            factory.populate(session);
            session.save();
            waitForAsyncCompletion();
        }
    }

    public void setRepositorySettings(RepositorySettings settings) {
        repository.importSettings(settings);
    }

}
