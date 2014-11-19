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

import java.util.Collection;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Server side component used to export all services provided by a given server as defined by
 * an {@link AstrixServiceDescriptor}. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceExporter implements ApplicationContextAware {
	
	private ApplicationContext applicationContext;
	private AstrixServiceExporterBeans AstrixServiceExporterBeans;
	private Collection<AstrixExportedServiceInfo> exportedServices;

	@Autowired
	public AstrixServiceExporter(AstrixServiceExporterBeans AstrixServiceExporters) {
		this.AstrixServiceExporterBeans = AstrixServiceExporters;
	}
	
	public Collection<AstrixExportedServiceInfo> getExportedServices() {
		return exportedServices;
	}
	
	public void setExportedServices(Collection<AstrixExportedServiceInfo> exportedServices) {
		this.exportedServices = exportedServices;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}
	
	@PostConstruct
	public void register() {
		for (AstrixExportedServiceInfo exportedService : exportedServices) {
			Object provider = applicationContext.getBean(exportedService.getProvidingBeanName());
			AstrixServiceExporterBean serviceExporterBean = AstrixServiceExporterBeans.getServiceExporter(exportedService.getComponentName());
			serviceExporterBean.register(provider, exportedService.getApiDescriptor(), exportedService.getProvidedService());
		}
	}

}