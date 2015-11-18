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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: DirectorySelectOneWidgetTypeHandler.java 30416 2008-02-21 19:10:37Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets.plugins;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributes;
import javax.faces.view.facelets.TagConfig;

import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.ui.web.component.seam.UIHtmlText;
import org.nuxeo.ecm.platform.ui.web.directory.DirectoryEntryOutputComponent;

import com.sun.faces.facelets.tag.TagAttributesImpl;

/**
 * Select one directory widget
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class DirectorySelectOneWidgetTypeHandler extends AbstractDirectorySelectWidgetTypeHandler {

    public DirectorySelectOneWidgetTypeHandler(TagConfig config) {
        super(config);
    }

    protected String getEditComponentType() {
        return HtmlSelectOneMenu.COMPONENT_TYPE;
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent, Widget widget) throws WidgetException, IOException {
        String mode = widget.getMode();
        if (BuiltinWidgetModes.EDIT.equals(mode)) {
            super.apply(ctx, parent, widget, getEditComponentType());
            return;
        }

        FaceletHandlerHelper helper = new FaceletHandlerHelper(tagConfig);
        FaceletHandler leaf = getNextHandler(ctx, tagConfig, widget, null, helper);
        String widgetId = widget.getId();
        String widgetTagConfigId = widget.getTagConfigId();
        TagAttributes attributes;
        if (BuiltinWidgetModes.isLikePlainMode(mode)) {
            // use attributes without id
            attributes = helper.getTagAttributes(widget);
        } else {
            attributes = helper.getTagAttributes(widgetId, widget);
        }
        ComponentHandler output = helper.getHtmlComponentHandler(widgetTagConfigId, attributes, leaf,
                DirectoryEntryOutputComponent.COMPONENT_TYPE, null);
        if (BuiltinWidgetModes.PDF.equals(mode)) {
            // add a surrounding p:html tag handler
            FaceletHandler h = helper.getHtmlComponentHandler(widgetTagConfigId, new TagAttributesImpl(new TagAttribute[0]),
                    output, UIHtmlText.class.getName(), null);
            h.apply(ctx, parent);
        } else {
            output.apply(ctx, parent);
        }
    }
}
