/*
 * Copyright 2014 Open Networking Foundation
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
package org.onosproject.ipfix;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cfg.ComponentConfigAdapter;

/**
 * Set of tests for the ONOS IPFIX application.
 */
public class Of2IpfixTest {

    private IpfixManager component;

    @Before
    public void setUp() {
        component = new IpfixManager();
        component.cfgService = new ComponentConfigAdapter();
        // component.activate(null);

    }

    @After
    public void tearDown() {
        // component.deactivate(null);
    }

    @Test
    public void basics() {

    }

}
