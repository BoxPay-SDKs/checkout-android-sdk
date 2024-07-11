import android.util.Log

class FailureScreenSharedViewModel(val openFailureScreen : () -> Unit) {
    fun openFailureScreenPrivate(result : String){
        Log.d("Failure Screen View Model","openFailureScreenPrivate")
        openFailureScreen()
    }
}