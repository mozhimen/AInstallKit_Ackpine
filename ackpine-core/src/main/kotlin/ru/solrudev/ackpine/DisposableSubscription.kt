package ru.solrudev.ackpine

public interface DisposableSubscription {
	public val isDisposed: Boolean
	public fun dispose()
}

internal object DummyDisposableSubscription : DisposableSubscription {
	override val isDisposed: Boolean = true
	override fun dispose() {}
}

public class DisposableSubscriptionContainer : DisposableSubscription {

	override var isDisposed: Boolean = false
		private set

	private val subscriptions = mutableListOf<DisposableSubscription>()

	public fun add(subscription: DisposableSubscription) {
		if (!isDisposed) {
			subscriptions += subscription
		}
	}

	public fun clear() {
		subscriptions.forEach { it.dispose() }
		subscriptions.clear()
	}

	public override fun dispose() {
		if (!isDisposed) {
			clear()
			isDisposed = true
		}
	}
}

public operator fun DisposableSubscriptionContainer.plusAssign(subscription: DisposableSubscription) {
	add(subscription)
}