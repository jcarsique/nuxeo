package org.nuxeo.ecm.platform.publisher.impl.service;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.publisher.impl.finder.DefaultRootSectionsFinder;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/*
 * (C) Copyright 2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tmartins - test methods for DefaultRootSectionsFinder
 */

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestRootSectionFinder {

    @Inject
    protected CoreSession session;

    Set<String> sectionRootTypes = new HashSet<String>();

    Set<String> sectionTypes = new HashSet<String>();

    @Test
    public void testBuildQuery() throws Exception {
        sectionTypes.add("Sections");
        RootSectionsFinderForTest rsf = new RootSectionsFinderForTest(session);

        rsf.setSectionTypes();

        String path;
        String query;

        path = "/default-domain/workspaces/space";
        query = rsf.buildQuery(path);
        assertEquals(
                "SELECT * FROM Document WHERE ecm:path STARTSWITH '/default-domain/workspaces/space' and ( ecm:primaryType = 'Section' ) order by ecm:path ",
                query);

        path = "/default-domain/workspaces/thierry's space";
        query = rsf.buildQuery(path);
        assertEquals(
                "SELECT * FROM Document WHERE ecm:path STARTSWITH '/default-domain/workspaces/thierry\\'s space' and ( ecm:primaryType = 'Section' ) order by ecm:path ",
                query);

        // test if query is valid
        DocumentModelList dml = session.query(query);
        assertEquals(0, dml.size());

    }

    /**
     * test class to call protected method
     *
     * @author tmartins
     */
    private class RootSectionsFinderForTest extends DefaultRootSectionsFinder {

        public RootSectionsFinderForTest(CoreSession userSession) {
            super(userSession);
        }

        @Override
        public String buildQuery(String path) {
            return super.buildQuery(path);
        }

        public void setSectionTypes() {
            sectionTypes = new HashSet<String>();
            sectionTypes.add("Section");
        }

    }
}
