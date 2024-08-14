package org.axonframework.testutils;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.util.EnumSet;

/**
 * This converter would have to be registered for each enum that is serialized by Axon.
 * Example:
 * <pre>{@code
 *  xStream.registerConverter(
 *    new XStreamEnumSetConverter(TestEventWithEnumSet.SomeEnum.class)
 *  );
 * }</pre>
 */
public class XStreamEnumSetConverter implements Converter {

  private final Class<? extends Enum> enumType;

  public XStreamEnumSetConverter(Class<? extends Enum> enumType) {
    this.enumType = enumType;
  }

  @Override
  public boolean canConvert(Class clazz) {
    return EnumSet.class.isAssignableFrom(clazz);
  }

  @Override
  public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
    EnumSet<?> enumSet = (EnumSet<?>) value;
    for (Object enumValue : enumSet) {
      writer.startNode("enumValue");
      writer.setValue(enumValue.toString());
      writer.endNode();
    }
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    EnumSet enumSet = EnumSet.noneOf(enumType);
    while (reader.hasMoreChildren()) {
      reader.moveDown();
      String enumValue = reader.getValue();
      enumSet.add(Enum.valueOf(enumType, enumValue));
      reader.moveUp();
    }
    return enumSet;
  }
}
