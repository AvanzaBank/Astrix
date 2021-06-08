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
package lunch.grader.pu;

import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import lunch.api.LunchRestaurant;
import lunch.api.LunchService;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class LunchClient {
	
	public static void main(String[] args) throws InterruptedException {
		Logger.getLogger("com.avanza.astrix").setLevel(Level.DEBUG);
		AstrixConfigurer configurer = new AstrixConfigurer();
		AstrixContext astrixContext = configurer.configure();
		LunchService lunchService = astrixContext.waitForBean(LunchService.class, 5000);
		
		LunchRestaurant r = new LunchRestaurant();
		r.setFoodType("vegetarian");
		r.setName("Martins Green Room");
		lunchService.addLunchRestaurant(r);
		
	}

}
