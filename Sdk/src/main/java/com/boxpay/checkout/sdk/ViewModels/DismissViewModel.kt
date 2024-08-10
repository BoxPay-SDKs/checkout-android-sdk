import androidx.lifecycle.ViewModel

class DismissViewModel : ViewModel() {
    // LiveData to observe when the child bottom sheet is dismissed
    private val _childDismissed = SingleLiveEvent<Unit>()
    val childDismissed get() = _childDismissed

    // Function to notify ViewModel when child bottom sheet is dismissed
    fun onChildDismissed() {
        _childDismissed.call()
    }
}
