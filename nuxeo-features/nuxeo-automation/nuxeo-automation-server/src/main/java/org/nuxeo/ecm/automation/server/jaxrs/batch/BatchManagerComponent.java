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
package org.nuxeo.ecm.automation.server.jaxrs.batch;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.core.util.ComplexTypeJSONDecoder;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime Component implementing the {@link BatchManager} service with the {@link TransientStore}.
 *
 * @since 5.4.2
 */
public class BatchManagerComponent extends DefaultComponent implements BatchManager {

    protected static final String DEFAULT_CONTEXT = "None";

    protected static final Log log = LogFactory.getLog(BatchManagerComponent.class);

    protected static final String TRANSIENT_STORE_NAME = "BatchManagerCache";

    protected final AtomicInteger uploadInProgress = new AtomicInteger(0);

    static {
        ComplexTypeJSONDecoder.registerBlobDecoder(new JSONBatchBlobDecoder());
    }

    @Override
    public TransientStore getTransientStore() {
        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        return tss.getStore(TRANSIENT_STORE_NAME);
    }

    @Override
    public String initBatch() {
        return initBatch(null, null);
    }

    @Override
    public String initBatch(String batchId, String contextName) {
        Batch batch = initBatchInternal(batchId, contextName);
        return batch.getKey();
    }

    protected Batch initBatchInternal(String batchId, String contextName) {
        if (batchId == null || batchId.isEmpty()) {
            batchId = "batchId-" + UUID.randomUUID().toString();
        }
        if (contextName == null || contextName.isEmpty()) {
            contextName = DEFAULT_CONTEXT;
        }

        // That's the way of storing an empty entry
        getTransientStore().setCompleted(batchId, false);
        return new Batch(batchId);
    }

    public Batch getBatch(String batchId) {
        Map<String, Serializable> batchEntryParams = getTransientStore().getParameters(batchId);
        if (batchEntryParams == null) {
            if (!hasBatch(batchId)) {
                return null;
            }
            batchEntryParams = new HashMap<>();
        }
        return new Batch(batchId, batchEntryParams);
    }

    @Override
    public void addStream(String batchId, String index, InputStream is, String name, String mime) throws IOException {
        uploadInProgress.incrementAndGet();
        try {
            Batch batch = getBatch(batchId);
            if (batch == null) {
                batch = initBatchInternal(batchId, null);
            }
            batch.addFile(index, is, name, mime);
            log.debug(String.format("Added file %s [%s] to batch %s", index, name, batch.getKey()));
        } finally {
            uploadInProgress.decrementAndGet();
        }
    }

    @Override
    public void addStream(String batchId, String index, InputStream is, int chunkCount, int chunkIndex, String name,
            String mime, long fileSize) throws IOException {
        uploadInProgress.incrementAndGet();
        try {
            Batch batch = getBatch(batchId);
            if (batch == null) {
                batch = initBatchInternal(batchId, null);
            }
            batch.addChunk(index, is, chunkCount, chunkIndex, name, mime, fileSize);
            log.debug(String.format("Added chunk %s to file %s [%s] in batch %s", chunkIndex, index, name,
                    batch.getKey()));
        } finally {
            uploadInProgress.decrementAndGet();
        }
    }

    @Override
    public boolean hasBatch(String batchId) {
        return batchId != null && getTransientStore().exists(batchId);
    }

    @Override
    public List<Blob> getBlobs(String batchId) {
        return getBlobs(batchId, 0);
    }

