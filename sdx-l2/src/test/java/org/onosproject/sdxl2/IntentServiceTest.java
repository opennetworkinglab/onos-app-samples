/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.sdxl2;

import com.google.common.collect.Sets;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentServiceAdapter;
import org.onosproject.net.intent.Key;

import java.util.Set;

/**
 * Represents a fake IntentService class that easily allows to store and
 * retrieve intents without implementing the IntentService logic.
 */
public class IntentServiceTest extends IntentServiceAdapter {

    private Set<Intent> intents;

    /**
     * Defines a set of intents.
     */
    IntentServiceTest() {
        intents = Sets.newHashSet();
    }

    @Override
    public void submit(Intent intent) {
        intents.add(intent);
    }

    @Override
    public long getIntentCount() {
        return intents.size();
    }

    @Override
    public Iterable<Intent> getIntents() {
        return  Sets.newHashSet(intents.iterator());
    }

    @Override
    public Intent getIntent(Key intentKey) {
        for (Intent intent : intents) {
            if (intent.key().equals(intentKey)) {
                return intent;
            }
        }
        return null;
    }

    @Override
    public void withdraw(Intent intent) {
        intents.remove(intent);
    }

}
