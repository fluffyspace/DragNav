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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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

    var viewModel: NewRAdapterViewModel = viewModel
    var idOtvorenogMenija:Int = -1

    private val SHOW_MENU = 1
    private val HIDE_MENU = 2

    //Our menu view
    class MenuViewHolder(view: View) : RecyclerView.ViewHolder(view){
        var infobutton:LinearLayout = view.findViewById(R.id.appinfobutton)
        var addappbutton:LinearLayout = view.findViewById(R.id.addappbutton)
        var backbutton:LinearLayout = view.findViewById(R.id.backbutton)

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
        //Toast.makeText(context, context.getString(R.string.drag_and_drop_app), Toast.LENGTH_SHORT).show()
        EventBus.getDefault().post(MessageEvent(viewModel.appsList.value!![pos].label, pos, viewModel.appsList.value!![pos].packageName, viewModel.appsList.value!![pos].color, draganddrop = true, app = viewModel.appsList.value!![pos]))
    }

    /**
     * Replaces the contents of a view (invoked by the layout manager)
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        val appInfo: AppInfo = getItem(pos)
        if (holder is MarsPhotosViewHolder) {
            holder.bind(appInfo, viewModel)
            if(viewModel.appsList.value!![pos].color != "") holder.textView.setBackgroundColor(viewModel.appsList.value!![pos].color.toInt())
            holder.itemView.setOnLongClickListener{ v ->
                showMenu(pos)
                return@setOnLongClickListener true
            }
            holder.itemView.setOnClickListener { v ->
                val context = v.context
                val launchIntent: Intent? =
                    context.packageManager.getLaunchIntentForPackage(viewModel.appsList.value!![pos].packageName)
                if(launchIntent != null) {
                    EventBus.getDefault().post(MessageEvent(viewModel.appsList.value!![pos].label, pos, viewModel.appsList.value!![pos].packageName, viewModel.appsList.value!![pos].color, app=viewModel.appsList.value!![pos]))
                } else {
                    Log.d("ingo", "No launch intent")
                }
                //context.startActivity(launchIntent)

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
                val uri = Uri.fromParts("package", viewModel.appsList.value!![pos].packageName, null)
                intent.data = uri
                it.context.startActivity(intent)
            }
        }
    }

    // return the size of languageList
    override fun getItemCount(): Int {
        return viewModel.appsList.value!!.size
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