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
package com.avanza.astrix.versioning.jackson2;

import com.avanza.astrix.versioning.core.AstrixObjectSerializer;
import com.avanza.astrix.versioning.core.AstrixObjectSerializerConfigurer;
import com.avanza.astrix.versioning.core.ObjectSerializerDefinition;
import com.avanza.astrix.versioning.jackson2.VersionedJsonObjectMapper.VersionedObjectMapperBuilder;

import java.lang.reflect.Type;

class Jackson2AstrixObjectSerializer implements AstrixObjectSerializer {

	private JsonObjectMapper objectMapper;
	private int version;

	public Jackson2AstrixObjectSerializer(ObjectSerializerDefinition serializerDefinition) {
		Class<? extends AstrixObjectSerializerConfigurer> serializerBuilder = serializerDefinition.getObjectSerializerConfigurerClass();
		this.version = serializerDefinition.version();
		try {
			this.objectMapper = buildObjectMapper(Jackson2ObjectSerializerConfigurer.class.cast(serializerBuilder.newInstance()));
		} catch (Exception e) {
			throw new RuntimeException("Failed to init JsonObjectMapper", e);
		}
	}
	
	private JsonObjectMapper buildObjectMapper(Jackson2ObjectSerializerConfigurer serializerBuilder) {
		VersionedObjectMapperBuilder objectMapperBuilder = new VersionedObjectMapperBuilder(serializerBuilder.apiMigrations());
		serializerBuilder.configure(objectMapperBuilder);
		return JsonObjectMapper.create(objectMapperBuilder.build());
	}

	@Override
	public <T> T deserialize(Object element, Type type, int fromVersion) {
		if (fromVersion == NoVersioningSupport.NO_VERSIONING) {
			return (T) element;
		}
		return objectMapper.deserialize((String) element, type, fromVersion);
	}

	@Override
	public Object serialize(Object element, int version) {
		if (version == NoVersioningSupport.NO_VERSIONING) {
			return element;
		}
		return objectMapper.serialize(element, version);
	}

	@Override
	public int version() {
		return version;
	}

}
