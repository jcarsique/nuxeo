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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.contentview.jsf.facelets.plugins;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlSelectManyCheckbox;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagConfig;

import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.ui.web.renderer.NxSelectManyCheckboxListRenderer;

/**
 * @since 6.0
 */
public class SelectManyCheckboxDirectoryAggregateWidgetTypeHandler extends SelectDirectoryAggregateWidgetTypeHandler {

    public SelectManyCheckboxDirectoryAggregateWidgetTypeHandler(TagConfig config) {
        super(config);
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent, Widget widget) throws WidgetException, IOException {
        apply(ctx, parent, widget, HtmlSelectManyCheckbox.COMPONENT_TYPE,
                NxSelectManyCheckboxListRenderer.RENDERER_TYPE);
    }

}
