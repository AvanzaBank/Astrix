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
package com.avanza.astrix.remoting.client;

import com.avanza.astrix.core.AstrixRouting;
import com.avanza.astrix.core.remoting.Router;
import com.avanza.astrix.core.remoting.RoutingKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultAstrixRoutingStrategyTest {
	
	@Test
	void routesOnRoutingAnnotatedArgument() throws Exception {
		class Service {
			public void hello(@AstrixRouting String routingArg, String anotherArg) {
			}
		}
		Router router = new DefaultAstrixRoutingStrategy().create(Service.class.getMethod("hello", String.class, String.class));
		RoutingKey routingKey = router.getRoutingKey(new Object[]{"routing-arg", "another-arg"});
		assertEquals(RoutingKey.create("routing-arg"), routingKey);
	}
	
	@Test
	void routesOnRoutingAnnotatedArgumentPropertyIfDefined() throws Exception {
		class Service {
			@SuppressWarnings("unused")
			public void hello(@AstrixRouting("getRouting") ProperRoutingMethod routingArg) {
			}
		}
		
		Router router = new DefaultAstrixRoutingStrategy().create(Service.class.getMethod("hello", ProperRoutingMethod.class));
		RoutingKey routingKey = router.getRoutingKey(new Object[]{new ProperRoutingMethod()});
		assertEquals(RoutingKey.create("routing-arg"), routingKey);
	}
	
	@Test
	void missingPropertyMethod_throwsIllegalArgumentException() {
		class Service {
			@SuppressWarnings("unused")
			public void hello(@AstrixRouting("getRouting") MissingRoutingMethod routingArg) {
			}
		}
		assertThrows(IllegalArgumentException.class, () -> new DefaultAstrixRoutingStrategy().create(Service.class.getMethod("hello", MissingRoutingMethod.class)));
	}
	
	@Test
	void invalidPropertyMethod_throwsIllegalArgumentException() {
		class Service {
			@SuppressWarnings("unused")
			public void hello(@AstrixRouting("getRouting") IllegalRoutingMethod routingArg) {
			}
		}
		
		assertThrows(IllegalArgumentException.class, () -> new DefaultAstrixRoutingStrategy().create(Service.class.getMethod("hello", IllegalRoutingMethod.class)));
	}
	
	@Test
	void multipleRoutingAnnotations_throwsAmbiguousRoutingException() {
		class Service {
			@SuppressWarnings("unused")
			public void hello(@AstrixRouting String routingArg, @AstrixRouting String routingArg2) {
			}
		}
		assertThrows(AmbiguousRoutingException.class, () -> new DefaultAstrixRoutingStrategy().create(Service.class.getMethod("hello", String.class, String.class)));
	}
	
	public static class ProperRoutingMethod {
		public String getRouting() {
			return "routing-arg";
		}
	}
	
	public static class MissingRoutingMethod {
	}
	
	public static class IllegalRoutingMethod {
		public String getRouting(String illegalArgument) {
			return "";
		}
	}


}
