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
package com.avanza.astrix.context;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.avanza.astrix.beans.publish.ApiProviderClass;



public class AstrixApiProviderClassScannerTest {
	
	@Test
	public void scansDefinedPackagesForDefinedAnnotations() throws Exception {
		List<ApiProviderClass> apiDescriptors = new AstrixApiProviderClassScanner(asList(DummyDescriptor.class), "com.avanza.astrix.context").getAll().collect(toList());
		assertEquals(2, apiDescriptors.size());
		Assert.assertThat(apiDescriptors, hasItem(equalTo(ApiProviderClass.create(DescriptorA.class))));
		Assert.assertThat(apiDescriptors, hasItem(equalTo(ApiProviderClass.create(DescriptorB.class))));
	}
	
	@Test
	public void doesNotFinedDescriptorsOutsideDefinedPackge() throws Exception {
		List<ApiProviderClass> apiDescriptors = new AstrixApiProviderClassScanner(asList(DummyDescriptor.class), "com.avanza.astrix.context.foo.bar").getAll().collect(toList());
		assertEquals(0, apiDescriptors.size());
	}

	private List<Class<? extends Annotation>> asList(Class<? extends Annotation> classes) {
		return Arrays.<Class<? extends Annotation>>asList(classes);
	}
	
	public @interface DummyDescriptor {
	}
	
	
	@DummyDescriptor
	public static class DescriptorA {
	}
	
	@DummyDescriptor
	public static class DescriptorB {
	}

}