    @Override
    public List<Blob> getBlobs(String batchId, int timeoutS) {
        if (uploadInProgress.get() > 0 && timeoutS > 0) {
            for (int i = 0; i < timeoutS * 5; i++) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (uploadInProgress.get() == 0) {
                    break;
                }
            }
        }
        Batch batch = getBatch(batchId);
        if (batch == null) {
            log.error("Unable to find batch with id " + batchId);
            return Collections.emptyList();
        }
        return batch.getBlobs();
    }

    @Override
    public Blob getBlob(String batchId, String fileIndex) {
        return getBlob(batchId, fileIndex, 0);
    }

    @Override
    public Blob getBlob(String batchId, String fileIndex, int timeoutS) {
        Blob blob = getBatchBlob(batchId, fileIndex);
        if (blob == null && timeoutS > 0 && uploadInProgress.get() > 0) {
            for (int i = 0; i < timeoutS * 5; i++) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                blob = getBatchBlob(batchId, fileIndex);
                if (blob != null) {
                    break;
                }
            }
        }
        if (!hasBatch(batchId)) {
            log.error("Unable to find batch with id " + batchId);
            return null;
        }
        return blob;
    }

    protected Blob getBatchBlob(String batchId, String fileIndex) {
        Blob blob = null;
        Batch batch = getBatch(batchId);
        if (batch != null) {
            blob = batch.getBlob(fileIndex);
        }
        return blob;
    }

    @Override
    public List<BatchFileEntry> getFileEntries(String batchId) {
        Batch batch = getBatch(batchId);
        if (batch == null) {
            return null;
        }
        return batch.getFileEntries();
    }

    @Override
    public BatchFileEntry getFileEntry(String batchId, String fileIndex) {
        Batch batch = getBatch(batchId);
        if (batch == null) {
            return null;
        }
        return batch.getFileEntry(fileIndex);
    }

    @Override
    public void clean(String batchId) {
        Batch batch = getBatch(batchId);
        if (batch != null) {
            batch.clean();
        }
    }

    @Override
    public Object execute(String batchId, String chainOrOperationId, CoreSession session,
            Map<String, Object> contextParams, Map<String, Object> operationParams) {
        List<Blob> blobs = getBlobs(batchId, getUploadWaitTimeout());
        if (blobs == null) {
            String message = String.format("Unable to find batch associated with id '%s'", batchId);
            log.error(message);
            throw new NuxeoException(message);
        }
        return execute(new BlobList(blobs), chainOrOperationId, session, contextParams, operationParams);
    }

    @Override
    public Object execute(String batchId, String fileIndex, String chainOrOperationId, CoreSession session,
            Map<String, Object> contextParams, Map<String, Object> operationParams) {
        Blob blob = getBlob(batchId, fileIndex, getUploadWaitTimeout());
        if (blob == null) {
            String message = String.format(
                    "Unable to find batch associated with id '%s' or file associated with index '%s'", batchId,
                    fileIndex);
            log.error(message);
            throw new NuxeoException(message);
        }
        return execute(blob, chainOrOperationId, session, contextParams, operationParams);
    }

    protected Object execute(Object blobInput, String chainOrOperationId, CoreSession session,
            Map<String, Object> contextParams, Map<String, Object> operationParams) {
        if (contextParams == null) {
            contextParams = new HashMap<>();
        }
        if (operationParams == null) {
            operationParams = new HashMap<>();
        }

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(blobInput);
        ctx.putAll(contextParams);

        try {
            Object result = null;
            AutomationService as = Framework.getLocalService(AutomationService.class);
            // Drag and Drop action category is accessible from the chain sub context as chain parameters
            result = as.run(ctx, chainOrOperationId, operationParams);
            return result;
        } catch (OperationException e) {
            log.error("Error while executing automation batch ", e);
            throw new NuxeoException(e);
        }
    }

    protected int getUploadWaitTimeout() {
        String t = Framework.getProperty("org.nuxeo.batch.upload.wait.timeout", "5");
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            log.error("Wrong number format for upload wait timeout property", e);
            return 5;
        }
    }

    @Override
    public Object executeAndClean(String batchId, String chainOrOperationId, CoreSession session,
            Map<String, Object> contextParams, Map<String, Object> operationParams) {
        try {
            return execute(batchId, chainOrOperationId, session, contextParams, operationParams);
        } finally {
            clean(batchId);
        }
    }
}
