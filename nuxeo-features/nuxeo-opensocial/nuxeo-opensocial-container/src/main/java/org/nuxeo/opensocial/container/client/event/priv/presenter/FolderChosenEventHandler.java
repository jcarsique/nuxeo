package org.nuxeo.opensocial.container.client.event.priv.presenter;

import com.google.gwt.event.shared.EventHandler;

/**
 * @author Stéphane Fourrier
 */
public interface FolderChosenEventHandler extends EventHandler {
    void onFolderChosen(FolderChosenEvent event);
}
