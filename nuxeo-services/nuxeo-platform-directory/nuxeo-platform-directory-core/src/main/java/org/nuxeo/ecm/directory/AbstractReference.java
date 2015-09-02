/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.directory;

import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of common Reference logic.
 *
 * @author ogrisel
 */
public abstract class AbstractReference implements Reference {

    protected String sourceDirectoryName;

    protected String targetDirectoryName;

    protected String fieldName;

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public Directory getSourceDirectory() throws DirectoryException {
        return Framework.getService(DirectoryService.class).getDirectory(sourceDirectoryName);
    }

    @Override
    public void setSourceDirectoryName(String sourceDirectoryName) {
        this.sourceDirectoryName = sourceDirectoryName;
    }

    @Override
    public Directory getTargetDirectory() throws DirectoryException {
        return Framework.getService(DirectoryService.class).getDirectory(targetDirectoryName);
    }

    @Override
    public void setTargetDirectoryName(String targetDirectoryName) {
        this.targetDirectoryName = targetDirectoryName;
    }


    /**
     * @since 5.6
     */
    @Override
    public AbstractReference clone() {
        AbstractReference clone = newInstance();
        clone.sourceDirectoryName = sourceDirectoryName;
        clone.targetDirectoryName = targetDirectoryName;
        clone.fieldName = fieldName;
        return clone;
    }

    /**
     * Override to instantiate sub class, used in {@link #clone()} method
     *
     * @since 5.6
     */
    protected abstract AbstractReference newInstance();

}
