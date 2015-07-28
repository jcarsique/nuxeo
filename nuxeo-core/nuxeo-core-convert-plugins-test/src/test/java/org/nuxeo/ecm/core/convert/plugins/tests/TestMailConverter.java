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

package org.nuxeo.ecm.core.convert.plugins.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author Anahide Tchertchian
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestMailConverter extends SimpleConverterTest {

    private static final String CONVERTER_NAME = "rfc822totext";

    private static Blob getTestBlob(String filePath) throws IOException {
        File file = FileUtils.getResourceFileFromContext(filePath);
        return Blobs.createBlob(file);
    }

    @Inject
    protected ConversionService cs;

    @Test
    public void testTextEmailTransformation() throws Exception {
        BlobHolder bh;
        if (isWindows()) {
            bh = cs.convert(CONVERTER_NAME, getBlobFromPath("test-docs\\email\\text.eml"), null);
        } else {
            bh = cs.convert(CONVERTER_NAME, getBlobFromPath("test-docs/email/text.eml"), null);
        }
        assertNotNull(bh);

        Blob result = bh.getBlob();
        assertNotNull(result);
        assertEquals("text/plain", result.getMimeType());

        Blob expected;
        if (isWindows()) {
            expected = getTestBlob("test-docs\\email\\text.txt");
        } else {
            expected = getTestBlob("test-docs/email/text.txt");
        }
        assertEquals(expected.getString().trim(), result.getString().trim());
    }

    protected boolean textEquals(String txt1, String txt2) {
        txt1 = txt1.replaceAll("\n\r", " ");
        txt1 = txt1.replaceAll("\n", " ");

        txt2 = txt2.replaceAll("\n\r", " ");
        txt2 = txt2.replaceAll("\n", " ");

        txt1 = txt1.trim();
        txt2 = txt2.trim();

        return txt1.equals(txt2);
    }

    @Test
    public void testTextAndHtmlEmailTransformation() throws Exception {
        BlobHolder bh;
        if (isWindows()) {
            bh = cs.convert(CONVERTER_NAME, getBlobFromPath("test-docs\\email\\text_and_html_with_attachments.eml"),
                    null);
        } else {
            bh = cs.convert(CONVERTER_NAME, getBlobFromPath("test-docs/email/text_and_html_with_attachments.eml"), null);
        }
        assertNotNull(bh);

        Blob result = bh.getBlob();
        assertNotNull(result);
        assertEquals("text/plain", result.getMimeType());

        String actual = result.getString();
        String expected;
        if (isWindows()) {
            expected = getTestBlob("test-docs\\email\\text_and_html_with_attachments.txt").getString();
        } else {
            expected = getTestBlob("test-docs/email/text_and_html_with_attachments.txt").getString();
        }
        assertTrue(FileUtils.areFilesContentEquals(expected.trim(), actual.trim()));
    }

    @Test
    public void testOnlyHtmlEmailTransformation() throws Exception {
        BlobHolder bh;
        if (isWindows()) {
            bh = cs.convert(CONVERTER_NAME, getBlobFromPath("test-docs\\email\\only_html_with_attachments.eml"), null);
        } else {
            bh = cs.convert(CONVERTER_NAME, getBlobFromPath("test-docs/email/only_html_with_attachments.eml"), null);
        }
        assertNotNull(bh);

        Blob result = bh.getBlob();
        assertNotNull(result);
        assertEquals("text/plain", result.getMimeType());
        Blob expected;
        if (isWindows()) {
            expected = getTestBlob("test-docs\\email\\only_html_with_attachments.txt");
        } else {
            expected = getTestBlob("test-docs/email/only_html_with_attachments.txt");
        }
        assertTrue(FileUtils.areFilesContentEquals(expected.getString().trim(), result.getString().trim()));
    }

}
