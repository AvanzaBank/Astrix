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
package com.avanza.astrix.beans.registry;

import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.context.AstrixApplicationContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.versioning.core.AstrixObjectSerializer;
import com.avanza.astrix.versioning.core.ObjectSerializerDefinition;
import com.avanza.astrix.versioning.core.ObjectSerializerFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class ServiceRegistryV1ApiMigrationTest {
	
	@Test
	void generatesApplicationInstanceIdFromQualifierAndApiTypeFor_AstrixServiceRegistryEntry_onVersion1() {
		AstrixApplicationContext context = (AstrixApplicationContext) new TestAstrixConfigurer().configure();
		ObjectSerializerFactory versioningPlugin = context.getInstance(ObjectSerializerFactory.class);
		AstrixObjectSerializer objectSerializer = versioningPlugin.create(ObjectSerializerDefinition.versionedService(2, ServiceRegistryObjectSerializerConfigurer.class));

		
		AstrixServiceRegistryEntry v1Entry = new AstrixServiceRegistryEntry();
		v1Entry.getServiceProperties().put(ServiceProperties.QUALIFIER, "my-qualifier");
		v1Entry.getServiceProperties().put(ServiceProperties.API, "foo.MyService");
		
		Object serializedV1 = objectSerializer.serialize(v1Entry, 1); //
		
		AstrixServiceRegistryEntry deserializedToV2 = objectSerializer.deserialize(serializedV1, AstrixServiceRegistryEntry.class, 1);
		assertEquals("foo.MyService_my-qualifier", deserializedToV2.getServiceProperties().get(ServiceProperties.APPLICATION_INSTANCE_ID));
		
		AstrixServiceRegistryEntry v2Entry = new AstrixServiceRegistryEntry();
		v2Entry.getServiceProperties().put(ServiceProperties.QUALIFIER, "my-qualifier");
		v2Entry.getServiceProperties().put(ServiceProperties.API, "foo.MyService");
		v2Entry.getServiceProperties().put(ServiceProperties.APPLICATION_INSTANCE_ID, "my-instance-id");

		Object serializedV2 = objectSerializer.serialize(v2Entry, 2); //
		
		deserializedToV2 = objectSerializer.deserialize(serializedV2, AstrixServiceRegistryEntry.class, 2);
		assertEquals("my-instance-id", deserializedToV2.getServiceProperties().get(ServiceProperties.APPLICATION_INSTANCE_ID));
	}
	
}
