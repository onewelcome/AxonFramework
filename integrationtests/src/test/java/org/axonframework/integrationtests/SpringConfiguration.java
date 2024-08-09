package org.axonframework.integrationtests;

import org.axonframework.integrationtests.commandhandling.StubAggregateChangedEvent;
import org.axonframework.integrationtests.commandhandling.StubAggregateCreatedEvent;
import org.axonframework.serializer.Serializer;
import org.axonframework.testutils.XStreamSerializerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringConfiguration {
  @Bean
  public Serializer serializer() {
    return XStreamSerializerFactory.create(StubAggregateCreatedEvent.class, StubAggregateChangedEvent.class);
  }
}
