package com.rajon.moneytracker.export

import com.google.gson.GsonBuilder
import com.rajon.moneytracker.data.model.FullBackupPayload
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object JsonBackupManager {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun exportToJson(payload: FullBackupPayload, file: File): Boolean {
        return try {
            FileWriter(file).use { writer ->
                gson.toJson(payload, writer)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun importFromJson(file: File): FullBackupPayload? {
        return try {
            FileReader(file).use { reader ->
                gson.fromJson(reader, FullBackupPayload::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }
}
