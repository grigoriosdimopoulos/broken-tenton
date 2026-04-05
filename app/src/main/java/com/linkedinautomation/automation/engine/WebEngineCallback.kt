package com.linkedinautomation.automation.engine

interface WebEngineCallback {
    fun onPageLoaded(url: String)
    fun onJsResult(tag: String, result: String)
    fun onJsError(tag: String, error: String)
    fun onFileChooserRequested(callback: (String?) -> Unit)
    fun onBlockedNavigation(url: String)
}
