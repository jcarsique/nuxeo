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
 * $Id: ListWidgetTypeHandler.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets.plugins;

import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.ui.web.runtime.JSFConfigurationService;
import org.nuxeo.runtime.api.Framework;

/**
 * List widget type, using a fixed template.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class ListWidgetTypeHandler extends TemplateWidgetTypeHandler {

    private static final long serialVersionUID = 6886289896957398368L;

    public static final String COMPAT_TEMPLATE_PROPERTY_NAME = "compatTemplate";

    @Override
    protected String getTemplateValue(Widget widget) {
        JSFConfigurationService configurationService = Framework.getService(JSFConfigurationService.class);
        boolean useCompat = configurationService.isBooleanPropertyTrue("nuxeo.jsf.listWidget.compatEnabled");
        if (useCompat) {
            return lookupProperty(COMPAT_TEMPLATE_PROPERTY_NAME, widget);
        } else {
            return super.getTemplateValue(widget);
        }
    }

}
