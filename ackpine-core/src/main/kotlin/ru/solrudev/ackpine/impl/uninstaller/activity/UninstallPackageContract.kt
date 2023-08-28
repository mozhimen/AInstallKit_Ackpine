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

package ru.solrudev.ackpine.impl.uninstaller.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RestrictTo

/**
 * Returns package uninstall [UninstallContract] for current API level.
 */
@Suppress("FunctionName")
@JvmSynthetic
internal fun UninstallPackageContract(): UninstallContract {
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
		return ActionUninstallPackageContract()
	}
	return ActionDeletePackageContract()
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface UninstallContract {
	fun createIntent(context: Context, input: String): Intent
	fun parseResult(context: Context, resultCode: Int): Boolean
}

private class ActionDeletePackageContract : UninstallContract {

	private lateinit var packageName: String

	override fun createIntent(context: Context, input: String): Intent {
		packageName = input
		val packageUri = Uri.parse("package:$input")
		return Intent(Intent.ACTION_DELETE, packageUri)
	}

	override fun parseResult(context: Context, resultCode: Int) = !context.isPackageInstalled(packageName)

	private fun Context.isPackageInstalled(packageName: String) = try {
		packageManager.getPackageInfoCompat(packageName, PackageManager.GET_ACTIVITIES)
		true
	} catch (_: PackageManager.NameNotFoundException) {
		false
	}

	@Suppress("DEPRECATION")
	private fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int): PackageInfo {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
		} else {
			getPackageInfo(packageName, flags)
		}
	}
}

private class ActionUninstallPackageContract : UninstallContract {

	@Suppress("DEPRECATION")
	override fun createIntent(context: Context, input: String) = Intent().apply {
		action = Intent.ACTION_UNINSTALL_PACKAGE
		data = Uri.parse("package:$input")
		putExtra(Intent.EXTRA_RETURN_RESULT, true)
	}

	override fun parseResult(context: Context, resultCode: Int) = resultCode == Activity.RESULT_OK
}