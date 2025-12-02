package com.ingokodba.dragnav

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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dragnav.R
import com.example.dragnav.databinding.ApplicationRowBindingBinding
import com.ingokodba.dragnav.modeli.AppInfo
import com.ingokodba.dragnav.modeli.MessageEvent
import com.ingokodba.dragnav.modeli.MessageEventType
import org.greenrobot.eventbus.EventBus

class ApplicationsListAdapter(viewModel: ViewModel, designEnum: UiDesignEnum) :
    ListAdapter<AppInfo, RecyclerView.ViewHolder>(DiffCallback) {

    var viewModel: ViewModel = viewModel
    var designEnum: UiDesignEnum = designEnum
    var idOtvorenogMenija:Int = -1
    var showAddToHomescreen: Boolean = true

    private val SHOW_MENU = 1
    private val HIDE_MENU = 2

    //Our menu view
    class MenuViewHolder(view: View) : RecyclerView.ViewHolder(view){
        var infobutton:LinearLayout = view.findViewById(R.id.appinfobutton)
        var addappbutton:LinearLayout = view.findViewById(R.id.addappbutton)
        var favoritebutton:LinearLayout = view.findViewById(R.id.favoritebutton)
        var favoriteimage:ImageView = view.findViewById(R.id.favoriteimage)
    }

    /**
     * The MarsPhotosViewHolder constructor takes the binding variable from the associated
     * GridViewItem, which nicely gives it access to the full [MarsPhoto] information.
     */
    class MarsPhotosViewHolder(
        private var binding: ApplicationRowBindingBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        var textView: TextView
        var img: ImageView
        init {
            textView = itemView.findViewById(R.id.text) as TextView
            img = itemView.findViewById(R.id.img) as ImageView
        }

        fun bind(aplikacija: AppInfo, viewModel: ViewModel) {
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
                ApplicationRowBindingBinding.inflate(LayoutInflater.from(parent.context))
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

    fun dragAndDropApp(pos:Int, appInfo: AppInfo){
        //Toast.makeText(context, context.getString(R.string.drag_and_drop_app), Toast.LENGTH_SHORT).show()
        EventBus.getDefault().post(MessageEvent(appInfo.label, pos, appInfo.packageName, appInfo.color, type = MessageEventType.DRAG_N_DROP, app = appInfo))
    }

    /**
     * Replaces the contents of a view (invoked by the layout manager)
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        val appInfo: AppInfo
        try{
            appInfo = getItem(pos)
        }catch (e: ArrayIndexOutOfBoundsException){
            return
        }
        holder.itemView.visibility = if(appInfo.visible) View.VISIBLE else View.GONE
        if (holder is MarsPhotosViewHolder) {
            holder.bind(appInfo, viewModel)
            if(appInfo.color != "") holder.textView.setBackgroundColor(appInfo.color.toInt())
            holder.textView.text = if(designEnum != UiDesignEnum.KEYPAD) appInfo.label else appInfo.label + " - " + pos
            holder.itemView.setOnLongClickListener{ v ->
                EventBus.getDefault().post(MessageEvent(appInfo.label, pos, appInfo.packageName, appInfo.color, type = MessageEventType.LONG_HOLD, app = appInfo))
                //showMenu(pos)
                return@setOnLongClickListener true
            }
            holder.itemView.setOnClickListener { v ->
                val context = v.context
                val launchIntent: Intent? =
                    context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
                if(launchIntent != null) {
                    EventBus.getDefault().post(MessageEvent(appInfo.label, pos, appInfo.packageName, appInfo.color, app=appInfo, type = MessageEventType.LAUNCH_APP))
                } else {
                    Log.d("ingo", "No launch intent")
                }
                //context.startActivity(launchIntent)

                Log.d("ingo", "id grr rv klika je " + appInfo.id.toString())
            }
        }

        if (holder is MenuViewHolder) {
            holder.favoriteimage.setImageResource((if (!appInfo.favorite) R.drawable.star_empty else R.drawable.star_fill))
            //Menu Actions
            holder.favoritebutton.setOnClickListener{
                toggleFavorite(pos, appInfo)
            }
            holder.addappbutton.setOnClickListener { dragAndDropApp(pos, appInfo) }
            holder.infobutton.setOnClickListener {
                closeMenu()
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", appInfo.packageName, null)
                intent.data = uri
                it.context.startActivity(intent)
            }
        }
    }

    // return the size of languageList
    /*override fun getItemCount(): Int {
        return viewModel.appsListFiltered.value!!.size
    }*/

    fun toggleFavorite(pos: Int, appInfo: AppInfo){
        appInfo.favorite = !appInfo.favorite
        notifyItemChanged(pos)
        EventBus.getDefault().post(MessageEvent(appInfo.label, pos, appInfo.packageName, appInfo.color, type = MessageEventType.FAVORITE, app = appInfo))
    }

    fun showMenu(position: Int) {
        if(position == idOtvorenogMenija){
            closeMenu()
        } else {
            val tmp = idOtvorenogMenija
            idOtvorenogMenija = position
            if (tmp != -1) notifyItemChanged(tmp)
            notifyItemChanged(idOtvorenogMenija)
        }
    }


    fun isMenuShown(): Boolean {
        return (idOtvorenogMenija != -1)
    }

    fun closeMenu() {
        val tmp = idOtvorenogMenija
        idOtvorenogMenija = -1
        if(tmp != -1) notifyItemChanged(tmp)
    }
}