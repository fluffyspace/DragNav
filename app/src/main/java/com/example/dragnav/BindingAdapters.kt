package com.example.dragnav

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dragnav.modeli.AppInfo

@BindingAdapter("listApps")
fun listRecipeData(recyclerView: RecyclerView,
                   data: List<AppInfo>?) {
    val adapter = recyclerView.adapter as NewRAdapter
    adapter.submitList(data)
}