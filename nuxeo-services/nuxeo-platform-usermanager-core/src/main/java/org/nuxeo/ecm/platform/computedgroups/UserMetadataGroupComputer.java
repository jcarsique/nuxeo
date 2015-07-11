/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Benjamin Jalon
 */

package org.nuxeo.ecm.platform.computedgroups;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;

/**
 * Configurable Group Computer based on metadata of the user.
 *
 * @since 5.7.3
 */
public class UserMetadataGroupComputer extends AbstractGroupComputer {

    public static final Log log = LogFactory.getLog(UserMetadataGroupComputer.class);

    private String groupPattern;

    private String xpath;

    public UserMetadataGroupComputer(String xpath, String groupPattern) {
        this.xpath = xpath;
        this.groupPattern = groupPattern;

        if (xpath == null || xpath.isEmpty() || groupPattern == null || groupPattern.isEmpty()) {
            throw new NuxeoException("Bad configuration");
        }

    }

    @Override
    public List<String> getAllGroupIds() {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getGroupMembers(String groupId) {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getGroupsForUser(NuxeoPrincipalImpl user) {
        String value = (String) user.getModel().getPropertyValue(xpath);

        if (value == null || "".equals(value.trim())) {
            return new ArrayList<String>();
        }

        ArrayList<String> result = new ArrayList<String>();
        result.add(String.format(groupPattern, value));

        return result;
    }

    @Override
    public List<String> getParentsGroupNames(String arg0) {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getSubGroupsNames(String arg0) {
        return new ArrayList<String>();
    }

    @Override
    public boolean hasGroup(String groupId) {
        return false;
    }
}
