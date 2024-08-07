package org.axonframework.testutils;

import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.mapper.Mapper;

import java.util.concurrent.CopyOnWriteArraySet;

public class XStreamCopyOnWriteArraySetConverter extends CollectionConverter {

  public XStreamCopyOnWriteArraySetConverter(Mapper mapper) {
    super(mapper, CopyOnWriteArraySet.class);
  }

  @Override
  public boolean canConvert(Class type) {
    return CopyOnWriteArraySet.class.isAssignableFrom(type);
  }
}
