/*
 * Copyright (c) 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.dbs;

import static java.lang.Boolean.FALSE;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Provides sharing behavior for repository sessions and other basic functions.
 *
 * @since 5.9.4
 */
public abstract class DBSRepositoryBase implements DBSRepository {

    public static final String TYPE_ROOT = "Root";

    // change to have deterministic pseudo-UUID generation for debugging
    protected final boolean DEBUG_UUIDS = false;

    private static final String UUID_ZERO = "00000000-0000-0000-0000-000000000000";

    private static final String UUID_ZERO_DEBUG = "UUID_0";

    protected final String repositoryName;

    protected final boolean fulltextDisabled;

    protected final BlobManager blobManager;

    public DBSRepositoryBase(String repositoryName, boolean fulltextDisabled) {
        this.repositoryName = repositoryName;
        this.fulltextDisabled = fulltextDisabled;
        blobManager = Framework.getService(BlobManager.class);
    }

    @Override
    public void shutdown() {
    }

    @Override
    public String getName() {
        return repositoryName;
    }

    /**
     * Initializes the root and its ACP.
     */
    public void initRoot() {
        Session session = getSession();
        Document root = session.importDocument(getRootId(), null, "", TYPE_ROOT, new HashMap<String, Serializable>());
        ACLImpl acl = new ACLImpl();
        acl.add(new ACE(SecurityConstants.ADMINISTRATORS, SecurityConstants.EVERYTHING, true));
        acl.add(new ACE(SecurityConstants.ADMINISTRATOR, SecurityConstants.EVERYTHING, true));
        acl.add(new ACE(SecurityConstants.MEMBERS, SecurityConstants.READ, true));
        ACPImpl acp = new ACPImpl();
        acp.addACL(acl);
        session.setACP(root, acp, true);
        session.save();
        session.close();
        if (TransactionHelper.isTransactionActive()) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

    @Override
    public String getRootId() {
        return DEBUG_UUIDS ? UUID_ZERO_DEBUG : UUID_ZERO;
    }

    @Override
    public BlobManager getBlobManager() {
        return blobManager;
    }

    @Override
    public boolean isFulltextDisabled() {
        return fulltextDisabled;
    }

    @Override
    public int getActiveSessionsCount() {
        return 0;
    }

    @Override
    public Session getSession() {
        Transaction transaction;
        try {
            transaction = TransactionHelper.lookupTransactionManager().getTransaction();
            if (transaction != null && transaction.getStatus() != Status.STATUS_ACTIVE) {
                transaction = null;
            }
        } catch (SystemException | NamingException e) {
            transaction = null;
        }

        if (transaction == null) {
            // no active transaction, use a regular session
            return newSession();
        }

        TransactionContext context = transactionContexts.get(transaction);
        if (context == null) {
            context = new TransactionContext(transaction, newSession());
            context.init();
        }
        return context.newSession();
    }

    protected DBSSession newSession() {
        return new DBSSession(this);
    }

    public Map<Transaction, TransactionContext> transactionContexts = new ConcurrentHashMap<>();

    /**
     * Context maintained during a transaction, holding the base session used, and all session proxy handles that have
     * been returned to callers.
     */
    public class TransactionContext implements Synchronization {

        protected final Transaction transaction;

        protected final DBSSession baseSession;

        protected final Set<Session> proxies;

        public TransactionContext(Transaction transaction, DBSSession baseSession) {
            this.transaction = transaction;
            this.baseSession = baseSession;
            proxies = new HashSet<>();
        }

        public void init() {
            transactionContexts.put(transaction, this);
            // make sure it's closed (with handles) at transaction end
            try {
                transaction.registerSynchronization(this);
            } catch (RollbackException | SystemException e) {
                throw new RuntimeException(e);
            }
        }

        public Session newSession() {
            ClassLoader cl = getClass().getClassLoader();
            DBSSessionInvoker invoker = new DBSSessionInvoker(this);
            Session proxy = (Session) Proxy.newProxyInstance(cl, new Class[] { Session.class }, invoker);
            add(proxy);
            return proxy;
        }

        public void add(Session proxy) {
            proxies.add(proxy);
        }

        public boolean remove(Object proxy) {
            return proxies.remove(proxy);
        }

        @Override
        public void beforeCompletion() {
            baseSession.commit();
        }

        @Override
        public void afterCompletion(int status) {
            baseSession.close();
            for (Session proxy : proxies.toArray(new Session[0])) {
                proxy.close(); // so that users of the session proxy see it's not live anymore
            }
            transactionContexts.remove(transaction);
        }
    }

    /**
     * An indirection to a base {@link DBSSession} intercepting {@code close()} to not close the base session until the
     * transaction itself is closed.
     */
    public static class DBSSessionInvoker implements InvocationHandler {

        private static final String METHOD_HASHCODE = "hashCode";

        private static final String METHOD_EQUALS = "equals";

        private static final String METHOD_CLOSE = "close";

        private static final String METHOD_ISLIVE = "isLive";

        protected final TransactionContext context;

        protected boolean closed;

        public DBSSessionInvoker(TransactionContext context) {
            this.context = context;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if (methodName.equals(METHOD_HASHCODE)) {
                return doHashCode();
            }
            if (methodName.equals(METHOD_EQUALS)) {
                return doEquals(args);
            }
            if (methodName.equals(METHOD_CLOSE)) {
                return doClose(proxy);
            }
            if (methodName.equals(METHOD_ISLIVE)) {
                return doIsLive();
            }

            if (closed) {
                throw new NuxeoException("Cannot use closed connection handle");
            }

            try {
                return method.invoke(context.baseSession, args);
            } catch (ReflectiveOperationException e) {
                throw ExceptionUtils.unwrapInvoke(e);
            }
        }

        protected Integer doHashCode() {
            return Integer.valueOf(this.hashCode());
        }

        protected Boolean doEquals(Object[] args) {
            if (args.length != 1 || args[0] == null) {
                return FALSE;
            }
            Object other = args[0];
            if (!(Proxy.isProxyClass(other.getClass()))) {
                return FALSE;
            }
            InvocationHandler otherInvoker = Proxy.getInvocationHandler(other);
            return Boolean.valueOf(this.equals(otherInvoker));
        }

        protected Object doClose(Object proxy) {
            closed = true;
            context.remove(proxy);
            return null;
        }

        protected Boolean doIsLive() {
            if (closed) {
                return FALSE;
            } else {
                return Boolean.valueOf(context.baseSession.isLive());
            }
        }
    }

}
