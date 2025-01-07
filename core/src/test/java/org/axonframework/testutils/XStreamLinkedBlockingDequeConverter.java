package org.axonframework.testutils;

import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.mapper.Mapper;

import java.util.concurrent.LinkedBlockingDeque;

public class XStreamLinkedBlockingDequeConverter extends CollectionConverter {
  public XStreamLinkedBlockingDequeConverter(Mapper mapper) {
    super(mapper, LinkedBlockingDeque.class);
  }

  @Override
  public boolean canConvert(Class type) {
    return LinkedBlockingDeque.class.isAssignableFrom(type);
  }
}
