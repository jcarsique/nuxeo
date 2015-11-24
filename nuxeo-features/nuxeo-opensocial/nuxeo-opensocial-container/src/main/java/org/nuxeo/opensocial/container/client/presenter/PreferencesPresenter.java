package org.nuxeo.opensocial.container.client.presenter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

import org.nuxeo.opensocial.container.client.event.priv.presenter.FolderChosenEvent;
import org.nuxeo.opensocial.container.client.event.priv.presenter.FolderChosenEventHandler;
import org.nuxeo.opensocial.container.client.model.AppModel;
import org.nuxeo.opensocial.container.client.model.FolderPickerModel;
import org.nuxeo.opensocial.container.client.model.NXIDPreference;
import org.nuxeo.opensocial.container.client.ui.ColorsPanelWidget;
import org.nuxeo.opensocial.container.client.ui.NXIDTextBox;
import org.nuxeo.opensocial.container.client.ui.api.HasMultipleValue;
import org.nuxeo.opensocial.container.client.view.FolderPickerWidget;
import org.nuxeo.opensocial.container.shared.webcontent.OpenSocialData;
import org.nuxeo.opensocial.container.shared.webcontent.UserPref;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasKeyUpHandlers;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.HasName;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * @author Stéphane Fourrier
 */
public class PreferencesPresenter extends
        WidgetPresenter<PreferencesPresenter.Display> {
    public interface Display extends WidgetDisplay {
        HasClickHandlers getBorderColors();

        void setBorderColor(String color);

        HasClickHandlers getHeaderColors();

        void setHeaderColor(String color);

        HasClickHandlers getTitleColors();

        void setTitleColor(String color);

        HasClickHandlers getSaveButton();

        HasClickHandlers getCancelButton();

        HasKeyUpHandlers getTitleEvent();

        HasText getTitleBox();

        void showPopup();

        void hidePopup();

        HasValue<String> addStringUserPref(String name, String displayName);

        HasValue<Boolean> addBooleanUserPref(String name, String displayName);

        HasValue<String> addColorsUserPref(String name, String displayName);

        HasMultipleValue<String> addEnumUserPref(String name, String displayName);

        NXIDTextBox addNXIDUserPref(String name, String displayName);
    }

    private PortletPresenter portletPresenter;

    private WebContentData data;

    private AppModel model;

    private List<Widget> prefsValues;

    @Inject
    public PreferencesPresenter(final Display display, final EventBus eventBus,
            final PortletPresenter portletPresenter, AppModel model) {
        super(display, eventBus);

        this.portletPresenter = portletPresenter;
        this.data = model.getWebContent(portletPresenter.getDisplay().getId());
        this.model = model;

        this.prefsValues = new ArrayList<Widget>();
    }

    @Override
    public Place getPlace() {
        return null;
    }

    private void fetchContent() {
        display.getTitleBox().setText(data.getTitle());
        display.setTitleColor(data.getPreferences().get(
                WebContentData.WC_TITLE_COLOR));
        display.setBorderColor(data.getPreferences().get(
                WebContentData.WC_BORDER_COLOR));
        display.setHeaderColor(data.getPreferences().get(
                WebContentData.WC_HEADER_COLOR));

        // TODO Should be done with any type of WebContentData ! To be improved
        // ...
        if (data instanceof OpenSocialData) {
            for (UserPref userPref : ((OpenSocialData) data).getUserPrefs()) {
                addUserPref(userPref);
            }
        }
    }

    private void addUserPref(UserPref pref) {
        String value = pref.getActualValue();
        if (value == null) {
            value = pref.getDefaultValue();
        }

        switch (pref.getDataType()) {
        case STRING:
            // TODO Work only for PictureBook ! Has to be improved for any type
            // of document
            if (pref.getName().startsWith("NXID_")) {
                final NXIDTextBox idBox = display.addNXIDUserPref(
                        pref.getName(), pref.getDisplayName());

                if (value != null && !("").equals(value)) {
                    NXIDPreference evaluatedNXIDPref = evaluateNXIDPref(value);
                    idBox.setValue(evaluatedNXIDPref.getNXName());
                    idBox.setHiddenValue(evaluatedNXIDPref.getNXId());
                }

                idBox.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        FolderPickerModel folderPickerModel = new FolderPickerModel(
                                data.getId(), idBox.getHiddenValue());
                        FolderPickerWidget folderPickerWidget = new FolderPickerWidget();
                        FolderPickerPresenter folderPicker = new FolderPickerPresenter(
                                folderPickerWidget, eventBus, folderPickerModel);

                        folderPicker.bind();
                        folderPicker.getDisplay().showPicker();
                    }
                });

                prefsValues.add((Widget) idBox);

                eventBus.addHandler(FolderChosenEvent.TYPE,
                        new FolderChosenEventHandler() {
                            public void onFolderChosen(FolderChosenEvent event) {
                                idBox.setValue(event.getFolderName());
                                idBox.setHiddenValue(event.getFolderId());
                            }
                        });
            } else {
                HasValue<String> text = display.addStringUserPref(
                        pref.getName(), pref.getDisplayName());
                text.setValue(value);
                prefsValues.add((Widget) text);
            }
            break;
        case BOOL:
            HasValue<Boolean> bool = display.addBooleanUserPref(pref.getName(),
                    pref.getDisplayName());
            bool.setValue(Boolean.parseBoolean(value));
            prefsValues.add((Widget) bool);
            break;
        case ENUM:
            if (pref.getName().startsWith("COLOR_")) {
                HasValue<String> color = display.addColorsUserPref(
                        pref.getName(), pref.getDisplayName());
                color.setValue(value);
                prefsValues.add((Widget) color);
            } else {
                HasMultipleValue<String> list = display.addEnumUserPref(
                        pref.getName(), pref.getDisplayName());
                // Populate the list with the enum values
                for (Entry<String, String> enumValues : pref.getEnumValues().entrySet()) {
                    list.addValue(enumValues.getKey(), enumValues.getValue());
                    // Select the actual value. If null, select the default
                    // value
                    if (value.equals(enumValues.getValue())) {
                        list.setItemSelected(list.getItemCount() - 1);
                    }
                }
                prefsValues.add((Widget) list);
            }
            break;
        }
    }

    private native NXIDPreference evaluateNXIDPref(String json) /*-{
        return eval("(" + json + ")");
    }-*/;

    @Override
    protected void onBind() {
        fetchContent();

        registerTitleChangement();
        registerBorderColorChangement();
        registerHeaderColorChangement();
        registerTitleColorChangement();
        registerPreferencesSave();
        registerPreferencesCancel();
    }

    private void registerTitleChangement() {
        registerHandler(display.getTitleEvent().addKeyUpHandler(
                new KeyUpHandler() {
                    public void onKeyUp(KeyUpEvent arg0) {
                        portletPresenter.setTitle(display.getTitleBox().getText());
                    }
                }));
    }

    public void rollBack() {
        portletPresenter.setTitle(data.getTitle());

        portletPresenter.setBorderColor(data.getPreferences().get(
                WebContentData.WC_BORDER_COLOR));

        portletPresenter.setTitleColor(data.getPreferences().get(
                WebContentData.WC_TITLE_COLOR));

        portletPresenter.setHeaderColor(data.getPreferences().get(
                WebContentData.WC_HEADER_COLOR));
    }

    private void registerPreferencesSave() {
        registerHandler(display.getSaveButton().addClickHandler(
                new ClickHandler() {
                    @SuppressWarnings("unchecked")
                    public void onClick(ClickEvent event) {
                        data.setTitle(display.getTitleBox().getText());

                        data.addPreference(
                                WebContentData.WC_TITLE_COLOR,
                                ((ColorsPanelWidget) display.getTitleColors()).getSelectedColor().getColorAsString());

                        data.addPreference(
                                WebContentData.WC_BORDER_COLOR,
                                ((ColorsPanelWidget) display.getBorderColors()).getSelectedColor().getColorAsString());

                        data.addPreference(
                                WebContentData.WC_HEADER_COLOR,
                                ((ColorsPanelWidget) display.getHeaderColors()).getSelectedColor().getColorAsString());

                        // TODO Should be done with any type of WebContentData !
                        // To be improved ...
                        if (data instanceof OpenSocialData) {
                            for (Widget widget : prefsValues) {
                                String name = ((HasName) widget).getName();
                                String value = ((HasValue) widget).getValue().toString();
                                UserPref userPref = ((OpenSocialData) data).getUserPrefByName(name);
                                if (userPref != null)
                                    userPref.setActualValue(value);
                            }
                        }

                        model.updateWebContent(data.getId(), null);

                        display.hidePopup();
                    }
                }));
    }

    private void registerPreferencesCancel() {
        registerHandler(display.getCancelButton().addClickHandler(
                new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        rollBack();
                        display.hidePopup();
                    }
                }));
    }

    private void registerHeaderColorChangement() {
        registerHandler(display.getHeaderColors().addClickHandler(
                new ClickHandler() {
                    public void onClick(ClickEvent arg0) {
                        String color = ((ColorsPanelWidget) display.getHeaderColors()).getSelectedColor().getColorAsString();
                        portletPresenter.setHeaderColor(color);
                    }
                }));
    }

    private void registerBorderColorChangement() {
        registerHandler(display.getBorderColors().addClickHandler(
                new ClickHandler() {
                    public void onClick(ClickEvent arg0) {
                        String color = ((ColorsPanelWidget) display.getBorderColors()).getSelectedColor().getColorAsString();
                        portletPresenter.setBorderColor(color);
                    }
                }));
    }

    private void registerTitleColorChangement() {
        registerHandler(display.getTitleColors().addClickHandler(
                new ClickHandler() {
                    public void onClick(ClickEvent arg0) {
                        String color = ((ColorsPanelWidget) display.getTitleColors()).getSelectedColor().getColorAsString();
                        portletPresenter.setTitleColor(color);
                    }
                }));
    }

    @Override
    protected void onPlaceRequest(PlaceRequest request) {
    }

    @Override
    protected void onUnbind() {
    }

    public void refreshDisplay() {
    }

    public void revealDisplay() {
        display.showPopup();
    }
}
