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
package com.avanza.asterix.context;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
/**
 * Bridges between spring and asterix to make asterix-beans available in
 * spring-application context.   
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public class AsterixSpringFactoryBean<T> implements FactoryBean<T> {
	
	private Class<T> type;
	private AsterixContext asterix;

	@Autowired
	public void setAsterixContext(AsterixContext asterix) {
		this.asterix = asterix;
	}
	
	public AsterixContext getAsterixContext() {
		return asterix;
	}
	
	public void setType(Class<T> type) {
		this.type = type;
	}
	
	public Class<T> getType() {
		return type;
	}

	@Override
	public T getObject() throws Exception {
		return asterix.getBean(type);
	}

	@Override
	public Class<?> getObjectType() {
		return type;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}