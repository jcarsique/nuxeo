/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.pathsegment;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Service with a method generating a path segment (name) given a {@link DocumentModel} about to be created. Usually the
 * title is used to derive the path segment.
 */
public interface PathSegmentService {

    /**
     * @since 7.4
     */
    public static final String NUXEO_MAX_SEGMENT_SIZE_PROPERTY = "nuxeo.path.segment.maxsize";

    /**
     * Generate the path segment to use for a {@link DocumentModel} that's about to be created.
     *
     * @param doc the document
     * @return the path segment, which must not contain any {@code /} character
     */
    String generatePathSegment(DocumentModel doc);

    /**
     * Generate the path segment to use from a string.
     *
     * @param s the string
     * @return the path segment, which must not contain any {@code /} character
     * @since 5.9.2
     */
    String generatePathSegment(String s);

    /**
     * Return the path segment max size
     *
     * @since 7.4
     */
    int getMaxSize();
}
