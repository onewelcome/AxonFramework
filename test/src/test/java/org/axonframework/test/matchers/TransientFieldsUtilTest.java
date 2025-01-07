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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.List;

/**
 * @author Allard Buijze
 */
public class TransientFieldsUtilTest {

    @SuppressWarnings("unused") private transient String transientField;
    @SuppressWarnings("unused") private transient String transientField2;
    @SuppressWarnings("unused") private String nonTransientField;
    @SuppressWarnings("unused") private String nonTransientField2;

    @Test
    public void testAcceptNonTransientField() {
        List<String> transientFields = TransientFieldsUtil.getTransientFields(this);

        assertThat(transientFields)
            .containsExactlyInAnyOrder("transientField", "transientField2")
            .doesNotContain("nonTransientField", "nonTransientField2");
    }
}