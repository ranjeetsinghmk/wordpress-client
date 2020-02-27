package com.sikhsiyasat.wordpress.demo

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.sikhsiyasat.wordpress.detail.PostFragment
import com.sikhsiyasat.wordpress.list.PostsFragment
import com.sikhsiyasat.wordpress.models.Post

class MainActivity : AppCompatActivity() {

    private val interactionListener = object : PostsFragment.InteractionListener {
        override fun goToPostDetailPage(post: Post) {
            post.link?.let { postLink ->
                addFragment(PostFragment.newInstance(postLink))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val postsFragment = PostsFragment.newInstance(
            "",
            interactionListener
        )

        addFragment(postsFragment)
    }

    private fun addFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .add(
                R.id.fragment_container,
                fragment
            )
            .addToBackStack(fragment.tag)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
