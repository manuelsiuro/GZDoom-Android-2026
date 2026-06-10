/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.mobeta.android.dslv

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ResourceCursorAdapter

// taken from v4 rev. 10 ResourceCursorAdapter.java

/**
 * Static library support version of the framework's [ResourceCursorAdapter].
 * Used to write apps that run on platforms prior to Android 3.0.  When running
 * on Android 3.0 or above, this implementation is still used; it does not try
 * to switch to the framework's implementation.  See the framework SDK
 * documentation for a class overview.
 */
abstract class ResourceDragSortCursorAdapter : DragSortCursorAdapter {
    private var mLayout: Int

    private var mDropDownLayout: Int

    private val mInflater: LayoutInflater

    /**
     * Constructor the enables auto-requery.
     *
     * @param context The context where the ListView associated with this adapter is running
     * @param layout  resource identifier of a layout file that defines the views
     * for this list item.  Unless you override them later, this will
     * define both the item views and the drop down views.
     */
    @Deprecated("This option is discouraged, as it results in Cursor queries being performed on the application's UI thread.")
    constructor(context: Context, layout: Int, c: Cursor?) : super(context, c) {
        mLayout = layout
        mDropDownLayout = layout
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    constructor(
        context: Context,
        layout: Int,
        c: Cursor?,
        autoRequery: Boolean
    ) : super(context, c, autoRequery) {
        mLayout = layout
        mDropDownLayout = layout
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    /**
     * Standard constructor.
     *
     * @param context The context where the ListView associated with this adapter is running
     * @param layout  Resource identifier of a layout file that defines the views
     * for this list item.  Unless you override them later, this will
     * define both the item views and the drop down views.
     * @param c       The cursor from which to get the data.
     * @param flags   Flags used to determine the behavior of the adapter.
     */
    constructor(context: Context, layout: Int, c: Cursor?, flags: Int) : super(context, c, flags) {
        mLayout = layout
        mDropDownLayout = layout
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    /**
     * Inflates view(s) from the specified XML file.
     */
    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        return mInflater.inflate(mLayout, parent, false)
    }

    override fun newDropDownView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        return mInflater.inflate(mDropDownLayout, parent, false)
    }

    /**
     * Sets the layout resource of the item views.
     *
     * @param layout the layout resources used to create item views
     */
    fun setViewResource(layout: Int) {
        mLayout = layout
    }

    /**
     * Sets the layout resource of the drop down views.
     *
     * @param dropDownLayout the layout resources used to create drop down views
     */
    fun setDropDownViewResource(dropDownLayout: Int) {
        mDropDownLayout = dropDownLayout
    }
}
