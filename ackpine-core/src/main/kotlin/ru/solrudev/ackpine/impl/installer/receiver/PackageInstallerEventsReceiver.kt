package ru.solrudev.ackpine.impl.installer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import ru.solrudev.ackpine.impl.installer.activity.InstallActivity
import ru.solrudev.ackpine.impl.installer.activity.SessionBasedInstallConfirmationActivity
import ru.solrudev.ackpine.impl.installer.receiver.helpers.getParcelableExtraCompat
import ru.solrudev.ackpine.impl.installer.receiver.helpers.getSerializableExtraCompat
import ru.solrudev.ackpine.impl.session.CompletableSession
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.session.Session
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class InstallationEventsReceiver : BroadcastReceiver() {

	@Suppress("UNCHECKED_CAST")
	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action != getAction(context)) {
			return
		}
		val packageInstaller = ru.solrudev.ackpine.installer.PackageInstaller.getInstance(context)
		val ackpineSessionId = intent.getSerializableExtraCompat<UUID>(InstallActivity.SESSION_ID_KEY)!!
		val session = packageInstaller.getSessionAsync(ackpineSessionId).get() as? CompletableSession<InstallFailure>
		val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
		val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
		if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
			val confirmationIntent = intent.getParcelableExtraCompat<Intent>(Intent.EXTRA_INTENT)
			if (confirmationIntent != null) {
				val wrapperIntent = Intent(context, SessionBasedInstallConfirmationActivity::class.java)
					.putExtra(Intent.EXTRA_INTENT, confirmationIntent)
					.putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId)
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				context.startActivity(wrapperIntent)
			} else {
				session?.completeExceptionally(
					IllegalArgumentException("InstallationEventsReceiver: confirmationIntent was null.")
				)
			}
			return
		}
		val result = if (status == PackageInstaller.STATUS_SUCCESS) {
			Session.State.Succeeded
		} else {
			val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
			val otherPackageName = intent.getStringExtra(PackageInstaller.EXTRA_OTHER_PACKAGE_NAME)
			val storagePath = intent.getStringExtra(PackageInstaller.EXTRA_STORAGE_PATH)
			Session.State.Failed(InstallFailure.fromStatusCode(status, message, otherPackageName, storagePath))
		}
		session?.complete(result)
	}

	internal companion object {

		@JvmSynthetic
		internal fun getAction(context: Context) = "${context.packageName}.INSTALLATION_STATUS"
	}
}