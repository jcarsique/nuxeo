/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     eugen
 */
package org.nuxeo.ecm.platform.query.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:ei@nuxeo.com">Eugen Ionica</a>
 * @author <a href="mailto:tm@nuxeo.com">Thierry Martins</a>
 */
public class TestTextSearchCleaner extends NXRuntimeTestCase {

    @Test
    public void testCleaner() throws Exception {
        assertEquals("= 'a'", NXQLQueryBuilder.serializeFullText("a"));
        assertEquals("= 'a b'", NXQLQueryBuilder.serializeFullText("a b"));
        assertEquals("= 'a b'", NXQLQueryBuilder.serializeFullText(" a b "));
        assertEquals("= 'a b'", NXQLQueryBuilder.serializeFullText("a  b"));
        assertEquals("= 'a b'", NXQLQueryBuilder.serializeFullText("a & b"));
        assertEquals("= 'a b'", NXQLQueryBuilder.serializeFullText("a : b"));
        assertEquals("= 'a b'", NXQLQueryBuilder.serializeFullText("a | b"));

        assertEquals("= 'a b'", NXQLQueryBuilder.serializeFullText("a { b"));

        assertEquals("= 'a b c d e f'", NXQLQueryBuilder.serializeFullText("a#b|c  d+e*f"));
        assertEquals("= '\"a b\"'", NXQLQueryBuilder.serializeFullText("\"a b\""));
        assertEquals("= '\"a b\"'", NXQLQueryBuilder.serializeFullText("\"a-b\""));
        assertEquals("= '\"a -b \"'", NXQLQueryBuilder.serializeFullText("\"a -b \""));

        assertEquals("= 'a* b'", NXQLQueryBuilder.serializeFullText("a* b-"));

        assertEquals("= 'a b'", NXQLQueryBuilder.serializeFullText("a*b"));

        assertEquals("= 'a  b'", NXQLQueryBuilder.serializeFullText("a*-b"));

        assertEquals("= 'a -b'", NXQLQueryBuilder.serializeFullText("*a -b"));

        assertEquals("= 'a -bc*'", NXQLQueryBuilder.serializeFullText("a | -bc*"));

        assertEquals("= 'a b'", NXQLQueryBuilder.serializeFullText("a !#$%&()*+,-./:;<=>?@^`{|}~ b"));

        // raw sanitizeFulltextInput API that does not wrap the input with the
        // quote and the predicate operator
        assertEquals("some stuff", NXQLQueryBuilder.sanitizeFulltextInput("some & stuff\\"));

        // test negative queries
        assertEquals("= 'a -b'", NXQLQueryBuilder.serializeFullText("a !#$%&()*+,-./:;<=>?@^`{|}~ -b"));
    }

    @Test
    public void testCustomCleaner() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.query.api.test", "configuration-test-contrib.xml");
        ConfigurationService cs = Framework.getService(ConfigurationService.class);
        String s = cs.getProperty(NXQLQueryBuilder.IGNORED_CHARS_KEY);
        assertEquals("&/{}()", s);
        assertNotNull(s);
        assertEquals("= 'a $ b'", NXQLQueryBuilder.serializeFullText("a $ b"));
        assertEquals("= '10.3'", NXQLQueryBuilder.serializeFullText("10.3"));
        undeployContrib("org.nuxeo.ecm.platform.query.api.test", "configuration-test-contrib.xml");
    }

}
