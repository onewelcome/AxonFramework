package org.axonframework.testutils;

import org.axonframework.eventhandling.scheduling.SimpleTimingSaga;
import org.axonframework.saga.AssociationValue;
import org.axonframework.saga.repository.StubSaga;
import org.axonframework.serializer.Serializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XStreamSerializerSpringConfiguration {
  @Bean
  public Serializer serializer() {
    return XStreamSerializerFactory.create(SimpleTimingSaga.class, AssociationValue.class, StubSaga.class);
  }
}
