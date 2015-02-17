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
package com.avanza.astrix.dashboard.web;

import java.util.Arrays;
import java.util.HashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.service.registry.client.AstrixServiceRegistryAdministrator;
import com.avanza.astrix.spring.AstrixFrameworkBean;

@Configuration
public class AstrixDashboardConfig {
	
	@Bean
	public AstrixFrameworkBean astrix() {
		AstrixFrameworkBean result = new AstrixFrameworkBean();
		result.setConsumedAstrixBeans(Arrays.<Class<?>>asList(
			AstrixServiceRegistryAdministrator.class
		));
		result.setSettings(new HashMap<String, String>() {{
			put(AstrixSettings.ASTRIX_SERVICE_REGISTRY_URI, AstrixServiceComponentNames.GS_REMOTING + 
//					":jini://*/*/service-registry-space?locators=testgssystem01.test.aza.se"); 
					":jini://*/*/service-registry-space?groups=astrix-demo-apps");
		}});
		return result;
	}
	
}
