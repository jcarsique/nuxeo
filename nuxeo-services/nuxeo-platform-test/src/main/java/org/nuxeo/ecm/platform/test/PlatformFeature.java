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
 *     bstefanescu
 */
package org.nuxeo.ecm.platform.test;

import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.directory.sql.SQLDirectoryFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Deploy({ "org.nuxeo.ecm.platform.api", "org.nuxeo.ecm.platform.content.template", "org.nuxeo.ecm.platform.dublincore",
        "org.nuxeo.ecm.platform.usermanager.api", "org.nuxeo.ecm.platform.usermanager", "org.nuxeo.ecm.core.io",
        "org.nuxeo.ecm.platform.query.api", "org.nuxeo.ecm.platform.test:test-usermanagerimpl/directory-config.xml" })
@Features({ CoreFeature.class, SQLDirectoryFeature.class })
public class PlatformFeature extends SimpleFeature {

}
