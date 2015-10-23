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

import org.nuxeo.runtime.deployment.preprocessor.DeploymentPreprocessor;
import org.nuxeo.runtime.service.TimestampedService;
import org.osgi.framework.BundleException;

/**
 * Service tracking reload related events or commands when installing a package
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ReloadService extends TimestampedService {

    public static final String RELOAD_TOPIC = "org.nuxeo.runtime.reload";

    public static final String FLUSH_EVENT_ID = "flush";

    public static final String RELOAD_EVENT_ID = "reload";

    public static final String FLUSH_SEAM_EVENT_ID = FLUSH_EVENT_ID+"SeamComponents";

    public static final String RELOAD_SEAM_EVENT_ID = "reloadSeamComponents";

    public static final String RELOAD_REPOSITORIES_ID = "reloadRepositories";

    /**
     * Sends a runtime event with id {@link #RELOAD_EVENT_ID} so that listeners can be notified that a reload has been
     * done.
     * <p>
     * Also calls {@link #reloadProperties()} by default, but not other reload methods as they could alter the running
     * application behaviour.
     *
     * @since 5.5
     * @see #reloadProperties()
     */
    void reload();

    /**
     * Reloads the Nuxeo repository configuration
     */
    void reloadRepository();

    /**
     * Reloads runtime framework properties
     */
    void reloadProperties() throws IOException;

    /**
     * Sends a runtime event with id {@link #RELOAD_SEAM_EVENT_ID}
     *
     * @since 5.5
     */
    void reloadSeamComponents();

    /**
     * Sends a runtime event with id {@link #FLUSH_EVENT_ID} so that listeners can be notified that a flush is needed
     * (after a reload for instance).
     * <p>
     * Also calls {@link #flushJaasCache()} by default, but not other flush methods as they could alter the running
     * application behaviour.
     *
     * @see {@link #flushJaasCache()}
     * @since 5.5
     */
    void flush();

    /**
     * Returns the last time one of the flush commands where called on this service instance ({@link #flush()} or
     * {@link #flushJaasCache()} or {@link #flushSeamComponents()}, or null if never called
     *
     * @since 5.6
     */
    Long lastFlushed();

    /**
     * Sends an event that can trigger reset of JaasCache
     */
    void flushJaasCache();

    /**
     * Sends a runtime event with id {@link #FLUSH_SEAM_EVENT_ID}
     *
     * @since 5.6
     */
    void flushSeamComponents();

    /**
     * Deploys bundle to the runtime, without reloading resources
     *
     * @since 5.5
     * @see #deployBundle(File, boolean)
     */
    String deployBundle(File file) throws BundleException;

    /**
     * Deploys bundle to the runtime, gives possibility to control resources reloading.
     *
     * @since 5.5
     */
    String deployBundle(File file, boolean reloadResources) throws BundleException;

    /**
     * Undeploys bundle from the runtime, given the bundle resource, gives possibility to control resources reloading
     *
     * @since 5.6
     */
    void undeployBundle(File file, boolean reloadResources) throws BundleException;

    /**
     * Undeploys bundle from the runtime, given the bundle filename
     *
     * @since 5.6
     */
    void undeployBundle(String bundleName) throws BundleException;

    /**
     * Runs the deployment preprocessor
     *
     * @since 5.6
     * @See {@link DeploymentPreprocessor}
     */
    public void runDeploymentPreprocessor() throws IOException;

    /**
     * Copies the bundle web resources into the nuxeo WAR directory.
     *
     * @since 5.5
     * @deprecated since 5.6: {@link #runDeploymentPreprocessor()} method now re-deploys all jars so that the nuxeo.war
     *             holds the same content than it would at startup.
     */
    @Deprecated
    void installWebResources(File file) throws IOException;

    /***
     * Returns the OSGI bundle name if given file can be identified as an OSGI bundle, or null. The OSGI bundle can be a
     * jar or an exploded jar on file system.
     *
     * @since 5.6
     */
    String getOSGIBundleName(File file);

}
