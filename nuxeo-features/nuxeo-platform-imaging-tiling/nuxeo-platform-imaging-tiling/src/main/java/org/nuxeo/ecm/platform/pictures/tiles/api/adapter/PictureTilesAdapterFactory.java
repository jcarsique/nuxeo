/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 *
 */
package org.nuxeo.ecm.platform.pictures.tiles.api.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTilingService;
import org.nuxeo.runtime.api.Framework;

/**
 * Factory method for the DocumentModelAdapter for PictureTiles Contains the logic to choose the correct implementation
 * according to DocumentModel.
 *
 * @author tiry
 */
public class PictureTilesAdapterFactory implements DocumentAdapterFactory {

    private static final Log log = LogFactory.getLog(PictureTilesAdapterFactory.class);

    protected static final String ORIGINAL_JPEG_VIEW_NAME = "OriginalJpeg";

    /**
     * @deprecated since 7.2. The Original view does not exist anymore. See NXP-16070.
     */
    @Deprecated
    protected static final String ORIGINAL_VIEW_NAME = "Original";

    public Object getAdapter(DocumentModel doc, Class itf) {
        PictureTilingService tilingService = Framework.getService(PictureTilingService.class);
        String blobProperty = tilingService.getBlobProperty(doc.getType());
        PictureTilesAdapter ptAdapter = getPictureTilesAdapterFor(doc, blobProperty);
        if (ptAdapter != null) {
            return ptAdapter;
        }
        // else fall back on default
        if (doc.hasSchema("file")) {
            Blob blob = (Blob) doc.getProperty("file", "content");
            if (blob == null) {
                return null;
            }
            PictureTilesAdapter adapter = new PictureTilesAdapterImpl(doc, "file:content");
            adapter.setFileName((String) doc.getProperty("file", "filename"));
            return adapter;
        } else {
            return new PictureTilesAdapterImpl(doc);
        }
    }

    private PictureTilesAdapter getPictureTilesAdapterFor(DocumentModel doc, String blobProperty)
            {
        if (blobProperty != null) {
            try {
                return getPictureTilesAdapter(doc, blobProperty);
            } catch (PropertyException | IndexOutOfBoundsException e) {
                return getPictureTilesAdapterForPicture(doc);
            }
        }
        return getPictureTilesAdapterForPicture(doc);
    }

    private PictureTilesAdapter getPictureTilesAdapterForPicture(DocumentModel doc) {
        if (doc.hasSchema("picture")) {
            PictureResourceAdapter adapter = doc.getAdapter(PictureResourceAdapter.class);
            // try OriginalJpeg view xpath
            String blobProperty = adapter.getViewXPath(ORIGINAL_JPEG_VIEW_NAME) + "content";
            return getPictureTilesAdapter(doc, blobProperty);
        }
        return null;
    }

    private PictureTilesAdapter getPictureTilesAdapter(DocumentModel doc, String blobProperty) {
        Blob blob = (Blob) doc.getPropertyValue(blobProperty);
        if (blob != null) {
            PictureTilesAdapter adapter = new PictureTilesAdapterImpl(doc, blobProperty);
            adapter.setFileName(blob.getFilename());
            return adapter;
        }
        return null;
    }

}
