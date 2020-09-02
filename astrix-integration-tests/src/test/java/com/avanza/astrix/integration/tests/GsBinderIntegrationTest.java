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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.openspaces.core.GigaSpace;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.gs.GsBinder;
import com.j_spaces.core.IJSpace;

public class GsBinderIntegrationTest {


	private MapConfigSource config = new MapConfigSource();
	private AnnotationConfigApplicationContext applicationContext;
	
	@After
	public void closeContext() {
		applicationContext.destroy(); 
	}

	@Test
	public void usesQualifierToIdentifyWhatEmbeddedSpaceToUse() throws Exception {
		applicationContext = new AnnotationConfigApplicationContext(AppWithTwoEmbeddedSpaces.class);
		GsBinder gsBinder = new GsBinder();
		gsBinder.setConfig(DynamicConfig.create(config));
		config.set(AstrixSettings.GIGA_SPACE_BEAN_NAME, "gigaSpace");
		GigaSpace gigaSpace = gsBinder.getEmbeddedSpace(applicationContext);
		
		GigaSpace expected = applicationContext.getBean("gigaSpace", GigaSpace.class);
		assertSame("Expected embedded space to be defined by GIGA_SPACE_BEAN_NAME prpoerty", expected, gigaSpace);
	}
	
	@Test
	public void usesEmbeddedSpaceOverClusterdProxies() throws Exception {
		applicationContext = new AnnotationConfigApplicationContext(AppWithClusterdProxyAndEmbeddedSpace.class);
		GsBinder gsBinder = new GsBinder();
		gsBinder.setConfig(DynamicConfig.create(config));
		GigaSpace gigaSpace = gsBinder.getEmbeddedSpace(applicationContext);
		
		GigaSpace expected = applicationContext.getBean("gigaSpace", GigaSpace.class);
		assertTrue(expected.getSpace().isEmbedded());
		assertSame("Expected embedded space to be returned", expected, gigaSpace);
	}
	
	@Configuration
	static class AppWithTwoEmbeddedSpaces {
		@Bean
		public GigaSpace gigaSpace() {
			IJSpace space = Mockito.mock(IJSpace.class);
			GigaSpace gs = Mockito.mock(GigaSpace.class);
			Mockito.when(gs.getSpace()).thenReturn(space);
			Mockito.when(space.isEmbedded()).thenReturn(true);
			return gs;
		}
		@Bean
		public GigaSpace otherEmbeddedGigaSpace() {
			IJSpace space = Mockito.mock(IJSpace.class);
			GigaSpace gs = Mockito.mock(GigaSpace.class);
			Mockito.when(gs.getSpace()).thenReturn(space);
			Mockito.when(space.isEmbedded()).thenReturn(true);
			return gs;
		}
	}
	
	@Configuration
	static class AppWithClusterdProxyAndEmbeddedSpace {
		@Bean
		public GigaSpace gigaSpace() {
			IJSpace space = Mockito.mock(IJSpace.class);
			GigaSpace gs = Mockito.mock(GigaSpace.class);
			Mockito.when(gs.getSpace()).thenReturn(space);
			Mockito.when(space.isEmbedded()).thenReturn(true);
			return gs;
		}
		@Bean
		public GigaSpace clusteredGigaSpace() {
			IJSpace space = Mockito.mock(IJSpace.class);
			GigaSpace gs = Mockito.mock(GigaSpace.class);
			Mockito.when(gs.getSpace()).thenReturn(space);
			Mockito.when(space.isEmbedded()).thenReturn(false);
			return gs;
		}
	}
}
