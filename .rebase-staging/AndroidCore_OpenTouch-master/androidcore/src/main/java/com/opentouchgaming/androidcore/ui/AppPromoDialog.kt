package com.opentouchgaming.androidcore.ui

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.opentouchgaming.androidcore.AppInfo
import com.opentouchgaming.androidcore.R
import com.opentouchgaming.androidcore.databinding.DialogAppPromoBinding
import com.opentouchgaming.androidcore.databinding.ListItemPromoAppBinding

/**
 * Cross-promotion dialog shown from the "Online" tools button. Lists the other OpenTouch
 * apps (the currently running app is hidden) with icon, three screenshots and a button that
 * opens the app's store / website page.
 *
 * The promo images are bundled drawables in AndroidCore, resolved by name at runtime so that
 * missing assets simply fall back to a placeholder instead of breaking the build:
 *   <assetBase>_icon, <assetBase>_1, <assetBase>_2, <assetBase>_3
 */
class AppPromoDialog
{
    /**
     * @param app        used to hide the current app from the list
     * @param name       display name
     * @param tagline    short one-line description
     * @param engines    engines / games shown as a bold bullet list on the right
     * @param assetBase  drawable name prefix, e.g. "promo_delta"
     * @param url        Google Play (https://play.google.com/store/apps/details?id=...) or website URL
     */
    data class PromoApp(
        val app: AppInfo.Apps,
        val name: String,
        val tagline: String,
        val engines: List<String>,
        val assetBase: String,
        val url: String
    )

    // The fixed set of OpenTouch launcher apps. Edit the urls here to adjust store/website links.
    private val allApps = listOf(
        PromoApp(
            AppInfo.Apps.ALPHA_TOUCH,
            "Alpha Touch",
            "Wolfenstein & other classic FPS!",
            listOf("RTCW", "RealRTCW", "ECWolf", "MOHAA", "LZWolf", "ROTT", "Blake Stone", "Perfect Dark"),
            "promo_alpha",
            "http://opentouchgaming.com/alpha-touch/"
        ),
        PromoApp(
            AppInfo.Apps.DELTA_TOUCH,
            "Delta Touch",
            "The ultimate DOOM app!",
            listOf("GZDoom", "UZDoom","Doom 3", "PrBoom+", "Chocolate Doom", "Doom Retro", "DSDA", "Zandronum", "Doom 64", ),
            "promo_delta",
            "https://play.google.com/store/apps/details?id=com.opentouchgaming.deltatouch"
        ),
        PromoApp(
            AppInfo.Apps.QUAD_TOUCH,
            "Quad Touch",
            "The BEST Quake engineson Android!",
            listOf("DarkPlaces", "QuakeSpasm", "FTEQW", "Quake 2", "Yamagi Q2", "Quake 3", "Hexen 2", "WRATH"),
            "promo_quad",
            "https://play.google.com/store/apps/details?id=com.opentouchgaming.quadtouch"
        ),
        PromoApp(
            AppInfo.Apps.RAZE_TOUCH,
            "Zeta Touch",
            "Duke Nukem 3D and other Build engine classics!",
            listOf("Duke Nukem 3D", "Blood", "Shadow Warrior", "Redneck Rampage", "Ion Fury", "AMC", "A.W.O.L", "NAM", "Powerslave"),
            "promo_zeta",
            "http://opentouchgaming.com/zeta-touch/"
        )
    )

    lateinit var binding: DialogAppPromoBinding

    fun showDialog(activity: Activity)
    {
        binding = DialogAppPromoBinding.inflate(activity.layoutInflater)

        val dialog = Dialog(activity, R.style.DialogAppPromo)
        dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        // Hide the app we are currently running in
        val promoApps = allApps.filter { it.app != AppInfo.app }

        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.adapter = PromoAdapter(activity, promoApps)

        binding.closeButton.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun drawableId(activity: Activity, name: String): Int
    {
        val id = activity.resources.getIdentifier(name, "drawable", activity.packageName)
        return if (id != 0) id else R.drawable.ic_baseline_world
    }

    inner class PromoAdapter(val activity: Activity, val apps: List<PromoApp>) :
        RecyclerView.Adapter<PromoAdapter.ViewHolder>()
    {
        inner class ViewHolder(view: View, val binding: ListItemPromoAppBinding) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
        {
            val b = ListItemPromoAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b.root, b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int)
        {
            val app = apps[position]

            holder.binding.appIconImageView.setImageResource(drawableId(activity, app.assetBase + "_icon"))
            holder.binding.appNameTextView.text = app.name
            holder.binding.appTaglineTextView.text = app.tagline

            holder.binding.shot1ImageView.setImageResource(drawableId(activity, app.assetBase + "_1"))
            holder.binding.shot2ImageView.setImageResource(drawableId(activity, app.assetBase + "_2"))

            holder.binding.enginesTextView.text = app.engines.joinToString("\n") { "• $it" }

            val open = View.OnClickListener {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(app.url)))
            }
            holder.binding.getButton.setOnClickListener(open)
            holder.binding.promoRoot.setOnClickListener(open)
        }

        override fun getItemCount() = apps.size
    }
}
