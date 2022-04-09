package com.ingokodba.dragnav

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dragnav.R
import com.example.dragnav.databinding.RowBindingBinding
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.MessageEvent
import org.greenrobot.eventbus.EventBus


class NewRAdapter(viewModel: NewRAdapterViewModel) :
    ListAdapter<AppInfo, RecyclerView.ViewHolder>(DiffCallback) {

    lateinit var viewMoo: NewRAdapterViewModel
    var idOtvorenogMenija:Int = -1

    private val SHOW_MENU = 1
    private val HIDE_MENU = 2

    // initializer block
    init {
        viewMoo = viewModel
    }

    //Our menu view
    class MenuViewHolder(view: View) : RecyclerView.ViewHolder(view){
        var infobutton:ImageView
        var addappbutton:ImageView
        var backbutton:ImageView
        init{
            backbutton = view.findViewById(R.id.backbutton);
            infobutton = view.findViewById(R.id.appinfobutton);
            addappbutton = view.findViewById(R.id.addappbutton)
        }

    }

    /**
     * The MarsPhotosViewHolder constructor takes the binding variable from the associated
     * GridViewItem, which nicely gives it access to the full [MarsPhoto] information.
     */
    class MarsPhotosViewHolder(
        private var binding: RowBindingBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        var textView: TextView
        var img: ImageView
        init {
            textView = itemView.findViewById(R.id.text) as TextView
            img = itemView.findViewById(R.id.img) as ImageView
        }

        fun bind(aplikacija: AppInfo, viewModel: NewRAdapterViewModel) {
            binding.aplikacija = aplikacija
            binding.viewModel = viewModel
            // This is important, because it forces the data binding to execute immediately,
            // which allows the RecyclerView to make the correct view size measurements

            binding.executePendingBindings()
        }
    }

    /**
     * Allows the RecyclerView to determine which items have changed when the [List] of
     * [MarsPhoto] has been updated.
     */
    companion object DiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.id == newItem.id
        }
    }

    /**
     * Create new [RecyclerView] item views (invoked by the layout manager)
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        if(viewType==SHOW_MENU){
            val v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_menu, parent, false);
            return MenuViewHolder(v);
        }else{
            return MarsPhotosViewHolder(
                RowBindingBinding.inflate(LayoutInflater.from(parent.context))
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        if(idOtvorenogMenija == position) {
            return SHOW_MENU;
        } else {
            return HIDE_MENU;
        }
    }

    fun dragAndDropApp(pos:Int, context: Context){
        Toast.makeText(context, "Drag and drop app!", Toast.LENGTH_SHORT).show()
        EventBus.getDefault().post(MessageEvent(viewMoo.appsList.value!![pos].label, pos, viewMoo.appsList.value!![pos].packageName, viewMoo.appsList.value!![pos].color, draganddrop = true))
    }

    /**
     * Replaces the contents of a view (invoked by the layout manager)
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        val appInfo: AppInfo = getItem(pos)
        if (holder is MarsPhotosViewHolder) {
            holder.bind(appInfo, viewMoo)
            if(viewMoo.appsList.value!![pos].color != "") holder.textView.setBackgroundColor(viewMoo.appsList.value!![pos].color.toInt())
            holder.itemView.setOnLongClickListener{ v ->
                showMenu(pos)
                /*Toast.makeText(v.context, "Loading app info...", Toast.LENGTH_SHORT).show()
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri: Uri = Uri.fromParts("package", viewMoo.appsList.value!![pos].packageName, null)
                intent.data = uri
                startActivity(v.context, intent, null)*/

                return@setOnLongClickListener true
            }
            holder.itemView.setOnClickListener { v ->
                val context = v.context
                val launchIntent: Intent? =
                    context.packageManager.getLaunchIntentForPackage(viewMoo.appsList.value!![pos].packageName)
                if(launchIntent != null) {
                    EventBus.getDefault().post(MessageEvent(viewMoo.appsList.value!![pos].label, pos, viewMoo.appsList.value!![pos].packageName, viewMoo.appsList.value!![pos].color))
                } else {
                    Log.d("ingo", "No launch intent")
                }
                //context.startActivity(launchIntent)
                Toast.makeText(v.context, viewMoo.appsList.value!![pos].label, Toast.LENGTH_SHORT).show()
                Log.d("ingo", "id grr rv klika je " + appInfo.id.toString())
            }
        }

        if (holder is MenuViewHolder) {
            //Menu Actions
            holder.backbutton.setOnClickListener{
                closeMenu()
            }
            holder.addappbutton.setOnClickListener { dragAndDropApp(pos, it.context) }
            holder.infobutton.setOnClickListener {
                closeMenu()
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", viewMoo.appsList.value!![pos].packageName, null)
                intent.data = uri
                it.context.startActivity(intent)
            }
        }
    }


    fun showMenu(position: Int) {
        idOtvorenogMenija = position
        notifyDataSetChanged()
    }


    fun isMenuShown(): Boolean {
        return (idOtvorenogMenija != -1)
    }

    fun closeMenu() {
        idOtvorenogMenija = -1
        notifyDataSetChanged()
    }
}