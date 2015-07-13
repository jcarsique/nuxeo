/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     btatar
 *     Damien METZLER (damien.metzler@leroymerlin.fr)
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.userworkspace.core.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * This component is used to register the service that provide the userworkspace service support.
 *
 * @author btatar
 * @author Damien METZLER (damien.metzler@leroymerlin.fr)
 */
public class UserWorkspaceServiceImplComponent extends DefaultComponent {

    public static final String NAME = "org.nuxeo.ecm.platform.userworkspace.UserWorkspaceService";

    private static final Log log = LogFactory.getLog(UserWorkspaceService.class);

    protected UserWorkspaceDescriptor descriptor;

    protected UserWorkspaceService userWorkspaceService;

    @Override
    public void activate(ComponentContext context) {
        log.info("UserWorkspaceService activated");
    }

    @Override
    public void deactivate(ComponentContext context) {
        log.info("UserWorkspaceService deactivated");
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == UserWorkspaceService.class) {
            return adapter.cast(getUserWorkspaceService());
        }
        return null;
    }

    private UserWorkspaceService getUserWorkspaceService() {
        if (userWorkspaceService == null) {
            Class<?> klass = descriptor.getUserWorkspaceClass();
            if (klass == null) {
                throw new NuxeoException("No class specified for the userWorkspace");
            }
            try {
                userWorkspaceService = (UserWorkspaceService) klass.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new NuxeoException("Failed to instantiate class " + klass, e);
            }
        }
        return userWorkspaceService;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor)
            {
        descriptor = (UserWorkspaceDescriptor) contribution;
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor)
            {
        descriptor = null;
    }

    public String getTargetDomainName() {
        return descriptor.getTargetDomainName();
    }

    public UserWorkspaceDescriptor getConfiguration() {
        return descriptor;
    }

    // for tests only
    public static void reset() {
        UserWorkspaceServiceImplComponent s = (UserWorkspaceServiceImplComponent) Framework.getRuntime().getComponent(
                NAME);
        s.userWorkspaceService = null;
    }

}
