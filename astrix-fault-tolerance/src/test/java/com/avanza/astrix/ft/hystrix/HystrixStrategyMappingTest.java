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
package com.avanza.astrix.ft.hystrix;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HystrixStrategyMappingTest {
	
	@Test
	void parsesAstrixContextIdFromCommandKey() {
		HystrixStrategyMapping strategiesMappingTest = new HystrixStrategyMapping();
		assertEquals(Optional.empty(), strategiesMappingTest.parseStrategiesId("fooBar"));
		assertEquals(Optional.empty(), strategiesMappingTest.parseStrategiesId("fooBar[1"));
		assertEquals(Optional.of("2"), strategiesMappingTest.parseStrategiesId("fooBar[2]"));
		assertEquals(Optional.empty(), strategiesMappingTest.parseStrategiesId("fooBar[3]]"));
		assertEquals(Optional.of("5"), strategiesMappingTest.parseStrategiesId("fooBar[4]][5]"));
	}
	
}
