package org.nuxeo.opensocial.container.client.event.priv.model;

import com.google.gwt.event.shared.EventHandler;

/**
 * @author Stéphane Fourrier
 */
public interface WebContentIdChangedEventHandler extends EventHandler {
    void onWebContentIdChange(WebContentIdChangedEvent event);
}
