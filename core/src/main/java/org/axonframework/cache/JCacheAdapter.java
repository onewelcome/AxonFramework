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

import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Factory;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;
import java.io.Serializable;

/**
 * Cache adapter implementation that allows providers implementing the JCache abstraction to be used.
 *
 * @author Allard Buijze
 * @since 2.1.2
 */
@SuppressWarnings("unchecked")
public class JCacheAdapter<K, V> extends AbstractCacheAdapter<K, V, CacheEntryListenerConfiguration<K, V>> {

    private final javax.cache.Cache<K, V> jCache;

    /**
     * Initialize the adapter to forward call to the given <code>jCache</code> instance
     *
     * @param jCache The cache to forward all calls to
     */
    public JCacheAdapter(javax.cache.Cache<K, V> jCache) {
        this.jCache = jCache;
    }

    @Override
    public V get(K key) {
        return jCache.get(key);
    }

    @Override
    public void put(K key, V value) {
        jCache.put(key, value);
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        return jCache.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(K key) {
        return jCache.remove(key);
    }

    @Override
    public boolean containsKey(K key) {
        return jCache.containsKey(key);
    }

    @Override
    public void clear() {
        jCache.clear();
    }

    @Override
    protected CacheEntryListenerConfiguration<K, V> createListenerAdapter(EntryListener cacheEntryListener) {
        return new JCacheListenerAdapter<>(cacheEntryListener);
    }

    @Override
    protected void doUnregisterListener(CacheEntryListenerConfiguration<K, V> listenerAdapter) {
        jCache.deregisterCacheEntryListener(listenerAdapter);
    }

    @Override
    protected void doRegisterListener(CacheEntryListenerConfiguration<K, V> listenerAdapter) {
        jCache.registerCacheEntryListener(listenerAdapter);
    }

    private static final class JCacheListenerAdapter<K, V> implements CacheEntryListenerConfiguration<K, V>,
            CacheEntryUpdatedListener<K, V>, CacheEntryCreatedListener<K, V>, CacheEntryExpiredListener<K, V>,
            CacheEntryRemovedListener<K, V>, Factory<CacheEntryListener<? super K, ? super V>>, Serializable {

        private static final long serialVersionUID = 3260575514029378445L;
        private final EntryListener delegate;

        public JCacheListenerAdapter(EntryListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onCreated(Iterable<CacheEntryEvent<? extends K, ? extends V>> cacheEntryEvents)
                throws CacheEntryListenerException {
            for (var event : cacheEntryEvents) {
                delegate.onEntryCreated(event.getKey(), event.getValue());
            }
        }

        @Override
        public void onExpired(Iterable<CacheEntryEvent<? extends K, ? extends V>> iterable)
                throws CacheEntryListenerException {
            for (var event : iterable) {
                delegate.onEntryExpired(event.getKey());
            }
        }

        @Override
        public Factory<CacheEntryListener<? super K, ? super V>> getCacheEntryListenerFactory() {
            return this;
        }

        @Override
        public boolean isOldValueRequired() {
            return false;
        }

        @Override
        public Factory<CacheEntryEventFilter<? super K, ? super V>> getCacheEntryEventFilterFactory() {
            return null;
        }

        @Override
        public boolean isSynchronous() {
            return true;
        }

        @Override
        public void onRemoved(Iterable<CacheEntryEvent<? extends K, ? extends V>> iterable)
                throws CacheEntryListenerException {
            for (var event : iterable) {
                delegate.onEntryRemoved(event.getKey());
            }
        }

        @Override
        public void onUpdated(Iterable<CacheEntryEvent<? extends K, ? extends V>> iterable)
                throws CacheEntryListenerException {
            for (var event : iterable) {
                delegate.onEntryUpdated(event.getKey(), event.getValue());
            }
        }

        @Override
        public CacheEntryListener<K, V> create() {
            return this;
        }
    }
}
