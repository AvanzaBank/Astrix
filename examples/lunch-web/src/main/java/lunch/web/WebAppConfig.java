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
package lunch.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.avanza.astrix.spring.AstrixFrameworkBean;

import lunch.api.LunchService;
import lunch.api.LunchUtil;
import lunch.grader.api.LunchRestaurantGrader;


@Configuration
public class WebAppConfig {
	
	@Bean
	public AstrixFrameworkBean astrixFrameworkBean() {
		AstrixFrameworkBean result = new AstrixFrameworkBean();
		result.setSubsystem("lunch-web");
		result.setConsumedAstrixBeans(LunchService.class, LunchRestaurantGrader.class, LunchUtil.class);
		return result;
	}

}
