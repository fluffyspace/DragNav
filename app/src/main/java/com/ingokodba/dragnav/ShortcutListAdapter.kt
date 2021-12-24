package com.ingokodba.dragnav

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.content.pm.LauncherApps
import android.view.ViewGroup
import android.view.LayoutInflater
import android.content.pm.ShortcutInfo
import android.view.View
import android.widget.*
import com.example.dragnav.R

class ShortcutListAdapter(c: Context, shortcuts:List<ShortcutInfo>) : RecyclerView.Adapter<ShortcutListAdapter.ViewHolder>() {
    var shortcuts:List<ShortcutInfo>
    var context:Context
    init {
        this.shortcuts = shortcuts
        context = c
    }
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var textView: TextView
        var img: ImageView


        //This is the subclass ViewHolder which simply
        //'holds the views' for us to show on each row
        init {
            //Finds the views from our row.xml
            textView = itemView.findViewById<View>(R.id.text) as TextView
            img = itemView.findViewById<View>(R.id.img) as ImageView

        }

        override fun onClick(v: View?) {
            TODO("Not yet implemented")
            context.startActivity(shortcuts[adapterPosition].intent)
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        val textView = viewHolder.textView
        textView.text = shortcuts[i].shortLabel
        val imageView = viewHolder.img
        val launcherApps:LauncherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        imageView.setImageDrawable(launcherApps.getShortcutIconDrawable(shortcuts[i],
            context.resources.displayMetrics.densityDpi))
    }

    override fun getItemCount(): Int {

        //This method needs to be overridden so that Androids knows how many items
        //will be making it into the list
        return shortcuts.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        //This is what adds the code we've written in here to our target view
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.small_row, parent, false)
        return ViewHolder(view)
    }

    init {

        //This is where we build our list of app details, using the app
        //object we created to store the label, package name and icon

    }
}