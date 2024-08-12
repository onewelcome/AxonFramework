package org.axonframework.mongo3;

import org.axonframework.saga.AssociationValue;
import org.axonframework.serializer.Serializer;
import org.axonframework.testutils.XStreamSerializerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringConfiguration {
  @Bean
  public Serializer serializer() {
    return XStreamSerializerFactory.create(
        AssociationValue.class
    );
  }
}