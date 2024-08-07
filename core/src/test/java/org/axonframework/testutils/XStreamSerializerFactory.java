package org.axonframework.testutils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.WildcardTypePermission;
import org.axonframework.serializer.xml.XStreamSerializer;

import java.util.stream.Stream;

public class XStreamSerializerFactory {
  public static XStreamSerializer create(Class<?>... permittedClasses) {
    XStream xStream = new XStream();
    String[] permittedClassNames = Stream.of(permittedClasses)
        .map(Class::getName)
        .toArray(String[]::new);
    xStream.addPermission(new WildcardTypePermission(permittedClassNames));
    return new XStreamSerializer(xStream);
  }


}
