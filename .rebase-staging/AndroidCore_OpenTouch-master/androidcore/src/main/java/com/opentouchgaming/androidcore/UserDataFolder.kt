package com.opentouchgaming.androidcore

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.DocumentsContract

/**
 * Opens the app's user data folder in the system file manager using the
 * registered OpenTouchDocumentProvider. Falls back through multiple strategies
 * to handle different OEM file manager implementations.
 */
fun openUserDataFolder(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val authority = "${activity.packageName}.user"

        try {
            activity.startActivity(buildDocumentProviderIntent(authority, Intent.ACTION_VIEW))
            return
        } catch (_: ActivityNotFoundException) {}

        try {
            activity.startActivity(buildDocumentProviderIntent(authority, "android.provider.action.BROWSE"))
            return
        } catch (_: ActivityNotFoundException) {}

        try {
            activity.startActivity(buildDocumentsUiIntent(activity, "com.google.android.documentsui"))
            return
        } catch (_: ActivityNotFoundException) {}

        try {
            activity.startActivity(buildDocumentsUiIntent(activity, "com.android.documentsui"))
            return
        } catch (_: ActivityNotFoundException) {}
    }
}

private fun buildDocumentProviderIntent(authority: String, action: String): Intent {
    val intent = Intent(action)
    intent.addCategory(Intent.CATEGORY_DEFAULT)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        intent.data = DocumentsContract.buildRootUri(authority, OpenTouchDocumentProvider.ROOT_ID)
    }
    intent.addFlags(
        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    )
    return intent
}

private fun buildDocumentsUiIntent(activity: Activity, packageName: String): Intent {
    return activity.packageManager.getLaunchIntentForPackage(packageName)
        ?: throw ActivityNotFoundException()
}
