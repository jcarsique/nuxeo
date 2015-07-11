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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.content.template.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.repository.RepositoryInitializationHandler;
import org.nuxeo.ecm.platform.content.template.listener.RepositoryInitializationListener;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class ContentTemplateServiceImpl extends DefaultComponent implements ContentTemplateService {

    public static final String NAME = "org.nuxeo.ecm.platform.content.template.service.TemplateService";

    public static final String FACTORY_DECLARATION_EP = "factory";

    public static final String FACTORY_BINDING_EP = "factoryBinding";

    public static final String POST_CONTENT_CREATION_HANDLERS_EP = "postContentCreationHandlers";

    private static final Log log = LogFactory.getLog(ContentTemplateServiceImpl.class);

    private final Map<String, ContentFactoryDescriptor> factories = new HashMap<String, ContentFactoryDescriptor>();

    private final Map<String, FactoryBindingDescriptor> factoryBindings = new HashMap<String, FactoryBindingDescriptor>();

    private final Map<String, ContentFactory> factoryInstancesByType = new HashMap<String, ContentFactory>();

    private final Map<String, ContentFactory> factoryInstancesByFacet = new HashMap<String, ContentFactory>();

    private PostContentCreationHandlerRegistry postContentCreationHandlers;

    private RepositoryInitializationHandler initializationHandler;

    @Override
    public void activate(ComponentContext context) {
        // register our Repo init listener
        initializationHandler = new RepositoryInitializationListener();
        initializationHandler.install();

        postContentCreationHandlers = new PostContentCreationHandlerRegistry();
    }

    @Override
    public void deactivate(ComponentContext context) {
        if (initializationHandler != null) {
            initializationHandler.uninstall();
        }
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        if (extensionPoint.equals(FACTORY_DECLARATION_EP)) {
            // store factories
            ContentFactoryDescriptor descriptor = (ContentFactoryDescriptor) contribution;
            factories.put(descriptor.getName(), descriptor);
        } else if (extensionPoint.equals(FACTORY_BINDING_EP)) {
            // store factories binding to types
            FactoryBindingDescriptor descriptor = (FactoryBindingDescriptor) contribution;
            if (factories.containsKey(descriptor.getFactoryName())) {
                String targetType = descriptor.getTargetType();
                String targetFacet = descriptor.getTargetFacet();

                // merge binding
                if (descriptor.getAppend()) {
                    descriptor = mergeFactoryBindingDescriptor(descriptor);
                }

                // store binding
                if (null != targetType) {
                    factoryBindings.put(targetType, descriptor);
                } else {
                    factoryBindings.put(targetFacet, descriptor);
                }

                // create factory instance : one instance per binding
                ContentFactoryDescriptor factoryDescriptor = factories.get(descriptor.getFactoryName());
                try {
                    ContentFactory factory = factoryDescriptor.getClassName().newInstance();
                    Boolean factoryOK = factory.initFactory(descriptor.getOptions(), descriptor.getRootAcl(),
                            descriptor.getTemplate());
                    if (!factoryOK) {
                        log.error("Error while initializing instance of factory " + factoryDescriptor.getName());
                        return;
                    }

                    // store initialized instance
                    if (null != targetType) {
                        factoryInstancesByType.put(targetType, factory);
                    } else {
                        factoryInstancesByFacet.put(targetFacet, factory);
                    }

                } catch (InstantiationException e) {
                    log.error("Error while creating instance of factory " + factoryDescriptor.getName() + " :"
                            + e.getMessage());
                } catch (IllegalAccessException e) {
                    log.error("Error while creating instance of factory " + factoryDescriptor.getName() + " :"
                            + e.getMessage());
                }
            } else {
                log.error("Factory Binding" + descriptor.getName() + " can not be registered since Factory "
                        + descriptor.getFactoryName() + " is not registered");
            }
        } else if (POST_CONTENT_CREATION_HANDLERS_EP.equals(extensionPoint)) {
            PostContentCreationHandlerDescriptor descriptor = (PostContentCreationHandlerDescriptor) contribution;
            postContentCreationHandlers.addContribution(descriptor);
        }
    }

    private FactoryBindingDescriptor mergeFactoryBindingDescriptor(FactoryBindingDescriptor newOne) {
        FactoryBindingDescriptor old = null;
        if (null != newOne.getTargetType()) {
            old = factoryBindings.get(newOne.getTargetType());
        } else {
            old = factoryBindings.get(newOne.getTargetFacet());
        }

        if (old != null) {
            log.info("FactoryBinding " + old.getName() + " is merging with " + newOne.getName());
            old.getOptions().putAll(newOne.getOptions());
            old.getRootAcl().addAll(newOne.getRootAcl());
            old.getTemplate().addAll(newOne.getTemplate());

            return old;
        }

        return newOne;
    }

    public ContentFactory getFactoryForType(String documentType) {
        return factoryInstancesByType.get(documentType);
    }

    public ContentFactory getFactoryForFacet(String facet) {
        return factoryInstancesByFacet.get(facet);
    }

    public void executeFactoryForType(DocumentModel createdDocument) {
        ContentFactory factory = getFactoryForType(createdDocument.getType());
        if (factory != null) {
            factory.createContentStructure(createdDocument);
        }
        Set<String> facets = createdDocument.getFacets();
        for (String facet : facets) {
            factory = getFactoryForFacet(facet);
            if (factory != null) {
                factory.createContentStructure(createdDocument);
            }
        }
    }

    @Override
    public void executePostContentCreationHandlers(CoreSession session) {
        for (PostContentCreationHandler handler : postContentCreationHandlers.getOrderedHandlers()) {
            handler.execute(session);
        }
    }

    // for testing
    public Map<String, ContentFactoryDescriptor> getFactories() {
        return factories;
    }

    public Map<String, FactoryBindingDescriptor> getFactoryBindings() {
        return factoryBindings;
    }

    public Map<String, ContentFactory> getFactoryInstancesByType() {
        return factoryInstancesByType;
    }

    public Map<String, ContentFactory> getFactoryInstancesByFacet() {
        return factoryInstancesByFacet;
    }

}
