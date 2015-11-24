/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.usermapper.service;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.usermapper.extension.UserMapper;

/**
 * This service allows to map Nuxeo Users with users coming from external system like SSO or IDM.
 *
 * @author tiry
 * @since 7.4
 */
public interface UserMapperService {

    /**
     * Should retrieve (create if needed) and update the NuxeoPrincipal according to the given userObject
     *
     * @param mappingName the name of the contributed mapping to use
     * @param userObject the native userObject
     * @return the matching {@link NuxeoPrincipal}
     * @throws NuxeoException
     */
    NuxeoPrincipal getOrCreateAndUpdateNuxeoPrincipal(String mappingName, Object userObject) throws NuxeoException;

    /**
     * Should retrieve (create if needed) and update the NuxeoPrincipal according to the given userObject
     *
     * @param mappingName the name of the contributed mapping to use
     * @param userObject the native userObject
     * @param createIfNeeded flag to allow creation (default is true)
     * @param update flag to run update (default is true)
     * @return the matching {@link NuxeoPrincipal}
     * @throws NuxeoException
     */
    NuxeoPrincipal getOrCreateAndUpdateNuxeoPrincipal(String mappingName, Object userObject, boolean createIfNeeded,
            boolean update, Map<String, Serializable> params) throws NuxeoException;

    /**
     * Wrap the {@link NuxeoPrincipal} as the userObject used in the external authentication system *
     *
     * @param mappingName the name of the contributed mapping to use
     * @param principal the {@link NuxeoPrincipal} to wrap
     * @param nativePrincipal the principal Object in the target system (can be null)
     * @throws NuxeoException
     */
    Object wrapNuxeoPrincipal(String mappingName, NuxeoPrincipal principal, Object nativePrincipal,
            Map<String, Serializable> params) throws NuxeoException;

    /**
     * Gives access to the contributed Mapping names
     */
    Set<String> getAvailableMappings();

    /**
     * returns the named mapper is any
     *
     * @param mappingName
     */
    UserMapper getMapper(String mappingName) throws NuxeoException;
}
