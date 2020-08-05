package com.sikhsiyasat.wordpress.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sikhsiyasat.wordpress.R
import com.sikhsiyasat.wordpress.models.TermEntity
import kotlinx.android.synthetic.main.tag_layout.view.*

class TermsAdapter(val context: Context, private val terms: List<TermEntity>, private val itemClickListener: ItemClickListener)
    : RecyclerView.Adapter<TermsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(LayoutInflater.from(context).inflate(R.layout.tag_layout, parent, false))

    override fun getItemCount(): Int = terms.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tagNameView.text = terms[position].name
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tagNameView: TextView = view.tag_name

        init {
            view.tag_name.setOnClickListener { itemClickListener.onClicked(terms[adapterPosition]) }
        }
    }

    interface ItemClickListener {
        fun onClicked(termEntity: TermEntity)
    }
}