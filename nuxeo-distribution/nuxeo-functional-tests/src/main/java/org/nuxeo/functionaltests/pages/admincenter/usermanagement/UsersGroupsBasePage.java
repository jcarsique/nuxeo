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
 *     Benoit Delbosc
 *     Nelson Silva
 */
package org.nuxeo.functionaltests.pages.admincenter.usermanagement;

import static org.junit.Assert.assertNotNull;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.AjaxRequestManager;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.admincenter.AdminCenterBasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Nuxeo User and Groups Base page. (New one in the admin center)
 *
 * @since 5.4.2
 */
public class UsersGroupsBasePage extends AdminCenterBasePage {

    @FindBy(xpath = "//div[@id=\"nxw_adminCenterSubTabs_panel\"]/ul/li[@class=\"selected\"]/form/a")
    public WebElement selectedTab;

    @Required
    @FindBy(xpath = "//a[@id=\"nxw_UsersManager_form:nxw_UsersManager\"]")
    public WebElement usersTabLink;

    @Required
    @FindBy(xpath = "//a[@id=\"nxw_GroupsManager_form:nxw_GroupsManager\"]")
    public WebElement groupsTabLink;

    protected void clickOnLinkIfNotSelected(WebElement tabLink) {
        assertNotNull(tabLink);
        assertNotNull(selectedTab);

        if (!selectedTab.equals(tabLink)) {
            AjaxRequestManager a = new AjaxRequestManager(driver);
            a.watchAjaxRequests();
            tabLink.click();
            a.waitForAjaxRequests();
        }
    }

    public UsersGroupsBasePage(WebDriver driver) {
        super(driver);
    }

    /**
     * View the Users tab.
     */
    public UsersTabSubPage getUsersTab(boolean force) {
        if (force) {
            AbstractTest.click(usersTabLink);
        } else {
            clickOnLinkIfNotSelected(usersTabLink);
        }
        return asPage(UsersTabSubPage.class);
    }

    public UsersTabSubPage getUsersTab() {
        return getUsersTab(false);
    }

    /**
     * View the Groups tab.
     */
    public GroupsTabSubPage getGroupsTab(boolean force) {
        if (force) {
            groupsTabLink.click();
        } else {
            clickOnLinkIfNotSelected(groupsTabLink);
        }
        return asPage(GroupsTabSubPage.class);
    }

    public GroupsTabSubPage getGroupsTab() {
        return getGroupsTab(false);
    }
}
