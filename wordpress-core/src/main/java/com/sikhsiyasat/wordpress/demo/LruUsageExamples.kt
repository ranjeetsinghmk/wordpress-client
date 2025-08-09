package com.sikhsiyasat.wordpress.demo

/**
 * Demonstration of LRU Post Management Usage
 * 
 * This file shows how the LRU functionality would be integrated
 * into the WordPress client application.
 */

/*

// Example 1: Automatic LRU tracking in Activity/Fragment
class PostDetailActivity : AppCompatActivity() {
    private lateinit var repository: WordpressRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val postUrl = intent.getStringExtra("POST_URL") ?: return
        
        // When user views a post, it's automatically tracked as accessed
        repository.getPost(postUrl).observe(this) { post ->
            // Post is automatically marked as accessed
            // No manual tracking needed
            displayPost(post)
        }
    }
}

// Example 2: Background LRU cleanup in Application class
class WordpressApplication : Application() {
    private lateinit var repository: WordpressRepository
    
    override fun onCreate() {
        super.onCreate()
        
        // Perform cleanup on app start (background thread)
        GlobalScope.launch(Dispatchers.IO) {
            performMaintenanceCleanup()
        }
    }
    
    private suspend fun performMaintenanceCleanup() {
        // Option 1: Simple cleanup if cache is too large
        val deletedCount = repository.performLruCleanupIfNeeded()
        Log.d("Cleanup", "Deleted $deletedCount posts due to cache size")
        
        // Option 2: Delete old posts (older than 30 days)
        val oldPostsDeleted = repository.deleteOldPosts()
        Log.d("Cleanup", "Deleted $oldPostsDeleted old posts")
        
        // Option 3: Comprehensive cleanup (recommended)
        val (ageDeletions, lruDeletions) = repository.performComprehensiveCleanup()
        Log.d("Cleanup", "Comprehensive cleanup: $ageDeletions age-based, $lruDeletions LRU-based")
    }
}

// Example 3: Periodic cleanup with WorkManager
class LruCleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val repository = DI.wordpressRepository
            
            val postCountBefore = repository.getCachedPostCount()
            val (ageDeletions, lruDeletions) = repository.performComprehensiveCleanup()
            val postCountAfter = repository.getCachedPostCount()
            
            Log.i("LruCleanup", "Cleanup completed: " +
                "$postCountBefore -> $postCountAfter posts " +
                "($ageDeletions age-based, $lruDeletions LRU-based deletions)")
            
            Result.success()
        } catch (e: Exception) {
            Log.e("LruCleanup", "Cleanup failed", e)
            Result.retry()
        }
    }
}

// Example 4: Setting up periodic cleanup
class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Schedule periodic cleanup (daily)
        val cleanupRequest = PeriodicWorkRequestBuilder<LruCleanupWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
            
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "lru_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }
}

// Example 5: Manual cleanup in Settings
class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var repository: WordpressRepository
    
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        
        findPreference<Preference>("clear_cache")?.setOnPreferenceClickListener {
            clearCacheWithConfirmation()
            true
        }
    }
    
    private fun clearCacheWithConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear Cache")
            .setMessage("This will remove old and rarely accessed posts. Continue?")
            .setPositiveButton("Clear") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val deletedCount = repository.performComprehensiveCleanup()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context, 
                            "Cleared ${deletedCount.first + deletedCount.second} posts",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

*/

/**
 * Key Benefits of this LRU Implementation:
 * 
 * 1. **Automatic Tracking**: Posts are automatically marked as accessed when viewed
 * 2. **Flexible Cleanup**: Multiple strategies (age-based, count-based, comprehensive)
 * 3. **Performance**: Efficient database queries with proper indexing
 * 4. **Background Processing**: Can be easily integrated with WorkManager for periodic cleanup
 * 5. **User Control**: Can provide manual cleanup options in settings
 * 6. **Memory Management**: Prevents unlimited growth of local database
 * 7. **Smart Retention**: Frequently accessed posts stay longer in cache
 * 
 * Configuration Options:
 * - MAX_POSTS: Maximum number of posts to keep (default: 1000)
 * - LRU_CLEANUP_COUNT: Number of posts to delete in one cleanup (default: 100) 
 * - MAX_AGE_DAYS: Maximum age in days before deletion (default: 30)
 * 
 * All these can be customized based on device storage and user preferences.
 */