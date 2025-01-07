package org.axonframework.test.matchers;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

class TransientFieldsUtil {
  static <T> List<String> getTransientFields(T expected) {
      return Arrays.stream(expected.getClass().getDeclaredFields())
          .filter(field -> Modifier.isTransient(field.getModifiers()))
          .map(Field::getName)
          .toList();
  }
}
