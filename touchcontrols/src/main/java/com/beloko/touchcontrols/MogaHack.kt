package com.beloko.touchcontrols

/**
 * Mupen64PlusAE, an N64 emulator for the Android platform
 *
 * Copyright (C) 2013 Paul Lamb
 *
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Authors: Paul Lamb
 */

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.util.Log
import com.bda.controller.Controller
import com.bda.controller.IControllerService

/**
 * Temporary hack for crash in MOGA library on Lollipop. The actual issue is caused by the use of
 * implicit service intents, which are illegal in Lollipop.
 */
object MogaHack {
    @JvmStatic
    fun init(controller: Controller, context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            var mIsBound = false
            var fIsBound: java.lang.reflect.Field? = null
            var mServiceConnection: ServiceConnection? = null
            try {
                val cMogaController: Class<*> = controller.javaClass
                fIsBound = cMogaController.getDeclaredField("mIsBound")
                fIsBound.isAccessible = true
                mIsBound = fIsBound.getBoolean(controller)
                val fServiceConnection = cMogaController.getDeclaredField("mServiceConnection")
                fServiceConnection.isAccessible = true
                mServiceConnection = fServiceConnection.get(controller) as ServiceConnection?
            } catch (e: NoSuchFieldException) {
                Log.e("MogaHack", "MOGA Lollipop Hack NoSuchFieldException (get)", e)
            } catch (e: IllegalAccessException) {
                Log.e("MogaHack", "MOGA Lollipop Hack IllegalAccessException (get)", e)
            } catch (e: IllegalArgumentException) {
                Log.e("MogaHack", "MOGA Lollipop Hack IllegalArgumentException (get)", e)
            }
            if (!mIsBound && mServiceConnection != null) {
                // Convert implicit intent to explicit intent.
                val intent = Intent(IControllerService::class.java.name)
                val resolveInfos = context.packageManager.queryIntentServices(intent, 0)
                if (resolveInfos.size != 1) {
                    Log.e(
                        "MogaHack",
                        "Somebody is trying to intercept our intent. Disabling MOGA controller for security.",
                    )
                    return
                }
                val serviceInfo = resolveInfos[0].serviceInfo
                val packageName = serviceInfo.packageName
                val className = serviceInfo.name
                intent.component = ComponentName(packageName, className)

                // Start the service explicitly
                context.startService(intent)
                context.bindService(intent, mServiceConnection, Context.BIND_IMPORTANT)
                try {
                    fIsBound!!.setBoolean(controller, true)
                } catch (e: IllegalAccessException) {
                    Log.e("MogaHack", "MOGA Lollipop Hack IllegalAccessException (set)", e)
                } catch (e: IllegalArgumentException) {
                    Log.e("MogaHack", "MOGA Lollipop Hack IllegalArgumentException (set)", e)
                }
            }
        } else {
            controller.init()
        }
    }
}
