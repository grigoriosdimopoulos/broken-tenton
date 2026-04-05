package com.linkedinautomation.automation.scripts

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsScriptLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cache = mutableMapOf<String, String>()

    fun load(assetPath: String): String = cache.getOrPut(assetPath) {
        context.assets.open(assetPath).bufferedReader().use { it.readText() }
    }

    fun loadAndSubstitute(assetPath: String, vars: Map<String, String>): String {
        var script = load(assetPath)
        vars.forEach { (key, value) ->
            script = script.replace("{{$key}}", value.replace("'", "\\'"))
        }
        return script
    }
}
