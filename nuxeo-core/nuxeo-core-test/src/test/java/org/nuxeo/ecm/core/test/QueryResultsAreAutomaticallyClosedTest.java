package org.nuxeo.ecm.core.test;

import javax.inject.Inject;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.storage.sql.ra.ConnectionImpl;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, LogCaptureFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class)
@LogCaptureFeature.FilterWith(QueryResultsAreAutomaticallyClosedTest.LogFilter.class)
public class QueryResultsAreAutomaticallyClosedTest {

    public static class LogFilter implements LogCaptureFeature.Filter {
        @Override
        public boolean accept(LoggingEvent event) {
            if (!Level.WARN.equals(event.getLevel())) {
                return false;
            }
            if (!ConnectionImpl.class.getName().equals(event.getLoggerName())) {
                return false;
            }
            if (!ConnectionImpl.QueryResultContextException.class.isAssignableFrom(event.getThrowableInformation().getThrowable().getClass())) {
                return false;
            }
            return true;
        }
    }

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected LogCaptureFeature.Result logCaptureResults;

    @Test
    public void testWithoutTransaction() throws Exception {
        TransactionHelper.commitOrRollbackTransaction();
        IterableQueryResult results;
        try (CoreSession session = coreFeature.openCoreSessionSystem()) {
            results = session.queryAndFetch("SELECT * from Document", "NXQL");
        }
        TransactionHelper.startTransaction();
        Assert.assertFalse(results.isLife());
        logCaptureResults.assertHasEvent();
    }

    // needs a JCA connection for this to work
    @Test
    public void testTransactional() throws Exception {
        try (CoreSession session = coreFeature.openCoreSessionSystem()) {
            IterableQueryResult results = session.queryAndFetch("SELECT * from Document", "NXQL");
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
            logCaptureResults.assertHasEvent();
            Assert.assertFalse(results.isLife());
        }
    }

    protected static class NestedQueryRunner extends UnrestrictedSessionRunner {

        public NestedQueryRunner(String reponame) {
            super(reponame);
        }

        protected IterableQueryResult result;

        @Override
        public void run() {
            result = session.queryAndFetch("SELECT * from Document", "NXQL");
        }

    }

    @Test
    public void testNested() throws Exception {
        IterableQueryResult mainResults;
        try (CoreSession main = coreFeature.openCoreSessionSystem()) {
            NestedQueryRunner runner = new NestedQueryRunner(main.getRepositoryName());
            mainResults = main.queryAndFetch("SELECT * from Document", "NXQL");
            runner.runUnrestricted();
            Assert.assertFalse(runner.result.isLife());
            Assert.assertTrue(mainResults.isLife());
            logCaptureResults.assertHasEvent();
            logCaptureResults.clear();
        }
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        logCaptureResults.assertHasEvent();
        Assert.assertFalse(mainResults.isLife());
    }
}
