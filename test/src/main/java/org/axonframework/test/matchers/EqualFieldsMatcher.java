/*
 * Copyright (c) 2010-2014. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.test.matchers;

import org.assertj.core.api.recursive.comparison.ComparisonDifference;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonDifferenceCalculator;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Matcher that will match an Object if all the fields on that Object contain values equal to the same field in the
 * expected instance.
 *
 * @param <T> The type of object
 * @author Allard Buijze
 * @since 1.1
 */
public class EqualFieldsMatcher<T> extends BaseMatcher<T> {

    private final T expected;
    private final FieldFilter filter;
    private Field failedField;
    private Object failedFieldExpectedValue;
    private Object failedFieldActualValue;

    /**
     * Initializes an EqualFieldsMatcher that will match an object with equal properties as the given
     * <code>expected</code> object.
     *
     * @param expected The expected object
     */
    public EqualFieldsMatcher(T expected) {
        this(expected, AllFieldsFilter.instance());
    }

    /**
     * Initializes an EqualFieldsMatcher that will match an object with equal properties as the given
     * <code>expected</code> object.
     *
     * @param expected The expected object
     * @param filter   The filter describing the fields to include in the comparison
     */
    public EqualFieldsMatcher(T expected, FieldFilter filter) {
        this.expected = expected;
        this.filter = filter;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public boolean matches(Object item) {
        return expected.getClass().isInstance(item) && matchesSafely(item);
    }

    private boolean matchesSafely(Object actual) {
        var configuration = RecursiveComparisonConfiguration.builder()
            .withStrictTypeChecking(true)
            .build();

        // Optionally configure the comparison here if needed
        // e.g., ignoring fields, custom comparators, etc.

        RecursiveComparisonDifferenceCalculator calculator = new RecursiveComparisonDifferenceCalculator();
        List<ComparisonDifference> comparisonDifferences = calculator.determineDifferences(actual, expected, configuration);

        if (comparisonDifferences.isEmpty()) {
            return true; // No differences found
        }

        // Assuming we're interested in the first difference:
        ComparisonDifference firstDifference = comparisonDifferences.get(0);
        failedFieldExpectedValue = firstDifference.getExpected();
        failedFieldActualValue = firstDifference.getActual();
        failedField = getField(actual.getClass(), firstDifference.getDecomposedPath());
        return false;
    }

    private Field getField(Class<?> clazz, List<String> decomposedPath) {
        try {
            if (decomposedPath.size() > 1) {
                Field field = findFieldInHierarchy(clazz, decomposedPath.get(0));
                return getField(field.getType(), decomposedPath.subList(1, decomposedPath.size()));
            }
            return clazz.getDeclaredField(decomposedPath.get(0));
        } catch (NoSuchFieldException e) {
            throw new MatcherNoSuchFieldException(clazz, decomposedPath, e);
        }
    }

    private static Field findFieldInHierarchy(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;

        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        throw new MatcherNoSuchFieldException(clazz, fieldName);
    }

    /**
     * Returns the field that failed comparison, if any. This value is only populated after {@link #matches(Object)} is
     * called and a mismatch has been detected.
     *
     * @return the field that failed comparison, if any
     */
    public Field getFailedField() {
        return failedField;
    }

    /**
     * Returns the expected value of a failed field comparison, if any. This value is only populated after {@link
     * #matches(Object)} is called and a mismatch has been detected.
     *
     * @return the expected value of the field that failed comparison, if any
     */
    public Object getFailedFieldExpectedValue() {
        return failedFieldExpectedValue;
    }

    /**
     * Returns the actual value of a failed field comparison, if any. This value is only populated after {@link
     * #matches(Object)} is called and a mismatch has been detected.
     *
     * @return the actual value of the field that failed comparison, if any
     */
    public Object getFailedFieldActualValue() {
        return failedFieldActualValue;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(expected.getClass().getName());
        if (failedField != null) {
            description.appendText(" (failed on field '")
                       .appendText(failedField.getName())
                       .appendText("')");
        }
    }

    public static class MatcherNoSuchFieldException extends RuntimeException {

        private static final String MESSAGE = "In the class %s, could not find the field of a given path of %s";

        public MatcherNoSuchFieldException(Class<?> clazz, List<String> decomposedPath, Throwable cause) {
            super(
                MESSAGE.formatted(
                    clazz.getName(),
                    String.join(".", decomposedPath)
                ),
                cause
            );
        }

        public MatcherNoSuchFieldException(Class<?> clazz, String fieldName) {
            super(MESSAGE.formatted(clazz.getName(), fieldName));
        }
    }
}
