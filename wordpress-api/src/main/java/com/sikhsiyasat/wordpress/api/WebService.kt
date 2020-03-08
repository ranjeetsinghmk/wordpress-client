package com.sikhsiyasat.wordpress.api

interface WebService {
    fun getPost(slug: String): Observable<ApiResponse<Post>>

    fun getPosts(
        page: Int = 0,
        perPage: Int = 10,
        context: String = "embed"
    ): Observable<ApiResponse<List<Post>>>
}

class Observable<T> {
    private var limit = Int.MAX_VALUE
    private val observers: MutableList<ObservableObserver<T>> = ArrayList()
    private lateinit var onSubscribe: ObservableOnSubscribe<T>

    fun subscribe(
        observer: ObservableObserver<T>
    ) {
        if (observers.size > limit) {
            throw RuntimeException("Limit exceeded")
        }

        observers.add(observer)

        onSubscribe.subscribe(object : ObservableEmitter<T> {
            override fun onComplete() {
                observers.forEach { it.onComplete() }
            }

            override fun onNext(t: T) {
                observers.forEach { it.onSubscribe(t) }
            }

            override fun onError(error: ApiError) {
                observers.forEach { it.onError(error) }
            }
        })
    }

    companion object {
        fun <T> create(subscribe: ObservableOnSubscribe<T>): Observable<T> {
            val observable = Observable<T>()
            observable.onSubscribe = subscribe
            return observable
        }
    }
}

interface Observer<T> {
    fun onChanged(t: T)
}


interface ObservableObserver<T> {
    fun onSubscribe(d: T)

    fun onComplete()

    fun onError(e: ApiError)
}

interface ObservableEmitter<T> {
    fun onComplete()
    fun onNext(t: T)
    fun onError(error: ApiError)
}

interface ObservableOnSubscribe<T> {
    fun subscribe(emitter: ObservableEmitter<T>)
}
