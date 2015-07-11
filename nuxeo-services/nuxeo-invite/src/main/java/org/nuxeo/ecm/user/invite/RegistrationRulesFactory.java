/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.user.invite;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

/**
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
public class RegistrationRulesFactory implements DocumentAdapterFactory {

    protected static final String REGISTRATION_CONFIG_FACET = "RegistrationConfiguration";

    @Override
    public Object getAdapter(DocumentModel doc, Class<?> itf) {
        if (doc.hasFacet(REGISTRATION_CONFIG_FACET)) {
            return new RegistrationRules(doc);
        }
        return null;
    }
}
