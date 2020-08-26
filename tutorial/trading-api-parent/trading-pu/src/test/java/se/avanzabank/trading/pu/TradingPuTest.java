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
package se.avanzabank.trading.pu;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import se.avanzabank.trading.api.Account;
import se.avanzabank.trading.api.AccountId;
import se.avanzabank.trading.api.AccountService;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.test.util.AutoCloseableRule;
import com.avanza.gs.test.PuConfigurers;
import com.avanza.gs.test.RunningPu;

public class TradingPuTest {
	
	private static final InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
	
	@ClassRule
	public static final RunningPu tradingPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/pu.xml")
														   .startAsync(false)
														   .contextProperty("configSourceId", serviceRegistry.getConfigSourceId())
														   .configure();
	@Rule
	public AutoCloseableRule autoCloseableRule = new AutoCloseableRule();
	
	@Test
	public void accountServiceConsumtionExample() throws Exception {
		AstrixContext context = autoCloseableRule.add(new AstrixConfigurer()
															.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri())
															.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 50)
															.configure());	
		AccountService accountService = context.waitForBean(AccountService.class, 10_000L);
	
		assertNull(accountService.getAccount(AccountId.valueOf("21")));
		
		accountService.registerAccount(new Account(AccountId.valueOf("21"), 1000D));
		assertNotNull(accountService.getAccount(AccountId.valueOf("21")));
	}

}
