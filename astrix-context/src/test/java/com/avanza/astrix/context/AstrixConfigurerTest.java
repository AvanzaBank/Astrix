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

import com.avanza.astrix.beans.config.BeanConfiguration;
import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.core.AstrixBeanSettings.BooleanBeanSetting;
import com.avanza.astrix.beans.core.AstrixBeanSettings.IntBeanSetting;
import com.avanza.astrix.beans.core.AstrixBeanSettings.LongBeanSetting;
import com.avanza.astrix.beans.core.AstrixBeanSettings.StringBeanSetting;
import com.avanza.astrix.beans.publish.ApiProviderClass;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.DefaultBeanSettings;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.test.util.AutoCloseableExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AstrixConfigurerTest {
	
	@RegisterExtension
	AutoCloseableExtension autoClosables = new AutoCloseableExtension();
	
	@Test
	void passesBeanSettingsToConfiguration() {
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.setAstrixApiProviders(Stream::empty);
		IntBeanSetting intSetting = new IntBeanSetting("intSetting", 1);
		BooleanBeanSetting aBooleanSetting = new BooleanBeanSetting("booleanSetting", true);
		LongBeanSetting longSetting = new LongBeanSetting("longSetting", 2);
		StringBeanSetting stringSetting = new StringBeanSetting("stringSetting", "foo");
		
		configurer.set(aBooleanSetting, AstrixBeanKey.create(Ping.class), false);
		configurer.set(intSetting, AstrixBeanKey.create(Ping.class), 21);
		configurer.set(longSetting, AstrixBeanKey.create(Ping.class), 19);
		configurer.set(stringSetting, AstrixBeanKey.create(Ping.class), "bar");
		
		AstrixContextImpl astrixContext = autoClosables.add((AstrixContextImpl) configurer.configure());
		BeanConfiguration pingConfig = astrixContext.getBeanConfiguration(AstrixBeanKey.create(Ping.class));
		
		assertEquals(21, pingConfig.get(intSetting).get());
		assertFalse(pingConfig.get(aBooleanSetting).get());
		assertEquals(19, pingConfig.get(longSetting).get());
		assertEquals("bar", pingConfig.get(stringSetting).get());
	}
	
	@Test
	void customDefaultBeanSettings() {
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.setAstrixApiProviders(() -> Stream.of(ApiProviderClass.create(PingApiProvider.class)));
		
		AstrixContextImpl astrixContext = autoClosables.add((AstrixContextImpl) configurer.configure());
		BeanConfiguration pingConfig = astrixContext.getBeanConfiguration(AstrixBeanKey.create(Ping.class));

		assertEquals(2000, pingConfig.get(AstrixBeanSettings.TIMEOUT).get());
		assertFalse(pingConfig.get(AstrixBeanSettings.FAULT_TOLERANCE_ENABLED).get());
		assertFalse(pingConfig.get(AstrixBeanSettings.BEAN_METRICS_ENABLED).get());
		assertEquals(1, pingConfig.get(AstrixBeanSettings.MAX_CONCURRENT_REQUESTS).get());
		assertEquals(2, pingConfig.get(AstrixBeanSettings.CORE_SIZE).get());
		assertEquals(3, pingConfig.get(AstrixBeanSettings.QUEUE_SIZE_REJECTION_THRESHOLD).get());
	}
	
	@Test
	void itsPossibleToOverrideCustomDefaultBeanSettingsOnBeanDefinition() {
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.setAstrixApiProviders(() -> Stream.of(ApiProviderClass.create(PingApiProviderWithOverridingDefault.class)));
		
		AstrixContextImpl astrixContext = autoClosables.add((AstrixContextImpl) configurer.configure());
		BeanConfiguration pingConfig = astrixContext.getBeanConfiguration(AstrixBeanKey.create(Ping.class));

		assertEquals(3000, pingConfig.get(AstrixBeanSettings.TIMEOUT).get());
		assertTrue(pingConfig.get(AstrixBeanSettings.FAULT_TOLERANCE_ENABLED).get());
		assertFalse(pingConfig.get(AstrixBeanSettings.BEAN_METRICS_ENABLED).get());
		assertEquals(2, pingConfig.get(AstrixBeanSettings.MAX_CONCURRENT_REQUESTS).get());
		assertEquals(5, pingConfig.get(AstrixBeanSettings.CORE_SIZE).get());
		assertEquals(6, pingConfig.get(AstrixBeanSettings.QUEUE_SIZE_REJECTION_THRESHOLD).get());
	}
	
	@Test
	void customDefaultBeanSettingsAppliesToAsyncProxy() {
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.setAstrixApiProviders(() -> Stream.of(ApiProviderClass.create(PingApiProvider.class)));
		
		AstrixContextImpl astrixContext = autoClosables.add((AstrixContextImpl) configurer.configure());
		BeanConfiguration pingConfig = astrixContext.getBeanConfiguration(AstrixBeanKey.create(PingAsync.class));

		assertEquals(2000, pingConfig.get(AstrixBeanSettings.TIMEOUT).get());
	}
	
	@DefaultBeanSettings(
		initialTimeout = 2000,
		faultToleranceEnabled = false,
		beanMetricsEnabled = false,
		initialMaxConcurrentRequests = 1,
		initialCoreSize = 2,
		initialQueueSizeRejectionThreshold = 3
	)
	public interface Ping {
	}
	
	public interface PingAsync {
	}
	
	@AstrixApiProvider
	public interface PingApiProvider {
		@Service
		Ping ping();
	}
	
	@AstrixApiProvider
	public interface PingApiProviderWithOverridingDefault {
		@DefaultBeanSettings(
			initialTimeout=3000,
			faultToleranceEnabled = true,
			beanMetricsEnabled = false,
			initialMaxConcurrentRequests = 2,
			initialCoreSize = 5,
			initialQueueSizeRejectionThreshold = 6
		)
		@Service
		Ping ping();
	}
	
	
}
