package com.ingokodba.dragnav

import android.content.Context

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dragnav.R
import com.ingokodba.dragnav.modeli.Action
import com.ingokodba.dragnav.modeli.OnActionClick


class ActionsAdapter(c: Context, onActionClick: OnActionClick) : RecyclerView.Adapter<ActionsAdapter.ViewHolder>() {
    var actionsList: List<Action> = listOf()
    var context:Context
    var onActionClick:OnActionClick
    init {
        context = c
        this.onActionClick = onActionClick
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var action_title: TextView
        var action_description: TextView
        override fun onClick(v: View) {
            val pos = adapterPosition
            onActionClick.onActionClick(actionsList[pos])
            val context = v.context
            /*val launchIntent: Intent? =
                context.packageManager.getLaunchIntentForPackage(appsList[pos].packageName)
            if(launchIntent != null) {
                EventBus.getDefault().post(MessageEvent(appsList[pos].label, pos, appsList[pos].packageName, appsList[pos].color, app=appsList[pos]))
            } else {
                Log.d("ingo", "No launch intent")
            }
            //context.startActivity(launchIntent)*/
        }


        //This is the subclass ViewHolder which simply
        //'holds the views' for us to show on each row
        init {

            //Finds the views from our row.xml
            action_title = itemView.findViewById(R.id.action_title) as TextView
            action_description = itemView.findViewById(R.id.action_description) as TextView
            itemView.setOnClickListener(this)
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        //Here we use the information in the list we created to define the views
        //val appIcon:Drawable? = icons.get(appsList[i].packageName)
        viewHolder.action_title.text = actionsList[i].title
        viewHolder.action_description.text = actionsList[i].description

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
        val view: View = inflater.inflate(R.layout.cardview_action_row, parent, false)
        return ViewHolder(view)
    }

}