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
 *     Nuxeo
 *     Florent Guillaume
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.convert.plugins;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.StandardConversionTask;
import org.artofsolving.jodconverter.document.DocumentFamily;
import org.artofsolving.jodconverter.document.DocumentFormat;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.extension.ExternalConverter;
import org.nuxeo.ecm.platform.convert.ooomanager.OOoManagerService;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.FileSource;

import com.sun.star.uno.RuntimeException;

/**
 * Converter based on JOD which uses an external OpenOffice process to do actual conversions.
 */
public class JODBasedConverter implements ExternalConverter {

    protected static final String TMP_PATH_PARAMETER = "TmpDirectory";

    private static final Log log = LogFactory.getLog(JODBasedConverter.class);

    /**
     * Boolean conversion parameter for PDF/A-1.
     *
     * @since 5.6
     */
    public static final String PDFA1_PARAM = "PDF/A-1";

    /**
     * Boolean parameter to force update of the document TOC
     *
     * @since 5.6
     */
    public static final String UPDATE_INDEX_PARAM = StandardConversionTask.UPDATE_DOCUMENT_INDEX;

    protected static final Map<DocumentFamily, String> PDF_FILTER_NAMES = new HashMap<DocumentFamily, String>();
    {
        PDF_FILTER_NAMES.put(DocumentFamily.TEXT, "writer_pdf_Export");
        PDF_FILTER_NAMES.put(DocumentFamily.SPREADSHEET, "calc_pdf_Export");
        PDF_FILTER_NAMES.put(DocumentFamily.PRESENTATION, "impress_pdf_Export");
        PDF_FILTER_NAMES.put(DocumentFamily.DRAWING, "draw_pdf_Export");
    }

    protected ConverterDescriptor descriptor;

    protected String getDestinationMimeType() {
        return descriptor.getDestinationMimeType();
    }

    /**
     * Returns the destination format for the given plugin.
     * <p>
     * It takes the actual destination mimetype from the plugin configuration.
     *
     * @param sourceFormat the source format
     * @param pdfa1 true if PDF/A-1 is required
     */
    protected DocumentFormat getDestinationFormat(OfficeDocumentConverter documentConverter,
            DocumentFormat sourceFormat, boolean pdfa1) {
        String mimeType = getDestinationMimeType();
        DocumentFormat destinationFormat = documentConverter.getFormatRegistry().getFormatByMediaType(mimeType);
        if ("application/pdf".equals(mimeType)) {
            destinationFormat = extendPDFFormat(sourceFormat, destinationFormat, pdfa1);
        }
        return destinationFormat;
    }

    protected DocumentFormat extendPDFFormat(DocumentFormat sourceFormat, DocumentFormat defaultFormat, boolean pdfa1) {
        DocumentFamily sourceFamily = sourceFormat.getInputFamily();
        String sourceMediaType = sourceFormat.getMediaType();
        DocumentFormat pdfFormat = new DocumentFormat(pdfa1 ? "PDF/A-1" : "PDF", "pdf", "application/pdf");
        Map<DocumentFamily, Map<String, ?>> storePropertiesByFamily = new HashMap<DocumentFamily, Map<String, ?>>();
        Map<DocumentFamily, Map<String, ?>> defaultStorePropertiesByFamily = defaultFormat.getStorePropertiesByFamily();
        for (DocumentFamily family : defaultStorePropertiesByFamily.keySet()) {
            if (family.equals(sourceFamily)) {
                continue;
            }
            storePropertiesByFamily.put(family, defaultStorePropertiesByFamily.get(family));
        }
        storePropertiesByFamily.put(sourceFamily,
                extendPDFStoreProperties(sourceMediaType, pdfa1, defaultStorePropertiesByFamily.get(sourceFamily)));
        pdfFormat.setStorePropertiesByFamily(storePropertiesByFamily);
        return pdfFormat;
    }

