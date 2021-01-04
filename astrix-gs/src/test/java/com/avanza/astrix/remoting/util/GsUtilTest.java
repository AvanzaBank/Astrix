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
package com.avanza.astrix.remoting.util;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.Test;
import com.avanza.astrix.beans.async.ContextPropagation;
import com.gigaspaces.async.AsyncResult;
import com.gigaspaces.async.SettableFuture;
import com.gigaspaces.async.internal.DefaultAsyncResult;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.observers.Subscribers;

public class GsUtilTest {

	@Test
	public void asyncResultList_ToFlatObservable_EmitsEachResultAsSingleItem() throws Exception {
		Func1<List<AsyncResult<String>>, Observable<String>> listToObservable = GsUtil.asyncResultListToObservable();
		List<AsyncResult<String>> r = Arrays.<AsyncResult<String>> asList(new DefaultAsyncResult<String>("foo", null),
				new DefaultAsyncResult<String>("bar", null));

		Observable<String> result = Observable.just(r).flatMap(listToObservable);
		assertEquals(Arrays.asList("foo", "bar"), result.toList().toBlocking().first());
	}

	@Test
	public void asyncResultList_ToFlatObservable_Error() throws Exception {
		Func1<List<AsyncResult<String>>, Observable<String>> listToObservable = GsUtil.asyncResultListToObservable();
		List<AsyncResult<String>> r = Arrays.<AsyncResult<String>> asList(new DefaultAsyncResult<String>("foo", null),
				new DefaultAsyncResult<String>(null, new TestException()));

		Observable<String> result = Observable.just(r).flatMap(listToObservable);
		Iterator<String> resultIterator = result.toBlocking().getIterator();
		resultIterator.next();
		try {
			resultIterator.next();
			fail("Expected TestException to be thrown");
		} catch (TestException e) {
			// expected
		}
	}

	public static class TestException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}


	private final AtomicReference<String> onNext = new AtomicReference<>();
	private final AtomicReference<Throwable> onError = new AtomicReference<>();
	private final AtomicBoolean isCompleted = new AtomicBoolean();
	private final SettableFuture<String> asyncFuture = new SettableFuture<>();
	private final Subscriber<String> subscriber = Subscribers.create(
			onNext::set,
			onError::set,
			() -> isCompleted.set(true)
	);
	private final ContextPropagation propagation = ContextPropagation.NONE;

	@Test
	public void shouldSetSubscriberOnNextUsingAsyncFutureValue() {
		// Arrange
		GsUtil.subscribe(asyncFuture, subscriber, propagation);

		// Act
		asyncFuture.setResult("result");

		// Assert
		assertEquals("result", onNext.get());
		assertNull(onError.get());
		assertTrue(isCompleted.get());
	}

	@Test
	public void shouldSetSubscriberOnErrorUsingAsyncFutureException() {
		// Arrange
		GsUtil.subscribe(asyncFuture, subscriber, propagation);

		// Act
		asyncFuture.setResult(new RuntimeException("should set onError"));

		// Assert
		assertNull(onNext.get());
		assertThat(onError.get(), instanceOf(RuntimeException.class));
		assertEquals("should set onError", onError.get().getMessage());
		assertFalse(isCompleted.get());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void shouldSetSubscriberOnNextUsingAsyncFutureValueWithContextPropagators() {
		// Arrange
		final List<String> operations = new ArrayList<>();
		final ContextPropagation propagation = mock(ContextPropagation.class);
		doAnswer(i -> {
			operations.add("wrap");
			final Consumer<AsyncResult<String>> consumer = (Consumer<AsyncResult<String>>) i.getArguments()[0];
			return (Consumer<AsyncResult<String>>) c -> {
				operations.add("before consumer");
				consumer.accept(c);
				operations.add("after consumer");
			};
		}).when(propagation).wrap(any(Consumer.class));

		operations.add("first");
		GsUtil.subscribe(asyncFuture, subscriber, propagation);

		// Act
		operations.add("before");
		asyncFuture.setResult("result");
		operations.add("after");

		// Assert
		assertEquals("result", onNext.get());
		assertTrue(isCompleted.get());
		final List<String> expected = Arrays.asList(
				"first",
				"wrap",
				"before",
				"before consumer",
				"after consumer",
				"after"
		);
		assertEquals(expected, operations);
	}
}
