package ru.solrudev.ackpine.session

import ru.solrudev.ackpine.DisposableSubscription
import java.util.UUID

public interface Session<out F : Failure> {

	public val id: UUID
	public val isActive: Boolean
	public fun launch()
	public fun commit()
	public fun cancel()
	public fun addStateListener(listener: StateListener<F>): DisposableSubscription
	public fun removeStateListener(listener: StateListener<F>)

	public sealed interface State<out F : Failure> {

		public sealed interface Terminal

		public val isTerminal: Boolean
			get() = this is Terminal

		public data object Pending : State<Nothing>
		public data object Active : State<Nothing>
		public data object Awaiting : State<Nothing>
		public data object Committed : State<Nothing>
		public data object Cancelled : State<Nothing>, Terminal
		public data object Succeeded : State<Nothing>, Terminal
		public data class Failed<out F : Failure>(public val failure: F) : State<F>, Terminal
	}

	public fun interface StateListener<in F : Failure> {
		public fun onStateChanged(sessionId: UUID, state: State<F>)
	}
}

internal class StateDisposableSubscription<F : Failure> internal constructor(
	private var session: Session<F>?,
	private var listener: Session.StateListener<F>?
) : DisposableSubscription {

	override var isDisposed: Boolean = false
		private set

	override fun dispose() {
		if (isDisposed) {
			return
		}
		val listener = this.listener
		if (listener != null) {
			session?.removeStateListener(listener)
		}
		this.listener = null
		session = null
		isDisposed = true
	}
}