    protected Map<String, Object> extendPDFStoreProperties(String mediatype, boolean pdfa1,
            Map<String, ?> originalProperties) {
        Map<String, Object> extendedProperties = new HashMap<String, Object>();
        for (Map.Entry<String, ?> entry : originalProperties.entrySet()) {
            extendedProperties.put(entry.getKey(), entry.getValue());
        }
        if ("text/html".equals(mediatype)) {
            extendedProperties.put("FilterName", "writer_web_pdf_Export");
        }
        if (pdfa1) {
            Map<String, Object> filterData = new HashMap<String, Object>();
            filterData.put("SelectPdfVersion", Integer.valueOf(1)); // PDF/A-1
            filterData.put("UseTaggedPDF", Boolean.TRUE); // per spec
            extendedProperties.put("FilterData", filterData);
        }
        return extendedProperties;
    }

    /**
     * Returns the format for the file passed as a parameter.
     * <p>
     * We will ask the mimetype registry service to sniff its mimetype.
     *
     * @return DocumentFormat for the given file
     */
    private static DocumentFormat getSourceFormat(OfficeDocumentConverter documentConverter, File file)
            throws Exception {
        MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
        String mimetypeStr = mimetypeRegistry.getMimetypeFromFile(file);
        DocumentFormat format = documentConverter.getFormatRegistry().getFormatByMediaType(mimetypeStr);
        return format;
    }

