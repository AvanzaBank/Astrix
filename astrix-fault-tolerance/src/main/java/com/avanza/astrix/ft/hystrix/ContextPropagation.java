/*
 * Copyright 2016 Avanza Bank AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.avanza.astrix.ft.hystrix;

import com.avanza.astrix.beans.ft.ContextPropagator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ContextPropagation {

    private final List<ContextPropagator> propagators;

    public static ContextPropagation create(List<ContextPropagator> propagators) {
        return new ContextPropagation(propagators);
    }

    public ContextPropagation(List<ContextPropagator> propagators) {
        this.propagators = Collections.unmodifiableList(new ArrayList<>(propagators));
    }

    public <T> ContextPropagator.ThrowingCallable<T> wrap(ContextPropagator.ThrowingCallable<T> call) {
        ContextPropagator.ThrowingCallable<T> wrapping = call;
        for (ContextPropagator propagator : propagators) {
            wrapping = propagator.wrap(wrapping);
        }
        return wrapping;
    }

}
