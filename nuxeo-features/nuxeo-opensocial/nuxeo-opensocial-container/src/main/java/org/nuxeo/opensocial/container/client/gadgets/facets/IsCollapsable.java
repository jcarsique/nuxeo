/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.client.gadgets.facets;

import org.nuxeo.opensocial.container.client.event.priv.app.portlet.CollapsePortletEvent;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.UncollapsePortletEvent;
import org.nuxeo.opensocial.container.client.gadgets.facets.api.Facet;
import org.nuxeo.opensocial.container.client.presenter.AppPresenter;

/**
 * @author Stéphane Fourrier
 */
public class IsCollapsable extends Facet {
    public IsCollapsable() {
        super(AppPresenter.containerConstants.fold(), "facet-collapse", new CollapsePortletEvent(),
                AppPresenter.containerConstants.unfold(), "facet-collapsed", new UncollapsePortletEvent());
    }
}