/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.automation.test.service.enrichers;

import java.io.IOException;
import java.util.Map;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.io.services.enricher.AbstractContentEnricher;
import org.nuxeo.ecm.automation.io.services.enricher.ContentEnricherService;
import org.nuxeo.ecm.automation.io.services.enricher.RestEvaluationContext;

/**
 * @since 6.0
 * @deprecated since 7.10 see {@link ContentEnricherService}
 */
@Deprecated
public class MockEnricher extends AbstractContentEnricher {

    Map<String, String> parameters;

    @Override
    public void enrich(JsonGenerator jg, RestEvaluationContext ec) throws IOException {
        //
    }

    @Override
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }
}
