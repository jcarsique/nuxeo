/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.user.invite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserConfig;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

public class DefaultInvitationUserFactory implements InvitationUserFactory {

    private static final Log log = LogFactory.getLog(DefaultInvitationUserFactory.class);

    public static final String PASSWORD_KEY = "invitationPassword";

    @Override
    public void doPostUserCreation(CoreSession session, DocumentModel registrationDoc, NuxeoPrincipal user)
            throws UserRegistrationException {
        // Nothing to do in the default implementation
    }

    @Override
    public NuxeoPrincipal doCreateUser(CoreSession session, DocumentModel registrationDoc,
            UserRegistrationConfiguration configuration) throws UserRegistrationException {
        UserManager userManager = Framework.getService(UserManager.class);

        String email = (String) registrationDoc.getPropertyValue(configuration.getUserInfoEmailField());
        if (email == null) {
            throw new UserRegistrationException("Email address must be specififed");
        }

        String login = (String) registrationDoc.getPropertyValue(configuration.getUserInfoUsernameField());
        NuxeoPrincipal user = userManager.getPrincipal(login);
        if (user == null) {
            DocumentModel newUserDoc = userManager.getBareUserModel();
            newUserDoc.setPropertyValue(UserConfig.USERNAME_COLUMN, login);
            newUserDoc.setPropertyValue(UserConfig.PASSWORD_COLUMN,
                    registrationDoc.getContextData(PASSWORD_KEY));
            newUserDoc.setPropertyValue(UserConfig.FIRSTNAME_COLUMN,
                    registrationDoc.getPropertyValue(configuration.getUserInfoFirstnameField()));
            newUserDoc.setPropertyValue(UserConfig.LASTNAME_COLUMN,
                    registrationDoc.getPropertyValue(configuration.getUserInfoLastnameField()));
            newUserDoc.setPropertyValue(UserConfig.EMAIL_COLUMN,
                    registrationDoc.getPropertyValue(configuration.getUserInfoEmailField()));
            newUserDoc.setPropertyValue(UserConfig.COMPANY_COLUMN,
                    registrationDoc.getPropertyValue(configuration.getUserInfoCompanyField()));
            newUserDoc.setPropertyValue(UserConfig.GROUPS_COLUMN,
                    registrationDoc.getPropertyValue(configuration.getUserInfoGroupsField()));
            userManager.createUser(newUserDoc);
            user = userManager.getPrincipal(login);

            log.info("New user created:" + user.getName());
        } else {
            if (!email.equals(((NuxeoPrincipalImpl) user).getEmail())) {
                throw new UserRegistrationException("This login is not available");
            }
        }
        return user;
    }
}
