/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.commandline.executor.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.commandline.executor.service.executors.ShellExecutor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Tests commands parsing.
 *
 * @author tiry
 * @author Vincent Dutat
 */
public class TestCommands extends NXRuntimeTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
    }

    @Test
    public void testReplaceParams() throws Exception {
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        CmdParameters params = cles.getDefaultCmdParameters();

        // test default param
        List<String> res = ShellExecutor.replaceParams("-tmp=#{java.io.tmpdir}", params);
        assertEquals(Arrays.asList("-tmp=" + System.getProperty("java.io.tmpdir")), res);

        // test String param
        params.addNamedParameter("foo", "/some/path");
        res = ShellExecutor.replaceParams("foo=#{foo}", params);
        assertEquals(Arrays.asList("foo=/some/path"), res);
        params.addNamedParameter("width", "320");
        params.addNamedParameter("height", "200");
        res = ShellExecutor.replaceParams("#{width}x#{height}", params);
        assertEquals(Arrays.asList("320x200"), res);

        // test File param
        File tmp = File.createTempFile("testCommands", "txt");
        tmp.delete();
        params.addNamedParameter("foo", tmp);
        res = ShellExecutor.replaceParams("-file=#{foo}[0]", params);
        assertEquals(Arrays.asList("-file=" + tmp.getAbsolutePath() + "[0]"), res);

        // test List param
        params.addNamedParameter("tags", Arrays.asList("-foo", "-bar", "-baz"));
        res = ShellExecutor.replaceParams("#{tags}", params);
        assertEquals(Arrays.asList("-foo", "-bar", "-baz"), res);
    }

    @Test
    public void testCmdEnvironment() throws Exception {
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);

        deployContrib("org.nuxeo.ecm.platform.commandline.executor", "OSGI-INF/commandline-env-test-contrib.xml");
        List<String> cmds = cles.getRegistredCommands();
        assertNotNull(cmds);
        assertTrue(cmds.contains("echo"));

        ExecResult result = cles.execCommand("echo", cles.getDefaultCmdParameters());
        assertTrue(result.isSuccessful());
        assertSame(0, result.getReturnCode());
        assertTrue(String.join("", result.getOutput()).contains(System.getProperty("java.io.tmpdir")));
    }

    @Test
    public void testCmdPipe() throws Exception {
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);

        deployContrib("org.nuxeo.ecm.platform.commandline.executor", "OSGI-INF/commandline-env-test-contrib.xml");

        ExecResult result = cles.execCommand("pipe", cles.getDefaultCmdParameters());
        assertTrue(result.isSuccessful());
        assertEquals(0, result.getReturnCode());
        String line = String.join("", result.getOutput());
        // window's echo displays things exactly as is including quotes
        String expected = SystemUtils.IS_OS_WINDOWS ? "\"a   b\" \"c   d\" e" : "a   b c   d e";
        assertEquals(expected, line);
    }

}
