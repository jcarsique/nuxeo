/*
 * (C) Copyright 2010-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 * $Id$
 */

package org.nuxeo.launcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.launcher.config.JettyConfigurator;

/**
 * Main Nuxeo server thread
 *
 * @author jcarsique
 * @since 5.4.2
 */
public class NuxeoJettyLauncher extends NuxeoLauncher {

    /**
     * @param configurationGenerator
     */
    public NuxeoJettyLauncher(ConfigurationGenerator configurationGenerator) {
        super(configurationGenerator);
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getClassPath() {
        String cp = ".";
        cp = addToClassPath(cp, "lib");
        cp = addToClassPath(cp, "nuxeo-runtime-launcher-*.jar");
        cp = addToClassPath(cp, "bundles");
        return cp;
    }

    @Override
    protected void setServerStartCommand(List<String> command) {
        command.add(JettyConfigurator.STARTUP_CLASS);
        command.add("-home");
        command.add(configurationGenerator.getNuxeoHome().getPath());
    }

    @Override
    protected void setServerStopCommand(List<String> command) {
        command.add(JettyConfigurator.STARTUP_CLASS);
        command.add("stop");
    }

    @Override
    protected Collection<? extends String> getServerProperties() {
        ArrayList<String> serverProperties = new ArrayList<String>();
        return serverProperties;
    }

    @Override
    protected String getServerPrint() {
        return JettyConfigurator.STARTUP_CLASS;
    }

    @Override
    protected String getInstallClassPath(File tmpDir) throws IOException {
        String cp = ".";
        tmpDir.delete();
        tmpDir.mkdirs();
        File baseDir = new File(configurationGenerator.getRuntimeHome(),
                "bundles");
        String[] filenames = new String[] { "nuxeo-runtime-osgi",
                "nuxeo-runtime", "nuxeo-common", "nuxeo-connect-update",
                "nuxeo-connect-client", "nuxeo-connect-offline-update",
                "nuxeo-connect-client-wrapper", "nuxeo-runtime-reload",
                "nuxeo-launcher-commons" };
        cp = getTempClassPath(tmpDir, cp, baseDir, filenames);
        baseDir = configurationGenerator.getServerConfigurator().getNuxeoLibDir();
        filenames = new String[] { "commons-io", "commons-jexl", "groovy-all",
                "osgi-core", "xercesImpl", "commons-collections" };
        cp = getTempClassPath(tmpDir, cp, baseDir, filenames);
        baseDir = configurationGenerator.getServerConfigurator().getServerLibDir();
        filenames = new String[] { "commons-lang", "commons-logging", "log4j" };
        cp = getTempClassPath(tmpDir, cp, baseDir, filenames);
        baseDir = new File(configurationGenerator.getNuxeoHome(), "bin");
        filenames = new String[] { "nuxeo-launcher" };
        cp = getTempClassPath(tmpDir, cp, baseDir, filenames);
        return cp;
    }

}