/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.picture.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author Max Stepanov
 */
public interface ImagingService {

    /**
     * Returns the registered picture conversions.
     *
     * @since 7.1
     */
    List<PictureConversion> getPictureConversions();

    /**
     * Returns a {@link org.nuxeo.ecm.platform.picture.api.PictureConversion} given its {@code id}.
     *
     * @since 7.1
     */
    PictureConversion getPictureConversion(String id);

    /**
     * Crops an image.
     */
    Blob crop(Blob blob, int x, int y, int width, int height);

    /**
     * Resizes image.
     */
    Blob resize(Blob blob, String finalFormat, int width, int height, int depth);

    /**
     * Rotates an image.
     *
     * @param blob a Blob containing the image
     * @param angle the angle of the rotation
     * @return a Blob holding the rotated image
     */
    Blob rotate(Blob blob, int angle);

    /**
     * Retrieves metadata from an image contained in a {@link Blob}.
     *
     * @return the image metadata as a map String -> Object
     * @deprecated since 7.2. Please use instead
     * {@link org.nuxeo.binary.metadata.api.BinaryMetadataService#readMetadata(Blob)}
     */
    Map<String, Object> getImageMetadata(Blob blob);

    /**
     * Returns the mime-type for the given file.
     */
    String getImageMimeType(File file);

    /**
     * Returns the mime-type for the given Blob
     *
     * @since 5.7
     */
    String getImageMimeType(Blob blob);

    /**
     * Retrieves the {@link ImageInfo} of the {@link Blob} that is received as parameter.
     * <p>
     * The information provided by the <b>ImageInfo</b>, like width, height or format, is obtained using ImageMagick
     * (see http://www.imagemagick.org/script/index.php for more details on ImageMagick).
     *
     * @param blob the blob of a picture
     * @return the {@link ImageInfo} of a blob
     */
    ImageInfo getImageInfo(Blob blob);

    /**
     * Returns the value a configuration which name is received as parameter.
     *
     * @param configurationName the name of the configuration
     * @return the value of the configuration, which can be null in case no configuration with the specified name was
     *         registered
     */
    String getConfigurationValue(String configurationName);

    /**
     * Returns the value a configuration which name is received as parameter. In case no configuration with the
     * specified name was registered, the received <b>defaultValue</b> parameter will be returned.
     *
     * @param configurationName the name of the configuration
     * @param defaultValue the value of the configuration
     */
    String getConfigurationValue(String configurationName, String defaultValue);

    /**
     * Sets the value of a configuration which could be used by the ImagingService.
     *
     * @param configurationName the name of the configuration
     * @param configurationValue the value of the configuration
     */
    void setConfigurationValue(String configurationName, String configurationValue);

    /**
     * Computes a {@link PictureView} for the given {@code blob} and {@code pictureConversion}.
     *
     * @param convert true if the {@code blob} is converted to fit the {@code pictureConversion}, false if the
     *            {@code blob} is put as it in the PictureView (without any conversion)
     * @return the computed picture view
     * @since 5.7
     */
    PictureView computeViewFor(Blob blob, PictureConversion pictureConversion, boolean convert) throws IOException;

    /**
     * Computes a {@link PictureView} for the given {@code blob} and {@code pictureConversion}.
     *
     * @param imageInfo the {@code ImageInfo} to use when computing the view
     * @param convert true if the {@code blob} is converted to fit the {@code pictureConversion}, false if the
     *            {@code blob} is put as it in the PictureView (without any conversion)
     * @return the computed picture view
     * @since 5.7
     */
    PictureView computeViewFor(Blob blob, PictureConversion pictureConversion, ImageInfo imageInfo, boolean convert)
            throws IOException;

    /**
     * Computes a List of {@link PictureView}s for the given {@code blob} and {@code pictureConversions}.
     *
     * @param convert true if the {@code blob} is converted to fit each {@code PictureConversion}, false if the
     *            {@code blob} is put as it in the PictureView (without any conversion)
     * @return the computed picture views as a List of {@code PictureView}s
     * @since 5.7
     */
    List<PictureView> computeViewsFor(Blob blob, List<PictureConversion> pictureConversions, boolean convert)
            throws IOException;

    /**
     * Computes a List of {@link PictureView}s for the given {@code blob} and {@code pictureConversions}.
     *
     * @param imageInfo the {@code ImageInfo} to use when computing the view
     * @param convert true if the {@code blob} is converted to fit each {@code PictureConversion}, false if the
     *            {@code blob} is put as it in the PictureView (without any conversion)
     * @return the computed picture views as a List of {@code PictureView}s
     * @since 5.7
     */
    List<PictureView> computeViewsFor(Blob blob, List<PictureConversion> pictureConversions, ImageInfo imageInfo,
            boolean convert) throws IOException;

    /**
     * Computes a List of all {@link PictureView}s for each {@link Blob} of {@code blobs}.
     *
     * @param convert true if the {@code blob} is converted to fit each {@code PictureConversion}, false if the
     *            {@code blob} is put as it in the PictureView (without any conversion)
     * @since 5.7
     */
    List<List<PictureView>> computeViewsFor(List<Blob> blobs, List<PictureConversion> pictureConversions,
            boolean convert) throws IOException;

    /**
     * Computes a List of all {@link PictureView}s for each {@link Blob} of {@code blobs}.
     *
     * @param imageInfo the {@code ImageInfo} to use when computing the view
     * @param convert true if the {@code blob} is converted to fit each {@code PictureConversion}, false if the
     *            {@code blob} is put as it in the PictureView (without any conversion)
     * @since 5.7
     */
    List<List<PictureView>> computeViewsFor(List<Blob> blobs, List<PictureConversion> pictureConversions,
            ImageInfo imageInfo, boolean convert) throws IOException;

    /**
     * Compute all the registered {@link PictureConversion} For each picture template the
     * {@link ImagingService#computeViewFor(Blob, PictureConversion, ImageInfo, boolean)} method is call
     *
     * @since 7.1
     */
    List<PictureView> computeViewsFor(DocumentModel doc, Blob blob, ImageInfo imageInfo, boolean convert) throws IOException;

}
