/*
 * Copyright 2014-2015 Avanza Bank AB
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
package com.avanza.astrix.remoting.client;

import java.lang.reflect.Method;

import com.gigaspaces.annotation.pojo.SpaceRouting;
/**
 * Scans a given class for a @SpaceRouting annotated method. 
 * 
 * @author Elias Lindholm (elilin)
 *
 */
final class RoutingKeyMethodScanner {

	/**
	 * Inspects the given Object looking for a routing key method. 
	 * 
	 * @param spaceObject
	 * @return	the routing key method if one exists, null otherwise
	 */
	public Method getRoutingKeyMethod(Class<?> spaceObjectClass) {
		Method result = null;
		for (Method m : spaceObjectClass.getMethods()) {
			if (m.isAnnotationPresent(SpaceRouting.class)) {
				if (result != null) {
					throw new IllegalArgumentException("Multiple methods annotated with @SpaceRouting found on class: " + spaceObjectClass.getName());
				}
				result = m;
			}
		}
		return result;
	}

}
