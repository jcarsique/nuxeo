/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.automation.scripting.internals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.scripting.DocumentWrapper;
import org.nuxeo.ecm.automation.core.util.DataModelProperties;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Class injected/published in Nashorn engine to execute automation service.
 *
 * @since 7.2
 */
public class AutomationMapper {

    protected final CoreSession session;
    public OperationContext ctx;

    public AutomationMapper(CoreSession session, OperationContext operationContext) {
        this.session = session;
        ctx = operationContext;
    }

    public Object executeOperation(String opId, Object input, ScriptObjectMirror parameters) throws Exception {
        AutomationService automationService = Framework.getService(AutomationService.class);
        populateContext(ctx, input);
        Map<String, Object> params = unwrapParameters(parameters);
        Object output = automationService.run(ctx, opId, params);
        if (output instanceof DocumentModel) {
            return new DocumentWrapper(ctx.getCoreSession(), (DocumentModel) output);
        } else if (output instanceof DocumentModelList) {
            List<DocumentWrapper> docs = new ArrayList<>();
            for (DocumentModel doc : (DocumentModelList) output) {
                docs.add(new DocumentWrapper(ctx.getCoreSession(), doc));
            }
            return docs;
        }
        return output;
    }

    protected Map<String, Object> unwrapParameters(ScriptObjectMirror parameters) {
        Map<String, Object> params = new HashMap<String, Object>();
        for (String k : parameters.keySet()) {
            Object value = parameters.get(k);
            if (value instanceof ScriptObjectMirror) {
                ScriptObjectMirror jso = (ScriptObjectMirror) value;
                if (jso.isArray()) {
                    params.put(k, MarshalingHelper.unwrap(jso));
                } else {
                    params.put(k, extractProperties(jso));
                }
            } else {
                params.put(k, value);
            }
        }
        return params;
    }

    protected void populateContext(OperationContext ctx, Object input) {
        if (input instanceof ScriptObjectMirror) {
            ctx.setInput(extractProperties((ScriptObjectMirror) input));
        } else if (input instanceof DocumentWrapper) {
            ctx.setInput(((DocumentWrapper) input).getDoc());
        } else if (input instanceof List<?>) {
            DocumentModelList docs = new DocumentModelListImpl();
            List<?> l = (List<?>) input;
            for (Object item : l) {
                if (item instanceof DocumentWrapper) {
                    docs.add(((DocumentWrapper) item).getDoc());
                }
            }
            if (docs.size() == l.size() && docs.size() > 0) {
                ctx.setInput(docs);
            }
        } else {
            ctx.setInput(input);
        }
    }

    protected Properties extractProperties(ScriptObjectMirror parameters) {
        DataModelProperties props = new DataModelProperties();
        Map<String, Object> data = MarshalingHelper.unwrapMap(parameters);
        for (String k : data.keySet()) {
            props.getMap().put(k, (Serializable) data.get(k));
        }
        return props;
    }


}
