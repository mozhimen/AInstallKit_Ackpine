package ru.solrudev.ackpine.impl.database.dao

import androidx.annotation.RestrictTo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import ru.solrudev.ackpine.impl.database.AckpineDatabase
import ru.solrudev.ackpine.impl.database.model.SessionEntity
import ru.solrudev.ackpine.uninstaller.UninstallFailure

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Dao
internal abstract class UninstallSessionDao(private val database: AckpineDatabase) {

	@Transaction
	open fun insertUninstallSession(session: SessionEntity.UninstallSession) {
		database.sessionDao().insertSession(session.session)
		insertPackageName(session.session.id, session.packageName)
	}

	@Transaction
	open fun setFailure(id: String, failure: UninstallFailure) {
		database.sessionDao().updateSessionState(id, SessionEntity.State.FAILED)
		insertUninstallFailure(id, failure)
	}

	@Transaction
	@Query("SELECT * FROM sessions WHERE id = :id")
	abstract fun getUninstallSession(id: String): SessionEntity.UninstallSession?

	@Query("SELECT failure FROM sessions_uninstall_failures WHERE session_id = :id")
	abstract fun getUninstallFailure(id: String): UninstallFailure?

	@Query("INSERT OR IGNORE INTO sessions_uninstall_failures(session_id, failure) VALUES (:id, :failure)")
	abstract fun insertUninstallFailure(id: String, failure: UninstallFailure)

	@Query("INSERT INTO sessions_package_names(session_id, package_name) VALUES (:id, :packageName)")
	abstract fun insertPackageName(id: String, packageName: String)
}