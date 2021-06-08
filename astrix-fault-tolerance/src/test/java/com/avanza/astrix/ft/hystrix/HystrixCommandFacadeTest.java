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
package com.avanza.astrix.ft.hystrix;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.ft.BeanFaultToleranceFactorySpi;
import com.avanza.astrix.beans.service.DirectComponent;
import com.avanza.astrix.context.AstrixApplicationContext;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixConfigDiscovery;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.test.util.AssertBlockPoller;
import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;


@Disabled
class HystrixCommandFacadeTest {
	
	// TODO: Rename to sync-service invocation test

	private AstrixContext context;
	private Ping ping;
	private PingImpl pingServer = new PingImpl();
	
	@BeforeEach
	void before() {
		Hystrix.reset();
		context = new TestAstrixConfigurer().enableFaultTolerance(true)
											.registerApiProvider(PingApi.class)
											.set(AstrixBeanSettings.TIMEOUT, AstrixBeanKey.create(Ping.class), 250)
											.set(AstrixBeanSettings.CORE_SIZE, AstrixBeanKey.create(Ping.class), 1)
											.set(AstrixThreadPoolProperties.MAX_QUEUE_SIZE, AstrixBeanKey.create(Ping.class), -1) // NO QUEUE
											.set("pingUri", DirectComponent.registerAndGetUri(Ping.class, pingServer))
											.configure();
		ping = context.getBean(Ping.class);
		initMetrics(ping);
	}
	
	@AfterEach
	void after() {
		context.destroy();
	}
	
	@Test
	void underlyingObservableIsWrappedWithFaultTolerance() throws Throwable {
		String result = ping.ping("foo");

		assertEquals("foo", result);
		eventually(() -> {
			assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS));
		});
	}
	
	@Test
	void serviceUnavailableThrownByUnderlyingObservableShouldCountAsFailure() throws Exception {
		initMetrics(ping);
		
		pingServer.setFault(() -> {
			throw new ServiceUnavailableException("");
		});

		assertThrows(ServiceUnavailableException.class, () -> ping.ping("foo"),"Expected service unavailable");

		eventually(() -> {
			assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS)); // 1 from init
			assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.FAILURE));
		});
	}
	
	@Test
	void normalExceptionsThrownIsTreatedAsPartOfNormalApiFlowAndDoesNotCountAsFailure() throws Throwable {
		initMetrics(ping);
		pingServer.setFault(() -> {
			throw new MyDomainException();
		});

		assertThrows(MyDomainException.class, () -> ping.ping("foo"), "Expected service unavailable");

		eventually(() -> {
			// Note that from the perspective of a circuit-breaker an exception thrown
			// by the underlying service call (typically a service call) should not
			// count as failure and therefore not (possibly) trip circuit breaker.
			assertEquals(2, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS)); //+1 from init
			assertEquals(0, getEventCountForCommand(HystrixRollingNumberEvent.FAILURE));
		});
	}
	
	@Test
	void throwsServiceUnavailableOnTimeouts() throws Throwable {
		initMetrics(ping);
		pingServer.setFault(() -> {
			sleep(10_000); //simulate timeout by sleeping
		});

		assertThrows(ServiceUnavailableException.class, () -> ping.ping("foo"), "A ServiceUnavailableException should be thrown on timeout");

		eventually(() -> {
			assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS)); // 1 from init
			assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.TIMEOUT));
			assertEquals(0, getEventCountForCommand(HystrixRollingNumberEvent.SEMAPHORE_REJECTED));
		});
	}
	
	@Test
	void threadPoolRejectedCountsAsFailure() throws Exception {
		initMetrics(ping);
		pingServer.setFault(() -> {
			sleep(10_000); //simulate timeout by sleeping
		});
		// One of these invocations should be rejected
		CountDownLatch done = new CountDownLatch(1);
		new Thread(() -> {
			try {
				ping.ping("foo");
			} catch (Exception ignore) {
			}
			done.countDown();
		}).start();
		new Thread(() -> {
			try {
				ping.ping("foo");
			} catch (Exception ignore) {
			}
			done.countDown();
		}).start();

		done.await(5, SECONDS);
		eventually(() -> {
			assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS)); // 1 from init
			assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.THREAD_POOL_REJECTED));
		});
	}
	
	@Test
	void doesNotInvokeServiceWhenBulkHeadIsFull() throws Exception {
		AtomicInteger invocationCount = new AtomicInteger();
		CountDownLatch done = new CountDownLatch(1);
		CountDownLatch serverInvocationCompleted = new CountDownLatch(1);
		pingServer.setFault(() -> {
			invocationCount.incrementAndGet();
			try {
				done.await(10, SECONDS); //simulate timeout by waiting on latch
			} catch (Exception ignore) {
			}
			serverInvocationCompleted.countDown();
		});
		Thread t1 = new Thread(() -> {
			try {
				ping.ping("foo");
			} catch (ServiceUnavailableException ignore) {
			}
			done.countDown();
		});
		t1.start();
		Thread t2 = new Thread(() -> {
			try {
				ping.ping("foo");
			} catch (ServiceUnavailableException ignore) {
			}
			done.countDown();
		});
		t2.start();
		
		if (!done.await(3, SECONDS)) {
			fail("Expected one ping invocation to be aborted by fault tolerance layer");
		}
		if (!serverInvocationCompleted.await(3, SECONDS)) {
			fail("Expected server invocation to complete when second service call is aborted");
		}
		assertEquals(1, invocationCount.get());
	}
	
	private void eventually(Runnable assertion) throws InterruptedException {
		new AssertBlockPoller(3000, 25).check(assertion);
	}
	
	private void initMetrics(Ping ping) {
		// Black hystrix magic here :(
		try {
			ping.ping("foo");
		} catch (Exception ignore) {
		}
		HystrixFaultToleranceFactory faultTolerance = (HystrixFaultToleranceFactory) ((AstrixApplicationContext) this.context).getInstance(BeanFaultToleranceFactorySpi.class);
		HystrixCommandKey key = faultTolerance.getCommandKey(AstrixBeanKey.create(Ping.class));
		
		HystrixCommandMetrics.getInstance(key).getCumulativeCount(HystrixEventType.SUCCESS);
	}
	
	@SuppressWarnings("SameParameterValue")
	private static void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException ignore) {
		}
	}
	
	private int getEventCountForCommand(HystrixRollingNumberEvent hystrixRollingNumberEvent) {
		HystrixFaultToleranceFactory faultTolerance = (HystrixFaultToleranceFactory) ((AstrixApplicationContext) this.context).getInstance(BeanFaultToleranceFactorySpi.class);
		HystrixCommandKey commandKey = faultTolerance.getCommandKey(AstrixBeanKey.create(Ping.class));
		HystrixCommandMetrics metrics = HystrixCommandMetrics.getInstance(commandKey);
		int currentConcurrentExecutionCount = (int) metrics.getCumulativeCount(hystrixRollingNumberEvent);
		return currentConcurrentExecutionCount;
	}
	
	private static class MyDomainException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
	
	public interface Ping {
		String ping(String msg);
	}
	
	public static class PingImpl implements Ping {
		private Runnable simulatedFault = () -> {};
		
		public String ping(String msg) {
			simulatedFault.run();
			return msg;
		}
		public void setFault(Runnable fault) {
			this.simulatedFault = fault;
		}
	}
	
	@AstrixApiProvider
	public static class PingApi {
		@AstrixConfigDiscovery("pingUri")
		@Service
		public Ping ping() {
			return new PingImpl();
		}
		
	}
	
}
