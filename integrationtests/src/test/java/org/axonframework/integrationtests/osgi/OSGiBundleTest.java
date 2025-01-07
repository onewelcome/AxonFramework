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
package org.axonframework.integrationtests.osgi;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lburgazzoli
 */
@RunWith(PaxExam.class)
@Ignore("OSGi is not used by any of our components")
public class OSGiBundleTest {

    private static final String AXON_GROUP = "org.axonframework";
    private static final String[] AXON_ARTIFACTS = new String[]{"axon-core",
            "axon-amqp",
            "axon-distributed-commandbus"};

    @Inject
    BundleContext context;

    @Configuration
    public Option[] config() {
        return options(
                systemProperty("org.osgi.framework.storage.clean").value("true"),
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("WARN"),
                mavenBundle("com.rabbitmq", "amqp-client", System.getProperty("rabbitmq.version")),
                new File("core/target/classes").exists()
                        ? bundle("reference:file:core/target/classes")
                        : bundle("reference:file:../core/target/classes"),
                new File("amqp/target/classes").exists()
                        ? bundle("reference:file:amqp/target/classes")
                        : bundle("reference:file:../amqp/target/classes"),
                new File("distributed-commandbus/target/classes").exists()
                        ? bundle("reference:file:distributed-commandbus/target/classes")
                        : bundle("reference:file:../distributed-commandbus/target/classes"),
                junitBundles(),
                systemPackage("org.slf4j;version=1.7.0"),
                systemPackage("com.sun.tools.attach"),
                cleanCaches()
        );
    }

    @Test
    public void checkInject() {
        assertNotNull(context);
    }

    @Test
    public void checkBundles() throws ClassNotFoundException {
        Map<String, Bundle> axonBundles = new HashMap<String, Bundle>();

        for (Bundle bundle : context.getBundles()) {
            if (bundle != null) {
                if (bundle.getSymbolicName().startsWith(AXON_GROUP)) {
                    axonBundles.put(bundle.getSymbolicName(), bundle);
                }
            }
        }

        for (String artifact : AXON_ARTIFACTS) {
            assertTrue("Bundle " + artifact + " isn't deployed", axonBundles.containsKey(AXON_GROUP + "." + artifact));
            assertEquals("Bundle " + artifact + " isn't started",
                         axonBundles.get(AXON_GROUP + "." + artifact).getState(),
                         Bundle.ACTIVE);
        }
    }
}
