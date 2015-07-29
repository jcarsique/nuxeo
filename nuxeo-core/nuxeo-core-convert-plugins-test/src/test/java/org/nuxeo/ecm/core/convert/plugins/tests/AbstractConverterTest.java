/*
 * (C) Copyright 2006-20015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.core.convert.plugins.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.SystemUtils;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractConverterTest {

    /**
     * @deprecated Since 7.4. Use {@link SystemUtils#IS_OS_WINDOWS}
     */
    @Deprecated
    protected final boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    protected final BlobHolder getBlobFromPath(String path) throws IOException {
        File file = FileUtils.getResourceFileFromContext(path);
        assertTrue(file.length() > 0);
        return new SimpleBlobHolder(Blobs.createBlob(file));
    }

    protected String doTestTextConverter(String srcMT, String converter, String fileName) throws Exception {
        ConversionService cs = Framework.getLocalService(ConversionService.class);
        String converterName = cs.getConverterName(srcMT, "text/plain");
        assertEquals(converter, converterName);

        BlobHolder hg;
        if (SystemUtils.IS_OS_WINDOWS) {
            hg = getBlobFromPath("test-docs\\" + fileName);
        } else {
            hg = getBlobFromPath("test-docs/" + fileName);
        }

        Map<String, Serializable> parameters = new HashMap<>();
        BlobHolder result = cs.convert(converterName, hg, parameters);
        assertNotNull(result);

        String textContent = result.getBlob().getString();
        checkTextConversion(textContent);
        return textContent;
    }

    protected String doTestAny2TextConverter(String srcMT, String converterName, String fileName) throws Exception {
        ConversionService cs = Framework.getLocalService(ConversionService.class);

        BlobHolder hg;
        if (SystemUtils.IS_OS_WINDOWS) {
            hg = getBlobFromPath("test-docs\\" + fileName);
        } else {
            hg = getBlobFromPath("test-docs/" + fileName);
        }
        hg.getBlob().setMimeType(srcMT);

        Map<String, Serializable> parameters = new HashMap<>();
        BlobHolder result = cs.convert(converterName, hg, parameters);
        assertNotNull(result);

        String textContent = result.getBlob().getString();
        checkAny2TextConversion(textContent);
        return textContent;
    }

    protected String doTestArabicTextConverter(String srcMT, String converter, String fileName) throws Exception {
        ConversionService cs = Framework.getLocalService(ConversionService.class);
        assertTrue(cs.isConverterAvailable(converter).isAvailable());

        String converterName = cs.getConverterName(srcMT, "text/plain");
        assertEquals(converter, converterName);

        BlobHolder hg;
        if (SystemUtils.IS_OS_WINDOWS) {
            hg = getBlobFromPath("test-docs\\right-to-left\\" + fileName);
        } else {
            hg = getBlobFromPath("test-docs/right-to-left/" + fileName);
        }

        BlobHolder result = cs.convert(converterName, hg, null);
        assertNotNull(result);

        String textContent = result.getBlob().getString();
        checkArabicConversion(textContent);
        return textContent;
    }

    protected abstract void checkTextConversion(String textContent);

    protected abstract void checkAny2TextConversion(String textContent);

    protected abstract void checkArabicConversion(String textContent);

}
