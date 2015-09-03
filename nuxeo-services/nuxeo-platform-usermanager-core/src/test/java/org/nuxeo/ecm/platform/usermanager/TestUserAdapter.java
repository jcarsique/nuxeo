/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     tmartins
 */
package org.nuxeo.ecm.platform.usermanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @author Thierry Martins
 */
@LocalDeploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/directory-config.xml")
public class TestUserAdapter extends UserManagerTestCase {

    @Test
    public void testAdministratorModel() throws Exception {
        DocumentModel userModel = userManager.getUserModel("Administrator");
        UserAdapter userAdapter = userModel.getAdapter(UserAdapter.class);

        List<String> groups = new ArrayList<String>();
        groups.add("administrators");

        assertEquals("Administrator", userAdapter.getName());
        assertEquals("devnull@nuxeo.com", userAdapter.getEmail());
        assertEquals("user", userAdapter.getSchemaName());
        assertEquals(groups, userAdapter.getGroups());
        assertTrue(StringUtils.isEmpty(userAdapter.getFirstName()));
        assertTrue(StringUtils.isEmpty(userAdapter.getLastName()));
        assertTrue(StringUtils.isEmpty(userAdapter.getCompany()));
    }

}