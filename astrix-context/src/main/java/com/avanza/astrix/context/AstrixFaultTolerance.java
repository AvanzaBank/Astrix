/*
 * Copyright 2014-2015 Avanza Bank AB
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.inject.AstrixPlugins;
import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.core.util.ProxyUtil;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class AstrixFaultTolerance implements AstrixPluginsAware, AstrixConfigAware {
	
	private AstrixFaultTolerancePlugin faultTolerancePlugin;
	private DynamicConfig config;
	
	public <T> T addFaultTolerance(FaultToleranceSpecification<T> spec) {
		DynamicBooleanProperty faultToleranceEnabledForCircuit = config.getBooleanProperty("astrix.faultTolerance." + spec.getApi().getName() + ".enabled", true);
		DynamicBooleanProperty faultToleranceEnabled = config.getBooleanProperty(AstrixSettings.ENABLE_FAULT_TOLERANCE, true);
		T withFaultTolerance = faultTolerancePlugin.addFaultTolerance(spec);
		return ProxyUtil.newProxy(spec.getApi(), new FaultToleranceToggle<>(withFaultTolerance, spec.getProvider(), faultToleranceEnabled, faultToleranceEnabledForCircuit));
	}

	@Override
	public void setPlugins(AstrixPlugins plugins) {
		this.faultTolerancePlugin = plugins.getPlugin(AstrixFaultTolerancePlugin.class);
	}
	
	private class FaultToleranceToggle<T> implements InvocationHandler {
		private final T faultToleranceProtectedProvider;
		private final T rawProvider;
		private final DynamicBooleanProperty useFaultTolerance;
		private final DynamicBooleanProperty faultToleranceEnabledForCircuit;
		
		public FaultToleranceToggle(T faultToleranceProtectedProvider, T rawProvider, DynamicBooleanProperty useFaultTolerance, DynamicBooleanProperty faultToleranceEnabledForCircuit) {
			this.faultToleranceProtectedProvider = faultToleranceProtectedProvider;
			this.rawProvider = rawProvider;
			this.useFaultTolerance = useFaultTolerance;
			this.faultToleranceEnabledForCircuit = faultToleranceEnabledForCircuit;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				T provider = getProvider();
				return method.invoke(provider, args);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		}

		private T getProvider() {
			if (useFaultTolerance.get() && faultToleranceEnabledForCircuit.get()) {
				return faultToleranceProtectedProvider;
			}
			return rawProvider;
		}
	}

	@Override
	public void setConfig(DynamicConfig config) {
		this.config = config;
	}
	
}
