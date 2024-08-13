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

import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventFiring;
import org.ehcache.event.EventOrdering;
import org.ehcache.event.EventType;

/**
 * Cache implementation that delegates all calls to an EhCache instance.
 *
 * @author Allard Buijze
 * @since 2.1.2
 */
public class EhCacheAdapter<K, V> extends AbstractCacheAdapter<K, V, CacheEventListener<K, V>> {

    private final org.ehcache.Cache<K, V> ehCache;

    /**
     * Initialize the adapter to forward all call to the given <code>ehCache</code> instance
     *
     * @param ehCache The cache instance to forward calls to
     */
    public EhCacheAdapter(org.ehcache.Cache<K, V> ehCache) {
        this.ehCache = ehCache;
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(K key) {
        return ehCache.get(key);
    }

    @Override
    public void put(K key, V value) {
        ehCache.put(key, value);
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        return (ehCache.putIfAbsent(key, value)) == null;
    }

    @Override
    public boolean remove(K key) {
        boolean hadKey = ehCache.containsKey(key);
        ehCache.remove(key);
        return hadKey;
    }

    @Override
    public boolean containsKey(K key) {
        return ehCache.containsKey(key);
    }

    @Override
    public void clear() {
        ehCache.clear();
    }

    @SuppressWarnings("ClassEscapesDefinedScope")
    @Override
    protected EhCacheAdapter.CacheEventListenerAdapter<K, V> createListenerAdapter(EntryListener cacheEntryListener) {
        return new EhCacheAdapter.CacheEventListenerAdapter<>(cacheEntryListener);
    }

    @Override
    protected void doUnregisterListener(CacheEventListener<K, V> listenerAdapter) {
        ehCache.getRuntimeConfiguration().deregisterCacheEventListener(listenerAdapter);
    }

    @Override
    protected void doRegisterListener(CacheEventListener<K, V> listenerAdapter) {
        ehCache.getRuntimeConfiguration()
            .registerCacheEventListener(
                listenerAdapter,
                EventOrdering.ORDERED,
                EventFiring.SYNCHRONOUS,
                EventType.CREATED,
                EventType.values()
            );
    }

    @SuppressWarnings("unchecked")
    private static class CacheEventListenerAdapter<K, V> implements CacheEventListener<K, V> {

        private EntryListener delegate;

        public CacheEventListenerAdapter(EntryListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onEvent(CacheEvent<? extends K, ? extends V> event) {
            EventType type = event.getType();
            K key = event.getKey();
            V newValue = event.getNewValue();
            switch (type) {
                case CREATED -> delegate.onEntryCreated(key, newValue);
                case UPDATED -> delegate.onEntryUpdated(key, newValue);
                case REMOVED -> delegate.onEntryRemoved(key);
                case EXPIRED, EVICTED -> delegate.onEntryExpired(key);
            }
        }
    }
}
