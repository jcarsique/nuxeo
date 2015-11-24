package org.nuxeo.ecm.spaces.core;

import static org.nuxeo.ecm.spaces.api.Constants.SPACE_DOCUMENT_TYPE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.spaces.api.AbstractSpaceProvider;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceException;
import org.nuxeo.ecm.spaces.api.exceptions.SpaceNotFoundException;
import org.nuxeo.opensocial.container.shared.layout.api.LayoutHelper;

public class SingleDocSpaceProvider extends AbstractSpaceProvider {

    private static final String PARAM_PATH = "path";

    private String path;

    private String title;

    @Override
    public void initialize(String name, Map<String, String> params)
            throws SpaceException {
        if (!params.containsKey(PARAM_PATH)) {
            throw new SpaceException(
                    "No path argument found for SingleDocSpaceProvider");
        }
        path = params.get(PARAM_PATH);
        title = params.get("title");
        if (null == title) {
            title = getDocName(path);
        }
    }

    @Override
    protected Space doGetSpace(CoreSession session,
            DocumentModel contextDocument, String spaceName, Map<String, String> parameters)
            throws SpaceException {
        return getOrCreateSingleSpace(session).getAdapter(Space.class);
    }

    @Override
    public List<Space> getAll(CoreSession session, DocumentModel contextDocument)
            throws SpaceException {
        List<Space> result = new ArrayList<Space>();
        result.add(getSpace(session, contextDocument, ""));
        return result;
    }

    @Override
    public long size(CoreSession session, DocumentModel contextDocument) {
        return 1;
    }

    public boolean isReadOnly(CoreSession session) {
        return true;
    }

    @Override
    public boolean isEmpty(CoreSession session, DocumentModel contextDocument) {
        return false;
    }

    private DocumentModel getOrCreateSingleSpace(CoreSession session)
            throws SpaceException {
        PathRef docRef = new PathRef(path);
        try {
            if (session.exists(docRef)) {
                return session.getDocument(docRef);
            } else {
                if (!session.exists(new PathRef(getParentPath(path)))) {
                    throw new ClientException(
                            "Parent path does not exist : unable to get or create space");
                }

                UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(
                        session) {
                    @Override
                    public void run() throws ClientException {
                        DocumentModel doc = session.createDocumentModel(
                                getParentPath(path), getDocName(path), SPACE_DOCUMENT_TYPE);
                        doc.setPropertyValue("dc:title", title);
                        doc = session.createDocument(doc);
                        Space space = doc.getAdapter(Space.class);
                        space.initLayout(LayoutHelper.Preset.X_1_DEFAULT.getLayout());
                        session.saveDocument(doc);
                        session.save();

                    }
                };

                runner.runUnrestricted();
                return session.getDocument(docRef);
            }
        } catch (ClientException e) {
            throw new SpaceNotFoundException(e);
        }
    }

    static String getParentPath(String fullPath) {
        int firstCharOfDocName = fullPath.lastIndexOf("/");
        if (firstCharOfDocName == -1) {
            return fullPath;
        } else {
            if (firstCharOfDocName > 0) {
                return fullPath.substring(0, firstCharOfDocName);
            } else {
                return "/";
            }
        }
    }

    static String getDocName(String fullPath) {
        int firstCharOfDocName = fullPath.lastIndexOf("/");
        if (firstCharOfDocName == -1) {
            return "";
        } else {
            return fullPath.substring(firstCharOfDocName + 1);
        }
    }

}
