/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.model.impl;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;

import org.nuxeo.common.collections.PrimitiveArrays;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.JavaTypes;
import org.nuxeo.ecm.core.schema.types.ListType;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ArrayProperty extends ScalarProperty {

    private static final long serialVersionUID = 0L;

    public ArrayProperty(Property parent, Field field, int flags) {
        super(parent, field, flags);
    }

    @Override
    public ListType getType() {
        return (ListType) super.getType();
    }

    @Override
    public boolean isContainer() {
        return false;
    }

    @Override
    public void setValue(Object value) throws PropertyException {
        // this code manage dirty status for the arrayproperty and its childs values
        // it checks whether the property changed, or their index changed
        if (value == null) {
            childDirty = new boolean[0];
            super.setValue(value);
        } else {
            Object[] oldValues = (Object[]) internalGetValue();
            boolean[] oldChildDirty = getChildDirty();
            super.setValue(value);
            Object[] newValues = (Object[]) internalGetValue();
            boolean[] newChildDirty = new boolean[newValues != null ? newValues.length : 0];
            for (int i = 0; i < newChildDirty.length; i++) {
                Object newValue = newValues[i];
                if (oldValues == null || i >= oldValues.length) {
                    newChildDirty[i] = true;
                } else {
                    Object oldValue = oldValues[i];
                    if (!((newValue == null && oldValue == null) || (newValue != null && newValue.equals(oldValue)))) {
                        newChildDirty[i] = true;
                    } else {
                        newChildDirty[i] = false || oldChildDirty[i];
                    }
                }
            }
            childDirty = newChildDirty;
        }
    }

    @Override
    protected boolean isSameValue(Serializable value1, Serializable value2) {
        Object[] castedtValue1 = (Object[]) value1;
        Object[] castedtValue2 = (Object[]) value2;
        return castedtValue1 == castedtValue2 || (castedtValue1 == null && castedtValue2.length == 0)
                || (castedtValue2 == null && castedtValue1.length == 0) || Arrays.equals(castedtValue1, castedtValue2);
    }

    @Override
    public boolean isNormalized(Object value) {
        return value == null || value.getClass().isArray();
    }

    @Override
    public Serializable normalize(Object value) throws PropertyConversionException {
        if (isNormalized(value)) {
            return (Serializable) value;
        }
        if (value instanceof Collection) {
            Collection<?> col = (Collection<?>) value;
            Class<?> klass = JavaTypes.getClass(getType().getFieldType());
            return col.toArray((Object[]) Array.newInstance(klass, col.size()));
        }
        throw new PropertyConversionException(value.getClass(), Object[].class, getPath());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T convertTo(Serializable value, Class<T> toType) throws PropertyConversionException {
        if (toType.isArray()) {
            return (T) PrimitiveArrays.toObjectArray(value);
        } else if (Collection.class.isAssignableFrom(toType)) {
            return (T) Arrays.asList((Object[]) value);
        }
        throw new PropertyConversionException(value.getClass(), toType);
    }

    @Override
    public Object newInstance() {
        return new Serializable[0];
    }

    // this boolean array managed the dirty flags for arrayproperty childs
    private boolean[] childDirty = null;

    protected boolean[] getChildDirty() {
        if (childDirty == null) {
            Object[] oldValues = (Object[]) internalGetValue();
            if (oldValues == null) {
                childDirty = new boolean[0];
            } else {
                childDirty = new boolean[oldValues.length];
                for (int i = 0; i < childDirty.length; i++) {
                    childDirty[i] = false;
                }
            }
        }
        return childDirty;
    }

    /**
     * This method provides a way to know if some arrayproperty values are dirty: value or index changed. since 7.2
     */
    public boolean isDirty(int index) {
        if (index > getChildDirty().length) {
            throw new IndexOutOfBoundsException("Index out of bounds: " + index + ". Bounds are: 0 - "
                    + (getChildDirty().length - 1));
        }
        return getChildDirty()[index];
    }

    @Override
    public void clearDirtyFlags() {
        // even makes child properties not dirty
        super.clearDirtyFlags();
        for (int i = 0; i < getChildDirty().length; i++) {
            childDirty[i] = false;
        }
    }

}
