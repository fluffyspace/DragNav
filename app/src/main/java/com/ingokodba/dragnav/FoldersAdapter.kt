package com.ingokodba.dragnav

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dragnav.R
import com.ingokodba.dragnav.modeli.MeniJednoPolje
import org.greenrobot.eventbus.EventBus


class FoldersAdapter(c: Context, meniPolja: List<MeniJednoPolje>) : RecyclerView.Adapter<FoldersAdapter.ViewHolder>() {
    var folders: List<MeniJednoPolje>
    var context:Context
    init {
        context = c
        folders = meniPolja
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var textView: TextView
        var img: ImageView
        override fun onClick(v: View) {
            val pos = adapterPosition
            Log.d("ingo", "selected " + pos + " " + folders[pos].text)
            val context = v.context
            EventBus.getDefault().post(AddShortcutActivity.Veve(pos))
            v.setBackgroundColor(Color.YELLOW)
        }


        //This is the subclass ViewHolder which simply
        //'holds the views' for us to show on each row
        init {

            //Finds the views from our row.xml
            textView = itemView.findViewById(R.id.text) as TextView
            img = itemView.findViewById(R.id.img) as ImageView
            itemView.setOnClickListener(this)
            textView.setTextColor(Color.BLACK)
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        //Here we use the information in the list we created to define the views
        //val appIcon:Drawable? = icons.get(appsList[i].packageName)
        viewHolder.textView.text = folders[i].text
    }

    override fun getItemCount(): Int {
        //This method needs to be overridden so that Androids knows how many items
        //will be making it into the list
        return folders.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        //This is what adds the code we've written in here to our target view
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.image_text_row, parent, false)
        return ViewHolder(view)
    }

    init {

        //This is where we build our list of app details, using the app
        //object we created to store the label, package name and icon

    }
}