/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Maxime Hilaire
 *
 */
package org.nuxeo.ecm.core.cache;

import java.io.Serializable;

/**
 * The nuxeo cache interface that define generic methods to use cache technologies
 *
 * @since 6.0
 */
public interface Cache {

    /**
     * Get cache name as specified in the descriptor
     *
     * @return the cache name
     * @since 6.0
     */
    public String getName();

    /**
     * Get method to retrieve value from cache Must not raise exception if the key is null, but return null
     *
     * @param key the string key
     * @return the {@link Serializable} value, return null if the key does not exist or if the key is null
     * @since 6.0
     */
    public Serializable get(String key);

    /**
     * Invalidate the given key
     *
     * @param key, the key to remove from the cache, if null will do nothing
     * @since 6.0
     */
    public void invalidate(String key);

    /**
     * Invalidate all key-value stored in the cache
     *
     * @since 6.0
     */
    public void invalidateAll();

    /**
     * Put method to store a {@link Serializable} value
     *
     * @param key the string key, if null, the value will not be stored
     * @param value the value to store, if null, the value will not be stored
     * @since 6.0
     */
    public void put(String key, Serializable value);

    /**
     * Check if a given key is present inside the cache. Compared to the get() method, this method must not update
     * internal cache state and change TTL
     *
     * @param key the string key
     * @return true if a corresponding entry exists, false otherwise
     * @since 7.2
     */
    public boolean hasEntry(String key);

}
