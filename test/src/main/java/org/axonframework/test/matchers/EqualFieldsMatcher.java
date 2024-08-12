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
import org.assertj.core.error.ErrorMessageFactory;
import org.assertj.core.error.ShouldBeEqualByComparingFieldByFieldRecursively;
import org.assertj.core.presentation.StandardRepresentation;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final List<String> fieldsToIgnore = new ArrayList<>();
    private String errorMessage;
    private String failedField;

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
        populateIgnoredFieldsList(filter);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public boolean matches(Object item) {
        return expected.getClass().isInstance(item) && matchesSafely(item);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(expected.getClass().getName());
        if (errorMessage != null) {
            description.appendText(errorMessage);
        }
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    private boolean matchesSafely(Object actual) {
        var configuration = RecursiveComparisonConfiguration.builder()
            .withStrictTypeChecking(true)
            .withIgnoredFields(fieldsToIgnore.toArray(String[]::new))
            .build();

        RecursiveComparisonDifferenceCalculator calculator = new RecursiveComparisonDifferenceCalculator();
        List<ComparisonDifference> comparisonDifferences = calculator.determineDifferences(actual, expected, configuration);

        if (comparisonDifferences.isEmpty()) {
            return true; // No differences found
        }

        ErrorMessageFactory errorMessageFactory =
            ShouldBeEqualByComparingFieldByFieldRecursively.shouldBeEqualByComparingFieldByFieldRecursively(
                actual,
                expected,
                comparisonDifferences,
                configuration,
                new StandardRepresentation()
            );
        errorMessage = errorMessageFactory.create();
        failedField = comparisonDifferences.get(0)
            .getDecomposedPath()
            .stream()
            .filter(Objects::nonNull)
            .collect(Collectors.joining());
        return false;
    }

    private void populateIgnoredFieldsList(FieldFilter currentFilter) {
        if (currentFilter instanceof IgnoreField ignoreFieldFilter) {
            fieldsToIgnore.add(ignoreFieldFilter.getField());
        } else if (currentFilter instanceof NonTransientFieldsFilter) {
            List<String> transientFieldsToIgnore = Arrays.stream(expected.getClass().getDeclaredFields())
                .filter(field -> Modifier.isTransient(field.getModifiers()))
                .map(Field::getName)
                .toList();
            fieldsToIgnore.addAll(transientFieldsToIgnore);
        } else if (currentFilter instanceof MatchAllFieldFilter matchAllFieldFilter) {
            for (FieldFilter fieldSubFilter : matchAllFieldFilter.getFilters()) {
                populateIgnoredFieldsList(fieldSubFilter);
            }
        }
    }

    public String getFailedField() {
        return failedField;
    }
}
