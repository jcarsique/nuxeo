/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: UIDirectorySelectItems.java 29556 2008-01-23 00:59:39Z jcarsique $
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.component.UISelectItems;

/**
 * Component that deals with a list of select items from a directory.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class UIDirectorySelectItems extends UISelectItems {

    private static final Log log = LogFactory.getLog(UIDirectorySelectItems.class);

    public static final String COMPONENT_TYPE = UIDirectorySelectItems.class.getName();

    protected enum DirPropertyKeys {
        directoryName, keySeparator, itemOrdering, allValues,
        //
        displayAll, displayObsoleteEntries, filter, localize, dbl10n;
    }

    // setters & getters

    public Long getItemOrdering() {
        return (Long) getStateHelper().eval(DirPropertyKeys.itemOrdering);
    }

    public void setItemOrdering(Long itemOrdering) {
        getStateHelper().put(DirPropertyKeys.itemOrdering, itemOrdering);
    }

    public String getKeySeparator() {
        return (String) getStateHelper().eval(DirPropertyKeys.keySeparator, ChainSelect.DEFAULT_KEY_SEPARATOR);
    }

    public void setKeySeparator(String keySeparator) {
        getStateHelper().put(DirPropertyKeys.keySeparator, keySeparator);
    }

    public String getDirectoryName() {
        return (String) getStateHelper().eval(DirPropertyKeys.directoryName);
    }

    public void setDirectoryName(String directoryName) {
        getStateHelper().put(DirPropertyKeys.directoryName, directoryName);
    }

    public SelectItem[] getAllValues() {
        return (SelectItem[]) getStateHelper().eval(DirPropertyKeys.allValues);
    }

    public void setAllValues(SelectItem[] allValues) {
        getStateHelper().put(DirPropertyKeys.allValues, allValues);
    }

    @SuppressWarnings("boxing")
    public boolean isDisplayAll() {
        return (Boolean) getStateHelper().eval(DirPropertyKeys.displayAll, Boolean.TRUE);
    }

    @SuppressWarnings("boxing")
    public void setDisplayAll(boolean displayAll) {
        getStateHelper().put(DirPropertyKeys.displayAll, displayAll);
    }

    @SuppressWarnings("boxing")
    public boolean isDisplayObsoleteEntries() {
        return (Boolean) getStateHelper().eval(DirPropertyKeys.displayObsoleteEntries, Boolean.FALSE);
    }

    @SuppressWarnings("boxing")
    public void setDisplayObsoleteEntries(boolean displayObsoleteEntries) {
        getStateHelper().put(DirPropertyKeys.displayObsoleteEntries, displayObsoleteEntries);
    }

    public String getFilter() {
        return (String) getStateHelper().eval(DirPropertyKeys.filter);
    }

    public void setFilter(String filter) {
        getStateHelper().put(DirPropertyKeys.filter, filter);
    }

    @Override
    @SuppressWarnings("boxing")
    public boolean isLocalize() {
        return (Boolean) getStateHelper().eval(DirPropertyKeys.localize, Boolean.FALSE);
    }

    @Override
    @SuppressWarnings("boxing")
    public void setLocalize(boolean localize) {
        getStateHelper().put(DirPropertyKeys.localize, localize);
    }

    @Override
    @SuppressWarnings("boxing")
    public boolean isdbl10n() {
        return (Boolean) getStateHelper().eval(DirPropertyKeys.dbl10n, Boolean.FALSE);
    }

    @Override
    @SuppressWarnings("boxing")
    public void setdbl10n(boolean dbl10n) {
        getStateHelper().put(DirPropertyKeys.dbl10n, dbl10n);
    }

    @Override
    public Object getValue() {
        DirectorySelectItemsFactory f = new DirectorySelectItemsFactory() {

            @Override
            protected String getVar() {
                return UIDirectorySelectItems.this.getVar();
            }

            @Override
            protected DirectorySelectItem createSelectItem(String label, Long ordering) {
                return UIDirectorySelectItems.this.createSelectItem(label, ordering);
            }

            @Override
            protected String getDirectoryName() {
                return UIDirectorySelectItems.this.getDirectoryName();
            }

            @Override
            protected boolean isDisplayObsoleteEntries() {
                return UIDirectorySelectItems.this.isDisplayObsoleteEntries();
            }

            @Override
            protected String getFilter() {
                return UIDirectorySelectItems.this.getFilter();
            }

            @Override
            protected String[] retrieveSelectEntryId() {
                return UIDirectorySelectItems.this.retrieveSelectEntryId();
            }

            @Override
            protected Object retrieveItemLabel() {
                return UIDirectorySelectItems.this.getItemLabel();
            }

            @Override
            protected String retrieveLabelFromEntry(DocumentModel directoryEntry) {
                return UIDirectorySelectItems.this.retrieveLabelFromEntry(directoryEntry);
            }

            @Override
            protected Long retrieveOrderingFromEntry(DocumentModel directoryEntry) {
                return UIDirectorySelectItems.this.retrieveOrderingFromEntry(directoryEntry);
            }

        };

        List<DirectorySelectItem> items;
        if (isDisplayAll()) {
            items = f.createAllDirectorySelectItems();
        } else {
            Object value = getStateHelper().eval(PropertyKeys.value);
            items = f.createDirectorySelectItems(value, getKeySeparator());
        }

        String ordering = getOrdering();
        boolean caseSensitive = isCaseSensitive();
        if (!StringUtils.isBlank(ordering)) {
            Collections.sort(items, new DirectorySelectItemComparator(ordering, Boolean.valueOf(caseSensitive)));
        }
        DirectorySelectItem[] res = items.toArray(new DirectorySelectItem[0]);
        if (isDisplayAll()) {
            setAllValues(res);
        }
        return res;

    }

    protected DirectorySelectItem createSelectItem(String label, Long ordering) {
        if (!isItemRendered()) {
            return null;
        }
        Object valueObject = getItemValue();
        String value = valueObject == null ? null : valueObject.toString();
        if (isDisplayIdAndLabel() && label != null) {
            label = value + getDisplayIdAndLabelSeparator() + label;
        }
        // make sure label is never blank
        if (StringUtils.isBlank(label)) {
            label = value;
        }
        String labelPrefix = getItemLabelPrefix();
        if (!StringUtils.isBlank(labelPrefix)) {
            label = labelPrefix + getItemLabelPrefixSeparator() + label;
        }
        String labelSuffix = getItemLabelSuffix();
        if (!StringUtils.isBlank(labelSuffix)) {
            label = label + getItemLabelSuffixSeparator() + labelSuffix;
        }
        return new DirectorySelectItem(value, label, ordering == null ? 0L : ordering.longValue(), isItemDisabled(),
                isItemEscaped());
    }

    protected String[] retrieveSelectEntryId() {
        // assume option id and vocabulary entry id will match
        Object itemValue = getItemValue();
        String id = itemValue != null ? itemValue.toString() : null;
        if (StringUtils.isBlank(id)) {
            return null;
        }
        String keySeparator = getKeySeparator();
        if (!StringUtils.isBlank(keySeparator)) {
            String[] split = id.split(keySeparator);
            return split;
        }
        return new String[] {id};
    }

    protected String retrieveLabelFromEntry(DocumentModel docEntry) {
        if (docEntry == null) {
            return null;
        }
        String schema = docEntry.getSchemas()[0];
        // compute label
        Object labelObject = getItemLabel();
        String label = labelObject != null ? labelObject.toString() : null;
        FacesContext ctx = FacesContext.getCurrentInstance();
        Locale locale = ctx.getViewRoot().getLocale();
        if (StringUtils.isBlank(label)) {
            if (isLocalize() && isdbl10n()) {
                // lookup label property, hardcode the "label_" prefix for
                // now
                String defaultPattern = "label_en";
                String pattern = "label_" + locale.getLanguage();
                label = (String) docEntry.getProperty(schema, pattern);
                if (StringUtils.isBlank(label)) {
                    label = (String) docEntry.getProperty(schema, defaultPattern);
                }
                if (StringUtils.isBlank(label)) {
                    label = docEntry.getId();
                    log.warn("Could not find label column for entry " + label + " (falling back on entry id)");
                }
            } else {
                label = (String) docEntry.getProperties(schema).get("label");
                if (isLocalize()) {
                    label = translate(ctx, locale, label);
                }
            }
        } else if (isLocalize()) {
            label = translate(ctx, locale, label);
        }
        return label;
    }

    protected Long retrieveOrderingFromEntry(DocumentModel docEntry) {
        Long ordering = getItemOrdering();
        if (ordering != null) {
            return ordering;
        }
        // fallback on default ordering key
        if (docEntry == null) {
            return null;
        }
        String schema = docEntry.getSchemas()[0];
        try {
            ordering = (Long) docEntry.getProperties(schema).get("ordering");
        } catch (ClassCastException e) {
            // nevermind
        }
        return ordering;
    }

    @Override
    protected String translate(FacesContext context, Locale locale, String label) {
        if (StringUtils.isBlank(label)) {
            return label;
        }
        String bundleName = context.getApplication().getMessageBundle();
        label = I18NUtils.getMessageString(bundleName, label, null, locale);
        return label;
    }

}