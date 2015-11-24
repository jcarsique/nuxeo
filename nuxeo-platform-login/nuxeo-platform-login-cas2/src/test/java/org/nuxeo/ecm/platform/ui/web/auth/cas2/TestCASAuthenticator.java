/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Academie de Rennes - proxy CAS support
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.auth.cas2;

import javax.security.auth.login.LoginContext;
import javax.servlet.ServletException;

import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.simple.AbstractAuthenticator;

/**
 * @author Benjamin JALON
 */
public class TestCASAuthenticator extends AbstractAuthenticator {

    protected static final String CAS_USER = "CasUser";

    protected String TICKET_KEY = "ticket";
    
    public TestCASAuthenticator() {
        super();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.platform.login.cas2");
        deployContrib("org.nuxeo.ecm.platform.login.cas2.test",
                "OSGI-INF/login-yes-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.login.cas2.test",
                "OSGI-INF/login-cas-contrib.xml");
    }

    public void testCASAuthentication() throws Exception {

        initRequest();
        doAuthentificationToCasServer(CAS_USER);

        naf.doFilter(request, response, chain);

        String loginError = (String) request.getAttribute(NXAuthConstants.LOGIN_ERROR);
        LoginContext loginContext = (LoginContext) request.getAttribute("org.nuxeo.ecm.login.context");
        assertNull(loginError);
        assertNotNull(loginContext);
        assertEquals(
                CAS_USER,
                ((UserPrincipal) loginContext.getSubject().getPrincipals().toArray()[0]).getName());
    }

    /**
     * TODO : create a random number for the ticket, add it to the
     * MockServiceValidators and associate this ticket to the username
     * 
     * @throws ServletException
     */
    protected void doAuthentificationToCasServer(String username)
            throws ServletException {
        String casTicket = username;
        request.setParameter(TICKET_KEY, new String[] { casTicket, });
    }

}
