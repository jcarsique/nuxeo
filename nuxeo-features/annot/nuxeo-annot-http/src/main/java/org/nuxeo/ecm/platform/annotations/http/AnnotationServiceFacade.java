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

package org.nuxeo.ecm.platform.annotations.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationManager;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Alexandre Russel
 */
public class AnnotationServiceFacade {

    private static final String TRANSIENT_GRAPH_TYPE = "jena";

    private AnnotationsService service;

    private final AnnotationManager manager = new AnnotationManager();

    public AnnotationServiceFacade() {
    }

    protected AnnotationsService getService() {
        if (service == null) {
            service = Framework.getService(AnnotationsService.class);
        }
        return service;
    }

    public void query(String uri, OutputStream outputStream, NuxeoPrincipal name) {
        List<Annotation> annotations;
        try {
            annotations = getService().queryAnnotations(new URI(uri), null, name);
        } catch (URISyntaxException e) {
            throw new NuxeoException(e);
        }
        List<Statement> statements = new ArrayList<Statement>();
        for (Annotation annotation : annotations) {
            statements.addAll(annotation.getStatements());
        }
        RelationManager service = Framework.getService(RelationManager.class);
        Graph graph = service.getTransientGraph(TRANSIENT_GRAPH_TYPE);
        graph.add(statements);
        try {
            outputStream.write("<?xml version='1.0'?>\n".getBytes());
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        graph.write(outputStream, null, null);
    }

    public void getAnnotation(String annId, NuxeoPrincipal name, OutputStream os, String baseUrl) {
        Annotation annotation = getService().getAnnotation(annId, name, baseUrl);
        manager.writeAnnotation(os, annotation);
    }

    public void updateAnnotation(InputStream is, NuxeoPrincipal name, OutputStream outputStream, String baseUrl) {
        Annotation annotation = manager.getAnnotation(is);
        annotation = getService().updateAnnotation(annotation, name, baseUrl);
        manager.writeAnnotation(outputStream, annotation);
    }

    public String getAnnotationBody(String id, NuxeoPrincipal name, String baseUrl) {
        Annotation annotation = getService().getAnnotation(id, name, baseUrl);
        return annotation.getBodyAsText();
    }

    public void createAnnotation(InputStream inputStream, NuxeoPrincipal name, OutputStream outputStream,
            String baseUrl) {
        Annotation annotation = manager.getAnnotation(inputStream);
        annotation = getService().addAnnotation(annotation, name, baseUrl);
        manager.writeAnnotation(outputStream, annotation);
    }

    public void delete(String annId, NuxeoPrincipal name, String baseUrl) {
        Annotation annotation = getService().getAnnotation(annId, name, baseUrl);
        getService().deleteAnnotation(annotation, name);
    }

    public void deleteFor(String uri, String annId, NuxeoPrincipal name, String baseUrl) {
        try {
            Annotation annotation = getService().getAnnotation(annId, name, baseUrl);
            getService().deleteAnnotationFor(new URI(uri), annotation, name);
        } catch (URISyntaxException e) {
            throw new NuxeoException(e);
        }
    }

}
