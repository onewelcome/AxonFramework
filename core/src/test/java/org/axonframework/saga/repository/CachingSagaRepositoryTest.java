/*
 * Copyright (c) 2010-2012. Axon Framework
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

package org.axonframework.saga.repository;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.axonframework.cache.Cache;
import org.axonframework.cache.EhCacheAdapter;
import org.axonframework.saga.AssociationValue;
import org.axonframework.saga.Saga;
import org.axonframework.saga.SagaRepository;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Allard Buijze
 */
public class CachingSagaRepositoryTest {

    private Cache<String, Set<String>> associationsCache;
    private Cache<String, Saga> sagaCache;
    private SagaRepository repository;
    private CachingSagaRepository testSubject;
    private CacheManager cacheManager;

    @Before
    public void setUp() throws Exception {
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
            .withCache("test-saga", CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String.class,
                Saga.class,
                ResourcePoolsBuilder.heap(100)
            ))
            .withCache("test-associations", CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String.class,
                Set.class,
                ResourcePoolsBuilder.heap(100)
            ))
            .build(true);
        //noinspection unchecked
        Class<Set<String>> associationsCacheValueType = (Class<Set<String>>) (Class<?>) Set.class;
        associationsCache = spy(new EhCacheAdapter<>(cacheManager.getCache("test-associations", String.class, associationsCacheValueType)));
        sagaCache = spy(new EhCacheAdapter<>(cacheManager.getCache("test-saga", String.class, Saga.class)));
        repository = mock(SagaRepository.class);
        testSubject = new CachingSagaRepository(repository, associationsCache, sagaCache);
    }

    @After
    public void tearDown() throws Exception {
        cacheManager.close();
    }

    @Test
    public void testSagaAddedToCacheOnAdd() throws Exception {
        final StubSaga saga = new StubSaga("id");
        saga.associate("key", "value");
        testSubject.add(saga);

        verify(sagaCache).put("id", saga);
        verify(associationsCache, never()).put(any(), any());
        verify(repository).add(saga);
    }

    @Test
    public void testConcurrentAccessToSagaRepository() {
        final StubSaga saga = new StubSaga("id");
        saga.associate("key", "value");
        testSubject.add(saga);
        testSubject.commit(saga);

        // to make sure this saga is found
        when(repository.find(any(Class.class), any(AssociationValue.class)))
                .thenReturn(new HashSet<String>(Arrays.asList(saga.getSagaIdentifier())));

        Set<String> found = testSubject.find(StubSaga.class, new AssociationValue("key", "value"));
        Iterator<String> iterator = found.iterator();

        final StubSaga saga2 = new StubSaga("id");
        saga2.associate("key", "value");
        testSubject.add(saga2);
        testSubject.commit(saga2);

        assertEquals(saga.getSagaIdentifier(), iterator.next());
    }

    @Test
    public void testAssociationsAddedToCacheOnLoad() {
        final StubSaga saga = new StubSaga("id");
        saga.associate("key", "value");
        testSubject.add(saga);
        associationsCache.clear();
        sagaCache.clear();
        reset(sagaCache, associationsCache);

        final AssociationValue associationValue = new AssociationValue("key", "value");
        when(repository.find(StubSaga.class, associationValue)).thenReturn(Collections.singleton("id"));

        Set<String> actual = testSubject.find(StubSaga.class, associationValue);
        assertEquals(actual, singleton("id"));
        verify(associationsCache, atLeast(1)).get("org.axonframework.saga.repository.StubSaga/key=value");
        verify(associationsCache).put("org.axonframework.saga.repository.StubSaga/key=value",
                                                  Collections.singleton("id"));
    }

    @Test
    public void testSagaAddedToCacheOnLoad() {
        final StubSaga saga = new StubSaga("id");
        saga.associate("key", "value");
        testSubject.add(saga);
        associationsCache.clear();
        sagaCache.clear();
        reset(sagaCache, associationsCache);

        when(repository.load("id")).thenReturn(saga);

        Saga actual = testSubject.load("id");
        assertSame(saga, actual);

        verify(sagaCache).get("id");
        verify(sagaCache).put("id", saga);
        verify(associationsCache, never()).put(any(), any());
    }

    @Test
    public void testCommitDelegatedAfterAddingToCache() {
        final StubSaga saga = new StubSaga("id");
        saga.associate("key", "value");
        testSubject.add(saga);
        associationsCache.clear();
        sagaCache.clear();

        saga.associate("new", "id");
        saga.removeAssociationValue("key", "value");
        testSubject.commit(saga);

        verify(repository).commit(saga);
        verify(associationsCache, never()).put(any(), any());
    }
}
