/*
 * Copyright 2010 Proofpoint, Inc.
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
package com.proofpoint.example.classifier;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.proofpoint.discovery.client.DiscoveryBinder;

public class MainModule
        implements Module
{
    public void configure(Binder binder)
    {
        binder.requireExplicitBindings();
        binder.disableCircularProxies();

        DiscoveryBinder.discoveryBinder(binder).bindHttpAnnouncement("classifier");

        binder.bind(ClassifierResource.class).in(Scopes.SINGLETON);
        binder.bind(UsersOnHoldResource.class).in(Scopes.SINGLETON);
        binder.bind(UsersOnHold.class).in(Scopes.SINGLETON);
        binder.bind(ClassificationStore.class).to(InMemoryClassificationStore.class).in(Scopes.SINGLETON);
    }
}
