/*
 * Copyright (c) 2010-2014. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.cache;

/**
 * Cache implementation that does absolutely nothing. Objects aren't cached, making it a special case implementation
 * for
 * the case when caching is disabled.
 *
 * @author Allard Buijze
 * @since 0.3
 */
public final class NoCache<K, V> implements Cache<K, V> {

    @Override
    public V get(K key) {
        return null;
    }

    @Override
    public void put(K key, V value) {
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        return true;
    }

    @Override
    public boolean remove(K key) {
        return false;
    }

    @Override
    public boolean containsKey(K key) {
        return false;
    }

    @Override
    public void registerCacheEntryListener(EntryListener cacheEntryListener) {
    }

    @Override
    public void unregisterCacheEntryListener(EntryListener cacheEntryRemovedListener) {
    }

    @Override
    public void clear() {
        // no-op
    }
}
