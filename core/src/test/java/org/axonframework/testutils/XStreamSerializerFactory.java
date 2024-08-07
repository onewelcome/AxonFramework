package org.axonframework.testutils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.ExplicitTypePermission;
import org.axonframework.serializer.xml.XStreamSerializer;

public class XStreamSerializerFactory {
  public static XStreamSerializer create(Class<?>... permittedClasses) {
    XStream xStream = new XStream();
    xStream.registerConverter(new XStreamCopyOnWriteArraySetConverter(xStream.getMapper()));
    xStream.addPermission(new ExplicitTypePermission(permittedClasses));
    return new XStreamSerializer(xStream);
  }


}
