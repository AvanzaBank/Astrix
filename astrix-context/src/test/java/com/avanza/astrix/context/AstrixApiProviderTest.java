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
package com.avanza.astrix.context;

import com.avanza.astrix.beans.service.DirectComponent;
import com.avanza.astrix.context.JavaSerializationSerializerPlugin.JavaSerializationConfigurer;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixConfigDiscovery;
import com.avanza.astrix.provider.core.AstrixDynamicQualifier;
import com.avanza.astrix.provider.core.AstrixQualifier;
import com.avanza.astrix.provider.core.Library;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.versioning.core.AstrixObjectSerializerConfig;
import com.avanza.astrix.versioning.core.ObjectSerializerDefinition;
import com.avanza.astrix.versioning.core.ObjectSerializerFactoryPlugin;
import com.avanza.astrix.versioning.core.Versioned;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class AstrixApiProviderTest {
	
	@Test
	void apiWithOneLibrary() {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingLibraryProvider.class);
		AstrixContext context = astrixConfigurer.configure();
		
		PingLib ping = context.getBean(PingLib.class);
		assertEquals("foo", ping.ping("foo"));
	}
	
	@Test
	void librariesShouldNotBeStateful() {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingLibraryProvider.class);
		AstrixContext context = astrixConfigurer.configure();
		
		PingLib ping = context.getBean(PingLib.class);
		assertEquals(PingLibImpl.class, ping.getClass(), "Expected non-stateful astrix bean without a proxy.");
	}
	
	@Test
	void librariesCanBeQualifiedToDistinguishProviders() {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingAndReversePingLibraryProvider.class);
		AstrixContext context = astrixConfigurer.configure();
		
		PingLib ping = context.getBean(PingLib.class, "ping");
		PingLib reversePing = context.getBean(PingLib.class, "reverse-ping");
		assertEquals("hello", ping.ping("hello"));
		assertEquals("olleh", reversePing.ping("hello"));
	}
	
	@Test
	void servicesCanBeQualifiedToDistinguishProviders() {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.set("pingServiceUri", DirectComponent.registerAndGetUri(PingService.class, new PingServiceImpl()));
		astrixConfigurer.set("reversePingServiceUri", DirectComponent.registerAndGetUri(PingService.class, new ReversePingServiceImpl()));
		astrixConfigurer.registerApiProvider(PingAndReversePingServiceProvider.class);
		AstrixContext context = astrixConfigurer.configure();
		
		PingService ping = context.getBean(PingService.class, "ping");
		PingService reversePing = context.getBean(PingService.class, "reverse-ping");
		assertEquals("hello", ping.ping("hello"));
		assertEquals("olleh", reversePing.ping("hello"));
	}
	
	@Test
	void apiWithOneLibraryAndOneService() {
		String pingServiceUri = DirectComponent.registerAndGetUri(PingService.class, new PingServiceImpl());
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingServiceAndLibraryProvider.class);
		astrixConfigurer.set("pingServiceUri", pingServiceUri);
		AstrixContext context = astrixConfigurer.configure();
		
		PingLib pingLib = context.getBean(PingLib.class);
		assertEquals("foo", pingLib.ping("foo"));
		
		PingService pingService = context.getBean(PingService.class);
		assertEquals("bar", pingService.ping("bar"));
	}
	
	
	@Test
	void versionedApi() {
		String pingServiceUri = DirectComponent.registerAndGetUri(PingService.class, 
																		new PingServiceImpl(), 
																		ObjectSerializerDefinition.versionedService(1, JavaSerializationSerializerPlugin.JavaSerializationConfigurer.class));
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerStrategy(ObjectSerializerFactoryPlugin.class, new JavaSerializationSerializerPlugin());
		astrixConfigurer.registerApiProvider(VersionedPingServiceProvider.class);
		astrixConfigurer.set("pingServiceUri", pingServiceUri);
		AstrixContext context = astrixConfigurer.configure();
		
		PingService pingService = context.getBean(PingService.class);
		assertEquals("bar", pingService.ping("bar"));
	}
	
	@Test
	void apiWithVersionedAndTxNonVersionedService() {
		String pingServiceUri = DirectComponent.registerAndGetUri(PingService.class, 
																		new PingServiceImpl(), 
																		ObjectSerializerDefinition.versionedService(1, JavaSerializationSerializerPlugin.JavaSerializationConfigurer.class));
		String internalPingServiceUri = DirectComponent.registerAndGetUri(InternalPingService.class, 
																		new InternalPingServiceImpl(), 
																		ObjectSerializerDefinition.nonVersioned());
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerStrategy(ObjectSerializerFactoryPlugin.class, new JavaSerializationSerializerPlugin());
		astrixConfigurer.registerApiProvider(PublicAndInternalPingServiceProvider.class);
		astrixConfigurer.set("pingServiceUri", pingServiceUri);
		astrixConfigurer.set("internalPingServiceUri", internalPingServiceUri);
		AstrixContext context = astrixConfigurer.configure();
		
		PingService pingService = context.getBean(PingService.class);
		InternalPingService internalPingService = context.getBean(InternalPingService.class);
		assertEquals("foo", pingService.ping("foo"));
		assertEquals("bar", internalPingService.ping("bar"));
	}
	
	@Test
	void supportsDynamicQualifiedServices() {
		String pingServiceUri = DirectComponent.registerAndGetUri(PingService.class, 
																		new PingServiceImpl());
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(DynamicPingServiceProvider.class);
		astrixConfigurer.set("pingServiceUri", pingServiceUri);
		AstrixContext context = astrixConfigurer.configure();
		
		PingService pingService1 = context.getBean(PingService.class, "foo");
		PingService pingService2 = context.getBean(PingService.class, "bar");
		
		assertEquals("foo", pingService1.ping("foo"));
		assertEquals("foo", pingService2.ping("foo"));
		
	}
	
	public interface PingLib {
		String ping(String msg);
	}
	
	public interface PingService {
		String ping(String msg);
	}

	public interface InternalPingService {
		String ping(String msg);
	}
	
	public static class PingLibImpl implements PingLib {
		@Override
		public String ping(String msg) {
			return msg;
		}
	}
	
	public static class ReversePingLibImpl implements PingLib {
		@Override
		public String ping(String msg) {
			return new StringBuilder(msg).reverse().toString();
		}
	}
	
	public static class PingServiceImpl implements PingService {
		@Override
		public String ping(String msg) {
			return msg;
		}
	}
	
	public static class ReversePingServiceImpl implements PingService {
		@Override
		public String ping(String msg) {
			return new StringBuilder(msg).reverse().toString();
		}
	}
	
	public static class InternalPingServiceImpl implements InternalPingService {
		@Override
		public String ping(String msg) {
			return msg;
		}
	}
	
	@AstrixApiProvider
	public static class PingLibraryProvider {
		@Library
		public PingLib myLib() {
			return new PingLibImpl();
		}
	}
	
	@AstrixApiProvider
	public static class PingAndReversePingLibraryProvider {

		@AstrixQualifier("ping")
		@Library
		public PingLib ping() {
			return new PingLibImpl();
		}

		@AstrixQualifier("reverse-ping")
		@Library
		public PingLib reversePing() {
			return new ReversePingLibImpl();
		}
	}
	@AstrixApiProvider
	public interface PingAndReversePingServiceProvider {

		@AstrixConfigDiscovery("pingServiceUri")
		@AstrixQualifier("ping")
		@Service
		PingService pingLib();

		@AstrixConfigDiscovery("reversePingServiceUri")
		@AstrixQualifier("reverse-ping")
		@Service
		PingService reversePingLib();
	}

	@AstrixApiProvider
	public static class PingServiceAndLibraryProvider {
		@Library
		public PingLib myLib() {
			return new PingLibImpl();
		}
		
		@AstrixConfigDiscovery("pingServiceUri")
		@Service
		public PingService pingService() {
			return null;
		}
	}
	
	@AstrixObjectSerializerConfig(
		version = 1,
		objectSerializerConfigurer = JavaSerializationConfigurer.class
	)
	@AstrixApiProvider
	public interface VersionedPingServiceProvider {
		
		@Versioned
		@AstrixConfigDiscovery("pingServiceUri")
		@Service
		PingService pingService();
	}

	public interface InternalPingServiceApi {
		@AstrixConfigDiscovery("internalPingServiceUri")
		@Service
		InternalPingService internalPingService();
	}
	
	@AstrixObjectSerializerConfig(
		version = 1,
		objectSerializerConfigurer = JavaSerializationConfigurer.class
	)
	@AstrixApiProvider
	public interface PublicAndInternalPingServiceProvider {
		
		@Versioned
		@AstrixConfigDiscovery("pingServiceUri")
		@Service
		PingService pingService();
		
		@AstrixConfigDiscovery("internalPingServiceUri")
		@Service
		InternalPingService internalPingService();
	}
	
	@AstrixApiProvider
	public interface DynamicPingServiceProvider {

		@AstrixConfigDiscovery("pingServiceUri")
		@AstrixDynamicQualifier
		@Service
		PingService pingService();
	}

}
