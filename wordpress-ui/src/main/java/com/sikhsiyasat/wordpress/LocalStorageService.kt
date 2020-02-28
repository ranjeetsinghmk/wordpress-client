package com.sikhsiyasat.wordpress

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.sikhsiyasat.wordpress.models.Post
import com.sikhsiyasat.wordpress.models.PostDao
import com.sikhsiyasat.wordpress.models.PostEntity

//TODO move to core
class LocalStorageService constructor(
    private val postDao: PostDao
) {
    fun getPost(postUrl: String): LiveData<Post?> {
        return Transformations.map(postDao.loadByLink(postUrl)) { postEntity ->
            postEntity?.let { it ->
                Post(
                    it
                )
            }
        }
    }

    fun savePosts(posts: List<Post>) {
        postDao.save(posts.map { PostEntity(it) })
    }

    fun getPosts(websiteUrl: String): LiveData<List<Post>> {
        return Transformations.map(
            postDao.loadWhereLinkLike("$websiteUrl%")
        ) { posts ->
            posts.map { postEntity ->
                Post(postEntity)
            }
        }
    }
}
