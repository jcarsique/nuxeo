/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.functionaltests.forms;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 * Element representing a select many directory widget.
 *
 * @since 7.4
 */
public class SelectManyDirectoryWidgetElement extends WidgetElement {

    public SelectManyDirectoryWidgetElement(WebDriver driver, String id) {
        super(driver, id);
    }

    @Override
    public void setInput(WebElement elt, String value) {
        Select select = new Select(elt);
        select.deselectAll();
        if (value != null) {
            String[] split = value.split(",");
            for (String v : split) {
                select.selectByVisibleText(v);
            }
        }
    }

}
