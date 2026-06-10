@file:Suppress("DEPRECATION", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package net.nullsum.freedoom

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.io.File
import java.util.ArrayList

class LaunchFragmentGZdoom : Fragment() {

    private val LOG = "LaunchFragment"

    private var gameArgsTextView: TextView? = null
    private lateinit var argsEditText: EditText
    private lateinit var listview: ListView
    private lateinit var copyWadsTextView: TextView

    private var listAdapter: GamesListAdapter? = null

    private val games = ArrayList<DoomWad>()
    private var selectedMod: DoomWad? = null

    private var fullBaseDir: String? = null

    private val argsHistory = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fullBaseDir = AppSettings.getQuakeFullDir()

        AppSettings.createDirectories(requireActivity())

        Utils.loadArgs(requireActivity(), argsHistory)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        Log.d(LOG, "onHiddenChanged")
        fullBaseDir = AppSettings.getQuakeFullDir()

        //rare device call onHiddenchange before the view is created, detect this to prevent crash
        if (gameArgsTextView == null)
            return

        refreshGames()
        super.onHiddenChanged(hidden)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val mainView = inflater.inflate(R.layout.fragment_launch_gzdoom, null)

        Log.d(LOG, "fullBaseDir is: $fullBaseDir")

        Utils.copyFreedoomFilesToSD(requireActivity())

//         Nasty hack to refresh view if this is the first launch and Freedoom files were copied
        val hasRunTester = File("$fullBaseDir/firstrun")
        if (!hasRunTester.exists()) {
            Log.d(LOG, "firstrun file not found, proceeding with first launch hack")

            Utils.copyAsset(requireActivity(), "firstrun", fullBaseDir!!)
            // Info of hack
            // https://stackoverflow.com/questions/15262747/refresh-or-force-redraw-the-fragment

            // simple refresh of wad list
            Handler().postDelayed({ listRefreshHack() }, 10000)

            // END HACK
        }

        argsEditText = mainView.findViewById(R.id.extra_args_edittext)
        gameArgsTextView = mainView.findViewById(R.id.extra_args_textview)
        listview = mainView.findViewById(R.id.listView)

        //listview.setBackgroundDrawable(new BitmapDrawable(getResources(), Utils.decodeSampledBitmapFromResource(getResources(), R.drawable.chco_doom, 635, 284)));
        listAdapter = GamesListAdapter()
        listview.adapter = listAdapter

        listview.setOnItemClickListener { _, _, pos, _ -> selectGame(pos) }
        copyWadsTextView = mainView.findViewById(R.id.copy_wads_textview)

        val startfull = mainView.findViewById<Button>(R.id.start_full)
        startfull.setOnClickListener {
            if (selectedMod == null) {
                val builder = AlertDialog.Builder(activity)
                builder.setMessage((R.string.no_iwads_err.toString() + fullBaseDir))
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok_confirm) { _, _ -> }

                val alert = builder.create()
                alert.show()
            } else {
                startGame(fullBaseDir!!, false, null)
            }
        }

        val wadButton = mainView.findViewById<Button>(R.id.start_wads)
        wadButton.setOnClickListener {
            object : ModSelectDialog(activity, fullBaseDir, false) {
                override fun resultResult(result: String) {
                    argsEditText.setText(result)
                }
            }
        }

        val deleteArgs = mainView.findViewById<ImageView>(R.id.args_delete_imageview)
        deleteArgs.setOnClickListener { argsEditText.setText("") }

        val history = mainView.findViewById<ImageView>(R.id.args_history_imageview)
        history.setOnClickListener {
            val servers = arrayOfNulls<String>(argsHistory.size)
            for (n in argsHistory.indices) servers[n] = argsHistory[n]

            val builder = AlertDialog.Builder(activity)
            builder.setTitle("Extra Args History")
            builder.setItems(servers) { _, which -> argsEditText.setText(argsHistory[which]) }
            builder.show()
        }

        refreshGames()

        return mainView
    }

    private fun listRefreshHack() {
        Log.d(LOG, "HACK refreshing wad list")

        // Doesn't work
        listAdapter?.notifyDataSetChanged()

//         // Extremely aggressive redraw of wad list
//         listview.invalidateViews();

        // Force the main activity to respawn (nuclear option) @TODO fix me
        (activity as EntryActivity).restart()

////         Aggressive hack which forces the entire fragment to redraw
//        FragmentTransaction tr = getFragmentManager().beginTransaction();
//        tr.replace(((ViewGroup)getView().getParent()).getId(), this);
//        tr.commit();
    }

    private fun startGame(base: String, ignoreMusic: Boolean, moreArgs: String?) {
        //Check gzdoom.pk3 wad exists
        //File extrawad = new File(base + "/gzdoom.pk3");
        //if (!extrawad.exists())
        run {
            Utils.copyAsset(requireActivity(), "gzdoom.pk3", base)
            Utils.copyAsset(requireActivity(), "gzdoom.sf2", base)
            //Utils.copyAsset(getActivity(), "lights_dt.pk3", base);
            //Utils.copyAsset(getActivity(), "brightmaps_dt.pk3", base);
        }

        //File[] files = new File(basePath).listFiles();

        val extraArgs = argsEditText.text.toString().trim()

        if (extraArgs.isNotEmpty()) {
            val it = argsHistory.iterator()
            while (it.hasNext()) {
                val s = it.next()
                if (s.contentEquals(extraArgs))
                    it.remove()
            }

            while (argsHistory.size > 50)
                argsHistory.removeAt(argsHistory.size - 1)

            argsHistory.add(0, extraArgs)
            Utils.saveArgs(requireActivity(), argsHistory)
        }

        AppSettings.setStringOption(requireActivity(), "last_tab", "Freedoom")

        var args = gameArgsTextView!!.text.toString() + " " + argsEditText.text.toString()

        val intent = Intent(activity, Game::class.java)
        intent.action = Intent.ACTION_MAIN
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val resDiv = AppSettings.getIntOption(requireActivity(), "gzdoom_res_div", 1)
        intent.putExtra("res_div", resDiv)

        intent.putExtra("game_path", base)
        intent.putExtra("game", "net.nullsum.freedoom")

        if (moreArgs != null)
            args = "$args $moreArgs"
        val saveDir = " -savedir $base/gzdoom_saves"

        val fluidSynthFile = "gzdoom.sf2"

        intent.putExtra(
            "args",
            args + saveDir + " +set fluid_patchset " + fluidSynthFile + " +set midi_dmxgus 0 "
        )

        startActivity(intent)
    }

    private fun selectGame(pos: Int) {
        if (pos == -1 || pos >= games.size) {
            selectedMod = null
            gameArgsTextView?.text = ""
            return
        }

        val game = games[pos]

        for (g in games)
            g.selected = false

        selectedMod = game

        game.selected = true

        gameArgsTextView?.text = game.args

        AppSettings.setIntOption(requireActivity(), "last_iwad", pos)

        listAdapter?.notifyDataSetChanged()
    }

    private fun refreshGames() {
        games.clear()

        val files = File(fullBaseDir.orEmpty()).listFiles()

        if (files != null) {
            for (f in files) {
                if (!f.isDirectory) {
                    val file = f.name.lowercase()
                    Log.d(LOG, "refreshGames $file")
                    if ((file.endsWith(".wad") || file.endsWith(".pk3") || file.endsWith(".pk7"))
                        && !file.contentEquals("prboom-plus.wad")
                        && !file.contentEquals("gzdoom.pk3")
                        && !file.contentEquals("gzdoom_dev.pk3")
                        && !file.contentEquals("lights_dt.pk3")
                        && !file.contentEquals("brightmaps_dt.pk3")
                        && !file.contentEquals("lights.pk3")
                        && !file.contentEquals("brightmaps.pk3")
                    ) {
                        val game = DoomWad(file)
                        game.args = "-iwad $file"

                        games.add(game)
                    }
                }
            }
        }

        listAdapter?.notifyDataSetChanged()

        selectGame(AppSettings.getIntOption(requireActivity(), "last_iwad", -1))
    }

    inner class GamesListAdapter : BaseAdapter() {

        fun add(string: String) {
        }

        override fun getCount(): Int {
            return games.size
        }

        override fun getItem(arg0: Int): Any? {
            // TODO Auto-generated method stub
            return null
        }

        override fun getItemId(arg0: Int): Long {
            // TODO Auto-generated method stub
            return 0
        }

        override fun getView(position: Int, convertView: View?, list: ViewGroup?): View {
            val view: View = convertView
                ?: requireActivity().layoutInflater.inflate(R.layout.games_listview_item, null)

            val game = games[position]

            if (game.selected)
                view.setBackgroundResource(R.drawable.layout_sel_background)
            else
                view.setBackgroundResource(0)

            val title = view.findViewById<TextView>(R.id.title_textview)
            title.text = game.file

            return view
        }
    }
}
