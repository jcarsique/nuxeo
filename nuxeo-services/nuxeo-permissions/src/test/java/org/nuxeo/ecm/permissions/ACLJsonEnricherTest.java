/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.permissions;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.registry.context.DepthValues;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.sql.SQLDirectoryFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(SQLDirectoryFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.usermanager.api", "org.nuxeo.ecm.platform.usermanager",
        "org.nuxeo.ecm.platform.test:test-usermanagerimpl/directory-config.xml", "org.nuxeo.ecm.permissions" })
@LocalDeploy("org.nuxeo.ecm.core.io:OSGI-INF/doc-type-contrib.xml")
public class ACLJsonEnricherTest extends AbstractJsonWriterTest.Local<DocumentModelJsonWriter, DocumentModel> {

    public ACLJsonEnricherTest() {
        super(DocumentModelJsonWriter.class, DocumentModel.class);
    }

    @Inject
    private CoreSession session;

    @Before
    public void before() {
        DocumentModel root = session.getDocument(new PathRef("/"));
        ACP acp = root.getACP();
        Map<String, Serializable> contextData = new HashMap<>();
        contextData.put(Constants.NOTIFY_KEY, false);
        contextData.put(Constants.COMMENT_KEY, "sample comment");
        acp.addACE(ACL.LOCAL_ACL, ACE.builder("Administrator", "Read")
                                     .creator("Administrator")
                                     .contextData(contextData)
                                     .build());
        root.setACP(acp, true);
    }

    @Test
    public void test() throws Exception {
        DocumentModel root = session.getDocument(new PathRef("/"));
        JsonAssert json = jsonAssert(root, CtxBuilder.enrichDoc("acls").get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has("acls").length(1).has(0);
        json.has("name").isEquals("local");
        json.has("aces").isArray();
        json = json.has("aces").get(0);
        json.has("username").isText();
        json.has("creator").isNull();
    }

    @Test
    public void testUsersFetching() throws IOException {
        DocumentModel root = session.getDocument(new PathRef("/"));
        JsonAssert json = jsonAssert(
                root,
                CtxBuilder.enrichDoc("acls")
                          .fetch("acls", "username")
                          .fetch("acls", "creator")
                          .depth(DepthValues.children)
                          .get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has("acls").length(1).has(0);
        json.has("name").isEquals("local");
        json.has("aces").isArray();
        json = json.has("aces").get(3);
        json.has("username").isObject();
        json.has("creator").isObject();
    }

    @Test
    public void testExtendedFetching() throws IOException {
        DocumentModel root = session.getDocument(new PathRef("/"));
        JsonAssert json = jsonAssert(root,
                CtxBuilder.enrichDoc("acls").fetch("acls", "extended").depth(DepthValues.children).get());
        json = json.has("contextParameters").isObject();
        json = json.has("acls").length(1).has(0);
        json.has("name").isEquals("local");
        json.has("aces").isArray();
        json = json.has("aces").get(3);
        json.has("notify").isEquals(false);
        json.has("comment").isEquals("sample comment");
    }
}
