/*
 * Copyright (C) 2023 Ilya Fomichev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.solrudev.ackpine.session

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Launches the [Session] if it's not already, awaits for its completion without blocking a thread and resumes when it's
 * complete, returning the resulting value or throwing the corresponding exception if the session was cancelled.
 *
 * This suspending function is cancellable.
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this
 * function immediately resumes with [CancellationException] and cancels the session.
 *
 * This function handles session's lifecycle appropriately, like [Session.DefaultStateListener].
 *
 * @return [SessionResult]
 */
public suspend fun <F : Failure> Session<F>.await(): SessionResult<F> = suspendCancellableCoroutine { continuation ->
	val subscription = addStateListener(AwaitSessionStateListener(this, continuation))
	continuation.invokeOnCancellation {
		subscription.dispose()
		cancel()
	}
}

private class AwaitSessionStateListener<F : Failure>(
	private val session: Session<F>,
	private val continuation: CancellableContinuation<SessionResult<F>>
) : Session.StateListener<F> {

	override fun onStateChanged(sessionId: UUID, state: Session.State<F>) {
		if (state.isTerminal) {
			session.removeStateListener(this)
		}
		when (state) {
			Session.State.Pending -> session.launch()
			Session.State.Active -> session.launch() // re-launch if preparations were interrupted
			Session.State.Awaiting -> session.commit()
			Session.State.Committed -> {}
			Session.State.Cancelled -> continuation.cancel()
			Session.State.Succeeded -> continuation.resume(SessionResult.Success())
			is Session.State.Failed -> state.failure.let { failure ->
				if (failure is Failure.Exceptional) {
					continuation.resumeWithException(failure.exception)
				} else {
					continuation.resume(SessionResult.Error(failure))
				}
			}
		}
	}
}