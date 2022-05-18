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
package com.avanza.astrix.integration.tests;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.AstrixServiceRegistry;
import com.avanza.astrix.beans.registry.ServiceRegistryClient;
import com.avanza.astrix.beans.registry.ServiceRegistryExporterClient;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.gs.GsBinder;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.test.util.AutoCloseableRule;
import com.avanza.gs.test.PuConfigurers;
import com.avanza.gs.test.RunningPu;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class ServiceRegistryPuIntegrationTest {
	
	@Rule
	public RunningPu serviceRegistrypu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/service-registry-pu.xml")
															.numberOfPrimaries(1)
															.numberOfBackups(0)
															.startAsync(false)
															.configure();
	
	private final MapConfigSource clientConfig = new MapConfigSource() {{
		set(AstrixSettings.SERVICE_REGISTRY_URI, AstrixServiceComponentNames.GS_REMOTING + ":jini://*/*/service-registry-space?locators=" + serviceRegistrypu.getLookupLocator());
	}};
	
	private AstrixContext clientContext;
	
	@Rule
	public AutoCloseableRule autoCloseableRule = new AutoCloseableRule();
	
	@Before
	public void setup() throws Exception {
		this.clientContext = autoCloseableRule.add(new AstrixConfigurer().setConfig(DynamicConfig.create(clientConfig)).configure());
	}
	
	@Test
	public void serviceRegistration() throws Exception {
		AstrixServiceRegistry serviceRegistry = clientContext.getBean(AstrixServiceRegistry.class);
		ServiceRegistryClient serviceRegistryClient = clientContext.getBean(ServiceRegistryClient.class);
		ServiceRegistryExporterClient exporterClient1 =  new ServiceRegistryExporterClient(serviceRegistry, "default", "app-instance-1");
		ServiceRegistryExporterClient exporterClient2 =  new ServiceRegistryExporterClient(serviceRegistry, "default", "app-instance-2");
		
		ServiceProperties server1Props = new ServiceProperties();
		server1Props.getProperties().put("myProp", "1");
		ServiceProperties server2 = new ServiceProperties();
		server2.getProperties().put("myProp", "1");
		exporterClient1.register(SomeService.class, server1Props, 10000);
		exporterClient2.register(SomeService.class, server2, 10000);
		exporterClient2.register(AnotherService.class, new ServiceProperties(), 10000);
		
		List<ServiceProperties> providers = serviceRegistryClient.list(AstrixBeanKey.create(SomeService.class));
		assertEquals(2, providers.size());
		
		server1Props = new ServiceProperties();
		server1Props.getProperties().put("myProp", "3");
		exporterClient1.register(SomeService.class, server1Props, 10000);
		
		providers = serviceRegistryClient.list(AstrixBeanKey.create(SomeService.class));
		assertEquals(2, providers.size());
		ServiceProperties serviceProperties = getPropertiesForAppInstance("app-instance-1", providers);
		assertEquals("3", serviceProperties.getProperty("myProp"));
	}
	
	private ServiceProperties getPropertiesForAppInstance(String appInstanceId, List<ServiceProperties> providers) {
		for (ServiceProperties properties : providers) {
			if (appInstanceId.equals(properties.getProperties().get(ServiceProperties.APPLICATION_INSTANCE_ID))) {
				return properties;
			}
		}
		return null;
	}

	interface SomeService {
		
	}
	
	interface AnotherService {
	}

	@Test
	public void serviceRegistrationRetainsHighestValueOfPuStartTime() {
		// Arrange
		AstrixServiceRegistry serviceRegistry = clientContext.getBean(AstrixServiceRegistry.class);
		ServiceRegistryClient serviceRegistryClient = clientContext.getBean(ServiceRegistryClient.class);
		ServiceRegistryExporterClient exporterClient = new ServiceRegistryExporterClient(serviceRegistry, "default", "app-instance-1");
		Instant t0 = Instant.parse("2021-01-01T12:00:00.000Z");
		Instant t1 = t0.plus(5, ChronoUnit.MINUTES);
		String t0AsString = String.valueOf(t0.toEpochMilli());
		String t1AsString = String.valueOf(t1.toEpochMilli());
		ServiceProperties props0 = new ServiceProperties(Map.of(GsBinder.START_TIME, t0AsString));
		ServiceProperties props1 = new ServiceProperties(Map.of(GsBinder.START_TIME, t1AsString));
		long lease = 5000;

		// Act
		exporterClient.register(SomeService.class, props0, lease);
		exporterClient.register(SomeService.class, props1, lease);
		exporterClient.register(AnotherService.class, props1, lease);
		exporterClient.register(AnotherService.class, props0, lease);

		// Assert
		ServiceProperties foundProps1 = serviceRegistryClient.lookup(AstrixBeanKey.create(SomeService.class));
		ServiceProperties foundProps2 = serviceRegistryClient.lookup(AstrixBeanKey.create(AnotherService.class));
		// We want to make sure that both registrations have stored the time of
		// "t1" in its registration (the max value of all "startTime").
		// Specifically, they should *not* have "t0".
		assertEquals(t1AsString, foundProps1.getProperty(GsBinder.START_TIME));
		assertEquals(t1AsString, foundProps2.getProperty(GsBinder.START_TIME));
	}
}
