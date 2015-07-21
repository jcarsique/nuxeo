/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transaction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.execution.RunDocumentChain;
import org.nuxeo.ecm.automation.core.operations.execution.RunFileChain;
import org.nuxeo.ecm.automation.core.operations.execution.RunOperationOnList;
import org.nuxeo.ecm.automation.core.operations.execution.RunOperationOnListInNewTransaction;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.TransactionalConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@TransactionalConfig(autoStart = false)
@LocalDeploy("org.nuxeo.ecm.automation.core:test-operations.xml")
@RepositoryConfig(cleanup = Granularity.METHOD, init = DefaultRepositoryInit.class)
public class OperationsAndTxTests {

    protected DocumentModel document;

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Before
    public void initRepo() throws Exception {
        document = session.createDocumentModel("/", "src", "Folder");
        document.setPropertyValue("dc:title", "Source");
        document = session.createDocument(document);
        session.save();
        document = session.getDocument(document.getRef());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunOperationOnArrayWithTx() throws Exception {

        TransactionHelper.startTransaction();
        OperationContext ctx = null;
        try {
            service.putOperation(RunOnListItemWithTx.class);
            try {
                ctx = new OperationContext(session);
                String input = "dummyInput";
                ctx.setInput(input);
                String[] groups = new String[3];
                groups[0] = "tic";
                groups[1] = "tac";
                groups[2] = "toc";
                ctx.put("groups", groups);
                OperationChain chain = new OperationChain("testChain");

                // Test with deprecated RunOperationOnListInNewTransaction.ID
                chain.add(RunOperationOnListInNewTransaction.ID).set("list", "groups").set("id", "runOnListItemWithTx").set(
                        "isolate", "false");
                service.run(ctx, chain);
                List<String> result = (List<String>) ctx.get("result");
                List<String> txids = (List<String>) ctx.get("txids");
                List<String> sids = (List<String>) ctx.get("sids");
                List<String> sqlsids = (List<String>) ctx.get("sqlsids");

                assertTrue(result.contains("tic"));
                assertTrue(result.contains("tac"));
                assertTrue(result.contains("toc"));
                assertFalse(txids.get(0).equals(txids.get(1)));
                assertTrue(sids.get(0).equals(sids.get(1)));
                assertTrue(sids.get(2).equals(sids.get(1)));
                assertTrue(sqlsids.get(0).equals(sqlsids.get(1)));
                assertTrue(sqlsids.get(2).equals(sqlsids.get(1)));

                // Same test with RunOperationOnList.ID
                chain.add(RunOperationOnList.ID).set("list", "groups").set("id", "runOnListItemWithTx").set("isolate",
                        "false").set("newTx", "true");
                service.run(ctx, chain);
                result = (List<String>) ctx.get("result");
                txids = (List<String>) ctx.get("txids");
                sids = (List<String>) ctx.get("sids");

                assertTrue(result.contains("tic"));
                assertTrue(result.contains("tac"));
                assertTrue(result.contains("toc"));
                assertFalse(txids.get(0).equals(txids.get(1)));
                assertTrue(sids.get(0).equals(sids.get(1)));
                assertTrue(sids.get(2).equals(sids.get(1)));

            } finally {
                service.removeOperation(RunOnListItemWithTx.class);
            }
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunOperationOnDocumentWithTx() throws Exception {
        TransactionHelper.startTransaction();
        OperationContext ctx = null;
        try {
            service.putOperation(RunOnListItemWithTx.class);
            try {
                // storing in context which session and transaction id is
                // used in main process.
                ctx = new OperationContext(session);
                Transaction tx = TransactionHelper.lookupTransactionManager().getTransaction();
                getOrCreateList(ctx, "sids").add(session.getSessionId());
                getOrCreateList(ctx, "txids").add(tx.toString());
                ctx.setInput(document);
                OperationChain chain = new OperationChain("testChain");
                chain.add(RunDocumentChain.ID).set("id", "runOnListItemWithTx").set("isolate", "false").set("newTx",
                        "true");
                DocumentModel result = (DocumentModel) service.run(ctx, chain);

                // Checking if new transaction id has been registered if same
                // session has been used.
                List<String> txids = (List<String>) ctx.get("txids");
                List<String> sids = (List<String>) ctx.get("sids");

                assertNotNull(result);
                assertFalse(txids.get(0).equals(txids.get(1)));
                assertTrue(sids.get(0).equals(sids.get(1)));
            } finally {
                service.removeOperation(RunOnListItemWithTx.class);
            }
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRunOperationOnBlobWithTx() throws Exception {

        TransactionHelper.startTransaction();
        OperationContext ctx = null;
        try {
            service.putOperation(RunOnListItemWithTx.class);
            try {
                // storing in context which session and transaction id is
                // used in main process.
                ctx = new OperationContext(session);
                Transaction tx = TransactionHelper.lookupTransactionManager().getTransaction();
                getOrCreateList(ctx, "sids").add(session.getSessionId());
                getOrCreateList(ctx, "txids").add(tx.toString());
                Blob blob = Blobs.createBlob("blob");
                ctx.setInput(blob);
                OperationChain chain = new OperationChain("testChain");
                chain.add(RunFileChain.ID).set("id", "runOnListItemWithTx").set("isolate", "false").set("newTx", "true");
                Blob result = (Blob) service.run(ctx, chain);

                // Checking if new transaction id has been registered if same
                // session has been used.
                List<String> txids = (List<String>) ctx.get("txids");
                List<String> sids = (List<String>) ctx.get("sids");

                assertNotNull(result);
                assertFalse(txids.get(0).equals(txids.get(1)));
                assertTrue(sids.get(0).equals(sids.get(1)));
            } finally {
                service.removeOperation(RunOnListItemWithTx.class);
            }
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    @SuppressWarnings("unchecked")
    protected List<String> getOrCreateList(OperationContext ctx, String name) {
        List<String> list = (List<String>) ctx.get(name);
        if (list == null) {
            list = new ArrayList<String>();
            ctx.put(name, list);
        }
        return list;
    }
}
