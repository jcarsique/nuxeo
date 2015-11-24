package org.nuxeo.opensocial.container.client.presenter;

import org.nuxeo.opensocial.container.client.event.priv.app.HideMessageEvent;
import org.nuxeo.opensocial.container.client.event.priv.app.HideMessageEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.app.SendMessageEvent;
import org.nuxeo.opensocial.container.client.event.priv.app.SendMessageEventHandler;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HasText;
import com.google.inject.Inject;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

/**
 * @author Stéphane Fourrier
 */
public class MessagePresenter extends WidgetPresenter<MessagePresenter.Display> {

    public interface Display extends WidgetDisplay {
        HasText getMessageBox();

        void showMessage();

        void hideMessage();

        void setPriorityColor(String color);
    }

    public static final Place PLACE = new Place("Message");

    private static final int TIMER_DELAY = 5000;

    private Timer timer;

    @Inject
    public MessagePresenter(final Display display, EventBus eventBus) {
        super(display, eventBus);

        timer = new Timer() {
            @Override
            public void run() {
                display.hideMessage();
            }
        };
    }

    @Override
    public Place getPlace() {
        return PLACE;
    }

    @Override
    protected void onBind() {
        registerMessageSending();
        registerMessageHide();
    }

    @Override
    protected void onUnbind() {
    }

    @Override
    protected void onPlaceRequest(PlaceRequest request) {
    }

    public void refreshDisplay() {
    }

    public void revealDisplay() {
    }

    private void registerMessageSending() {
        registerHandler(eventBus.addHandler(SendMessageEvent.TYPE,
                new SendMessageEventHandler() {
                    public void onMessageSent(SendMessageEvent event) {
                        display.getMessageBox().setText(event.getMessage());
                        display.setPriorityColor(event.getSeverity().getAssociatedColor());

                        display.showMessage();

                        if (!event.hasToBeKeptVisible()) {
                            timer.schedule(TIMER_DELAY);
                        }
                    }
                }));
    }

    private void registerMessageHide() {
        registerHandler(eventBus.addHandler(HideMessageEvent.TYPE,
                new HideMessageEventHandler() {
                    public void onMessageHidden(HideMessageEvent event) {
                        display.hideMessage();
                    }
                }));
    }
}
