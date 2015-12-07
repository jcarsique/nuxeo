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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.functionaltests.pages;

import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 5.9.3
 */
public class HomePage extends DocumentBasePage {

    @Required
    @FindBy(id = "nxw_homeTabs_panel")
    protected WebElement menu;

    public HomePage(WebDriver driver) {
        super(driver);
    }

    public CollectionsPage goToCollections() {
        if (useAjaxTabs()) {
            AjaxRequestManager arm = new AjaxRequestManager(driver);
            arm.begin();
            menu.findElement(By.linkText("Collections")).click();
            arm.end();
        } else {
            menu.findElement(By.linkText("Collections")).click();
        }
        return asPage(CollectionsPage.class);
    }

}
