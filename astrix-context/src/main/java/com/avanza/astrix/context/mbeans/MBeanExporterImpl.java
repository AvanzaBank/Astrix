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
package com.avanza.astrix.context.mbeans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.core.AstrixSettings;

final class MBeanExporterImpl implements MBeanExporter {

	private final Logger logger = LoggerFactory.getLogger(MBeanExporterImpl.class);

	private final AstrixConfig astrixConfig;
	private final MBeanServerFacade mbeanServer;

	public MBeanExporterImpl(AstrixConfig astrixConfig, MBeanServerFacade mbeanServer) {
		this.astrixConfig = astrixConfig;
		this.mbeanServer = mbeanServer;
	}

	@Override
	public void registerMBean(Object mbean, String folder, String name) {
		if (!exportMBeans()) {
			logger.debug("Exporting of Astrix MBeans is disabled, won't export mbean with name={}", name);
			return;
		}
		mbeanServer.registerMBean(mbean, folder, name);
	}

	@Override
	public void unregisterMBean(String folder, String name) {
		mbeanServer.unregisterMBean(folder, name);
	}

	private boolean exportMBeans() {
		return astrixConfig.get(AstrixSettings.EXPORT_ASTRIX_MBEANS).get();
	}

}
