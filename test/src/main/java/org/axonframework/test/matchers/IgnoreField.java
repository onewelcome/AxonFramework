/*
 * Copyright (c) 2010-2015. Axon Framework
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

import org.axonframework.test.FixtureExecutionException;

/**
 * FieldFilter implementation that rejects a given Field
 *
 * @author Allard Buijze
 * @since 2.4.1
 */
public class IgnoreField implements FieldFilter {

    private String ignoredField;

    /**
     * Initialize an instance that ignores the a field with given <code>fieldName</code>, which is declared on the
     * given
     * <code>clazz</code>.
     *
     * @param clazz     The type that declares the field
     * @param fieldName The name of the field
     * @throws FixtureExecutionException when the given fieldName is not declared on given clazz.
     */
    public IgnoreField(Class<?> clazz, String fieldName) {
        try {
            clazz.getDeclaredField(fieldName);
            ignoredField = fieldName;
        } catch (NoSuchFieldException e) {
            throw new FixtureExecutionException("The given field does not exist", e);
        }
    }

    @Override
    public boolean accept(String field) {
        return !field.equals(ignoredField);
    }

    @Override
    public String getField() {
        return ignoredField;
    }
}
