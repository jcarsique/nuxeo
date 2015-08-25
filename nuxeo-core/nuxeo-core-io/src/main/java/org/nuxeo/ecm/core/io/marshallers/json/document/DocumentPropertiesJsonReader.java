/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json.document;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.api.model.impl.DocumentPartImpl;
import org.nuxeo.ecm.core.api.model.impl.PropertyFactory;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonReader;
import org.nuxeo.ecm.core.io.registry.MarshallingException;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;

/**
 * Convert Json as {@link List<Property>}.
 * <p>
 * Format is:
 *
 * <pre>
 * {
 *   "schema1Prefix:stringProperty": "stringPropertyValue", <-- each property may be marshall as object if a resolver is associated with that property and if a marshaller exists for the object, in this case, the resulting property will have the corresponding reference value.
 *   "schema1Prefix:booleanProperty": true|false,
 *   "schema2Prefix:integerProperty": 123,
 *   ...
 *   "schema3Prefix:complexProperty": {
 *      "subProperty": ...,
 *      ...
 *   },
 *   "schema4Prefix:listProperty": [
 *      ...
 *   ]
 * }
 * </pre>
 *
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentPropertiesJsonReader extends AbstractJsonReader<List<Property>> {

    public static String DEFAULT_SCHEMA_NAME = "DEFAULT_SCHEMA_NAME";

    @Inject
    private SchemaManager schemaManager;

    @Override
    public List<Property> read(JsonNode jn) throws IOException {
        List<Property> properties = new ArrayList<Property>();
        Iterator<Entry<String, JsonNode>> propertyNodes = jn.getFields();
        while (propertyNodes.hasNext()) {
            Entry<String, JsonNode> propertyNode = propertyNodes.next();
            String propertyName = propertyNode.getKey();
            Field field = null;
            Property parent = null;
            if (propertyName.contains(":")) {
                field = schemaManager.getField(propertyName);
                if (field == null) {
                    continue;
                }
                parent = new DocumentPartImpl(field.getType().getSchema());
            } else {
                String shemaName = ctx.getParameter(DEFAULT_SCHEMA_NAME);
                Schema schema = schemaManager.getSchema(shemaName);
                field = schema.getField(propertyName);
                parent = new DocumentPartImpl(schema);
            }
            Property property = readProperty(parent, field, propertyNode.getValue());
            if (property != null) {
                properties.add(property);
            }
        }
        return properties;
    }

    protected Property readProperty(Property parent, Field field, JsonNode jn) throws IOException {
        Property property = PropertyFactory.createProperty(parent, field, 0);
        if (jn.isNull()) {
            property.setValue(null);
        } else if (property.isScalar()) {
            fillScalarProperty(property, jn);
        } else if (property.isList()) {
            fillListProperty(property, jn);
        } else {
            if (!(property instanceof BlobProperty)) {
                fillComplexProperty(property, jn);
            } else {
                Blob blob = readEntity(Blob.class, Blob.class, jn);
                property.setValue(blob);
            }
        }
        return property;
    }

    private void fillScalarProperty(Property property, JsonNode jn) throws IOException {
        if ((property instanceof ArrayProperty) && jn.isArray()) {
            List<Object> values = new ArrayList<Object>();
            Iterator<JsonNode> it = jn.getElements();
            JsonNode item;
            Type fieldType = ((ListType) property.getType()).getFieldType();
            while (it.hasNext()) {
                item = it.next();
                values.add(getScalarPropertyValue(property, item, fieldType));
            }
            property.setValue(castArrayPropertyValue(((SimpleType) fieldType).getPrimitiveType(), values));
        } else {
            property.setValue(getScalarPropertyValue(property, jn, property.getType()));
        }
    }

    @SuppressWarnings({ "unchecked" })
    private <T> T[] castArrayPropertyValue(Type type, List<Object> values) throws IOException {
        if (type instanceof StringType) {
            return values.toArray((T[]) Array.newInstance(String.class, values.size()));
        } else if (type instanceof BooleanType) {
            return values.toArray((T[]) Array.newInstance(Boolean.class, values.size()));
        } else if (type instanceof LongType) {
            return values.toArray((T[]) Array.newInstance(Long.class, values.size()));
        } else if (type instanceof DoubleType) {
            return values.toArray((T[]) Array.newInstance(Double.class, values.size()));
        } else if (type instanceof IntegerType) {
            return values.toArray((T[]) Array.newInstance(Integer.class, values.size()));
        } else if (type instanceof BinaryType) {
            return values.toArray((T[]) Array.newInstance(Byte.class, values.size()));
        }
        throw new MarshallingException("Primitive type not found: " + type.getName());
    }

    private Object getScalarPropertyValue(Property property, JsonNode jn, Type type) throws IOException {
        Object value;
        if (jn.isObject()) {
            ObjectResolver resolver = type.getObjectResolver();
            if (resolver == null) {
                throw new MarshallingException("Unable to parse the property " + property.getPath());
            }
            Object object = null;
            for (Class<?> clazz : resolver.getManagedClasses()) {
                try {
                    object = readEntity(clazz, clazz, jn);
                } catch (Exception e) {
                    continue;
                }
            }
            if (object == null) {
                throw new MarshallingException("Unable to parse the property " + property.getPath());
            }
            value = resolver.getReference(object);
            if (value == null) {
                throw new MarshallingException("Property " + property.getPath()
                        + " value cannot be resolved by the matching resolver " + resolver.getName());
            }
        } else {
            value = getPropertyValue(((SimpleType) type).getPrimitiveType(), jn);
        }
        return value;
    }

    private Object getPropertyValue(Type type, JsonNode jn) throws IOException {
        Object value;
        if (jn.isNull()) {
            value = null;
        } else if (type instanceof BooleanType) {
            value = jn.getValueAsBoolean();
        } else if (type instanceof LongType) {
            value = jn.getValueAsLong();
        } else if (type instanceof DoubleType) {
            value = jn.getValueAsDouble();
        } else if (type instanceof IntegerType) {
            value = jn.getValueAsInt();
        } else if (type instanceof BinaryType) {
            value = jn.getBinaryValue();
        } else {
            value = type.decode(jn.getValueAsText());
        }
        return value;
    }

    private void fillListProperty(Property property, JsonNode jn) throws IOException {
        ListType listType = (ListType) property.getType();
        if (property instanceof ArrayProperty) {
            fillScalarProperty(property, jn);
        } else {
            JsonNode elNode = null;
            Iterator<JsonNode> it = jn.getElements();
            while (it.hasNext()) {
                elNode = it.next();
                Property child = readProperty(property, listType.getField(), elNode);
                property.addValue(child.getValue());
            }
        }
    }

    private void fillComplexProperty(Property property, JsonNode jn) throws IOException {
        Entry<String, JsonNode> elNode = null;
        Iterator<Entry<String, JsonNode>> it = jn.getFields();
        ComplexProperty complexProperty = (ComplexProperty) property;
        ComplexType type = complexProperty.getType();
        while (it.hasNext()) {
            elNode = it.next();
            String elName = elNode.getKey();
            Field field = type.getField(elName);
            if (field != null) {
                Property child = readProperty(property, field, elNode.getValue());
                property.setValue(elName, child.getValue());
            }
        }
    }

}
