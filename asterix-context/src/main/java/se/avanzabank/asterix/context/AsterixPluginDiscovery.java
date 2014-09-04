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
package se.avanzabank.asterix.context;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AsterixPluginDiscovery {
	
	// TODO: cache discovered plugins by type?
	
	private static final Logger log = LoggerFactory.getLogger(AsterixPluginDiscovery.class);
	
	static <T> void discoverAllPlugins(AsterixContext context, Class<T> type, T defaultProvider) {
		List<T> plugins = discoverPlugins(type);
		if (plugins.isEmpty()) {
			log.debug("No plugin discovered for {}, using default {}", type.getName(), defaultProvider.getClass().getName());
			plugins.add(defaultProvider);
		}
		for (T plugin : plugins) {
			log.debug("Found plugin for {}, provider={}", type.getName(), plugin.getClass().getName());
			context.registerPlugin(type, plugin);
		}
	}

	static <T> void discoverOnePlugin(AsterixContext context, Class<T> type) {
		List<T> plugins = discoverPlugins(type);
		if (plugins.isEmpty()) {
			throw new IllegalStateException("No provider found on classpath for: " + type + ". This typically means that you forgot to put the providing jar on the classpath.");
		}
		if (plugins.size() > 1) {
			throw new IllegalStateException("Multiple providers found for plugin: " + type + ". Plugins: " + plugins);
		}
		T provider = plugins.get(0);
		log.debug("Found plugin for {}, using {}", type.getName(), provider.getClass().getName());
		context.registerPlugin(type, provider);
	}
	
	static <T> List<T> discoverPlugins(Class<T> type) {
		Iterator<T> plugins = ServiceLoader.load(type).iterator();		
		List<T> result = new ArrayList<>();
		while (plugins.hasNext()) {
			result.add(plugins.next());
		}
		return result; 
	}
	

}