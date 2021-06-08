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
package com.avanza.astrix.service.registry.client;


import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.beans.registry.ServiceRegistryExporterClient;
import com.avanza.astrix.beans.service.DirectComponent;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.versioning.core.AstrixObjectSerializerConfig;
import com.avanza.astrix.versioning.core.AstrixObjectSerializerConfigurer;
import com.avanza.astrix.versioning.core.Versioned;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AstrixSubsystemTest {
	
	@Test
	void itsAllowedToInvokePublishedServicesInOtherSubsystems() {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		// Simulate two different subsystems using to AstrixContextImpl's
		TestAstrixConfigurer configurerA = new TestAstrixConfigurer();
		configurerA.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		configurerA.setSubsystem("A");
		configurerA.registerApiProvider(VersionedGreetingServiceProvider.class);
		AstrixContext contextA = configurerA.configure();
		
		TestAstrixConfigurer configurerB = new TestAstrixConfigurer();
		configurerB.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		configurerB.setSubsystem("B");
		configurerB.registerApiProvider(VersionedGreetingServiceProvider.class);
		AstrixContext contextB = configurerB.configure();

		// Publish non versioned service in contextB
		ServiceRegistryExporterClient serviceRegistryClientB = new ServiceRegistryExporterClient(serviceRegistry, "B", "FooInstanceId");
		ServiceProperties serviceProperties = DirectComponent.registerAndGetProperties(GreetingService.class, new GreetingServiceImpl("hello"));
		serviceProperties.setProperty(ServiceProperties.PUBLISHED, "true");
		serviceRegistryClientB.register(GreetingService.class, serviceProperties, 1000);
		
		GreetingService greetingServiceB = contextB.getBean(GreetingService.class);
		
		assertNotNull(greetingServiceB.hello("foo"), "Its allowed to invoke service provided in same subsystem");
		
		GreetingService greetingServiceA = contextA.getBean(GreetingService.class);
		
		// It should be allowed to invoke this service since its versioned
		greetingServiceA.hello("bar");
	}
	
	@AstrixApiProvider
	interface GreetingServiceProvider {
		@Service
		GreetingService greetingService();
	}
	
	@AstrixObjectSerializerConfig(
		objectSerializerConfigurer = DummyConfigurer.class,
		version = 1
	)
	@AstrixApiProvider
	interface VersionedGreetingServiceProvider {
		@Versioned
		@Service
		GreetingService greetingService();
	}
	
	public static class DummyConfigurer implements AstrixObjectSerializerConfigurer {
	}

	public interface GreetingService {
		String hello(String msg);
	}

	public static class GreetingServiceImpl implements GreetingService {

		private final String greeting;

		public GreetingServiceImpl(String greeting) {
			this.greeting = greeting;
		}

		@Override
		public String hello(String msg) {
			return greeting + msg;
		}
	}

}
