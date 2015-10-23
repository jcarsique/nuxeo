/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.reload;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.jar.Manifest;

import javax.transaction.Transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.JarUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.DefaultServiceProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServiceProvider;
import org.nuxeo.runtime.deployment.preprocessor.DeploymentPreprocessor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ReloadComponent extends DefaultComponent implements ReloadService {

    private static final Log log = LogFactory.getLog(ReloadComponent.class);

    protected static Bundle bundle;

    protected Long lastFlushed;

    public static BundleContext getBundleContext() {
        return bundle.getBundleContext();
    }

    public static Bundle getBundle() {
        return bundle;
    }

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        bundle = context.getRuntimeContext().getBundle();
    }

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
        bundle = null;
    }

    @Override
    public void reload() {
        if (log.isDebugEnabled()) {
            log.debug("Starting reload");
        }
        try {
            reloadProperties();
        } catch (IOException e) {
            throw new RuntimeServiceException(e);
        }
        triggerReloadWithNewTransaction(RELOAD_EVENT_ID);
    }

    @Override
    public void reloadProperties() throws IOException {
        log.info("Reload runtime properties");
        Framework.getRuntime().reloadProperties();
    }

    @Override
    public void reloadRepository() {
        log.info("Reload repository");
        triggerReloadWithNewTransaction(RELOAD_REPOSITORIES_ID);
    }

    @Override
    public void reloadSeamComponents() {
        log.info("Reload Seam components");
        triggerReload(RELOAD_SEAM_EVENT_ID);
    }

    @Override
    public void flush() {
        log.info("Flush caches");
        triggerReloadWithNewTransaction(FLUSH_EVENT_ID);
    }

    @Override
    public void flushJaasCache() {
        log.info("Flush the JAAS cache");
        Framework.getLocalService(EventService.class).sendEvent(
                new Event("usermanager", "user_changed", this, "Deployer"));
        setFlushedNow();
    }

    @Override
    public void flushSeamComponents() {
        log.info("Flush Seam components");
        triggerReload(FLUSH_SEAM_EVENT_ID);
    }

    @Override
    public String deployBundle(File file) throws BundleException {
        return deployBundle(file, false);
    }

    @Override
    public String deployBundle(File file, boolean reloadResourceClasspath) throws BundleException {
        String name = getOSGIBundleName(file);
        if (name == null) {
            log.error(
                    String.format("No Bundle-SymbolicName found in MANIFEST for jar at '%s'", file.getAbsolutePath()));
            return null;
        }

        String path = file.getAbsolutePath();

        log.info(String.format("Before deploy bundle for file at '%s'\n" + "%s", path, getRuntimeStatus()));

        if (reloadResourceClasspath) {
            URL url;
            try {
                url = new File(path).toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            Framework.reloadResourceLoader(Arrays.asList(url), null);
        }

        // check if this is a bundle first
        Bundle newBundle = getBundleContext().installBundle(path);
        if (newBundle == null) {
            throw new IllegalArgumentException("Could not find a valid bundle at path: " + path);
        }
        Transaction tx = TransactionHelper.suspendTransaction();
        try {
            newBundle.start();
        } finally {
            TransactionHelper.resumeTransaction(tx);
        }

        log.info(String.format("Deploy done for bundle with name '%s'.\n" + "%s", newBundle.getSymbolicName(),
                getRuntimeStatus()));

        return newBundle.getSymbolicName();
    }

    @Override
    public void undeployBundle(File file, boolean reloadResources) throws BundleException {
        String name = getOSGIBundleName(file);
        String path = file.getAbsolutePath();
        if (name == null) {
            log.error(String.format("No Bundle-SymbolicName found in MANIFEST for jar at '%s'", path));
            return;
        }

        undeployBundle(name);

        if (reloadResources) {
            URL url;
            try {
                url = new File(path).toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            Framework.reloadResourceLoader(null, Arrays.asList(url));
        }
    }

    @Override
    public void undeployBundle(String bundleName) throws BundleException {
        if (bundleName == null) {
            // ignore
            return;
        }
        log.info(String.format("Before undeploy bundle with name '%s'.\n" + "%s", bundleName, getRuntimeStatus()));
        BundleContext ctx = getBundleContext();
        ServiceReference ref = ctx.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin srv = (PackageAdmin) ctx.getService(ref);
        try {
            for (Bundle b : srv.getBundles(bundleName, null)) {
                if (b != null && b.getState() == Bundle.ACTIVE) {
                    Transaction tx = TransactionHelper.suspendTransaction();
                    try {
                        b.stop();
                        b.uninstall();
                    } finally {
                        TransactionHelper.resumeTransaction(tx);
                    }
                }
            }
        } finally {
            ctx.ungetService(ref);
        }
        log.info(String.format("Undeploy done.\n" + "%s", getRuntimeStatus()));
    }

    @Override
    public Long lastFlushed() {
        return lastFlushed;
    }

    /**
     * Sets the last date date to current date timestamp
     *
     * @since 5.6
     */
    protected void setFlushedNow() {
        lastFlushed = Long.valueOf(System.currentTimeMillis());
    }

    /**
     * @deprecated since 5.6, use {@link #runDeploymentPreprocessor()} instead
     */
    @Override
    @Deprecated
    public void installWebResources(File file) throws IOException {
        log.info("Install web resources");
        if (file.isDirectory()) {
            File war = new File(file, "web");
            war = new File(war, "nuxeo.war");
            if (war.isDirectory()) {
                FileUtils.copyTree(war, getAppDir());
            } else {
                // compatibility mode with studio 1.5 - see NXP-6186
                war = new File(file, "nuxeo.war");
                if (war.isDirectory()) {
                    FileUtils.copyTree(war, getAppDir());
                }
            }
        } else if (file.isFile()) { // a jar
            File war = getWarDir();
            ZipUtils.unzip("web/nuxeo.war", file, war);
            // compatibility mode with studio 1.5 - see NXP-6186
            ZipUtils.unzip("nuxeo.war", file, war);
        }
    }

    @Override
    public void runDeploymentPreprocessor() throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Start running deployment preprocessor");
        }
        String rootPath = Environment.getDefault().getHome().getAbsolutePath();
        File root = new File(rootPath);
        DeploymentPreprocessor processor = new DeploymentPreprocessor(root);
        // initialize
        processor.init();
        // and predeploy
        processor.predeploy();
        if (log.isDebugEnabled()) {
            log.debug("Deployment preprocessing done");
        }
    }

    protected static File getAppDir() {
        return Environment.getDefault().getConfig().getParentFile();
    }

    protected static File getWarDir() {
        return new File(getAppDir(), "nuxeo.war");
    }

    @Override
    public String getOSGIBundleName(File file) {
        Manifest mf = JarUtils.getManifest(file);
        if (mf == null) {
            return null;
        }
        String bundleName = mf.getMainAttributes().getValue("Bundle-SymbolicName");
        if (bundleName == null) {
            return null;
        }
        int index = bundleName.indexOf(';');
        if (index > -1) {
            bundleName = bundleName.substring(0, index);
        }
        return bundleName;
    }

    protected String getRuntimeStatus() {
        StringBuilder msg = new StringBuilder();
        RuntimeService runtime = Framework.getRuntime();
        runtime.getStatusMessage(msg);
        return msg.toString();
    }

    protected void triggerReload(String id) {
        final CountDownLatch reloadAchieved = new CountDownLatch(1);
        final Thread ownerThread = Thread.currentThread();
        try {
            ServiceProvider next = DefaultServiceProvider.getProvider();
            DefaultServiceProvider.setProvider(new ServiceProvider() {

                @Override
                public <T> T getService(Class<T> serviceClass) {
                    if (Thread.currentThread() != ownerThread) {
                        try {
                            reloadAchieved.await();
                        } catch (InterruptedException cause) {
                            Thread.currentThread().interrupt();
                            throw new AssertionError(serviceClass + "was interruped while waiting for reloading",
                                    cause);
                        }
                    }
                    if (next != null) {
                        return next.getService(serviceClass);
                    }
                    return  Framework.getRuntime().getService(serviceClass);
                }
            });
            try {
                if (log.isDebugEnabled()) {
                    log.debug("triggering reload("+id+")");
                }
                Framework.getLocalService(EventService.class).sendEvent(new Event(RELOAD_TOPIC, id, this, null));
                if (id.startsWith(FLUSH_EVENT_ID) || FLUSH_SEAM_EVENT_ID.equals(id)) {
                    setFlushedNow();
                }
            } finally {
                DefaultServiceProvider.setProvider(next);
            }
        } finally {
            reloadAchieved.countDown();
        }
    }

    protected void triggerReloadWithNewTransaction(String id) {
        if (TransactionHelper.isTransactionMarkedRollback()) {
            throw new AssertionError("The calling transaction is marked rollback=");
        } else if (TransactionHelper.isTransactionActive()) { // should flush the calling transaction
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
        try {
            try {
                triggerReload(id);
            } catch (RuntimeException cause) {
                TransactionHelper.setTransactionRollbackOnly();
                throw cause;
            }
        } finally {
            if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
                boolean wasRollbacked = TransactionHelper.isTransactionMarkedRollback();
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
                if (wasRollbacked) {
                    TransactionHelper.setTransactionRollbackOnly();
                }
            }
        }
    }
}
