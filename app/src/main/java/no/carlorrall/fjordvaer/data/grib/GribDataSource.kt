package no.carlorrall.fjordvaer.data.grib

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

private const val TAG = "GribDataSource"

/** The three GRIB products MET publishes for the Oslofjord. */
enum class GribContent(val apiName: String) {
    WEATHER("weather"),
    CURRENT("current"),
    WAVES("waves")
}

/**
 * Downloads GRIB files from api.met.no/weatherapi/gribfiles and keeps the newest one per
 * product on disk. A file younger than [maxAgeMillis] is reused instead of downloaded again,
 * and older files are deleted.
 */
class GribDataSource(
    private val client: OkHttpClient,
    private val directory: File,
    private val maxAgeMillis: Long = 60 * 60 * 1000L,
    private val clock: () -> Long = System::currentTimeMillis
) {
    suspend fun getFile(content: GribContent): File = withContext(Dispatchers.IO) {
        directory.mkdirs()
        newestFile(content)
            ?.takeIf { clock() - it.lastModified() < maxAgeMillis }
            ?.let { return@withContext it }

        val request = Request.Builder()
            .url("https://api.met.no/weatherapi/gribfiles/1.1/?area=oslofjord&content=${content.apiName}")
            .header("Accept", "application/x-grib")
            .build()

        val target = File(directory, "oslofjord_${content.apiName}_${clock()}.grb")
        val temp = File(directory, target.name + ".part")
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("GRIB download failed: HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty GRIB response")
            temp.outputStream().use { out -> body.byteStream().use { it.copyTo(out) } }
        }
        if (!temp.renameTo(target)) throw IOException("Could not save GRIB file")
        deleteAllExcept(content, target)
        Log.d(TAG, "Downloaded ${target.name} (${target.length() / 1024} kB)")
        target
    }

    private fun filesFor(content: GribContent): List<File> =
        directory.listFiles { file -> file.name.startsWith("oslofjord_${content.apiName}_") }?.toList().orEmpty()

    private fun newestFile(content: GribContent): File? =
        filesFor(content).filter { it.name.endsWith(".grb") }.maxByOrNull { it.lastModified() }

    private fun deleteAllExcept(content: GribContent, keep: File) {
        filesFor(content)
            .filterNot { it.name == keep.name }
            .forEach { it.delete() }
    }
}
