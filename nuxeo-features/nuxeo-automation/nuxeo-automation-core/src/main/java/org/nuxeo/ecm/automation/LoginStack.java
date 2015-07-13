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
package org.nuxeo.ecm.automation;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class LoginStack {

    protected List<Entry> stack = new ArrayList<Entry>();

    protected CoreSession originalSession;

    protected CoreSession currentSession;

    public LoginStack(CoreSession session) {
        setSession(session);
    }

    public void setSession(CoreSession session) {
        originalSession = session;
        currentSession = session;
    }

    /**
     * Get the current session
     *
     * @return
     */
    public CoreSession getSession() {
        return currentSession;
    }

    public void push(LoginContext lc) {
        Entry entry = new Entry(lc);
        String repositoryName;
        if (originalSession != null) {
            repositoryName = originalSession.getRepositoryName();
        } else {
            repositoryName = null; // default repository
        }
        entry.session = CoreInstance.openCoreSession(repositoryName);
        currentSession = entry.session;
        stack.add(entry);
    }

    public Entry peek() {
        if (!stack.isEmpty()) {
            return stack.get(stack.size() - 1);
        }
        return null;
    }

    /**
     * Remove the current login context from the stack.
     * <p>
     * If no login context in in the stack nothing is done. If the login context has an associated CoreSession the
     * session will be destroyed and the previous session is restored as the active session of the operation context.
     */
    public void pop() throws OperationException {
        if (!stack.isEmpty()) {
            Entry entry = stack.remove(stack.size() - 1);
            entry.dispose();
            entry = peek();
            if (entry != null) {
                currentSession = entry.session;
            } else {
                currentSession = originalSession;
            }
            if (currentSession != null) {
                refreshSession(currentSession);
            }
        }
    }

    protected void refreshSession(CoreSession session) throws OperationException {
        if (session != null && !session.isStateSharedByAllThreadSessions()) {
            // this will indirectly process refresh the session
            session.save();
        }
    }

    /**
     * Remove the stacked logins if any. This is called when chain execution is done.
     */
    protected void clear() throws OperationException {
        if (!stack.isEmpty()) {
            for (int i = stack.size() - 1; i > -1; i--) {
                stack.get(i).dispose();
            }
            stack.clear();
            currentSession = originalSession;
            if (currentSession != null) {
                refreshSession(currentSession);
            }
            stack.clear();
        }
    }

    public static class Entry {
        public LoginContext lc;

        public CoreSession session;

        public Entry(LoginContext lc) {
            this(lc, null);
        }

        public Entry(LoginContext lc, CoreSession session) {
            this.lc = lc;
            this.session = session;
        }

        public final boolean hasSession() {
            return session != null;
        }

        public final void dispose() throws OperationException {
            try {
                if (session != null) {
                    try {
                        session.save();
                    } finally {
                        session.close();
                    }
                }
            } finally {
                try {
                    session = null;
                    lc.logout();
                    lc = null;
                } catch (LoginException e) {
                    throw new OperationException(e);
                }
            }
        }
    }

}
