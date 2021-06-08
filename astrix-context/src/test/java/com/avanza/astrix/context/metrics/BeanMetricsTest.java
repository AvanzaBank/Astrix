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
package com.avanza.astrix.context.metrics;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.core.BasicFuture;
import com.avanza.astrix.beans.service.DirectComponent;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.function.CheckedCommand;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixConfigDiscovery;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.test.util.AstrixTestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import rx.Observable;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BeanMetricsTest {
	
	private AstrixContext astrixContext;

	@AfterEach
	void after() {
		AstrixTestUtil.closeQuiet(astrixContext);
	}
	
	@Test
	void timesReactiveInvocationsUsingTimeObservable() {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		final AtomicLong fakeClock = new AtomicLong(0);
		FakeTimer fakeTimer = new FakeTimer(fakeClock);
		astrixConfigurer.registerStrategy(MetricsSpi.class, fakeTimer);
		astrixConfigurer.registerApiProvider(PingApi.class);
		astrixConfigurer.set("ping", DirectComponent.registerAndGetUri(Ping.class, new TwoClockTickPing(fakeClock)));
		this.astrixContext = astrixConfigurer.configure();
		
		Ping ping = this.astrixContext.getBean(Ping.class);

		assertEquals(-1L, fakeTimer.getLastTimedObservableExecutionTime());
		assertEquals("foo", ping.observePing("foo").toBlocking().first());
		assertEquals(2, fakeTimer.getLastTimedObservableExecutionTime());
	}
	
	@Test
	void timesAsyncInvocationsWithFutureReturnTypeAsSynchronousInvocation() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		final AtomicLong fakeClock = new AtomicLong(0);
		FakeTimer fakeTimer = new FakeTimer(fakeClock);
		astrixConfigurer.registerStrategy(MetricsSpi.class, fakeTimer);
		astrixConfigurer.registerApiProvider(PingApi.class);
		astrixConfigurer.set("ping", DirectComponent.registerAndGetUri(Ping.class, new TwoClockTickPing(fakeClock)));
		this.astrixContext = astrixConfigurer.configure();
		
		Ping ping = this.astrixContext.getBean(Ping.class);

		assertEquals(-1L, fakeTimer.getLastTimedExecutionTime());
		assertEquals("foo", ping.pingAsync("foo").get());
		assertEquals(2, fakeTimer.getLastTimedExecutionTime());
	}
	
	@Test
	void itsPossibleToDisableBeanMetrics() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		final AtomicLong fakeClock = new AtomicLong(0);
		FakeTimer fakeTimer = new FakeTimer(fakeClock);
		astrixConfigurer.registerStrategy(MetricsSpi.class, fakeTimer);
		astrixConfigurer.registerApiProvider(PingApi.class);
		astrixConfigurer.set(AstrixBeanSettings.BEAN_METRICS_ENABLED.nameFor(AstrixBeanKey.create(Ping.class)), false);
		astrixConfigurer.set("ping", DirectComponent.registerAndGetUri(Ping.class, new TwoClockTickPing(fakeClock)));
		this.astrixContext = astrixConfigurer.configure();
		
		Ping ping = this.astrixContext.getBean(Ping.class);

		assertEquals(-1L, fakeTimer.getLastTimedObservableExecutionTime());
		assertEquals("foo", ping.pingAsync("foo").get());
		assertEquals(-1L, fakeTimer.getLastTimedObservableExecutionTime());
		
		assertEquals("foo", ping.ping("foo"));
		assertEquals(-1L, fakeTimer.getLastTimedObservableExecutionTime());
	}
	
	@Test
	void itsPossibleToDisableBeanMetricsGlobally() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		final AtomicLong fakeClock = new AtomicLong(0);
		FakeTimer fakeTimer = new FakeTimer(fakeClock);
		astrixConfigurer.registerStrategy(MetricsSpi.class, fakeTimer);
		astrixConfigurer.registerApiProvider(PingApi.class);
		astrixConfigurer.set(AstrixSettings.ENABLE_BEAN_METRICS, false);
		astrixConfigurer.set("ping", DirectComponent.registerAndGetUri(Ping.class, new TwoClockTickPing(fakeClock)));
		this.astrixContext = astrixConfigurer.configure();
		
		Ping ping = this.astrixContext.getBean(Ping.class);

		assertEquals(-1L, fakeTimer.getLastTimedObservableExecutionTime());
		assertEquals("foo", ping.pingAsync("foo").get());
		assertEquals(-1L, fakeTimer.getLastTimedObservableExecutionTime());
		
		assertEquals("foo", ping.ping("foo"));
		assertEquals(-1L, fakeTimer.getLastTimedObservableExecutionTime());
	}
	
	
	private static final class TwoClockTickPing implements Ping {
		private final AtomicLong fakeClock;

		private TwoClockTickPing(AtomicLong fakeClock) {
			this.fakeClock = fakeClock;
		}

		@Override
		public Observable<String> observePing(final String msg) {
			return Observable.unsafeCreate(t -> {
				fakeClock.incrementAndGet(); // Simulate 2 ms execution time by ticking clock
				fakeClock.incrementAndGet();
				t.onNext(msg);
				t.onCompleted();
			});
		}

		@Override
		public Future<String> pingAsync(String msg) {
			return new BasicFuture<>(ping(msg));
		}
		
		@Override
		public String ping(String msg) {
			fakeClock.incrementAndGet();
			fakeClock.incrementAndGet();
			return msg;
		}
	}

	private static final class FakeTimer implements MetricsSpi {
		private final AtomicLong fakeClock;
		private long lastTimedObservableTime = -1L;
		private long lastTimedExecutionTime = -1L;

		private FakeTimer(AtomicLong fakeClock) {
			this.fakeClock = fakeClock;
		}
		
		public long getLastTimedObservableExecutionTime() {
			return this.lastTimedObservableTime;
		}
		
		public long getLastTimedExecutionTime() {
			return lastTimedExecutionTime;
		}

		@Override
		public TimerSpi createTimer() {
			return new TimerSpi() {
				@Override
				public <T> CheckedCommand<T> timeExecution(CheckedCommand<T> execution) {
					return () -> {
						long start = fakeClock.get(); // Set time before execution
						T result = execution.call();
						lastTimedExecutionTime = fakeClock.get() - start;
						return result;
					};
				}
				@Override
				public <T> Supplier<Observable<T>> timeObservable(Supplier<Observable<T>> targetObservable) {
					return () -> {
						long start = fakeClock.get(); // Set time before execution
						return targetObservable.get().doOnTerminate(() -> lastTimedObservableTime = fakeClock.get() - start);
					};
				}
				@Override
				public TimerSnaphot getSnapshot() {
					return TimerSnaphot.empty();
				}
			};
		}
	}

	public interface Ping {
		Observable<String> observePing(String msg);
		Future<String> pingAsync(String msg);
		String ping(String msg);
	}
	
	@AstrixApiProvider
	public interface PingApi {
		@AstrixConfigDiscovery("ping")
		@Service
		Ping ping();
	}

}
