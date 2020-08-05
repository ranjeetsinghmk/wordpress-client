package com.sikhsiyasat.wordpress

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.sikhsiyasat.logger.SSLogger
import com.sikhsiyasat.wordpress.api.Post
import com.sikhsiyasat.wordpress.models.*

class LocalStorageService(
        private val postDao: PostDao,
        private val postContentDao: PostContentDao,
        private val postTermDao: PostTermDao,
        private val authorDao: AuthorDao,
        private val termDao: TermDao,
        private val featuredMediaDao: FeaturedMediaDao
) {

    private val log: SSLogger = SSLogger.getLogger("LocalStorageService")

    //Posts
    fun getPost(postUrl: String): LiveData<PostWithContent?> {
        return postDao.loadByLink(postUrl)
    }

    fun savePosts(posts: List<Post>) {
        posts
                .mapNotNull { PostMapper.postEntity(it) }
                .distinctBy { it.id }
                .toSet()
                .apply {
                    isNotEmpty()
                            .let {
                                postDao.save(this)
                            }
                }

        posts.mapNotNull { it.content?.let { it1 -> PostContentEntity(it.id!!, it1) } }
                .toSet()
                .apply {
                    isNotEmpty()
                            .let {
                                postContentDao.save(this)
                            }
                }

        posts.mapNotNull { it.embeddedData }
                .flatMap { it.author }
                .map { PostMapper.authorEntity(it) }
                .distinctBy { it.id }
                .toSet()
                .apply {
                    isNotEmpty()
                            .let {
                                authorDao.save(this)
                            }
                }

        posts.mapNotNull { it.embeddedData }
                .flatMap { it.terms }
                .flatten()
                .map { PostMapper.termEntity(it) }
                .distinctBy { it.id }
                .toSet()
                .apply {
                    isNotEmpty()
                            .let {
                                termDao.save(this)
                            }
                }

        posts.asSequence()
                .map { post ->
                    post.id?.let { postId ->
                        post.embeddedData
                                ?.terms
                                ?.flatten()
                                ?.map {
                                    PostTermEntity(
                                            postId,
                                            it.id
                                    )
                                }
                    }
                }
                .filterNotNull()
                .flatten()
                .toList()
                .apply {
                    isNotEmpty()
                            .let {
                                postTermDao.save(this)
                            }
                }

        posts.asSequence()
                .mapNotNull { it.embeddedData }
                .map { it.featuredMedia }
                .flatten()
                .map { PostMapper.featuredMediaEntity(it) }
                .distinctBy { it.id }
                .toSet()
                .apply {
                    isNotEmpty().let {
                        featuredMediaDao.save(this)
                    }
                }
    }

    fun getPosts(params: PostsSearchParams): LiveData<List<PostEntity>> {
        log.info("getPosts params {}", params)

        return Transformations.switchMap(postTermDao.findPostsByTermIdIn(
                params.categories.plus(params.tags)
        )) { postIds ->
            log.info("getPosts postIds from categories {}", postIds)

            postDao.findTopPosts(
                    "${params.websiteUrl}%",
                    postIds.toSet(),
                    params.authors,
                    emptySet()
            )
        }
    }

    fun getTopPosts(websiteUrl: String, tags: List<TermEntity>, categories: List<TermEntity>, author: AuthorEntity?, exceptIds: Set<String>): LiveData<List<PostEntity>> {
        log.info("getTopPosts for url {} with {} tags, {} categories, author {}", websiteUrl, tags.size, categories.size, author)

        return Transformations.switchMap(postTermDao.findPostsByTermIdIn(
                categories.map { it.id }
                        .plus(tags.map { it.id })
                        .toSet()
        )) { postIds ->
            log.info("getTopPosts postIds {}", postIds)

            postDao.findTopPosts(
                    "$websiteUrl%",
                    postIds.toSet(),
                    author?.id?.let { setOf(it) } ?: emptySet(),
                    exceptIds
            )
        }
    }

    fun getAuthors(ids: Set<String>): List<AuthorEntity> = authorDao.findByIdIn(ids)

    fun getTermsForPost(postIds: List<String>): Map<String, List<TermEntity>> {
        val postTerms = postTermDao.findByPostIds(postIds)
        val terms = termDao.findByIdIn(postTerms.map { it.termId }).associateBy { it.id }

        val postTermsGroup = postTerms.groupBy(keySelector = { it.postId }, valueTransform = { terms[it.termId] })

        return postTermsGroup.keys.associateBy({ it }, {
            postTermsGroup[it]?.filterNotNull() ?: emptyList()
        })
    }

    fun getFeaturedMediaByIdsNow(ids: List<String>) = featuredMediaDao.getByIdInNow(ids)
    fun getTermsByIds(ids: List<String>) = termDao.findByIdIn(ids)
}
