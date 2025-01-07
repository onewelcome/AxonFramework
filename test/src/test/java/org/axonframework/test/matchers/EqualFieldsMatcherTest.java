/*
 * Copyright (c) 2010-2012. Axon Framework
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

import org.axonframework.test.MyEvent;
import org.axonframework.test.MyOtherEvent;
import org.hamcrest.StringDescription;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Allard Buijze
 */
public class EqualFieldsMatcherTest {

    private EqualFieldsMatcher<MyEvent> testSubject;
    private MyEvent expectedEvent;
    private String aggregateId = "AggregateId";

    @Before
    public void setUp() {
        expectedEvent = new MyEvent(aggregateId, 1);
        testSubject = Matchers.equalTo(expectedEvent);
    }

    @Test
    public void testMatches_SameInstance() {
        assertThat(testSubject.matches(expectedEvent)).isTrue();
    }

    @Test
    public void testMatches_EqualInstance() {
        assertThat(testSubject.matches(new MyEvent(aggregateId, 1))).isTrue();
    }

    @Test
    public void testMatches_WrongEventType() {
        assertThat(testSubject.matches(new MyOtherEvent())).isFalse();
    }

    @Test
    public void testMatches_WrongFieldValue() {
        assertThat(testSubject.matches(new MyEvent(aggregateId, 2))).isFalse();
        assertThat(testSubject.getFailedField()).isEqualTo("someValue");
    }

    @Test
    public void testMatches_WrongFieldValueInIgnoredField() {
        testSubject = Matchers.equalTo(expectedEvent, new IgnoreField(MyEvent.class, "someValue"));
        assertThat(testSubject.matches(new MyEvent(aggregateId, 2))).isTrue();
    }

    @Test
    public void testMatches_WrongFieldValueInArray() {
        assertThat(testSubject.matches(new MyEvent(aggregateId, 1, new byte[]{1, 2}))).isFalse();
        assertThat(testSubject.getFailedField()).isEqualTo("someBytes");
    }

    @Test
    public void testDescription_AfterSuccess() {
        testSubject.matches(expectedEvent);
        StringDescription description = new StringDescription();
        testSubject.describeTo(description);
        assertThat(description).hasToString("org.axonframework.test.MyEvent");
    }

    @Test
    public void testDescription_AfterMatchWithWrongType() {
        testSubject.matches(new MyOtherEvent());
        StringDescription description = new StringDescription();
        testSubject.describeTo(description);
        assertThat(description).hasToString("org.axonframework.test.MyEvent");
    }

    @Test
    public void testDescription_AfterMatchWithWrongFieldValue() {
        testSubject.matches(new MyEvent(aggregateId, 2));
        StringDescription description = new StringDescription();
        testSubject.describeTo(description);
        assertThat(description.toString())
            .contains("field/property 'someValue' differ")
            .contains("- actual value  : 2\n- expected value: 1");
    }
}
