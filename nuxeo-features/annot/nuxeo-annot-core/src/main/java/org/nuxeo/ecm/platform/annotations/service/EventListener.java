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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.service;

import java.security.Principal;

import org.nuxeo.ecm.platform.annotations.api.Annotation;

/**
 * @author Alexandre Russel
 */
public interface EventListener {

    void beforeAnnotationCreated(Principal principal, Annotation annotation);

    void afterAnnotationCreated(Principal principal, Annotation annotation);

    void beforeAnnotationRead(Principal principal, String annotationId);

    void afterAnnotationRead(Principal principal, Annotation annotation);

    void beforeAnnotationUpdated(Principal principal, Annotation annotation);

    void afterAnnotationUpdated(Principal principal, Annotation annotation);

    void beforeAnnotationDeleted(Principal principal, Annotation annotation);

    void afterAnnotationDeleted(Principal principal, Annotation annotation);
}
