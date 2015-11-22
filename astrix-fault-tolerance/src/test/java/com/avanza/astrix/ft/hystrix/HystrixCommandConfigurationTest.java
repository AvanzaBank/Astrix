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

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.ft.FaultToleranceSpi;
import com.avanza.astrix.context.AstrixApplicationContext;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.AstrixFaultToleranceProxy;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.DefaultBeanSettings;
import com.avanza.astrix.provider.core.Library;
import com.avanza.astrix.test.util.AutoCloseableRule;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;

public class HystrixCommandConfigurationTest {
	
	@Rule
	public AutoCloseableRule autoClosables = new AutoCloseableRule();
	private AstrixContext astrixContext;
	private TestAstrixConfigurer astrixConfigurer;
	
	@Before
	public void setup() {
		astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.enableFaultTolerance(true);
		astrixConfigurer.registerApiProvider(PingApi.class);
		astrixContext = autoClosables.add(astrixConfigurer.configure());
	}
	
	@Test
	public void differentContextCanHaveDifferentSettingsForSameApi() throws Throwable {
		astrixConfigurer.set(AstrixBeanSettings.TIMEOUT, AstrixBeanKey.create(Ping.class), 100);
		
		TestAstrixConfigurer astrixConfigurer2 = new TestAstrixConfigurer();
		astrixConfigurer2.set(AstrixBeanSettings.TIMEOUT, AstrixBeanKey.create(Ping.class), 200);
		astrixConfigurer2.enableFaultTolerance(true);
		astrixConfigurer2.registerApiProvider(PingApi.class);
		AstrixContext astrixContext2 = autoClosables.add(astrixConfigurer2.configure());
		
		astrixContext.getBean(Ping.class).ping("foo");
		
		HystrixFaultTolerance hystrixFaultTolerance = getFaultTolerance(astrixContext);
		HystrixFaultTolerance hystrixFaultTolerance2 = getFaultTolerance(astrixContext2);
		
		HystrixCommandProperties pingCommandPropertiesContext1 = getHystrixCommandProperties(hystrixFaultTolerance, Ping.class);
		HystrixCommandProperties pingCommandPropertiesContext2 = getHystrixCommandProperties(hystrixFaultTolerance2, Ping.class);
		
		assertEquals(100, pingCommandPropertiesContext1.executionTimeoutInMilliseconds().get().intValue());
		assertEquals(200, pingCommandPropertiesContext2.executionTimeoutInMilliseconds().get().intValue());
		
	}
	
	@Test
	public void readsDefaultBeanSettingsFromBeanConfiguration() throws Throwable {
		astrixConfigurer.set(AstrixBeanSettings.CORE_SIZE, AstrixBeanKey.create(Ping.class), 4);
		astrixConfigurer.set(AstrixBeanSettings.QUEUE_SIZE_REJECTION_THRESHOLD, AstrixBeanKey.create(Ping.class), 6);
		astrixConfigurer.set(AstrixBeanSettings.TIMEOUT, AstrixBeanKey.create(Ping.class), 100);
		astrixConfigurer.set(AstrixBeanSettings.MAX_CONCURRENT_REQUESTS, AstrixBeanKey.create(Ping.class), 21);
		astrixContext.getBean(Ping.class).ping("foo");
		
		HystrixFaultTolerance hystrixFaultTolerance = getFaultTolerance(astrixContext);
		HystrixCommandProperties pingCommandProperties = getHystrixCommandProperties(hystrixFaultTolerance, Ping.class);
		HystrixThreadPoolProperties pingThreadPoolProperties = getThreadPoolProperties(hystrixFaultTolerance, Ping.class);
		
		assertEquals(100, pingCommandProperties.executionTimeoutInMilliseconds().get().intValue());
		assertEquals(21, pingCommandProperties.executionIsolationSemaphoreMaxConcurrentRequests().get().intValue());
		assertEquals(4, pingThreadPoolProperties.coreSize().get().intValue());
		assertEquals(6, pingThreadPoolProperties.queueSizeRejectionThreshold().get().intValue());
	}

	@Test
	public void defaultBeanSettingsFromBeanConfiguration() throws Throwable {
		astrixContext.getBean(Ping.class).ping("foo");
		
		HystrixFaultTolerance hystrixFaultTolerance = getFaultTolerance(astrixContext);
		HystrixCommandProperties pingCommandProperties = getHystrixCommandProperties(hystrixFaultTolerance, Ping.class);
		HystrixThreadPoolProperties pingThreadPoolProperties = getThreadPoolProperties(hystrixFaultTolerance, Ping.class);
		
		assertEquals(DefaultBeanSettings.DEFAULT_CORE_SIZE, pingThreadPoolProperties.coreSize().get().intValue());
		assertEquals(DefaultBeanSettings.DEFAULT_QUEUE_SIZE_REJECTION_THRESHOLD, pingThreadPoolProperties.queueSizeRejectionThreshold().get().intValue());
		assertEquals(DefaultBeanSettings.DEFAULT_TIMEOUT, pingCommandProperties.executionTimeoutInMilliseconds().get().intValue());
		assertEquals(DefaultBeanSettings.DEFAULT_MAX_CONCURRENT_REQUESTS, pingCommandProperties.executionIsolationSemaphoreMaxConcurrentRequests().get().intValue());
	}
	
	private static HystrixCommandProperties getHystrixCommandProperties(HystrixFaultTolerance hystrixFaultTolerance, Class<?> api) {
		HystrixPropertiesStrategy hystrixPropertiesStrategy = HystrixPlugins.getInstance().getPropertiesStrategy();
		HystrixCommandKey commandKey = hystrixFaultTolerance.getCommandKey(AstrixBeanKey.create(api));
		return hystrixPropertiesStrategy.getCommandProperties(commandKey, 
															  HystrixCommandProperties.Setter());
	}

	private static HystrixFaultTolerance getFaultTolerance(AstrixContext astrixContext) {
		FaultToleranceSpi ftStrategy = AstrixApplicationContext.class.cast(astrixContext).getInstance(FaultToleranceSpi.class);
		assertEquals(HystrixFaultTolerance.class, ftStrategy.getClass());
		return (HystrixFaultTolerance) ftStrategy;
	}
	
	private static HystrixThreadPoolProperties getThreadPoolProperties(HystrixFaultTolerance hystrixFaultTolerance,
			Class<?> api) {
		HystrixPropertiesStrategy hystrixPropertiesStrategy = HystrixPlugins.getInstance().getPropertiesStrategy();
		 HystrixCommandGroupKey groupKey = hystrixFaultTolerance.getGroupKey(AstrixBeanKey.create(api));
		return hystrixPropertiesStrategy.getThreadPoolProperties(HystrixThreadPoolKey.Factory.asKey(groupKey.name()), 
				  HystrixThreadPoolProperties.Setter());
	}
	
	@AstrixApiProvider
	public static class PingApi {

		@AstrixFaultToleranceProxy
		@Library
		public Ping ping() {
			return new PingImpl();
		}
		
	}
	
	private static class PingImpl implements Ping {
		@Override
		public String ping(String msg) {
			return msg;
		}
	}
	
	public interface Ping {
		String ping(String msg);
	}

}
