package org.axonframework.testutils;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.util.Collections;

public class XStreamEmptyListConverter implements Converter {

  private static final Class<?> SUPPORTED_TYPE = Collections.emptyList().getClass();

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
