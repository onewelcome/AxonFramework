package org.axonframework.testutils;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class XStreamUnmodifiableMapConverter extends MapConverter {

  private static final Class<?> SUPPORTED_TYPE = Collections.unmodifiableMap(new HashMap<>()).getClass();

  public XStreamUnmodifiableMapConverter(Mapper mapper) {
    super(mapper, SUPPORTED_TYPE);
  }

  @Override
  public boolean canConvert(Class type) {
    return SUPPORTED_TYPE.isAssignableFrom(type);
  }

  @Override
  public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
    Map map = (Map) source;
    context.convertAnother(new HashMap<>(map));
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    return context.convertAnother(reader, HashMap.class);
  }
}
