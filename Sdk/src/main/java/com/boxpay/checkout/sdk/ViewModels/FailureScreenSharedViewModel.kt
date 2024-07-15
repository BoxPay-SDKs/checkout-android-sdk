class FailureScreenSharedViewModel(val openFailureScreen : () -> Unit) {
    fun openFailureScreenPrivate(result : String){
        openFailureScreen()
    }
}