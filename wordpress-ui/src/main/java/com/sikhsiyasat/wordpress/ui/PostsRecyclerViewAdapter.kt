package com.sikhsiyasat.wordpress.ui


import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sikhsiyasat.wordpress.R
import com.sikhsiyasat.wordpress.models.Post
import kotlinx.android.synthetic.main.post.view.*
import java.text.SimpleDateFormat

class PostsRecyclerViewAdapter(
    private val posts: List<Post>,
    private val postsInteractionListener: PostsInteractionListener
) :
    RecyclerView.Adapter<PostsRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val post = v.tag as Post
            postsInteractionListener.onSelected(post)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]
        holder.mTitleView.text = Html.fromHtml(post.title?.rendered).toString()
        holder.mPublishDate.text =
            post.date?.let { SimpleDateFormat("MMM dd, yyyy").format(it) } ?: ""
        holder.mAuthor.text = "Sikh Siyasat"

        with(holder.mView) {
            tag = post
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = posts.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mTitleView: TextView = mView.post_title
        val mPublishDate: TextView = mView.published_date
        val mAuthor: TextView = mView.post_author
        val thumbnailView: ImageView = mView.post_thumbnail

        override fun toString(): String {
            return super.toString() + " '" + mPublishDate.text + "'"
        }
    }
}

interface PostsInteractionListener {
    fun onSelected(post: Post)
}
