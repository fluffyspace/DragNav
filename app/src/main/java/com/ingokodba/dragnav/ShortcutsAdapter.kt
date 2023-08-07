package com.ingokodba.dragnav

import android.content.Context
import android.graphics.drawable.Drawable

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dragnav.R
import com.ingokodba.dragnav.modeli.Action
import com.ingokodba.dragnav.modeli.OnActionClick


class ShortcutsAdapter(c: Context, onShortcutClick: OnShortcutClick) : RecyclerView.Adapter<ShortcutsAdapter.ViewHolder>() {
    var actionsList: List<ShortcutAction> = listOf()
    var context:Context
    var onShortcutClick:OnShortcutClick
    init {
        context = c
        this.onShortcutClick = onShortcutClick
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var action_icon: ImageView
        var action_title: TextView
        override fun onClick(v: View) {
            val pos = adapterPosition
            onShortcutClick.onShortcutClick(pos)
        }


        //This is the subclass ViewHolder which simply
        //'holds the views' for us to show on each row
        init {
            //Finds the views from our row.xml
            action_icon = itemView.findViewById(R.id.action_icon) as ImageView
            action_title = itemView.findViewById(R.id.action_title) as TextView
            itemView.setOnClickListener(this)
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        //Here we use the information in the list we created to define the views
        //val appIcon:Drawable? = icons.get(appsList[i].packageName)
        viewHolder.action_title.text = actionsList[i].label
        if(actionsList[i].icon != null) viewHolder.action_icon.setImageDrawable(actionsList[i].icon)
    }

    override fun getItemCount(): Int {
        //This method needs to be overridden so that Androids knows how many items
        //will be making it into the list
        return actionsList.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        //This is what adds the code we've written in here to our target view
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.shortcut_row, parent, false)
        return ViewHolder(view)
    }

}

data class ShortcutAction(
    var label: String = "",
    var icon: Drawable? = null,
)