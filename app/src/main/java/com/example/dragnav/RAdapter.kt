package com.example.dragnav

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.ListPopupWindow.WRAP_CONTENT
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dragnav.modeli.AppInfo
import com.example.dragnav.modeli.MessageEvent
import org.greenrobot.eventbus.EventBus
import java.util.*


class RAdapter(c: Context) : RecyclerView.Adapter<RAdapter.ViewHolder>() {
    var appsList: MutableList<AppInfo> = mutableListOf()
    val icons: MutableMap<String, Drawable?> = mutableMapOf()
    var context:Context
    var shortcutPopup:PopupWindow? = null
    init {
        context = c
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var textView: TextView
        var img: ImageView
        override fun onClick(v: View) {
            val pos = adapterPosition
            val context = v.context
            val launchIntent: Intent? =
                context.packageManager.getLaunchIntentForPackage(appsList[pos].packageName)
            if(launchIntent != null) {
                EventBus.getDefault().post(MessageEvent(appsList[pos].label, pos, appsList[pos].packageName, appsList[pos].color))
            } else {
                Log.d("ingo", "No launch intent")
            }
            //context.startActivity(launchIntent)
            Toast.makeText(v.context, appsList[pos].label, Toast.LENGTH_SHORT).show()
        }


        //This is the subclass ViewHolder which simply
        //'holds the views' for us to show on each row
        init {

            //Finds the views from our row.xml
            textView = itemView.findViewById(R.id.text) as TextView
            img = itemView.findViewById(R.id.img) as ImageView
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener{ v ->
                val pos = adapterPosition
                val context = v.context
                Toast.makeText(v.context, "shortcuts of " + appsList[pos].label, Toast.LENGTH_LONG).show()
                val shortcuts = getShortcutFromPackage(appsList[pos].packageName.toString(), v.context)
                shortcuts.forEach {
                    println("name = ${it.`package`}, label = ${it.shortLabel}")
                }
                showPopup(appsList[pos], itemView)

                return@setOnLongClickListener true
            }
        }
    }

    fun getIcon(pname:String):Drawable?{
        //if(radapter.icons.containsKey(pname)) continue
        try {
            return context.packageManager.getApplicationIcon(pname)
        } catch (e: PackageManager.NameNotFoundException){
            e.printStackTrace()
        }
        return null
        /*val applicationInfo = packageManager.getApplicationInfo(pname, 0)
        val res: Resources = packageManager.getResourcesForApplication(applicationInfo)
        val icon = res.getDrawableForDensity(
            applicationInfo.icon,
            DisplayMetrics.DENSITY_LOW,
            null
        )
        radapter.icons[pname] = icon*/
    }

    fun showPopup(app: AppInfo, itemView:View):Boolean{
        val shortcuts = getShortcutFromPackage(app.packageName.toString(), context)
        if(shortcuts.isNotEmpty()){

            val contentView = createShortcutListView(shortcuts)
            val locations = IntArray(2, {0})
            itemView.getLocationOnScreen(locations)
            shortcutPopup?.dismiss()
            shortcutPopup = PopupWindow(contentView, WRAP_CONTENT, WRAP_CONTENT, true)
            //shortcutPopup?.animationStyle = R.style.PopupAnimation
            shortcutPopup?.showAtLocation(itemView, Gravity.NO_GRAVITY, locations[0] + itemView.width/2, locations[1] + itemView.height/2)
        }
        return true
    }

    fun createShortcutListView(shortcuts: List<ShortcutInfo>):View{
        val view = LayoutInflater.from(context).inflate(R.layout.popup_shortcut, null)
        val shortcutList: RecyclerView = view.findViewById(R.id.shortcutList)
        shortcutList.adapter = ShortcutListAdapter(context, shortcuts)
        shortcutList.layoutManager = LinearLayoutManager(context)
        return view
    }

    fun getShortcutFromPackage(packageName:String, context:Context): List<ShortcutInfo>{
        val shortcutManager:LauncherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val shortcutQuery: LauncherApps.ShortcutQuery = LauncherApps.ShortcutQuery()
        shortcutQuery.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
        shortcutQuery.setPackage(packageName.toString())
        return try {
            shortcutManager.getShortcuts(
                    shortcutQuery,
                    android.os.Process.myUserHandle()
                ) as MutableList<ShortcutInfo>
        } catch (e: SecurityException){
            Collections.emptyList()
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        //Here we use the information in the list we created to define the views
        //val appIcon:Drawable? = icons.get(appsList[i].packageName)
        viewHolder.textView.text = appsList[i].label.toString()
        if(appsList[i].color != "") viewHolder.textView.setBackgroundColor(appsList[i].color.toInt())
        getIcon(appsList[i].packageName).let { viewHolder.img.setImageDrawable(it) }

    }

    override fun getItemCount(): Int {
        //This method needs to be overridden so that Androids knows how many items
        //will be making it into the list
        return appsList.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        //This is what adds the code we've written in here to our target view
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.row, parent, false)
        return ViewHolder(view)
    }

    init {

        //This is where we build our list of app details, using the app
        //object we created to store the label, package name and icon

    }
}