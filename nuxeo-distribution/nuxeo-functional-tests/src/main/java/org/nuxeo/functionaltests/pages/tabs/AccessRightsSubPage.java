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
 *     Sun Seng David TAN <stan@nuxeo.com>
 *     Antoine Taillefer
 */
package org.nuxeo.functionaltests.pages.tabs;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.forms.Select2WidgetElement;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
import org.openqa.selenium.support.pagefactory.ByChained;
import org.openqa.selenium.support.ui.Select;

/**
 * @author Sun Seng David TAN <stan@nuxeo.com>
 * @deprecated since 7.10. Use {@link PermissionsSubPage} instead.
 */
@Deprecated
public class AccessRightsSubPage extends AbstractPage {

    private static final Log log = LogFactory.getLog(AccessRightsSubPage.class);

    /*
     * @Required
     * @FindBy(id = "add_rights_form:nxl_user_group_suggestion:nxw_selection_suggest") WebElement
     * userSelectionSuggestInputText;
     */

    @Required
    @FindBy(id = "add_rights_form:rights_permission_select")
    WebElement selectPermissionElement;

    @Required
    @FindBy(id = "add_rights_form:rights_add_button")
    WebElement addButton;

    @FindBy(id = "validate_rights:document_rights_validate_button")
    WebElement validateButton;

    @Required
    @FindBys({ @FindBy(id = "block_inherit"), @FindBy(xpath = "//input[@type='checkbox']") })
    WebElement blockInherit;

    public AccessRightsSubPage(WebDriver driver) {
        super(driver);
    }

    public AccessRightsSubPage blockInheritance() {
        blockInherit.click();
        return asPage(AccessRightsSubPage.class);
    }

    public boolean hasPermissionForUser(String permission, String username) {
        List<WebElement> trElements = driver.findElements(new ByChained(By.className("dataOutput"), By.tagName("tr")));
        boolean hasPermission = false;
        for (WebElement trElement : trElements) {
            List<WebElement> tds = trElement.findElements(By.tagName("td"));
            if (tds.size() > 3) {
                String aceUsername = tds.get(1).getText();
                String aceGrantedPerm = tds.get(2).getText();
                String aceDeniedPerm = tds.get(3).getText();

                if (username.equals(aceUsername)) {
                    if (aceGrantedPerm.equals(permission) || "Manage everything".equals(aceGrantedPerm)) {
                        hasPermission = true;
                    } else {
                        if (aceDeniedPerm.equals(permission) || "Manage everything".equals(aceDeniedPerm)) {
                            hasPermission = false;
                        }
                    }
                }

            }

        }

        // get all the ace

        return hasPermission;
    }

    /**
     * @deprecated use {@link #grantPermissionForUser} unless negative ACL are enabled.
     */
    @Deprecated
    public AccessRightsSubPage addPermissionForUser(String username, String permission, boolean grant) {

        boolean allowNegativeACL = hasElement(By.id("add_rights_form:rights_grant_select"));

        if (!allowNegativeACL) {
            if (grant) {
                log.warn("addPermissionForUser with negative ACL disabled is deprecated.");
                return grantPermissionForUser(permission, username);
            } else {
                throw new UnsupportedOperationException("Negative ACL are currently disabled!");
            }
        }

        WebElement selectGrantElement = driver.findElement(By.id("add_rights_form:rights_grant_select"));

        Select2WidgetElement userSelection = new Select2WidgetElement(
                driver,
                driver.findElement(By.xpath("//*[@id='s2id_add_rights_form:nxl_user_group_suggestion:nxw_selection_select2']")),
                true);
        userSelection.selectValue(username);

        Select selectGrant = new Select(selectGrantElement);

        if (grant) {
            selectGrant.selectByValue("Grant");

        } else {
            selectGrant.selectByValue("Deny");
        }

        Select selectPermission = new Select(selectPermissionElement);
        selectPermission.selectByVisibleText(permission);

        addButton.click();

        return asPage(AccessRightsSubPage.class).saveChanges();
    }

    /**
     * @since 6.0
     */
    public AccessRightsSubPage grantPermissionForUser(String permission, String username) {

        Select2WidgetElement userSelection = new Select2WidgetElement(
                driver,
                driver.findElement(By.xpath("//*[@id='s2id_add_rights_form:nxl_user_group_suggestion:nxw_selection_select2']")),
                true);
        userSelection.selectValue(username);

        Select selectPermission = new Select(selectPermissionElement);
        selectPermission.selectByVisibleText(permission);

        addButton.click();

        return asPage(AccessRightsSubPage.class).saveChanges();
    }

    public AccessRightsSubPage saveChanges() {
        waitUntilEnabled(validateButton);
        validateButton.click();
        return asPage(AccessRightsSubPage.class);
    }

}
