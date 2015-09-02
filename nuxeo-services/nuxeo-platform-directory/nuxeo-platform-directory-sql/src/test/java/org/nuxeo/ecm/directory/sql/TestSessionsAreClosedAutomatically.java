package org.nuxeo.ecm.directory.sql;

import javax.inject.Inject;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogCaptureFeature.NoLogCaptureFilterException;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ LogCaptureFeature.class, CoreFeature.class, SQLDirectoryFeature.class })
@LocalDeploy({ "org.nuxeo.ecm.directory:test-sql-directories-schema-override.xml",
        "org.nuxeo.ecm.directory.sql:test-sql-directories-bundle.xml" })
@LogCaptureFeature.FilterWith(TestSessionsAreClosedAutomatically.CloseSessionFilter.class)
public class TestSessionsAreClosedAutomatically {

    public static class CloseSessionFilter implements LogCaptureFeature.Filter {

        @Override
        public boolean accept(LoggingEvent event) {
            if (!SQLDirectory.class.getName().equals(event.getLogger().getName())) {
                return false;
            }
            if (!Level.WARN.equals(event.getLevel())) {
                return false;
            }
            String msg = event.getMessage().toString();
            if (!msg.startsWith("Closing a sql directory session")) {
                return false;
            }
            return true;
        }

    }

    protected Directory userDirectory;

    protected @Inject LogCaptureFeature.Result caughtEvents;

    @Before
    public void fetchUserDirectory() throws DirectoryException {
        userDirectory = Framework.getService(DirectoryService.class).getDirectory("userDirectory");
        Assert.assertNotNull(userDirectory);
    }

    @Before
    public void resetContext() {
        TransactionHelper.commitOrRollbackTransaction();
    }

    @After
    public void restoreContext() {
        if (TransactionHelper.isNoTransaction()) {
            TransactionHelper.startTransaction();
        }
    }

    @Test
    public void hasNoWarns() throws DirectoryException, NoLogCaptureFilterException {
        TransactionHelper.startTransaction();
        try {
            try (Session session = userDirectory.getSession()) {
                // do nothing
            }
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        Assert.assertTrue(caughtEvents.getCaughtEvents().isEmpty());
    }

    @Test
    public void hasWarnsOnCommit() throws DirectoryException, NoLogCaptureFilterException {
        TransactionHelper.startTransaction();
        try {
            Session session = userDirectory.getSession();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        caughtEvents.assertHasEvent();
    }

    @Test
    public void hasWarnsOnRollback() throws DirectoryException, NoLogCaptureFilterException {
        TransactionHelper.startTransaction();
        try {
            Session session = userDirectory.getSession();
        } finally {
            TransactionHelper.setTransactionRollbackOnly();
            TransactionHelper.commitOrRollbackTransaction();
        }
        caughtEvents.assertHasEvent();
    }
}
