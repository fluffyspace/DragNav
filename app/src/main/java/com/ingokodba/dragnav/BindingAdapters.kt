package com.ingokodba.dragnav

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ingokodba.dragnav.modeli.AppInfo

@BindingAdapter("listApps")
fun listRecipeData(recyclerView: RecyclerView,
                   data: List<AppInfo>?) {
    val adapter = recyclerView.adapter as ApplicationsListAdapter
    adapter.submitList(data)
}