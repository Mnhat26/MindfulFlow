package com.example.dacs3.data

import com.example.dacs3.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class SupabaseStorageRepository {
    private val supabaseUrl = BuildConfig.SUPABASE_URL.trim()
    private val supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY.trim()
    private val bucketName = BuildConfig.SUPABASE_BUCKET_NAME.trim()

    private suspend fun uploadFileInternal(
        pathPrefix: String,
        fileBytes: ByteArray,
        contentType: String,
        extension: String
    ): String = withContext(Dispatchers.IO) {
        require(supabaseUrl.isNotBlank()) { "SUPABASE_URL is not configured" }
        require(supabaseAnonKey.isNotBlank()) { "SUPABASE_ANON_KEY is not configured" }
        require(bucketName.isNotBlank()) { "SUPABASE_BUCKET_NAME is not configured" }

        val objectPath = "$pathPrefix/${UUID.randomUUID()}.$extension"
        val uploadUrl = "$supabaseUrl/storage/v1/object/$bucketName/$objectPath"
        val publicUrl = "$supabaseUrl/storage/v1/object/public/$bucketName/$objectPath"

        val connection = java.net.URL(uploadUrl).openConnection() as java.net.HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $supabaseAnonKey")
            connection.setRequestProperty("apikey", supabaseAnonKey)
            connection.setRequestProperty("Content-Type", contentType)
            connection.setRequestProperty("x-upsert", "true")
            connection.outputStream.use { it.write(fileBytes) }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException("Supabase upload failed ($responseCode): $errorBody")
            }
            publicUrl
        } finally {
            connection.disconnect()
        }
    }

    suspend fun uploadGroupAvatarFile(groupId: String, file: File): String {
        return uploadFileInternal("group-avatars/$groupId", file.readBytes(), "image/jpeg", "jpg")
    }

    suspend fun uploadUserAvatar(userId: String, file: File): String {
        return uploadFileInternal("user-avatars/$userId", file.readBytes(), "image/jpeg", "jpg")
    }

    suspend fun uploadChatFile(groupId: String, file: File, contentType: String): String {
        val extension = file.extension.ifBlank { 
            when {
                contentType.contains("image") -> "jpg"
                contentType.contains("video") -> "mp4"
                else -> "bin"
            }
        }
        return uploadFileInternal("chat-files/$groupId", file.readBytes(), contentType, extension)
    }
}
