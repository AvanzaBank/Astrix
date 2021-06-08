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
package com.avanza.astrix.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AstrixRemoteResultTest {
	
	@Test
	void successfulResult() {
		AstrixRemoteResult<String> result = AstrixRemoteResult.successful("foo");
		
		assertFalse(result.hasThrownException());
		assertEquals("foo", result.getResult());
		assertNull(result.getThrownException());
	}
	
	@Test
	void serviceUnavailableResult() {
		AstrixRemoteResult<String> result = AstrixRemoteResult.unavailable("unavailable", CorrelationId.valueOf("foo"));
		
		assertTrue(result.hasThrownException());
		assertEquals(ServiceUnavailableException.class, result.getThrownException().getClass());

		assertThrows(ServiceUnavailableException.class, result::getResult, "Expected ServiceUnavailableException");
	}
	
	@Test
	void serviceInvocationExceptionResult() {
		AstrixRemoteResult<String> result = AstrixRemoteResult.failure(new FakeServiceInvocationException(), CorrelationId.valueOf("foo"));
		
		assertTrue(result.hasThrownException());
		assertEquals(FakeServiceInvocationException.class, result.getThrownException().getClass());

		assertThrows(FakeServiceInvocationException.class, result::getResult, "Expected FakeServiceInvocationException");
	}
	
	private static class FakeServiceInvocationException extends ServiceInvocationException {
	}

}
