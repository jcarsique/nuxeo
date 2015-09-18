/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 *
 */
package org.nuxeo.ecm.automation.server.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.server", "org.nuxeo.ecm.core.cache" })
public class BatchManagerTest {

    @Test
    public void testServiceRegistred() {
        BatchManager bm = Framework.getLocalService(BatchManager.class);
        assertNotNull(bm);
    }

    @Test
    public void testBatchCleanup() throws IOException {
        BatchManager bm = Framework.getLocalService(BatchManager.class);

        String batchId = bm.initBatch(null, null);
        assertNotNull(batchId);

        for (int i = 0; i < 10; i++) {
            bm.addStream(batchId, "" + i, new ByteArrayInputStream(("SomeContent" + i).getBytes()), i + ".txt",
                    "text/plain");
        }

        List<Blob> blobs = bm.getBlobs(batchId);
        assertNotNull(blobs);
        Assert.assertEquals(10, blobs.size());

        Assert.assertEquals("4.txt", blobs.get(4).getFilename());
        Assert.assertEquals("SomeContent7", blobs.get(7).getString());

        FileBlob fileBlob = (FileBlob) blobs.get(9);
        File tmplFile = fileBlob.getFile();
        assertNotNull(tmplFile);
        assertTrue(tmplFile.exists());

        bm.clean(batchId);
        // File is still there while transient store GC is not called
        assertTrue(tmplFile.exists());

        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore(null);
        ts.doGC();
        assertFalse(tmplFile.exists());
    }

}
