package com.sikhsiyasat.wordpress

import androidx.lifecycle.LiveData
import com.sikhsiyasat.wordpress.api.Post
import com.sikhsiyasat.wordpress.api.TermTaxonomy
import com.sikhsiyasat.wordpress.models.*

class LocalStorageService constructor(
    private val postDao: PostDao,
    private val authorDao: AuthorDao,
    private val categoryDao: CategoryDao,
    private val tagDao: TagDao,
    private val featuredMediaDao: FeaturedMediaDao
) {

    //Posts
    fun getPost(postUrl: String): LiveData<PostEntity?> {
        return postDao.loadByLink(postUrl)
    }

    fun savePosts(posts: List<Post>) {
        postDao.save(
            posts
                .mapNotNull { PostMapper.postEntity(it) }
                .distinctBy { it.id }
                .toSet()
        )
        authorDao.save(
            posts.mapNotNull { it.embeddedData }
                .flatMap { it.author }
                .map { PostMapper.authorEntity(it) }
                .distinctBy { it.id }
                .toSet()
        )
        categoryDao.save(
            posts.mapNotNull { it.embeddedData }
                .flatMap { it.terms }
                .flatten()
                .filter { it.taxonomy == TermTaxonomy.category }
                .map { PostMapper.categoryEntity(it) }
                .distinctBy { it.id }
                .toSet()
        )
        tagDao.save(
            posts.mapNotNull { it.embeddedData }
                .flatMap { it.terms }
                .flatten()
                .filter { it.taxonomy == TermTaxonomy.post_tag }
                .map { PostMapper.tagEntity(it) }
                .distinctBy { it.id }
                .toSet()
        )
        featuredMediaDao.save(
            posts.mapNotNull { it.embeddedData }
                .map { it.featuredMedia }
                .flatten()
                .map { PostMapper.featuredMediaEntity(it) }
                .distinctBy { it.id }
                .toSet()
        )
    }

    fun getPosts(websiteUrl: String): LiveData<List<PostEntity>> =
        postDao.loadWhereLinkLike("$websiteUrl%")

    fun getAuthors(ids: List<String>): LiveData<List<AuthorEntity>> = authorDao.load(ids)

    fun getCategories(ids: List<String>): LiveData<List<CategoryEntity>> = categoryDao.load(ids)

    fun getTags(ids: List<String>): LiveData<List<TagEntity>> = tagDao.load(ids)

    fun getFeaturedMedia(ids: List<String>) = featuredMediaDao.load(ids)
}
