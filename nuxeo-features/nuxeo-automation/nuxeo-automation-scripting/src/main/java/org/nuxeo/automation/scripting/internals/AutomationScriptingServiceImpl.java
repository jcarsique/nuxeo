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
 *  Stephane Lacoin <slacoin@nuxeo.com>
 *  Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.automation.scripting.internals;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.apache.commons.io.IOUtils;
import org.nuxeo.automation.scripting.api.AutomationScriptingConstants;
import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.context.ContextHelper;
import org.nuxeo.ecm.automation.context.ContextService;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.scripting.DateWrapper;
import org.nuxeo.ecm.automation.core.scripting.DocumentWrapper;
import org.nuxeo.ecm.automation.core.scripting.PrincipalWrapper;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.2
 */
public class AutomationScriptingServiceImpl implements AutomationScriptingService {

    protected String jsWrapper = null;

    protected ScriptOperationContext operationContext;

    protected String getJSWrapper(boolean refresh) throws OperationException {
        if (jsWrapper == null || refresh) {
            StringBuffer sb = new StringBuffer();
            AutomationService as = Framework.getService(AutomationService.class);
            Map<String, List<String>> opMap = new HashMap<>();
            List<String> flatOps = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            for (OperationType op : as.getOperations()) {
                ids.add(op.getId());
                if (op.getAliases() != null) {
                    Collections.addAll(ids, op.getAliases());
                }
            }
            // Create js object related to operation categories
            for (String id : ids) {
                parseAutomationIDSForScripting(opMap, flatOps, id);
            }
            for (String obName : opMap.keySet()) {
                List<String> ops = opMap.get(obName);
                sb.append("\nvar ").append(obName).append("={};");
                for (String opId : ops) {
                    generateFunction(sb, opId);
                }
            }
            for (String opId : flatOps) {
                generateFlatFunction(sb, opId);
            }
            jsWrapper = sb.toString();
        }
        return jsWrapper;
    }

    @Override
    public void setOperationContext(ScriptOperationContext ctx) {
        this.operationContext = operationContexts.get();
        this.operationContext = wrapContext(ctx);
    }

    protected ScriptOperationContext wrapContext(ScriptOperationContext ctx) {
        for (String entryId : ctx.keySet()) {
            Object entry = ctx.get(entryId);
            if (entry instanceof DocumentModel) {
                ctx.put(entryId, new DocumentWrapper(ctx.getCoreSession(), (DocumentModel) entry));
            }
            if (entry instanceof DocumentModelList) {
                List<DocumentWrapper> docs = new ArrayList<>();
                for (DocumentModel doc : (DocumentModelList) entry) {
                    docs.add(new DocumentWrapper(ctx.getCoreSession(), doc));
                }
                ctx.put(entryId, docs);
            }
        }
        return ctx;
    }

    @Override
    public String getJSWrapper() throws OperationException {
        return getJSWrapper(false);
    }

    protected final ThreadLocal<ScriptEngine> engines = new ThreadLocal<ScriptEngine>() {
        @Override
        protected ScriptEngine initialValue() {
            return Framework.getService(ScriptEngineManager.class).getEngineByName(
                    AutomationScriptingConstants.NX_NASHORN);
        }
    };

    protected final ThreadLocal<ScriptOperationContext> operationContexts = new ThreadLocal<ScriptOperationContext>() {
        @Override
        protected ScriptOperationContext initialValue() {
            return new ScriptOperationContext();
        }
    };

    @Override
    public void run(InputStream in, CoreSession session) throws ScriptException, OperationException {
        try {
            run(IOUtils.toString(in, "UTF-8"), session);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public void run(String script, CoreSession session) throws ScriptException, OperationException {
        ScriptEngine engine = engines.get();
        engine.setContext(new SimpleScriptContext());
        engine.eval(getJSWrapper());

        // Initialize Operation Context
        if (operationContext == null) {
            operationContext = operationContexts.get();
            operationContext.setCoreSession(session);
        }

        // Injecting Automation Mapper 'automation'
        AutomationMapper automationMapper = new AutomationMapper(session, operationContext);
        engine.put(AutomationScriptingConstants.AUTOMATION_MAPPER_KEY, automationMapper);

        // Inject operation context vars in 'Context'
        engine.put(AutomationScriptingConstants.AUTOMATION_CTX_KEY, automationMapper.ctx.getVars());
        // Session injection
        engine.put("Session", automationMapper.ctx.getCoreSession());
        // User injection
        PrincipalWrapper principalWrapper = new PrincipalWrapper((NuxeoPrincipal) automationMapper.ctx.getPrincipal());
        engine.put("CurrentUser", principalWrapper);
        engine.put("currentUser", principalWrapper);
        // Env Properties injection
        engine.put("Env", Framework.getProperties());
        // DateWrapper injection
        engine.put("CurrentDate", new DateWrapper());
        // Workflow variables injection
        if (automationMapper.ctx.get(Constants.VAR_WORKFLOW) != null) {
            engine.put(Constants.VAR_WORKFLOW, automationMapper.ctx.get(Constants.VAR_WORKFLOW));
        }
        if (automationMapper.ctx.get(Constants.VAR_WORKFLOW_NODE) != null) {
            engine.put(Constants.VAR_WORKFLOW_NODE, automationMapper.ctx.get(Constants.VAR_WORKFLOW_NODE));
        }

        // Helpers injection
        ContextService contextService = Framework.getService(ContextService.class);
        Map<String, ContextHelper> helperFunctions = contextService.getHelperFunctions();
        for (String helperFunctionsId : helperFunctions.keySet()) {
            engine.put(helperFunctionsId, helperFunctions.get(helperFunctionsId));
        }
        engine.eval(script);
    }

    @Override
    public <T> T getInterface(Class<T> scriptingOperationInterface, String script, CoreSession session)
            throws ScriptException, OperationException {
        run(script, session);
        Invocable inv = (Invocable) engines.get();
        return inv.getInterface(scriptingOperationInterface);
    }

    protected void parseAutomationIDSForScripting(Map<String, List<String>> opMap, List<String> flatOps, String id) {
        if (id.split("\\.").length > 2) {
            return;
        }
        int idx = id.indexOf(".");
        if (idx > 0) {
            String obName = id.substring(0, idx);
            List<String> ops = opMap.get(obName);
            if (ops == null) {
                ops = new ArrayList<>();
            }
            ops.add(id);
            opMap.put(obName, ops);
        } else {
            // Flat operation: no need of category
            flatOps.add(id);
        }
    }

    protected void generateFunction(StringBuffer sb, String opId) {
        sb.append("\n" + replaceDashByUnderscore(opId) + " = function(input,params) {");
        sb.append("\nreturn automation.executeOperation('" + opId + "', input , params);");
        sb.append("\n};");
    }

    protected void generateFlatFunction(StringBuffer sb, String opId) {
        sb.append("\nvar " + replaceDashByUnderscore(opId) + " = function(input,params) {");
        sb.append("\nreturn automation.executeOperation('" + opId + "', input , params);");
        sb.append("\n};");
    }

    /**
     * Prevents dashes in operation/chain ids. Only used to avoid javascript issues.
     * @since 7.3
     */
    public static String replaceDashByUnderscore(String id) {
        return id.replaceAll("[\\s\\-()]", "_");
    }
}
