package com.owncloud.android.ui.preview

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class CoroutinesTask<Params, Progress, Result> {

    protected var isCancelled = false

    protected open fun onPreExecute(){}

    protected abstract fun doInBackground(vararg params: Params?): Result

    open fun onProgressUpdate(vararg progress: Progress?) {}

    protected open fun onPostExecute(result: Result?) {}

    fun execute(vararg params: Params?)  {
        GlobalScope.launch(Dispatchers.Default) {
            val result = doInBackground(params[0])

            withContext(Dispatchers.Main){
                onPostExecute(result)
            }
        }
    }

    protected fun publishProgress(vararg progress: Progress?){
        GlobalScope.launch(Dispatchers.Main) {
            onProgressUpdate(progress[0])
        }
    }

    fun cancel(mayInterruptIfRunning: Boolean) {}


}