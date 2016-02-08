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

import com.avanza.astrix.versioning.core.AstrixObjectSerializerConfigurer;

import java.util.List;

/**
 * @author Elias Lindholm
 * 
 */
public interface Jackson2ObjectSerializerConfigurer extends AstrixObjectSerializerConfigurer {
	List<? extends AstrixJsonApiMigration> apiMigrations();
	void configure(JacksonObjectMapperBuilder objectMapperBuilder);
}
