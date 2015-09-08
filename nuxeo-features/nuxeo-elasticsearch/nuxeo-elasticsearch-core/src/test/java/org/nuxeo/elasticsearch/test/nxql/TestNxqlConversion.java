/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.test.nxql;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.SystemUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.elasticsearch.query.NxqlQueryConverter;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * Test that NXQL can be used to generate ES queries
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy({ "org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml" })
public class TestNxqlConversion {

    private static final String IDX_NAME = "nxutest";

    private static final String TYPE_NAME = "doc";

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchService ess;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected ElasticSearchIndexing esi;

    protected void buildDocs() throws Exception {
        for (int i = 0; i < 10; i++) {
            String name = "doc" + i;
            DocumentModel doc = session.createDocumentModel("/", name, "File");
            doc.setPropertyValue("dc:title", "File" + i);
            doc.setPropertyValue("dc:nature", "Nature" + i);
            doc.setPropertyValue("dc:rights", "Rights" + i % 2);
            doc = session.createDocument(doc);
        }
        TransactionHelper.commitOrRollbackTransaction();
        // wait for async jobs
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        Assert.assertEquals(0, esa.getPendingWorkerCount());

        esa.refresh();

        TransactionHelper.startTransaction();

    }

    @Test
    public void testQuery() throws Exception {

        buildDocs();

        SearchResponse searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.queryString(" dc\\:nature:\"Nature1\" AND dc\\:title:\"File1\"")).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());

        searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.queryString(" dc\\:nature:\"Nature2\" AND dc\\:title:\"File1\"")).setFrom(0).setSize(60).execute().actionGet();
        Assert.assertEquals(0, searchResponse.getHits().getTotalHits());

        searchResponse = esa.getClient().prepareSearch(IDX_NAME).setTypes(TYPE_NAME).setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(QueryBuilders.queryString(" NOT dc\\:nature:\"Nature2\"")).setFrom(
                0).setSize(60).execute().actionGet();
        Assert.assertEquals(9, searchResponse.getHits().getTotalHits());

        checkNXQL("select * from Document where dc:nature='Nature2' and dc:title='File2'", 1);
        checkNXQL("select * from Document where dc:nature='Nature2' and dc:title='File1'", 0);
        checkNXQL("select * from Document where dc:nature='Nature2' or dc:title='File1'", 2);
    }

    @Test
    public void testQueryLimits() throws Exception {
        buildDocs();

        // limit does not change the total size, only the returned number of docs
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document").limit(1));
        Assert.assertEquals(10, docs.totalSize());
        Assert.assertEquals(1, docs.size());
        // default is 10
        docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document"));
        Assert.assertEquals(10, docs.totalSize());
        Assert.assertEquals(10, docs.size());
        // all
        docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document").limit(-1));
        Assert.assertEquals(10, docs.totalSize());
        Assert.assertEquals(10, docs.size());
        // only interested about totalSize
        docs = ess.query(new NxQueryBuilder(session).nxql("select * from Document").limit(0));
        Assert.assertEquals(10, docs.totalSize());
        Assert.assertEquals(0, docs.size());
    }

    @Test
    public void testQueryWithSpecialCharacters() throws Exception {
        // special character should not raise syntax error
        String specialChars = "^..*+ - && || ! ( ) { } [ ] )^ \" (~ * ? : \\ / \\t$";
        checkNXQL("select * from Document where dc:title = '" + specialChars + "'", 0);
        checkNXQL("select * from Document where ecm:fulltext.dc:title = '" + specialChars + "'", 0);
        checkNXQL("select * from Document where dc:title LIKE '" + specialChars + "'", 0);
        checkNXQL("select * from Document where dc:title IN ('" + specialChars + "')", 0);
        checkNXQL("select * from Document where dc:title STARTSWITH '" + specialChars + "'", 0);
    }

    protected void checkNXQL(String nxql, int expectedNumberOfHis) throws Exception {
        // System.out.println(NXQLQueryConverter.toESQueryString(nxql));
        DocumentModelList docs = ess.query(new NxQueryBuilder(session).nxql(nxql).limit(0));
        Assert.assertEquals(expectedNumberOfHis, docs.totalSize());
    }

    @Test
    public void testConverterSelect() throws Exception {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"match_all\" : { }\n" + "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from File, Document").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"match_all\" : { }\n" + "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from File").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"filtered\" : {\n" + "    \"query\" : {\n"
                + "      \"match_all\" : { }\n" + "    },\n" + "    \"filter\" : {\n" + "      \"terms\" : {\n"
                + "        \"ecm:primaryType\" : [ \"File\" ]\n" + "      }\n" + "    }\n" + "  }\n" + "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from File, Note").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"filtered\" : {\n" + "    \"query\" : {\n"
                        + "      \"match_all\" : { }\n" + "    },\n" + "    \"filter\" : {\n" + "      \"terms\" : {\n"
                        + "        \"ecm:primaryType\" : [ \"File\", \"Note\" ]\n" + "      }\n" + "    }\n" + "  }\n" + "}",
                es);
    }

    @Test
    public void testConverterEQUALS() throws Exception {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1=1").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                + "      \"term\" : {\n" + "        \"f1\" : \"1\"\n" + "      }\n" + "    }\n" + "  }\n" + "}", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 != 1").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                + "      \"not\" : {\n" + "        \"filter\" : {\n" + "          \"term\" : {\n"
                + "            \"f1\" : \"1\"\n" + "          }\n" + "        }\n" + "      }\n" + "    }\n" + "  }\n"
                + "}", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 <> 1").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                + "      \"not\" : {\n" + "        \"filter\" : {\n" + "          \"term\" : {\n"
                + "            \"f1\" : \"1\"\n" + "          }\n" + "        }\n" + "      }\n" + "    }\n" + "  }\n"
                + "}", es);

    }

    @Test
    public void testConverterIN() throws Exception {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 IN (1)").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                + "      \"terms\" : {\n" + "        \"f1\" : [ \"1\" ]\n" + "      }\n" + "    }\n" + "  }\n" + "}",
                es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 NOT IN (1, '2', 3)").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                + "      \"not\" : {\n" + "        \"filter\" : {\n" + "          \"terms\" : {\n"
                + "            \"f1\" : [ \"1\", \"2\", \"3\" ]\n" + "          }\n" + "        }\n" + "      }\n"
                + "    }\n" + "  }\n" + "}", es);
    }

    @Test
    public void testConverterLIKE() throws Exception {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 LIKE 'foo%'").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"match\" : {\n" + "    \"f1\" : {\n" + "      \"query\" : \"foo\",\n"
                + "      \"type\" : \"phrase_prefix\"\n" + "    }\n" + "  }\n" + "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 LIKE '%Foo%'").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"wildcard\" : {\n" + "    \"f1\" : {\n"
                + "      \"wildcard\" : \"*Foo*\"\n" + "    }\n" + "  }\n" + "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 NOT LIKE 'Foo%'").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                + "      \"not\" : {\n" + "        \"filter\" : {\n" + "          \"query\" : {\n"
                + "            \"match\" : {\n" + "              \"f1\" : {\n"
                + "                \"query\" : \"Foo\",\n" + "                \"type\" : \"phrase_prefix\"\n"
                + "              }\n" + "            }\n" + "          }\n" + "        }\n" + "      }\n" + "    }\n"
                + "  }\n" + "}", es);
        // invalid input
        NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 LIKE '(foo.*$#@^'").toString();
    }

    @Test
    public void testConverterLIKEWildcard() throws Exception {
        String es;
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document WHERE f1 LIKE '%foo'").toString();
        assertEqualsEvenUnderWindows("{\n" //
                + "  \"wildcard\" : {\n" //
                + "    \"f1\" : {\n" //
                + "      \"wildcard\" : \"*foo\"\n" //
                + "    }\n" //
                + "  }\n" //
                + "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document WHERE f1 LIKE '_foo'").toString();
        assertEqualsEvenUnderWindows("{\n" //
                + "  \"wildcard\" : {\n" //
                + "    \"f1\" : {\n" //
                + "      \"wildcard\" : \"?foo\"\n" //
                + "    }\n" //
                + "  }\n" //
                + "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document WHERE f1 LIKE '?foo'").toString();
        assertEqualsEvenUnderWindows("{\n" //
                + "  \"wildcard\" : {\n" //
                + "    \"f1\" : {\n" //
                + "      \"wildcard\" : \"\\\\?foo\"\n" // backslash escaped for JSON
                + "    }\n" //
                + "  }\n" //
                + "}", es);
        // * is also accepted as a wildcard (compat)
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document WHERE f1 LIKE '*foo'").toString();
        assertEqualsEvenUnderWindows("{\n" //
                + "  \"wildcard\" : {\n" //
                + "    \"f1\" : {\n" //
                + "      \"wildcard\" : \"*foo\"\n" //
                + "    }\n" //
                + "  }\n" //
                + "}", es);
        // NXQL escaping
        es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document WHERE f1 LIKE 'foo\\_bar\\%'").toString();
        assertEqualsEvenUnderWindows("{\n" //
                + "  \"wildcard\" : {\n" //
                + "    \"f1\" : {\n" //
                + "      \"wildcard\" : \"foo_bar%\"\n" //
                + "    }\n" //
                + "  }\n" //
                + "}", es);
    }

    @Test
    public void testConverterILIKE() throws Exception {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 ILIKE 'Foo%'").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"match\" : {\n" + "    \"f1.lowercase\" : {\n"
                + "      \"query\" : \"foo\",\n" + "      \"type\" : \"phrase_prefix\"\n" + "    }\n" + "  }\n" + "}",
                es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 ILIKE '%Foo%'").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"wildcard\" : {\n" + "    \"f1.lowercase\" : {\n"
                + "      \"wildcard\" : \"*foo*\"\n" + "    }\n" + "  }\n" + "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 NOT ILIKE 'Foo%'").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                + "      \"not\" : {\n" + "        \"filter\" : {\n" + "          \"query\" : {\n"
                + "            \"match\" : {\n" + "              \"f1.lowercase\" : {\n"
                + "                \"query\" : \"foo\",\n" + "                \"type\" : \"phrase_prefix\"\n"
                + "              }\n" + "            }\n" + "          }\n" + "        }\n" + "      }\n" + "    }\n"
                + "  }\n" + "}", es);
    }

    @Test
    public void testConverterIsNULL() throws Exception {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 IS NULL").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                + "      \"missing\" : {\n" + "        \"field\" : \"f1\",\n" + "        \"null_value\" : true\n"
                + "      }\n" + "    }\n" + "  }\n" + "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 IS NOT NULL").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                        + "      \"exists\" : {\n" + "        \"field\" : \"f1\"\n" + "      }\n" + "    }\n" + "  }\n" + "}",
                es);
    }

    @Test
    public void testConverterBETWEEN() throws Exception {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 BETWEEN 1 AND 2").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                + "      \"range\" : {\n" + "        \"f1\" : {\n" + "          \"from\" : \"1\",\n"
                + "          \"to\" : \"2\",\n" + "          \"include_lower\" : true,\n"
                + "          \"include_upper\" : true\n" + "        }\n" + "      }\n" + "    }\n" + "  }\n" + "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1 NOT BETWEEN 1 AND 2").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                + "      \"not\" : {\n" + "        \"filter\" : {\n" + "          \"range\" : {\n"
                + "            \"f1\" : {\n" + "              \"from\" : \"1\",\n" + "              \"to\" : \"2\",\n"
                + "              \"include_lower\" : true,\n" + "              \"include_upper\" : true\n"
                + "            }\n" + "          }\n" + "        }\n" + "      }\n" + "    }\n" + "  }\n" + "}", es);
    }

    @Test
    public void testConverterSTARTSWITH() throws Exception {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where ecm:path STARTSWITH '/the/path'").toString();
        assertEqualsEvenUnderWindows("{\n" //
                + "  \"constant_score\" : {\n" //
                + "    \"filter\" : {\n" //
                + "      \"term\" : {\n" //
                + "        \"ecm:path.children\" : \"/the/path\"\n" //
                + "      }\n" + "    }\n" //
                + "  }\n" //
                + "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where ecm:path STARTSWITH '/'").toString();
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"constant_score\" : {\n" + //
                "    \"filter\" : {\n" + //
                "      \"exists\" : {\n" + //
                "        \"field\" : \"ecm:path.children\"\n" + //
                "      }\n" + //
                "    }\n" + //
                "  }\n" + //
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where ecm:path STARTSWITH '/the/path/'").toString();
        assertEqualsEvenUnderWindows("{\n" //
                + "  \"constant_score\" : {\n" //
                + "    \"filter\" : {\n" //
                + "      \"term\" : {\n" //
                + "        \"ecm:path.children\" : \"/the/path\"\n" //
                + "      }\n" + "    }\n" //
                + "  }\n" //
                + "}", es);
    }

    @Test
    public void testConverterIsVersion() throws Exception {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where ecm:isVersion = 1").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                + "      \"term\" : {\n" + "        \"ecm:isVersion\" : \"1\"\n" + "      }\n" + "    }\n" + "  }\n"
                + "}", es);
        String es2 = NxqlQueryConverter.toESQueryBuilder("select * from Document where ecm:isCheckedInVersion = 1").toString();
        assertEqualsEvenUnderWindows(es, es2);
    }

    @Test
    public void testConverterFulltext() throws Exception {
        // Given a search on a fulltext field
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where ecm:fulltext='+foo -bar'").toString();
        // then we have a simple query text and not a filter
        assertEqualsEvenUnderWindows("{\n" + "  \"simple_query_string\" : {\n" + "    \"query\" : \"+foo -bar\",\n"
                + "    \"fields\" : [ \"_all\" ],\n" + "    \"analyzer\" : \"fulltext\",\n"
                + "    \"default_operator\" : \"and\"\n" + "  }\n" + "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where ecm:fulltext_someindex LIKE '+foo -bar'").toString();
        // don't handle nxql fulltext index definition, match to _all field
        assertEqualsEvenUnderWindows("{\n" + "  \"simple_query_string\" : {\n" + "    \"query\" : \"+foo -bar\",\n"
                + "    \"fields\" : [ \"_all\" ],\n" + "    \"analyzer\" : \"fulltext\",\n"
                + "    \"default_operator\" : \"and\"\n" + "  }\n" + "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where ecm:fulltext.dc:title!='+foo -bar'").toString();
        // request on field match field.fulltext
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                + "      \"not\" : {\n" + "        \"filter\" : {\n" + "          \"query\" : {\n"
                + "            \"simple_query_string\" : {\n" + "              \"query\" : \"+foo -bar\",\n"
                + "              \"fields\" : [ \"dc:title.fulltext\" ],\n"
                + "              \"analyzer\" : \"fulltext\",\n" + "              \"default_operator\" : \"and\"\n"
                + "            }\n" + "          }\n" + "        }\n" + "      }\n" + "    }\n" + "  }\n" + "}", es);
    }

    @Test
    public void testConverterFulltextElasticsearchPrefix() throws Exception {
        // Given a search on a fulltext field with the
        // elasticsearch-specific prefix
        String es = NxqlQueryConverter.toESQueryBuilder("SELECT * FROM Document WHERE ecm:fulltext = 'es: foo bar'").toString();
        // then we have a simple query text and not a filter
        // and we have the OR operator
        assertEqualsEvenUnderWindows("{\n" + "  \"simple_query_string\" : {\n" + "    \"query\" : \"foo bar\",\n"
                + "    \"fields\" : [ \"_all\" ],\n" + "    \"analyzer\" : \"fulltext\",\n"
                + "    \"default_operator\" : \"or\"\n" + "  }\n" + "}", es);
    }

    @Test
    public void testConverterWhereCombination() throws Exception {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1=1 AND f2=2").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"bool\" : {\n" + "    \"must\" : [ {\n"
                + "      \"constant_score\" : {\n" + "        \"filter\" : {\n" + "          \"term\" : {\n"
                + "            \"f1\" : \"1\"\n" + "          }\n" + "        }\n" + "      }\n" + "    }, {\n"
                + "      \"constant_score\" : {\n" + "        \"filter\" : {\n" + "          \"term\" : {\n"
                + "            \"f2\" : \"2\"\n" + "          }\n" + "        }\n" + "      }\n" + "    } ]\n"
                + "  }\n" + "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1=1 OR f2=2").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"bool\" : {\n" + "    \"should\" : [ {\n"
                + "      \"constant_score\" : {\n" + "        \"filter\" : {\n" + "          \"term\" : {\n"
                + "            \"f1\" : \"1\"\n" + "          }\n" + "        }\n" + "      }\n" + "    }, {\n"
                + "      \"constant_score\" : {\n" + "        \"filter\" : {\n" + "          \"term\" : {\n"
                + "            \"f2\" : \"2\"\n" + "          }\n" + "        }\n" + "      }\n" + "    } ]\n"
                + "  }\n" + "}", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1=1 AND f2=2 AND f3=3").toString();
        // Assert.assertEquals("foo", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1=1 OR f2=2 OR f3=3").toString();
        // Assert.assertEquals("foo", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where f1=1 OR f2 LIKE 'foo' OR f3=3").toString();
        // Assert.assertEquals("foo", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where (f1=1 OR f2=2) AND f3=3").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"bool\" : {\n" + "    \"must\" : [ {\n" + "      \"bool\" : {\n"
                + "        \"should\" : [ {\n" + "          \"constant_score\" : {\n" + "            \"filter\" : {\n"
                + "              \"term\" : {\n" + "                \"f1\" : \"1\"\n" + "              }\n"
                + "            }\n" + "          }\n" + "        }, {\n" + "          \"constant_score\" : {\n"
                + "            \"filter\" : {\n" + "              \"term\" : {\n" + "                \"f2\" : \"2\"\n"
                + "              }\n" + "            }\n" + "          }\n" + "        } ]\n" + "      }\n"
                + "    }, {\n" + "      \"constant_score\" : {\n" + "        \"filter\" : {\n"
                + "          \"term\" : {\n" + "            \"f3\" : \"3\"\n" + "          }\n" + "        }\n"
                + "      }\n" + "    } ]\n" + "  }\n" + "}", es);
    }

    @Test
    public void testConverterComplex() throws Exception {
        String es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where (f1 LIKE '1%' OR f2 LIKE '2%') AND f3=3").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"bool\" : {\n" + "    \"must\" : [ {\n" + "      \"bool\" : {\n"
                + "        \"should\" : [ {\n" + "          \"match\" : {\n" + "            \"f1\" : {\n"
                + "              \"query\" : \"1\",\n" + "              \"type\" : \"phrase_prefix\"\n"
                + "            }\n" + "          }\n" + "        }, {\n" + "          \"match\" : {\n"
                + "            \"f2\" : {\n" + "              \"query\" : \"2\",\n"
                + "              \"type\" : \"phrase_prefix\"\n" + "            }\n" + "          }\n"
                + "        } ]\n" + "      }\n" + "    }, {\n" + "      \"constant_score\" : {\n"
                + "        \"filter\" : {\n" + "          \"term\" : {\n" + "            \"f3\" : \"3\"\n"
                + "          }\n" + "        }\n" + "      }\n" + "    } ]\n" + "  }\n" + "}", es);
        // Assert.assertEquals("foo", es);
        es = NxqlQueryConverter.toESQueryBuilder(
                "select * from Document where ecm:fulltext='foo bar' AND ecm:path STARTSWITH '/foo/bar' OR ecm:path='/foo/'").toString();
        // Assert.assertEquals("foo", es);

        es = NxqlQueryConverter.toESQueryBuilder(
                "select * from File, Note, Workspace where f1 IN ('foo', 'bar', 'foo') AND NOT f2>=3").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"filtered\" : {\n" + "    \"query\" : {\n" + "      \"bool\" : {\n"
                + "        \"must\" : [ {\n" + "          \"constant_score\" : {\n" + "            \"filter\" : {\n"
                + "              \"terms\" : {\n" + "                \"f1\" : [ \"foo\", \"bar\", \"foo\" ]\n"
                + "              }\n" + "            }\n" + "          }\n" + "        }, {\n"
                + "          \"bool\" : {\n" + "            \"must_not\" : {\n"
                + "              \"constant_score\" : {\n" + "                \"filter\" : {\n"
                + "                  \"range\" : {\n" + "                    \"f2\" : {\n"
                + "                      \"from\" : \"3\",\n" + "                      \"to\" : null,\n"
                + "                      \"include_lower\" : true,\n"
                + "                      \"include_upper\" : true\n" + "                    }\n"
                + "                  }\n" + "                }\n" + "              }\n" + "            }\n"
                + "          }\n" + "        } ]\n" + "      }\n" + "    },\n" + "    \"filter\" : {\n"
                + "      \"terms\" : {\n" + "        \"ecm:primaryType\" : [ \"File\", \"Note\", \"Workspace\" ]\n"
                + "      }\n" + "    }\n" + "  }\n" + "}", es);
    }

    @Test
    public void testConverterWhereWithoutSelect() throws Exception {
        String es = NxqlQueryConverter.toESQueryBuilder("f1=1").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                + "      \"term\" : {\n" + "        \"f1\" : \"1\"\n" + "      }\n" + "    }\n" + "  }\n" + "}", es);
        es = NxqlQueryConverter.toESQueryBuilder(null).toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"match_all\" : { }\n" + "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"match_all\" : { }\n" + "}", es);
    }

    @Test
    public void testConvertComplexProperties() throws Exception {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where file:content/name = 'foo'").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                + "      \"term\" : {\n" + "        \"file:content.name\" : \"foo\"\n" + "      }\n" + "    }\n"
                + "  }\n" + "}", es);
    }

    @Test
    public void testConvertComplexListProperties() throws Exception {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where dc:subjects/* = 'foo'").toString();
        // this is supported and match any element of the list
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                + "      \"term\" : {\n" + "        \"dc:subjects\" : \"foo\"\n" + "      }\n" + "    }\n" + "  }\n"
                + "}", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where files:files/*/file/length=123").toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                + "      \"term\" : {\n" + "        \"files:files.file.length\" : \"123\"\n" + "      }\n" + "    }\n"
                + "  }\n" + "}", es);

    }

    @Test
    public void testConvertComplexListPropertiesUnsupported() throws Exception {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where dc:subjects/3 = 'foo'").toString();
        // This is not supported and generate query that is going to match nothing
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                + "      \"term\" : {\n" + "        \"dc:subjects.3\" : \"foo\"\n" + "      }\n" + "    }\n" + "  }\n"
                + "}", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where dc:subjects/*1 = 'foo'").toString();
        // This is not supported and generate query that is going to match nothing
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                + "      \"term\" : {\n" + "        \"dc:subjects1\" : \"foo\"\n" + "      }\n" + "    }\n" + "  }\n"
                + "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where files:files/*1/file/length=123").toString();
        // This is not supported and generate query that is going to match nothing
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                + "      \"term\" : {\n" + "        \"files:files1.file.length\" : \"123\"\n" + "      }\n" + "    }\n"
                + "  }\n" + "}", es);

    }

    @Test
    public void testOrderByFromNxql() throws Exception {
        NxQueryBuilder qb = new NxQueryBuilder(session).nxql("name='foo' ORDER BY name DESC");
        String es = qb.makeQuery().toString();
        assertEqualsEvenUnderWindows("{\n" + "  \"constant_score\" : {\n" + "    \"filter\" : {\n"
                + "      \"term\" : {\n" + "        \"name\" : \"foo\"\n" + "      }\n" + "    }\n" + "  }\n" + "}", es);
        Assert.assertEquals(1, qb.getSortInfos().size());
        Assert.assertEquals("SortInfo [sortColumn=name, sortAscending=false]", qb.getSortInfos().get(0).toString());
    }

    @Test
    public void testConvertHint() throws Exception {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where /*+ES: INDEX(some:field) */ dc:title = 'foo'").toString();
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"constant_score\" : {\n" + //
                "    \"filter\" : {\n" + //
                "      \"term\" : {\n" + //
                "        \"some:field\" : \"foo\"\n" + //
                "      }\n" + //
                "    }\n" + //
                "  }\n" + //
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where /*+ES: INDEX(some:field) */ dc:title != 'foo'").toString();
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"constant_score\" : {\n" + //
                "    \"filter\" : {\n" + //
                "      \"not\" : {\n" + //
                "        \"filter\" : {\n" + //
                "          \"term\" : {\n" + //
                "            \"some:field\" : \"foo\"\n" + //
                "          }\n" + //
                "        }\n" + //
                "      }\n" + //
                "    }\n" + //
                "  }\n" + //
                "}", es);
    }

    @Test
    public void testConvertHintOperator() throws Exception {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where /*+ES: INDEX(some:field) ANALYZER(my_analyzer) OPERATOR(match) */ dc:subjects = 'foo'").toString();
        assertEqualsEvenUnderWindows("{\n" +
                "  \"match\" : {\n" + //
                "    \"some:field\" : {\n" + //
                "      \"query\" : \"foo\",\n" + //
                "      \"type\" : \"boolean\",\n" + //
                "      \"analyzer\" : \"my_analyzer\"\n" + //
                "    }\n" + //
                "  }\n" + //
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where /*+ES: OPERATOR(match_phrase) */ dc:title = 'foo'").toString();
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"match\" : {\n" + //
                "    \"dc:title\" : {\n" + //
                "      \"query\" : \"foo\",\n" + //
                "      \"type\" : \"phrase\"\n" + //
                "    }\n" + //
                "  }\n" + //
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where /*+ES: OPERATOR(match_phrase_prefix) */ dc:title = 'this is a test'").toString();
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"match\" : {\n" + //
                "    \"dc:title\" : {\n" + //
                "      \"query\" : \"this is a test\",\n" + //
                "      \"type\" : \"phrase_prefix\"\n" + //
                "    }\n" + //
                "  }\n" + //
                "}", es);
        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where /*+ES: INDEX(dc:title^3,dc:description) OPERATOR(multi_match) */ dc:title = 'this is a test'").toString();
        // fields are not ordered
        assertIn(es,
                "{\n" + //
                        "  \"multi_match\" : {\n" + //
                        "    \"query\" : \"this is a test\",\n" + //
                        "    \"fields\" : [ \"dc:title^3\", \"dc:description\" ]\n" + //
                        "  }\n" + //
                        "}",
                "{\n" + //
                        "  \"multi_match\" : {\n" + //
                        "    \"query\" : \"this is a test\",\n" + //
                        "    \"fields\" : [ \"dc:description\", \"dc:title^3\" ]\n" + //
                        "  }\n" + //
                        "}");

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where /*+ES: OPERATOR(regex) */ dc:title = 's.*y'").toString();
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"regexp\" : {\n" + //
                "    \"dc:title\" : {\n" + //
                "      \"value\" : \"s.*y\"\n" +  //
                "    }\n" + //
                "  }\n" + //
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where /*+ES: OPERATOR(fuzzy) */ dc:title = 'ki'").toString();
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"fuzzy\" : {\n" + //
                "    \"dc:title\" : {\n" + //
                "      \"value\" : \"ki\"\n" +  //
                "    }\n" + //
                "  }\n" + //
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where /*+ES: OPERATOR(wildcard) */ dc:title = 'ki*y'").toString();
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"wildcard\" : {\n" + //
                "    \"dc:title\" : {\n" + //
                "      \"wildcard\" : \"ki*y\"\n" +  //
                "    }\n" + //
                "  }\n" + //
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where /*+ES: OPERATOR(simple_query_string) */ dc:title = '\"fried eggs\" +(eggplant | potato) -frittata'").toString();
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"simple_query_string\" : {\n" + //
                "    \"query\" : \"\\\"fried eggs\\\" +(eggplant | potato) -frittata\",\n" + //
                "    \"fields\" : [ \"dc:title\" ]\n" + //
                "  }\n" + //
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where /*+ES: INDEX(dc:title,dc:description) ANALYZER(fulltext) OPERATOR(query_string) */ dc:title = 'this AND that OR thus'").toString();
        // fields are not ordered
        assertIn(es,
                "{\n" + //
                        "  \"query_string\" : {\n" + //
                        "    \"query\" : \"this AND that OR thus\",\n" + //
                        "    \"fields\" : [ \"dc:title\", \"dc:description\" ],\n" + //
                        "    \"analyzer\" : \"fulltext\"\n" + //
                        "  }\n" + //
                        "}",
                "{\n" + //
                        "  \"query_string\" : {\n" + //
                        "    \"query\" : \"this AND that OR thus\",\n" + //
                        "    \"fields\" : [ \"dc:description\", \"dc:title\" ],\n" + //
                        "    \"analyzer\" : \"fulltext\"\n" + //
                        "  }\n" + //
                        "}");

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where /*+ES: OPERATOR(common) */ dc:title = 'this is bonsai cool'").toString();
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"common\" : {\n" + //
                "    \"dc:title\" : {\n" + //
                "      \"query\" : \"this is bonsai cool\"\n" + //
                "    }\n" + //
                "  }\n" + //
                "}", es);

    }

    @Test
    public void testConvertHintLike() throws Exception {
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where /*+ES: INDEX(some:field) ANALYZER(my_analyzer) */ dc:subjects LIKE 'foo*'").toString();
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"match\" : {\n" + //
                "    \"some:field\" : {\n" + //
                "      \"query\" : \"foo\",\n" + //
                "      \"type\" : \"phrase_prefix\",\n" + //
                "      \"analyzer\" : \"my_analyzer\"\n" + //
                "    }\n" + //
                "  }\n" + //
                "}", es);

        es = NxqlQueryConverter.toESQueryBuilder("select * from Document where /*+ES: INDEX(some:field) */ dc:subjects LIKE '%foo%'").toString();
        assertEqualsEvenUnderWindows("{\n" + //
                "  \"wildcard\" : {\n" + //
                "    \"some:field\" : {\n" + //
                "      \"wildcard\" : \"*foo*\"\n" + //
                "    }\n" + //
                "  }\n" + //
                "}", es);

    }

    @Test
    public void testConvertHintFulltext() throws Exception {
        // search on title and description, boost title
        String es = NxqlQueryConverter.toESQueryBuilder("select * from Document where /*+ES: INDEX(dc:title.fulltext^3,dc:description.fulltext) */ ecm:fulltext = 'foo'").toString();
        // fields are not ordered
        assertIn(es,
                "{\n" + //
                        "  \"simple_query_string\" : {\n" + //
                        "    \"query\" : \"foo\",\n" + //
                        "    \"fields\" : [ \"dc:title.fulltext^3\", \"dc:description.fulltext\" ],\n" + //
                        "    \"analyzer\" : \"fulltext\",\n" + //
                        "    \"default_operator\" : \"and\"\n" + //
                        "  }\n" + //
                        "}",
                "{\n" + //
                        "  \"simple_query_string\" : {\n" + //
                        "    \"query\" : \"foo\",\n" + //
                        "    \"fields\" : [ \"dc:description.fulltext\", \"dc:title.fulltext^3\" ],\n" + //
                        "    \"analyzer\" : \"fulltext\",\n" + //
                        "    \"default_operator\" : \"and\"\n" + //
                        "  }\n" + //
                        "}");
    }

    protected void assertEqualsEvenUnderWindows(String expected, String actual) {
        expected = normalizeString(expected);
        actual = normalizeString(actual);
        Assert.assertEquals(expected, actual);
    }

    private String normalizeString(String str) {
        if (SystemUtils.IS_OS_WINDOWS) {
            str = str.trim();
            str = str.replace("\n", "");
            str = str.replace("\r", "");
        }
        return str;
    }

    protected void assertIn(String actual, String... expected) {
        actual = normalizeString(actual);
        for (String exp : expected) {
            exp = normalizeString(exp);
            if (exp.equals(actual)) {
                return;
            }
        }
        // fail
        Assert.assertEquals(expected[0], actual);
    }

}
