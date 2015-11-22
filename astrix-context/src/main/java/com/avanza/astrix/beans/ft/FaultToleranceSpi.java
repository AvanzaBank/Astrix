/*
 * Copyright 2014 Avanza Bank AB
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
package com.avanza.astrix.beans.ft;

import java.util.function.Supplier;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.core.function.CheckedCommand;

import rx.Observable;

public interface FaultToleranceSpi {

	<T> Observable<T> observe(Supplier<Observable<T>> observable, AstrixBeanKey<?> beanKey);

	<T> T execute(CheckedCommand<T> command, AstrixBeanKey<?> beanKey) throws Throwable;
	
}