package com.example.data.repository

import android.content.Context
import com.example.data.local.model.Cim10Disease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Read-only access to the bundled French CIM-10 disease dictionary. */
class Cim10Repository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private var diseasesCache: List<Cim10Disease>? = null

    suspend fun getDiseases(): List<Cim10Disease> = withContext(Dispatchers.IO) {
        diseasesCache ?: context.assets.open("cim10_simple.json").bufferedReader().use { reader ->
            json.decodeFromString<List<Cim10Disease>>(reader.readText()).also { diseasesCache = it }
        }
    }
}