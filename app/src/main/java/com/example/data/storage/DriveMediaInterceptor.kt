package com.example.data.storage

import coil.intercept.Interceptor
import coil.request.ImageResult
import com.example.data.telegram.DriveLog
import java.io.File

/**
 * Makes every existing `AsyncImage` call site drive-aware without touching one
 * of them.
 *
 * Patient images are rendered all over the app from an absolute path held in the
 * database. When Telegram Drive is the backend and the bytes have been evicted
 * from the local cache, that path points at nothing and Coil would render an
 * error. This interceptor spots exactly that case, pulls the object down through
 * [PatientMediaGateway], and hands Coil a request pointing at the fetched file.
 *
 * Deliberately narrow: it only acts on paths that resolve to a patient media
 * [StorageKey] and are genuinely missing. Everything else — network URLs, asset
 * figures, the surgeon's logo, an image that is already on disk — passes through
 * untouched, so the common path costs one `File.exists()` check.
 */
class DriveMediaInterceptor(
    private val gateway: PatientMediaGateway
) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val path = when (val data = request.data) {
            is File -> data.absolutePath
            is String -> data.takeIf { it.startsWith('/') }
            else -> null
        } ?: return chain.proceed(request)

        if (gateway.isLocallyAvailable(path)) return chain.proceed(request)
        if (gateway.keyFor(path) == null) return chain.proceed(request)

        val fetched = runCatching { gateway.resolve(path) }
            .onFailure { DriveLog.w(TAG, "Could not fetch image for display", it) }
            .getOrNull()
            ?: return chain.proceed(request)

        DriveLog.d(TAG, "Served ${fetched.name} from the drive")
        return chain.proceed(request.newBuilder().data(fetched).build())
    }

    companion object {
        private const val TAG = "DriveMediaInterceptor"
    }
}
