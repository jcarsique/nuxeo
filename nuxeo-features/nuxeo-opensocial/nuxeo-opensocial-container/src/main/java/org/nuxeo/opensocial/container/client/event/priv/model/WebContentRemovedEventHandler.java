package org.nuxeo.opensocial.container.client.event.priv.model;

import com.google.gwt.event.shared.EventHandler;

/**
 * @author Stéphane Fourrier
 */
public interface WebContentRemovedEventHandler extends EventHandler {
    void onWebContentRemoved(WebContentRemovedEvent event);
}
