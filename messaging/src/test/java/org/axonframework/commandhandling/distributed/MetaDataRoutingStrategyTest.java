package org.axonframework.commandhandling.distributed;

import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.common.AxonConfigurationException;
import org.axonframework.messaging.MetaData;
import org.junit.jupiter.api.*;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class validating the {@link MetaDataRoutingStrategy}.
 *
 * @author Steven van Beelen
 */
class MetaDataRoutingStrategyTest {

    private static final String META_DATA_KEY = "some-metadata-key";

    private MetaDataRoutingStrategy testSubject;

    private final RoutingStrategy fallbackRoutingStrategy = mock(RoutingStrategy.class);

    @BeforeEach
    void setUp() {
        testSubject = MetaDataRoutingStrategy.builder()
                                             .metaDataKey(META_DATA_KEY)
                                             .fallbackRoutingStrategy(fallbackRoutingStrategy)
                                             .build();
    }

    @Test
    void testResolvesRoutingKeyFromMetaData() {
        String expectedRoutingKey = "some-routing-key";

        MetaData testMetaData = MetaData.from(Collections.singletonMap(META_DATA_KEY, expectedRoutingKey));
        CommandMessage<String> testCommand = new GenericCommandMessage<>("some-payload", testMetaData);

        assertEquals(expectedRoutingKey, testSubject.getRoutingKey(testCommand));
        verifyNoInteractions(fallbackRoutingStrategy);
    }

    @Test
    void testResolvesRoutingKeyFromFallbackStrategy() {
        String expectedRoutingKey = "some-routing-key";
        when(fallbackRoutingStrategy.getRoutingKey(any())).thenReturn(expectedRoutingKey);

        CommandMessage<String> testCommand = new GenericCommandMessage<>("some-payload", MetaData.emptyInstance());

        assertEquals(expectedRoutingKey, testSubject.getRoutingKey(testCommand));
        verify(fallbackRoutingStrategy).getRoutingKey(testCommand);
    }

    @Test
    void testBuildMetaDataRoutingStrategyFailsForNullFallbackRoutingStrategy() {
        assertThrows(
                AxonConfigurationException.class, () -> MetaDataRoutingStrategy.builder().fallbackRoutingStrategy(null)
        );
    }

    @Test
    void testBuildMetaDataRoutingStrategyFailsForNullMetaDataKey() {
        assertThrows(
                AxonConfigurationException.class, () -> MetaDataRoutingStrategy.builder().metaDataKey(null)
        );
    }

    @Test
    void testBuildMetaDataRoutingStrategyFailsForEmptyMetaDataKey() {
        assertThrows(
                AxonConfigurationException.class, () -> MetaDataRoutingStrategy.builder().metaDataKey("")
        );
    }
}