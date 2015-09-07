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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */package org.nuxeo.ecm.automation.core.operations.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.document.AddPermission;
import org.nuxeo.ecm.automation.core.operations.document.BlockPermissionInheritance;
import org.nuxeo.ecm.automation.core.operations.document.UnblockPermissionInheritance;
import org.nuxeo.ecm.automation.core.operations.document.UpdatePermission;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.AdministratorGroupsProvider;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, MockitoFeature.class })
@Deploy("org.nuxeo.ecm.automation.core")
@LocalDeploy("org.nuxeo.ecm.automation.core:test-operations.xml")
public class PermissionAutomationTest {

    @Mock
    @RuntimeService
    protected AdministratorGroupsProvider administratorGroupsProvider;

    protected DocumentModel src;

    @Inject
    CoreSession session;

    @Inject
    AutomationService automationService;

    @Before
    public void initRepo() throws Exception {
        src = session.createDocumentModel("/", "src", "Folder");
        src.setPropertyValue("dc:title", "Source");
        src = session.createDocument(src);
        session.save();
        src = session.getDocument(src.getRef());

        when(administratorGroupsProvider.getAdministratorsGroups()).thenReturn(
                Collections.singletonList("administrators"));
    }

    @Test
    public void canAddPermission() throws OperationException {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        Map<String, Object> params = new HashMap<>();
        params.put("user", "members");
        params.put("permission", "Write");
        GregorianCalendar begin = new GregorianCalendar(2015, Calendar.JUNE, 20, 12, 34, 56);
        params.put("begin", begin);

        GregorianCalendar end = new GregorianCalendar(2015, Calendar.JULY, 14, 12, 34, 56);
        params.put("end", end);

        assertNull(src.getACP().getACL(ACL.LOCAL_ACL));
        automationService.run(ctx, AddPermission.ID, params);
        assertNotNull(src.getACP().getACL(ACL.LOCAL_ACL));
        assertEquals(end, src.getACP().getACL(ACL.LOCAL_ACL).get(0).getEnd());

        // Tear down
        src.getACP().removeACEsByUsername(ACL.LOCAL_ACL, "members");
    }

    @Test
    public void canUpdatePermission() throws OperationException {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);

        // Add permission
        Map<String, Object> params = new HashMap<>();
        params.put("user", "members");
        params.put("permission", "Write");
        automationService.run(ctx, AddPermission.ID, params);
        ctx.setInput(src);

        // Update permission
        params.put("user", "members");
        params.put("permission", "Everything");
        params.put("id", "members:Write:true:Administrator::");

        assertEquals("Write", src.getACP().getACL(ACL.LOCAL_ACL).get(0).getPermission());
        automationService.run(ctx, UpdatePermission.ID, params);
        src.refresh();
        assertEquals("Everything", src.getACP().getACL(ACL.LOCAL_ACL).get(0).getPermission());

        // Tear down
        src.getACP().removeACEsByUsername(ACL.LOCAL_ACL, "members");
    }

    @Test
    public void canBlockPermissionInheritance() throws OperationException {
        ACP acp = src.getACP();
        assertEquals(Access.GRANT, acp.getAccess("members", READ));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        automationService.run(ctx, BlockPermissionInheritance.ID, null);
        src.refresh();
        acp = src.getACP();
        assertEquals(Access.DENY, acp.getAccess("members", READ));
    }

    @Test
    public void canUnblockPermissionInheritance() throws OperationException {
        ACP acp = src.getACP();
        acp.blockInheritance(ACL.LOCAL_ACL, session.getPrincipal().getName());
        assertEquals(Access.DENY, acp.getAccess("members", READ));

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(src);
        automationService.run(ctx, UnblockPermissionInheritance.ID, null);
        src.refresh();
        acp = src.getACP();
        assertEquals(Access.GRANT, acp.getAccess("members", READ));
    }
}
