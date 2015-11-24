package org.nuxeo.opensocial.container.server.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.SpaceManager;
import org.nuxeo.opensocial.container.client.rpc.InitApplication;
import org.nuxeo.opensocial.container.client.rpc.InitApplicationResult;
import org.nuxeo.opensocial.container.shared.layout.api.YUILayout;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

import net.customware.gwt.dispatch.server.ExecutionContext;

/**
 * @author Stéphane Fourrier
 */
public class InitApplicationHandler extends
        AbstractActionHandler<InitApplication, InitApplicationResult> {
    protected InitApplicationResult doExecute(InitApplication action,
            ExecutionContext context, CoreSession session) throws Exception {
        // TODO Get the permissions (for the moment we just put the perms for
        // the current space)
        // Has to be done later on for the layout's units and for the
        // webcontents
        Space space = getOrCreateSpace(action, session);

        Map<String, Map<String, Boolean>> permissions = space.getPermissions();

        // Get the layout from NUXEO
        YUILayout layout = space.getLayout().getLayout();

        // Get the webcontents in the layout
        List<WebContentData> list = space.readWebContents();
        Map<String, List<WebContentData>> webContents = new HashMap<String, List<WebContentData>>();

        for (WebContentData data : list) {
            if (webContents.containsKey(data.getUnitId())) {
                // webContents.get(data.getUnitId())
                // .add((int) data.getPosition(), data);
                webContents.get(data.getUnitId()).add(data);
            } else {
                List<WebContentData> temp = new ArrayList<WebContentData>();
                temp.add(data);
                webContents.put(data.getUnitId(), temp);
            }
        }
        return new InitApplicationResult(layout, webContents, permissions,
                space.getId());
    }

    protected Space getOrCreateSpace(InitApplication action, CoreSession session)
            throws ClientException {
        String spaceId = action.getSpaceId();
        if (spaceId != null && !spaceId.isEmpty()) {
            return getSpaceFromId(spaceId, session);
        } else {
            String documentContextId = action.getDocumentContextId();
            DocumentModel documentContext = null;
            if (documentContextId != null) {
                DocumentRef documentContextRef = new IdRef(documentContextId);
                if (session.exists(documentContextRef)) {
                    documentContext = session.getDocument(documentContextRef);
                }
            }
            SpaceManager spaceManager = getSpaceManager();
            return spaceManager.getSpace(action.getSpaceProviderName(),
                    session, documentContext, action.getSpaceName());
        }
    }

    public Class<InitApplication> getActionType() {
        return InitApplication.class;
    }

}
