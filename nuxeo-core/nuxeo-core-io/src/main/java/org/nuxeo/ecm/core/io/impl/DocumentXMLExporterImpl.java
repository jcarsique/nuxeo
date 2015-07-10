/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.core.io.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.DocumentXMLExporter;
import org.nuxeo.ecm.core.io.impl.plugins.TypedSingleDocumentReader;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDocumentWriter;
import org.xml.sax.InputSource;

/**
 * Default implementation of a {@link DocumentXMLExporter}.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class DocumentXMLExporterImpl implements DocumentXMLExporter {

    private static final long serialVersionUID = 4086449614391137730L;

    /**
     * {@inheritDoc}
     */
    public InputStream exportXML(DocumentModel doc, CoreSession session) {

        byte[] xmlExportByteArray = exportXMLAsByteArray(doc, session);
        return new ByteArrayInputStream(xmlExportByteArray);
    }

    /**
     * {@inheritDoc}
     */
    public InputSource exportXMLAsInputSource(DocumentModel doc, CoreSession session) {

        InputStream xmlExportInputStream = exportXML(doc, session);
        return new InputSource(xmlExportInputStream);
    }

    /**
     * {@inheritDoc}
     */
    public final byte[] exportXMLAsByteArray(DocumentModel doc, CoreSession session) {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DocumentWriter documentWriter = new XMLDocumentWriter(outputStream);
        DocumentReader documentReader = new TypedSingleDocumentReader(session, doc);

        DocumentPipe pipe = new DocumentPipeImpl();
        pipe.setReader(documentReader);
        pipe.setWriter(documentWriter);

        try {
            pipe.run();
        } catch (IOException e) {
            throw new NuxeoException("Error while trying to export the document to XML.", e);
        } finally {
            if (documentReader != null) {
                documentReader.close();
            }
            if (documentWriter != null) {
                documentWriter.close();
            }
        }

        return outputStream.toByteArray();
    }
}
