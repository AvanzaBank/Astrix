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
package com.avanza.astrix.beans.core;

import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.core.DefaultBeanSettings;

/**
 *
 * @author Elias Lindholm (elilin)
 *
 */
public final class AstrixBeanSettings {

	/**
	 * Determines whether fault tolerance should be applied for invocations on the associated
	 * Astrix bean.
	 */
	public static final BooleanBeanSetting FAULT_TOLERANCE_ENABLED = 
			new BooleanBeanSetting("faultTolerance.enabled", DefaultBeanSettings.DEFAULT_FAULT_TOLERANCE_ENABLED);
	
	/**
	 * Determines whether statistics should be collected for each invocation on the associated
	 * Astrix bean.
	 */
	public static final BooleanBeanSetting BEAN_METRICS_ENABLED = 
			new BooleanBeanSetting("beanMetrics.enabled", DefaultBeanSettings.DEFAULT_BEAN_METRICS_ENABLED);
	

	/**
	 * When fault tolerance is enabled this setting defines the timeout 
	 * for invocations on the associated bean. 
	 */
	public static final IntBeanSetting TIMEOUT = 
			new IntBeanSetting("faultTolerance.timeout", DefaultBeanSettings.DEFAULT_TIMEOUT);
	
	/**
	 * Defines the "maxConcurrentRequests" when semaphore isolation is used to protect invocations 
	 * to the associated bean, i.e. the maximum number of concurrent requests before the 
	 * fault-tolerance layer starts rejecting invocations (by throwing ServiceUnavailableException)
	 */
	public static final IntBeanSetting MAX_CONCURRENT_REQUESTS = 
			new IntBeanSetting("faultTolerance.maxConcurrentRequests", DefaultBeanSettings.DEFAULT_MAX_CONCURRENT_REQUESTS);
	
	/**
	 * Defines the "coreSize" when thread isolation is used to protect invocations 
	 * to the associated bean, i.e. the number of threads in the bulkhead associated with a
	 * synchronous service invocation.
	 */
	public static final IntBeanSetting CORE_SIZE = 
			new IntBeanSetting("faultTolerance.coreSize", DefaultBeanSettings.DEFAULT_CORE_SIZE);
	
	/**
	 * Defines the "queueSizeRejectionThreshold" when thread isolation 
	 * is used to protect invocations to the associated bean, i.e. number of pending service invocations
	 * allowed in the queue to a thread-pool (bulk-head) before starting to throw {@link ServiceUnavailableException}.
	 */
	public static final IntBeanSetting QUEUE_SIZE_REJECTION_THRESHOLD = 
			new IntBeanSetting("faultTolerance.queueSizeRejectionThreshold", DefaultBeanSettings.DEFAULT_QUEUE_SIZE_REJECTION_THRESHOLD);
	
	/**
	 * @deprecated Replaced by {@link #TIMEOUT}
	 */
	@Deprecated
	public static final IntBeanSetting INITIAL_TIMEOUT = TIMEOUT;
	
	/**
	 * @deprecated Replaced by {@link #MAX_CONCURRENT_REQUESTS}
	 */
	@Deprecated
	public static final IntBeanSetting INITIAL_MAX_CONCURRENT_REQUESTS = MAX_CONCURRENT_REQUESTS;
	
	/**
	 * @deprecated Replaced by {@link #CORE_SIZE}
	 */
	@Deprecated
	public static final IntBeanSetting INITIAL_CORE_SIZE = CORE_SIZE; 
	
	/**
	 * @deprecated Replaced by {@link #QUEUE_SIZE_REJECTION_THRESHOLD}
	 */
	@Deprecated
	public static final IntBeanSetting INITIAL_QUEUE_SIZE_REJECTION_THRESHOLD = QUEUE_SIZE_REJECTION_THRESHOLD;
	
	/**
	 * Its possible to set service-beans in unavailable state, in which it throws
	 * a {@link ServiceUnavailableException} on each invocation. <p>
	 */
	public static final BooleanBeanSetting AVAILABLE = 
			new BooleanBeanSetting("available", true);

	
	private AstrixBeanSettings() {
	}

	public static abstract class BeanSetting<T> {
		private String name;
		private T defaultValue;

		private BeanSetting(String name, T defaultValue) {
			this.name = name;
			this.defaultValue = defaultValue;
		}

		public String nameFor(AstrixBeanKey<?> beanKey) {
			return resolveSettingName(beanKey);
		}

		private String resolveSettingName(AstrixBeanKey<?> beanKey) {
			return "astrix.bean." + beanKey.toString() + "." + name;
		}
		
		public T defaultValue() {
			return defaultValue;
		}
	}

	public static class BooleanBeanSetting extends BeanSetting<Boolean> {
		public BooleanBeanSetting(String name, boolean defaultValue) {
			super(name, defaultValue);
		}
	}

	public static class LongBeanSetting extends BeanSetting<Long> {
		public LongBeanSetting(String name, long defaultValue) {
			super(name, defaultValue);
		}
	}

	public static class IntBeanSetting extends BeanSetting<Integer> {
		public IntBeanSetting(String name, int defaultValue) {
			super(name, defaultValue);
		}
	}

	public static class StringBeanSetting extends BeanSetting<String> {
		public StringBeanSetting(String name, String defaultValue) {
			super(name, defaultValue);
		}
	}

}