    /**
     * Returns the DocumentFormat for the given mimetype.
     *
     * @return DocumentFormat for the given mimetype
     */
    private static DocumentFormat getSourceFormat(OfficeDocumentConverter documentConverter, String mimetype) {
        return documentConverter.getFormatRegistry().getFormatByMediaType(mimetype);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @SuppressWarnings("unchecked")
    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {
        blobHolder = new UTF8CharsetConverter().convert(blobHolder, parameters);
        Blob inputBlob;
        String blobPath;
        try {
            inputBlob = blobHolder.getBlob();
            blobPath = blobHolder.getFilePath();
        } catch (ClientException e) {
            throw new ConversionException("Error while getting Blob", e);
        }
        if (inputBlob == null) {
            return null;
        }

        OfficeDocumentConverter documentConverter = newDocumentConverter();
        // This plugin do deal only with one input source.
        String sourceMimetype = inputBlob.getMimeType();

        boolean pdfa1 = parameters != null && Boolean.TRUE.equals(parameters.get(PDFA1_PARAM));

        File sourceFile = null;
        File outFile = null;
        File[] files = null;
        try {

            // If the input blob has the HTML mime type, make sure the
            // charset meta is present, add it if not
            if ("text/html".equals(sourceMimetype)) {
                inputBlob = checkCharsetMeta(inputBlob);
            }

            // Get original file extension
            String ext = inputBlob.getFilename();
            int dotPosition = ext.lastIndexOf('.');
            if (dotPosition == -1) {
                ext = ".bin";
            } else {
                ext = ext.substring(dotPosition);
            }
            // Copy in a file to be able to read it several time
            sourceFile = File.createTempFile("NXJOOoConverterDocumentIn", ext);
            InputStream stream = inputBlob.getStream();
            // if (stream.markSupported()) {
            // stream.reset(); // works on a JCRBlobInputStream
            // }
            FileUtils.copyToFile(stream, sourceFile);

            DocumentFormat sourceFormat = null;
            if (sourceMimetype != null) {
                // Try to fetch it from the registry.
                sourceFormat = getSourceFormat(documentConverter, sourceMimetype);
            }
            // If not found in the registry or not given as a parameter.
            // Try to sniff ! What does that smell ? :)
            if (sourceFormat == null) {
                sourceFormat = getSourceFormat(documentConverter, sourceFile);
            }

            // From plugin settings because we know the destination
            // mimetype.
            DocumentFormat destinationFormat = getDestinationFormat(documentConverter, sourceFormat, pdfa1);

            // allow HTML2PDF filtering

            List<Blob> blobs = new ArrayList<Blob>();

            if (descriptor.getDestinationMimeType().equals("text/html")) {
                String tmpDirPath = getTmpDirectory();
                File myTmpDir = new File(tmpDirPath + "/JODConv_" + System.currentTimeMillis());
                boolean created = myTmpDir.mkdir();
                if (!created) {
                    throw new ConversionException("Unable to create temp dir");
                }

                outFile = new File(myTmpDir.getAbsolutePath() + "/" + "NXJOOoConverterDocumentOut."
                        + destinationFormat.getExtension());

                created = outFile.createNewFile();
                if (!created) {
                    throw new ConversionException("Unable to create temp file");
                }

                log.debug("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
                log.debug("Input File = " + outFile.getAbsolutePath());
                // Perform the actual conversion.
                documentConverter.convert(sourceFile, outFile, destinationFormat);

                files = myTmpDir.listFiles();
                for (File file : files) {
                    Blob blob = StreamingBlob.createFromByteArray(new FileSource(file).getBytes());
                    blob.setFilename(file.getName());
                    blobs.add(blob);
                    // add a blob for the index
                    if (file.getName().equals(outFile.getName())) {
                        File indexFile = File.createTempFile("idx-", file.getName());
                        FileUtils.copy(file, indexFile);
                        Blob indexBlob = StreamingBlob.createFromByteArray(new FileSource(indexFile).getBytes());
                        indexBlob.setFilename("index.html");
                        blobs.add(0, indexBlob);
                        indexFile.delete();
                    }
                }

            } else {
                outFile = File.createTempFile("NXJOOoConverterDocumentOut", '.' + destinationFormat.getExtension());

                // Perform the actual conversion.
                documentConverter.convert(sourceFile, outFile, destinationFormat, parameters);

                // load the content in the file since it will be deleted
                // soon: TODO: find a way to stream it to the streaming
                // server without loading it all in memory
                Blob blob = StreamingBlob.createFromByteArray(new FileSource(outFile).getBytes(),
                        getDestinationMimeType());
                blobs.add(blob);
            }
            return new SimpleCachableBlobHolder(blobs);
        } catch (Exception e) {
            String msg = String.format("An error occurred trying to convert file %s to from %s to %s", blobPath,
                    sourceMimetype, getDestinationMimeType());
            throw new ConversionException(msg, e);
        } finally {
            if (sourceFile != null) {
                sourceFile.delete();
            }
            if (outFile != null) {
                outFile.delete();
            }

            if (files != null) {
                for (File file : files) {
                    if (file.exists()) {
                        file.delete();
                    }
                }
            }
        }

    }

    protected OfficeDocumentConverter newDocumentConverter() throws ConversionException {
        OfficeDocumentConverter documentConverter = null;
        try {
            OOoManagerService oooManagerService = Framework.getService(OOoManagerService.class);
            documentConverter = oooManagerService.getDocumentConverter();
        } catch (Exception e) {
        }
        if (documentConverter == null) {
            throw new ConversionException("Could not connect to the remote OpenOffice server");
        }
        return documentConverter;
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public ConverterCheckResult isConverterAvailable() {
        ConverterCheckResult result = new ConverterCheckResult();
        try {
            OOoManagerService oooManagerService = Framework.getService(OOoManagerService.class);
            if (!oooManagerService.isOOoManagerStarted()) {
                result.setAvailable(false);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not get OOoManagerService");
        }
        return result;
    }

    protected String getTmpDirectory() {
        String tmp = null;
        Map<String, String> parameters = descriptor.getParameters();
        if (parameters != null && parameters.containsKey(TMP_PATH_PARAMETER)) {
            tmp = parameters.get(TMP_PATH_PARAMETER);
        }
        if (tmp == null) {
            tmp = System.getProperty("java.io.tmpdir");
        }
        return tmp;
    }

    /**
     * Checks if the {@code inputBlob} string contains a {@code charset} meta tag. If not, add it.
     *
     * @param inputBlob the input blob
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected Blob checkCharsetMeta(Blob inputBlob) throws IOException {

        String charset = inputBlob.getEncoding();
        if (!StringUtils.isEmpty(charset)) {
            Pattern charsetMetaPattern = Pattern.compile(String.format("content=\"text/html;\\s*charset=%s\"", charset));
            Matcher charsetMetaMatcher = charsetMetaPattern.matcher(inputBlob.getString());
            if (!charsetMetaMatcher.find()) {
                String charsetMetaTag = String.format(
                        "<META http-equiv=\"Content-Type\" content=\"text/html; charset=%s\">", charset);
                StringBuilder sb = new StringBuilder(charsetMetaTag);
                sb.append(new String(inputBlob.getByteArray(), charset));
                Blob blobWithCharsetMetaTag = new StringBlob(sb.toString(), "text/html", charset);
                blobWithCharsetMetaTag.setFilename(inputBlob.getFilename());
                return blobWithCharsetMetaTag;
            }
        }
        return inputBlob;
    }
}
