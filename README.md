# Wordpress-client

- A WordPress client with news app theme


## Features

### Posts listing
- Listing the posts using pagination
- Filter posts by categories
- Filter posts by tags
- Search posts by keyword

### Post details page with TextToSpeech, Bookmarks functions
- TextToSpeech: total news is separated into paragraphs and then sentences
- Font options
- Bookmarks [Client is responsible for data storage]
- Share option


### Fully customizable theme [Not available yet]
- Position of several widgets can be made customizable


## WordPress REST API Integration

This library integrates with WordPress REST API v2. For full API documentation, see:
https://developer.wordpress.org/rest-api/

### Supported Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /wp/v2/posts` | List posts with pagination |
| `GET /wp/v2/posts?slug={slug}` | Get post by slug |
| `GET /wp/v2/posts?categories={id}` | Get posts by category |
| `GET /wp/v2/posts?tags={id}` | Get posts by tag |
| `GET /wp/v2/posts?search={term}` | Search posts |
| `GET /wp/v2/categories` | List all categories |
| `GET /wp/v2/tags` | List all tags |
| `GET /wp/v2/search` | Search across content |

### Usage Example

```kotlin
// Create a web service instance for your WordPress site
val webClient = AppScope.webClient
val webService = webClient.webService("https://your-wordpress-site.com")

// Get posts
webService.getPosts(page = 1, perPage = 10)
    .subscribe(object : ObservableObserver<ApiResponse<List<Post>>> {
        override fun onSubscribe(response: ApiResponse<List<Post>>) {
            when (response) {
                is ApiResponse.Success -> {
                    val posts = response.data
                    // Handle posts
                }
                is ApiResponse.Error -> {
                    val error = response.error
                    // Handle error
                }
            }
        }
        override fun onComplete() { }
        override fun onError(e: ApiError) { }
    })

// Get categories
webService.getCategories()
    .subscribe(object : ObservableObserver<ApiResponse<List<Category>>> {
        override fun onSubscribe(response: ApiResponse<List<Category>>) {
            when (response) {
                is ApiResponse.Success -> {
                    val categories = response.data
                    // Handle categories
                }
                is ApiResponse.Error -> { }
            }
        }
        override fun onComplete() { }
        override fun onError(e: ApiError) { }
    })

// Search posts
webService.searchPosts("keyword")
    .subscribe(object : ObservableObserver<ApiResponse<List<Post>>> {
        override fun onSubscribe(response: ApiResponse<List<Post>>) {
            // Handle search results
        }
        override fun onComplete() { }
        override fun onError(e: ApiError) { }
    })
```

### API Response Models

- `Post` - WordPress post with title, content, excerpt, author, categories, tags, and featured media
- `Category` - WordPress category with name, slug, description, and post count
- `Tag` - WordPress tag with name, slug, description, and post count
- `Author` - WordPress user/author with name, avatar, and bio
- `SearchResult` - Search result with title, URL, and content type

Please refer to the [Wiki](https://github.com/ranjeetsinghmk/wordpress-client/wiki) for more information.

