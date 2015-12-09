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
package com.avanza.astrix.beans.factory;

import com.avanza.astrix.beans.core.AstrixBeanKey;



/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class MissingBeanProviderException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private AstrixBeanKey<? extends Object> beanType;
	
	public MissingBeanProviderException(AstrixBeanKey<? extends Object> beanType) {
		super(String.format("No provider found\n bean: %s \n"
				  + "Most common cause is that you don't have an ApiProvider on the classpath in any of the scanned packages that exports the given bean", beanType));
		this.beanType = beanType;
	}
	
	public MissingBeanProviderException(AstrixBeanKey<? extends Object> beanType, String msg) {
		super(msg);
		this.beanType = beanType;
	}
	
	public AstrixBeanKey<? extends Object> getBeanType() {
		return beanType;
	}

}
