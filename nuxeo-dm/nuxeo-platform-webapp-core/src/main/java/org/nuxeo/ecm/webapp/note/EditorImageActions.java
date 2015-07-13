/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.note;

import java.util.List;
import java.util.Map;

import javax.servlet.http.Part;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Provides actions related to inserting an image for Note documents.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public interface EditorImageActions {

    String getSelectedTab();

    /**
     * @since 7.1
     */
    void setUploadedImage(Part uploadedImage);

    /**
     * @since 7.1
     */
    Part getUploadedImage();

    /**
     * @deprecated since 7.1, Part already holds a filename
     */
    @Deprecated
    void setUploadedImageName(String uploadedImageName);

    /**
     * @deprecated since 7.1, Part already holds a filename
     */
    @Deprecated
    String getUploadedImageName();

    String uploadImage();

    boolean getIsImageUploaded();

    boolean getInCreationMode();

    String getUrlForImage();

    // image searching related methods
    String searchImages();

    String searchVideos();

    List<DocumentModel> getSearchImageResults();

    /**
     * List of result of the searched the videos.
     *
     * @return The list of results.
     * @since 5.9.5
     */
    List<DocumentModel> getSearchVideosResults();

    boolean getHasSearchResults();

    /**
     * Return true if the search has results.
     *
     * @return If the search has results.
     * @since 5.9.5
     */
    boolean getHasSearchVideosResults();

    String getSearchKeywords();

    void setSearchKeywords(final String searchKeywords);

    List<Map<String, String>> getSizes();

    void setSelectedSize(final String selectedSize);

    String getSelectedSize();

    String getImageProperty();

    /**
     * Get the URL of a transcoded video for a specific format.
     *
     * @param video The video document.
     * @param type The type of video.
     * @return The URL of the selected video.
     * @since 5.9.5
     */
    String getURLVideo(DocumentModel video, String type);
}
