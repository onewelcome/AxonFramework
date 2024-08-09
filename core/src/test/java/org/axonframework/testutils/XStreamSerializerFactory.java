package org.axonframework.testutils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.ExplicitTypePermission;
import org.axonframework.serializer.xml.XStreamSerializer;

public class XStreamSerializerFactory {
  public static XStreamSerializer create(Class<?>... permittedClasses) {
    XStream xStream = createXStream(permittedClasses);
    return new XStreamSerializer(xStream);
  }

  public static XStream createXStream(Class<?>... permittedClasses) {
    XStream xStream = new XStream();
    xStream.registerConverter(new XStreamCopyOnWriteArraySetConverter(xStream.getMapper()));
    xStream.registerConverter(new XStreamUnmodifiableMapConverter(xStream.getMapper()));
    xStream.registerConverter(new XStreamLinkedBlockingDequeConverter(xStream.getMapper()));
    xStream.registerConverter(new XStreamEmptyListConverter(xStream.getMapper()));
    xStream.addPermission(new ExplicitTypePermission(permittedClasses));
    return xStream;
  }
}