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

package org.axonframework.commandhandling.distributed.jgroups;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.SimpleCommandBus;
import org.axonframework.serializer.Serializer;
import org.axonframework.serializer.xml.XStreamSerializer;
import org.axonframework.testutils.XStreamSerializerFactory;
import org.jgroups.JChannel;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.ApplicationContext;

/**
 * @author Allard Buijze
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({JGroupsConnectorFactoryBean.class, JChannel.class, JGroupsConnector.class})
@Ignore("This test uses PowerMock in an incompatible way.")
public class JGroupsConnectorFactoryBeanTest {

    private JGroupsConnectorFactoryBean testSubject;
    private ApplicationContext mockApplicationContext;
    private JChannel mockChannel;
    private JGroupsConnector mockConnector;
    private HashChangeListener mockListener;

    @Before
    public void setUp() throws Exception {
        mockApplicationContext = mock(ApplicationContext.class);
        mockChannel = mock(JChannel.class);
        mockConnector = mock(JGroupsConnector.class);
        mockListener = mock(HashChangeListener.class);

        when(mockApplicationContext.getBean(Serializer.class)).thenReturn(XStreamSerializerFactory.create());
        whenNew(JChannel.class).withParameterTypes(String.class).withArguments(isA(String.class))
                .thenReturn(mockChannel);
        whenNew(JGroupsConnector.class)
                .withArguments(isA(JChannel.class), isA(String.class), isA(CommandBus.class), isA(Serializer.class), any())
                .thenReturn(mockConnector);

        testSubject = new JGroupsConnectorFactoryBean();
        testSubject.setBeanName("beanName");
        testSubject.setApplicationContext(mockApplicationContext);
    }

    @Test
    public void testCreateWithDefaultValues() throws Exception {
        testSubject.afterPropertiesSet();
        testSubject.start();
        testSubject.getObject();

        verifyNew(JChannel.class).withArguments("tcp_mcast.xml");
        verifyNew(JGroupsConnector.class).withArguments(eq(mockChannel), eq("beanName"), isA(
                SimpleCommandBus.class), isA(Serializer.class), isNull());
        verify(mockConnector).connect(100);
        verify(mockChannel, never()).close();

        testSubject.stop(new Runnable() {
            @Override
            public void run() {
            }
        });

        verify(mockChannel).close();
    }

    @Test
    public void testCreateWithSpecifiedValues() throws Exception {
        testSubject.setClusterName("ClusterName");
        testSubject.setConfiguration("custom.xml");
        testSubject.setLoadFactor(200);
        XStreamSerializer serializer = XStreamSerializerFactory.create();
        testSubject.setSerializer(serializer);
        SimpleCommandBus localSegment = new SimpleCommandBus();
        testSubject.setLocalSegment(localSegment);
        testSubject.setChannelName("localname");
        testSubject.setHashChangeListener(mockListener);
        testSubject.afterPropertiesSet();
        testSubject.start();
        testSubject.getObject();

        verifyNew(JChannel.class).withArguments("custom.xml");
        verifyNew(JGroupsConnector.class).withArguments(eq(mockChannel), eq("ClusterName"),
                same(localSegment), same(serializer), same(mockListener));
        verify(mockApplicationContext, never()).getBean(Serializer.class);
        verify(mockChannel).setName("localname");
        verify(mockConnector).connect(200);
        verify(mockChannel, never()).close();

        testSubject.stop(new Runnable() {
            @Override
            public void run() {
            }
        });

        verify(mockChannel).close();
    }

    @Test
    public void testSimpleProperties() {
        assertEquals(Integer.MAX_VALUE, testSubject.getPhase());
        testSubject.setPhase(100);
        assertEquals(100, testSubject.getPhase());
        assertTrue(testSubject.isAutoStartup());
    }
}
