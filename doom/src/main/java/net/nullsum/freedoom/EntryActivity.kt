package net.nullsum.freedoom

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.beloko.touchcontrols.GamePadFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class EntryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quake)

        AppSettings.reloadSettings(application)
        // App-specific external storage needs no runtime permission on modern Android.
        AppSettings.createDirectories(this)

        GamePadFragment.gamepadActions = Utils.getGameGamepadConfig(resources)

        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)

        val titles = listOf(
            getString(R.string.app_name),
            getString(R.string.gamepad_tab),
            getString(R.string.options_tab),
        )

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 3

            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> LaunchFragmentGZdoom()
                1 -> GamePadFragment()
                else -> OptionsFragment()
            }
        }
        // Keep the gamepad fragment resident so input forwarding works while another tab is shown.
        viewPager.offscreenPageLimit = 2

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()
    }

    fun restart() {
        val intent = Intent(this, EntryActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    private fun gamePadFragment(): GamePadFragment? =
        supportFragmentManager.fragments.filterIsInstance<GamePadFragment>().firstOrNull()

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val frag = gamePadFragment()
        return frag?.onGenericMotionEvent(event) ?: super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val frag = gamePadFragment()
        return if (frag != null && frag.onKeyDown(keyCode, event)) true else super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val frag = gamePadFragment()
        return if (frag != null && frag.onKeyUp(keyCode, event)) true else super.onKeyUp(keyCode, event)
    }
}
