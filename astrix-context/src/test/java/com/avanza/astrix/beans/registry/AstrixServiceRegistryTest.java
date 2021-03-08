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
package com.avanza.astrix.beans.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.service.DirectComponent;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.test.util.AstrixTestUtil;

public class AstrixServiceRegistryTest {
	
	InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
	TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
	AstrixContext clientContext;

	@Before
	public void setup() {
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryLibraryProvider.class);
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryServiceProvider.class);
	}
	
	@After
	public void cleanup() {
		AstrixTestUtil.closeQuiet(clientContext);
	}
	
	
	@Test
	public void serviceRegistrySupportsMultipleProvidersOfSameService() throws Exception {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		
		astrixConfigurer.setSubsystem("default");
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		clientContext = astrixConfigurer.configure();
		
		
		ServiceRegistryExporterClient server1serviceRegistryClient = new ServiceRegistryExporterClient(serviceRegistry, "default", "server-1");
		server1serviceRegistryClient.register(Ping.class, DirectComponent.registerAndGetProperties(Ping.class, new PingImpl("1")), Integer.MAX_VALUE);
		
		
		ServiceRegistryExporterClient server2serviceRegistryClient = new ServiceRegistryExporterClient(serviceRegistry, "default", "server-2");
		server2serviceRegistryClient.register(Ping.class, DirectComponent.registerAndGetProperties(Ping.class, new PingImpl("2")), Integer.MAX_VALUE);
		
		
		Ping ping1 = clientContext.getBean(Ping.class);
		ServiceRegistryClient serviceRegistryClient = clientContext.getBean(ServiceRegistryClient.class);
		List<ServiceProperties> providers = serviceRegistryClient.list(AstrixBeanKey.create(Ping.class));
		assertEquals(2, providers.size());
		assertNotNull(ping1.ping());
	}
	
	@Test
	public void doesNotBindToNonPublishedProvidersInOtherZones() throws Exception {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		
		astrixConfigurer.setSubsystem("default");
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		clientContext = astrixConfigurer.configure();
		
		
		ServiceRegistryExporterClient server1serviceRegistryClient = new ServiceRegistryExporterClient(serviceRegistry, "my-subsystem", "server-1");
		ServiceProperties service1Properties = DirectComponent.registerAndGetProperties(Ping.class, new PingImpl("1"));
		service1Properties.setProperty(ServiceProperties.PUBLISHED, "false");
		service1Properties.setProperty(ServiceProperties.SERVICE_ZONE, "foo-zone");
		server1serviceRegistryClient.register(Ping.class, service1Properties, Integer.MAX_VALUE);
		
		ServiceRegistryClient serviceRegistryClient = clientContext.getBean(ServiceRegistryClient.class);
		List<ServiceProperties> providers = serviceRegistryClient.list(AstrixBeanKey.create(Ping.class));
		assertEquals(1, providers.size());

		Ping ping = clientContext.getBean(Ping.class);
		try {
			ping.ping();
			fail("Expected service to not be available when server is INACTIVE");
		} catch (ServiceUnavailableException e) {
			// expected
		}
	}
	
	@Test
	public void bindsToNonPublishedProvidersInSameZone() throws Exception {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		
		astrixConfigurer.setSubsystem("my-subsystem");
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		clientContext = astrixConfigurer.configure();
		
		
		ServiceRegistryExporterClient server1serviceRegistryClient = new ServiceRegistryExporterClient(serviceRegistry, "my-subsystem", "server-1");
		ServiceProperties service1Properties = DirectComponent.registerAndGetProperties(Ping.class, new PingImpl("1"));
		service1Properties.setProperty(ServiceProperties.PUBLISHED, "false");
		server1serviceRegistryClient.register(Ping.class, service1Properties, Integer.MAX_VALUE);
		
		ServiceRegistryClient serviceRegistryClient = clientContext.getBean(ServiceRegistryClient.class);
		List<ServiceProperties> providers = serviceRegistryClient.list(AstrixBeanKey.create(Ping.class));
		assertEquals(1, providers.size());

		Ping ping = clientContext.getBean(Ping.class);
		assertNotNull(ping.ping());
	}
	
	@Test
	public void usesRoundRobinToDistributeConsumers() throws Exception {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		clientContext = astrixConfigurer.configure();
		
		
		ServiceRegistryExporterClient server1serviceRegistryClient = new ServiceRegistryExporterClient(serviceRegistry, "default", "server-1");
		ServiceProperties service1Properties = DirectComponent.registerAndGetProperties(Ping.class, new PingImpl("1"));
		server1serviceRegistryClient.register(Ping.class, service1Properties, Integer.MAX_VALUE);
		
		ServiceRegistryExporterClient server2serviceRegistryClient = new ServiceRegistryExporterClient(serviceRegistry, "default", "server-2");
		ServiceProperties service2Properties = DirectComponent.registerAndGetProperties(Ping.class, new PingImpl("1"));
		server2serviceRegistryClient.register(Ping.class, service2Properties, Integer.MAX_VALUE);
		
		ServiceRegistryClient serviceRegistryClient = clientContext.getBean(ServiceRegistryClient.class);
		int server1ConsumerCount = 0;
		int server2ConsumerCount = 0;
		for (int i = 0; i < 10; i++) {
			ServiceProperties props = serviceRegistryClient.lookup(AstrixBeanKey.create(Ping.class));
			if ("server-1".equals(props.getProperty(ServiceProperties.APPLICATION_INSTANCE_ID))) {
				server1ConsumerCount++;
			}
			if ("server-2".equals(props.getProperty(ServiceProperties.APPLICATION_INSTANCE_ID))) {
				server2ConsumerCount++;
			}
		}
		
		assertEquals(5, server1ConsumerCount);
		assertEquals(5, server2ConsumerCount);
		
	}

	@Test
	public void usesApplicationInstanceIdToDeregisterService() {
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		clientContext = astrixConfigurer.configure();

		ServiceRegistryExporterClient registryClient = new ServiceRegistryExporterClient(serviceRegistry, "default", "server-1");
		registryClient.register(Ping.class, DirectComponent.registerAndGetProperties(Ping.class, new PingImpl("1")), Integer.MAX_VALUE);

		AstrixServiceRegistryEntry entryToDeregister = new AstrixServiceRegistryEntry();
		entryToDeregister.getServiceProperties().put(ServiceProperties.APPLICATION_INSTANCE_ID, "server-1");
		entryToDeregister.setServiceBeanType(Ping.class.getName());
		serviceRegistry.deregister(entryToDeregister);

		ServiceRegistryClient serviceRegistryClient = clientContext.getBean(ServiceRegistryClient.class);
		List<ServiceProperties> providers = serviceRegistryClient.list(AstrixBeanKey.create(Ping.class));
		assertEquals(0, providers.size());
		assertEquals(0, serviceRegistry.listServices().size());
	}
	
	@AstrixApiProvider
	public interface PingApiProvider {
		@Service
		Ping ping();
	}
	
	public interface Ping {
		String ping();
	}
	
	public class PingImpl implements Ping {
		private String id;
		
		public PingImpl(String id) {
			this.id = id;
		}

		@Override
		public String ping() {
			return id;
		}
		
	}

}
