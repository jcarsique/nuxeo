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
package org.nuxeo.ecm.automation.server.jaxrs;

import java.io.IOException;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.ConflictOperationException;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationNotFoundException;
import org.nuxeo.ecm.automation.jaxrs.io.operations.ExecutionRequest;
import org.nuxeo.ecm.automation.server.AutomationServer;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.ExceptionHelper;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.runtime.api.Framework;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.core.Context;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class ExecutableResource {

    @Context
    protected HttpServletRequest request;

    protected AutomationService service;

    protected ExecutableResource(AutomationService service) {
        this.service = service;
    }

    public CoreSession getCoreSession() {
        return SessionFactory.getSession(request);
    }

    @POST
    public Object doPost(@Context HttpServletRequest request, ExecutionRequest xreq) {
        this.request = request;
        try {
            AutomationServer srv = Framework.getLocalService(AutomationServer.class);
            if (!srv.accept(getId(), isChain(), request)) {
                return ResponseHelper.notFound();
            }
            Object result = execute(xreq);
            int customHttpStatus = xreq.getRestOperationContext().getHttpStatus();
            if (customHttpStatus >= 100) {
                return ResponseHelper.getResponse(result, request, customHttpStatus);
            }
            return ResponseHelper.getResponse(result, request);
        } catch (OperationException | NuxeoException | SecurityException | MessagingException | IOException cause) {
            if (cause instanceof ConflictOperationException) {
                throw WebException.newException("Failed to invoke operation: " + getId(), cause,
                        HttpServletResponse.SC_CONFLICT);
            } else if (cause instanceof OperationNotFoundException) {
                throw WebException.newException("Failed to invoke operation: " + getId(), cause,
                        HttpServletResponse.SC_NOT_FOUND);
            } else {
                Throwable unWrapException = ExceptionHelper.unwrapException(cause);
                if (unWrapException instanceof RestOperationException) {
                    int customHttpStatus = ((RestOperationException) unWrapException).getStatus();
                    throw WebException.newException("Failed to invoke operation: " + getId(), cause, customHttpStatus);
                }
                throw WebException.newException("Failed to invoke operation: " + getId(), cause);
            }
        }
    }

    public abstract String getId();

    public abstract Object execute(ExecutionRequest req) throws OperationException;

    public abstract boolean isChain();

}
