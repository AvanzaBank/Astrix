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
package com.avanza.astrix.beans.service;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.config.BeanConfiguration;
import com.avanza.astrix.context.mbeans.MBeanExporter;

public class AstrixServiceBeanInstanceMbeanExporter {

	private MBeanExporter mbeanExporter;
	private AstrixConfig astrixConfig;
	
	public AstrixServiceBeanInstanceMbeanExporter(MBeanExporter mbeanExporter, AstrixConfig astrixConfig) {
		this.mbeanExporter = mbeanExporter;
		this.astrixConfig = astrixConfig;
	}

	public void register(ServiceBeanInstance<?> instance) {
		BeanConfiguration beanConfiguration = astrixConfig.getBeanConfiguration(instance.getBeanKey());
		mbeanExporter.registerMBean(new AstrixServiceBeanInstance(beanConfiguration, astrixConfig, instance),
				"ServiceBeanInstances",
				instance.getBeanKey().toString());
	}
	

}
