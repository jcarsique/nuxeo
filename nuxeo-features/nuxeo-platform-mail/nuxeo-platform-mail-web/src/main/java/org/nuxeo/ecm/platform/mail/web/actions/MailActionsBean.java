/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.mail.web.actions;

import static org.nuxeo.ecm.platform.mail.utils.MailCoreConstants.MAIL_FOLDER_TYPE;
import static org.nuxeo.ecm.platform.mail.web.utils.MailWebConstants.CURRENT_PAGE;

import java.io.Serializable;

import javax.mail.MessagingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.mail.utils.MailCoreHelper;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * Handles mail actions.
 *
 * @author Catalin Baican
 */
@Name("mailActions")
@Scope(ScopeType.CONVERSATION)
public class MailActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(MailActionsBean.class);

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    public String checkCurrentInbox() {
        DocumentModel mailFolder = navigationContext.getCurrentDocument();

        try {
            MailCoreHelper.checkMail(mailFolder, documentManager);
        } catch (MessagingException e) {
            log.debug(e, e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get("feedback.check.mail.error") + e.getMessage());

            return CURRENT_PAGE;
        }

        facesMessages.add(StatusMessage.Severity.INFO,
                resourcesAccessor.getMessages().get("feedback.check.mail.success"));
        Events.instance().raiseEvent("documentChildrenChanged");

        return CURRENT_PAGE;
    }

    public boolean isMailFolder() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return MAIL_FOLDER_TYPE.equals(currentDocument.getType());
    }

}
