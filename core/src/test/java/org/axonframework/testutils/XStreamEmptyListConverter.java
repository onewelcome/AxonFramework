package org.axonframework.testutils;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import java.util.Collections;

public class XStreamEmptyListConverter extends CollectionConverter {

  private static final Class<?> SUPPORTED_TYPE = Collections.emptyList().getClass();

  public XStreamEmptyListConverter(Mapper mapper) {
    super(mapper, SUPPORTED_TYPE);
  }

  @Override
  public boolean canConvert(Class type) {
    return SUPPORTED_TYPE.isAssignableFrom(type);
  }

  @Override
  public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
    // No need to write anything specific for EmptyList
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    return Collections.emptyList();
  }
}
