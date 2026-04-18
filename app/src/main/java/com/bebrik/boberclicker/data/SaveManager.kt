package com.bebrik.boberclicker.data

import android.content.Context
import com.google.gson.Gson
import java.io.File

object SaveManager {
    private const val SAVE_FILE = "save.json"
    private val gson = Gson()

    fun save(context: Context, data: GameSave) {
        try {
            data.lastSaveTime = System.currentTimeMillis()
            val json = gson.toJson(data)
            File(context.filesDir, SAVE_FILE).writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun load(context: Context): GameSave {
        return try {
            val file = File(context.filesDir, SAVE_FILE)
            if (file.exists()) gson.fromJson(file.readText(), GameSave::class.java) ?: GameSave()
            else GameSave()
        } catch (e: Exception) {
            e.printStackTrace()
            GameSave()
        }
    }

    fun deleteSave(context: Context) {
        File(context.filesDir, SAVE_FILE).delete()
    }
}
