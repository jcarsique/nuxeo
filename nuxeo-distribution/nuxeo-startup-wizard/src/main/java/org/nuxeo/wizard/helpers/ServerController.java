/*
 * (C) Copyright 2011-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     tdelprat, jcarsique
 *
 */
package org.nuxeo.wizard.helpers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;

import org.nuxeo.launcher.config.ConfigurationGenerator;
import org.nuxeo.log4j.ThreadedStreamGobbler;
import org.nuxeo.wizard.context.Context;
import org.nuxeo.wizard.context.ParamCollector;

/**
 * Manages execution of NuxeoCtl
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
public class ServerController {

    protected static final String CMD_POSIX = "nuxeoctl";

    protected static final String CMD_WIN = "nuxeoctl.bat";

    protected static Log log = LogFactory.getLog(ServerController.class);

    /**
     * @deprecated Since 7.4. Use {@link SystemUtils#IS_OS_WINDOWS}
     */
    @Deprecated
    public static boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    private static String winEscape(String command) {
        return command.replaceAll("([ ()<>&])", "^$1");
    }

    protected static boolean doExec(String path, String logPath) {
        String[] cmd;
        if (SystemUtils.IS_OS_WINDOWS) {
            cmd = new String[] { "cmd", "/C", winEscape(new File(path, CMD_WIN).getPath()), "--gui=false", "restartbg" };
        } else {
            cmd = new String[] { "/bin/sh", "-c", "\"" + new File(path, CMD_POSIX).getPath() + "\"" + " restartbg" };
        }

        Process p1;
        try {
            if (log.isDebugEnabled()) {
                log.debug("Restart command: " + StringUtils.join(cmd, " "));
            }
            ProcessBuilder pb = new ProcessBuilder(cmd);
            p1 = pb.start();
        } catch (IOException e) {
            log.error("Unable to restart server", e);
            return false;
        }

        if (SystemUtils.IS_OS_WINDOWS) {
            File logPathDir = new File(logPath);
            File out = new File(logPathDir, "restart-" + System.currentTimeMillis() + ".log");
            File err = new File(logPathDir, "restart-err-" + System.currentTimeMillis() + ".log");
            OutputStream fout = null;
            OutputStream ferr = null;
            try {
                fout = new FileOutputStream(out);
                ferr = new FileOutputStream(err);
            } catch (Exception e) {
            }
            new ThreadedStreamGobbler(p1.getInputStream(), fout).start();
            new ThreadedStreamGobbler(p1.getErrorStream(), ferr).start();
        } else {
            new ThreadedStreamGobbler(p1.getInputStream(), SimpleLog.LOG_LEVEL_OFF).start();
            new ThreadedStreamGobbler(p1.getErrorStream(), SimpleLog.LOG_LEVEL_ERROR).start();
        }
        return true;
    }

    private static boolean restartInProgress = false;

    private static ConfigurationGenerator cgForRestart;

    public static synchronized boolean restart(Context context) {
        if (restartInProgress) {
            return false;
        }
        ParamCollector collector = context.getCollector();
        ConfigurationGenerator cg = collector.getConfigurationGenerator();
        File nuxeoHome = cg.getNuxeoHome();
        final String logDir = cg.getLogDir().getPath();
        final String binPath = new File(nuxeoHome, "bin").getPath();
        new Thread("restart thread") {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                    doExec(binPath, logDir);
                } catch (InterruptedException e) {
                    log.error("Restart failed", e);
                }
            }
        }.start();
        return true;
    }

    /**
     * @since 5.6
     * @return Configured server URL (may differ from current URL)
     */
    public static synchronized String getServerURL() {
        if (cgForRestart == null) {
            cgForRestart = new ConfigurationGenerator();
            cgForRestart.init();
        }
        return cgForRestart.getUserConfig().getProperty(ConfigurationGenerator.PARAM_NUXEO_URL);
    }
}
