import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.test.espresso.IdlingResource

class LiveDataIdlingResource(private val liveData: LiveData<String>) : IdlingResource {

    @Volatile
    private var callback: IdlingResource.ResourceCallback? = null

    private val observer = Observer<String> {
        if (!it.isNullOrEmpty()) {
            callback?.onTransitionToIdle()
        }
    }

    override fun getName(): String {
        return LiveDataIdlingResource::class.java.name
    }

    override fun isIdleNow(): Boolean {
        val idle = !liveData.value.isNullOrEmpty()
        if (idle) {
            callback?.onTransitionToIdle()
        }
        return idle
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.callback = callback
        liveData.observeForever(observer)
    }

    fun unregisterIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        liveData.removeObserver(observer)
    }
}
