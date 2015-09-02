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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.sql;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryFactory;
import org.nuxeo.ecm.directory.DirectoryServiceImpl;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

public class SQLDirectoryFactory extends DefaultComponent implements DirectoryFactory {

    public static final ComponentName NAME = new ComponentName("org.nuxeo.ecm.directory.sql.SQLDirectoryFactory");

    private static final Log log = LogFactory.getLog(SQLDirectoryFactory.class);

    protected SQLDirectoryRegistry directories;

    @Override
    public Directory getDirectory(String name) throws DirectoryException {
        return directories.getDirectory(name);
    }

    @Override
    public String getName() {
        return NAME.getName();
    }

    @Override
    public void activate(ComponentContext context) {
        directories = new SQLDirectoryRegistry(Framework.getService(DirectoryService.class), this);
    }

    @Override
    public void deactivate(ComponentContext context) {
        directories = null;
    }

    protected static DirectoryServiceImpl getDirectoryService() {
        return (DirectoryServiceImpl) Framework.getRuntime().getComponent(DirectoryService.NAME);
    }

    @Override
    public void registerExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            SQLDirectoryDescriptor descriptor = (SQLDirectoryDescriptor) contrib;
            directories.addContribution(descriptor);
        }
    }

    @Override
    public void unregisterExtension(Extension extension) {
        Object[] contribs = extension.getContributions();
        for (Object contrib : contribs) {
            SQLDirectoryDescriptor descriptor = (SQLDirectoryDescriptor) contrib;
            directories.removeContribution(descriptor);
        }
    }

    @Override
    public void shutdown() throws DirectoryException {
        for (Directory directory : directories.getDirectories()) {
            directory.shutdown();
        }
    }

    @Override
    public List<Directory> getDirectories() throws DirectoryException {
        return new ArrayList<Directory>(directories.getDirectories());
    }

}
