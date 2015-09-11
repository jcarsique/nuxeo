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
 *      Andre Justo
 *      Anahide Tchertchian
 */
package org.nuxeo.runtime.services.config;

/**
 * Service holding runtime configuration properties.
 * <p>
 * If a property is defined in the nuxeo.conf file, it will take precedence over properties defined via the runtime
 * extension point.
 *
 * @since 7.4
 */
public interface ConfigurationService {

    /**
     * Returns the given property value if any, otherwise null.
     *
     * @param key the property key
     */
    String getProperty(String key);

    /**
     * Returns the given property value if any, otherwise returns the given default value.
     *
     * @param key the property key
     * @param defaultValue the default value for this key
     */
    String getProperty(String key, String defaultValue);

    /**
     * Returns true if given property is true when compared to a boolean value.
     */
    boolean isBooleanPropertyTrue(String key);

    /**
     * Returns true if given property is false when compared to a boolean value.
     */
    boolean isBooleanPropertyFalse(String key);

}