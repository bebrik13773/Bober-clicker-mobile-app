package com.bebrik.boberclicker.data

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpCookie
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    private const val TAG = "BoberApi"
    const val BASE = "https://bober-api.gt.tc"
    private const val TIMEOUT = 12_000

    private val cookieManager = CookieManager(null, CookiePolicy.ACCEPT_ALL).also {
        java.net.CookieHandler.setDefault(it)
    }

    // ----- PUBLIC API -----

    data class LoginResult(
        val success: Boolean,
        val message: String,
        val userId: Int = 0,
        val login: String = "",
        val score: Long = 0,
        val plus: Int = 1,
        val energyMax: Int = 5000,
        val energy: Float = 5000f,
        val lastEnergyUpdate: Long = System.currentTimeMillis(),
        val upgrades: UpgradeCounts = UpgradeCounts(),
        val skin: SkinState = SkinState()
    )

    data class SyncResult(
        val success: Boolean,
        val message: String = "",
        val score: Long = 0,
        val plus: Int = 1,
        val energyMax: Int = 5000,
        val energy: Float = 5000f,
        val lastEnergyUpdate: Long = System.currentTimeMillis(),
        val upgrades: UpgradeCounts = UpgradeCounts(),
        val skin: SkinState = SkinState(),
        val leaderboard: List<LeaderboardEntry> = emptyList()
    )

    fun login(login: String, password: String): LoginResult {
        return try {
            val body = JSONObject().apply {
                put("login", login)
                put("password", password)
            }
            val resp = post("/api/auth/login.php", body)
            if (!resp.optBoolean("success")) {
                return LoginResult(false, resp.optString("message", "Ошибка входа"))
            }
            LoginResult(
                success = true,
                message = resp.optString("message", ""),
                userId = resp.optInt("userId"),
                login  = resp.optString("login", login),
                score  = resp.optLong("score"),
                plus   = resp.optInt("plus", 1),
                energyMax = resp.optInt("ENERGY_MAX", 5000),
                energy    = resp.optDouble("energy", 5000.0).toFloat(),
                lastEnergyUpdate = resp.optLong("lastEnergyUpdate", System.currentTimeMillis()),
                upgrades = parseUpgrades(resp.optJSONObject("upgradePurchases")),
                skin     = parseSkin(resp.optString("skin"))
            )
        } catch (e: Exception) {
            Log.e(TAG, "login error", e)
            LoginResult(false, "Ошибка сети: ${e.message}")
        }
    }

    /** Full sync: sends current state, receives server state + leaderboard */
    fun sync(userId: Int, state: GameState): SyncResult {
        return try {
            val body = buildSavePayload(userId, state)
            val resp = post("/api/state/sync.php", body)

            val acc = resp.optJSONObject("account") ?: resp
            val lb  = parseLeaderboard(resp.optJSONObject("leaderboards"))

            SyncResult(
                success  = resp.optBoolean("success", true),
                message  = resp.optString("message", ""),
                score    = acc.optLong("score", state.score),
                plus     = acc.optInt("plus", state.plus),
                energyMax = acc.optInt("ENERGY_MAX", state.energyMax),
                energy    = acc.optDouble("energy", state.energy.toDouble()).toFloat(),
                lastEnergyUpdate = acc.optLong("lastEnergyUpdate", System.currentTimeMillis()),
                upgrades = parseUpgrades(acc.optJSONObject("upgradePurchases")) ,
                skin     = parseSkin(acc.optString("skin", "")),
                leaderboard = lb
            )
        } catch (e: Exception) {
            Log.e(TAG, "sync error", e)
            SyncResult(false, "Ошибка сети: ${e.message}")
        }
    }

    /** Restore session: call sync with userId=0 to check if cookie still valid */
    fun restoreSession(): SyncResult {
        return try {
            val resp = post("/api/state/sync.php", JSONObject())
            val acc = resp.optJSONObject("account")
            if (acc == null || acc.optInt("userId") == 0) {
                return SyncResult(false, "Сессия истекла")
            }
            SyncResult(
                success  = true,
                score    = acc.optLong("score"),
                plus     = acc.optInt("plus", 1),
                energyMax = acc.optInt("ENERGY_MAX", 5000),
                energy    = acc.optDouble("energy", 5000.0).toFloat(),
                lastEnergyUpdate = acc.optLong("lastEnergyUpdate", System.currentTimeMillis()),
                upgrades = parseUpgrades(acc.optJSONObject("upgradePurchases")),
                skin     = parseSkin(acc.optString("skin", "")),
                leaderboard = parseLeaderboard(resp.optJSONObject("leaderboards"))
            )
        } catch (e: Exception) {
            Log.e(TAG, "restoreSession error", e)
            SyncResult(false, "Нет сети")
        }
    }

    fun logout() {
        try { post("/api/auth/logout.php", JSONObject()) } catch (_: Exception) {}
        cookieManager.cookieStore.removeAll()
    }

    fun restoreCookies(raw: String) {
        if (raw.isBlank()) return
        try {
            val uri = URL(BASE).toURI()
            raw.split(";").forEach { part ->
                val kv = part.trim()
                if (kv.contains("=")) {
                    val cookie = HttpCookie(kv.substringBefore("=").trim(), kv.substringAfter("=").trim())
                    cookie.path = "/"
                    cookie.domain = "bober-api.gt.tc"
                    cookieManager.cookieStore.add(uri, cookie)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "restoreCookies: $e")
        }
    }

    fun exportCookies(): String {
        return try {
            val uri = URL(BASE).toURI()
            cookieManager.cookieStore.get(uri).joinToString("; ") { "${it.name}=${it.value}" }
        } catch (_: Exception) { "" }
    }

    // ----- HELPERS -----

    private fun buildSavePayload(userId: Int, s: GameState): JSONObject {
        val u = s.upgrades
        return JSONObject().apply {
            put("userId", userId)
            put("score", s.score)
            put("plus", s.plus)
            put("skin", JSONObject().apply {
                put("equippedSkinId", s.skin.equippedSkinId)
                put("ownedSkinIds", JSONArray(s.skin.ownedSkinIds))
            }.toString())
            put("energy", s.energy.toInt())
            put("lastEnergyUpdate", s.lastEnergyUpdate)
            put("ENERGY_MAX", s.energyMax)
            put("upgradePurchases", JSONObject().apply {
                put("tapSmall",   u.tapSmall)
                put("tapBig",     u.tapBig)
                put("energy",     u.energy)
                put("tapHuge",    u.tapHuge)
                put("regenBoost", u.regenBoost)
                put("energyHuge", u.energyHuge)
            })
        }
    }

    private fun parseUpgrades(j: JSONObject?): UpgradeCounts {
        if (j == null) return UpgradeCounts()
        return UpgradeCounts(
            tapSmall   = j.optInt("tapSmall"),
            tapBig     = j.optInt("tapBig"),
            energy     = j.optInt("energy"),
            tapHuge    = j.optInt("tapHuge"),
            regenBoost = j.optInt("regenBoost"),
            energyHuge = j.optInt("energyHuge")
        )
    }

    private fun parseSkin(raw: String): SkinState {
        if (raw.isBlank()) return SkinState()
        return try {
            val j = JSONObject(raw)
            val arr = j.optJSONArray("ownedSkinIds")
            val owned = mutableListOf<String>()
            if (arr != null) for (i in 0 until arr.length()) owned.add(arr.getString(i))
            if (owned.isEmpty()) owned.addAll(listOf("classic", "standard"))
            SkinState(
                equippedSkinId = j.optString("equippedSkinId", "classic"),
                ownedSkinIds = owned
            )
        } catch (_: Exception) { SkinState() }
    }

    private fun parseLeaderboard(j: JSONObject?): List<LeaderboardEntry> {
        if (j == null) return emptyList()
        val arr = j.optJSONArray("main") ?: return emptyList()
        val list = mutableListOf<LeaderboardEntry>()
        for (i in 0 until minOf(arr.length(), 3)) {
            val e = arr.getJSONObject(i)
            list.add(LeaderboardEntry(
                login  = e.optString("login", "?"),
                score  = e.optLong("score"),
                userId = e.optInt("userId")
            ))
        }
        return list
    }

    private fun post(path: String, body: JSONObject): JSONObject {
        val url = URL("$BASE$path")
        val conn = url.openConnection() as HttpURLConnection
        conn.apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT
            readTimeout = TIMEOUT
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "BoberClickerApp/3.0")
        }
        val bytes = body.toString().toByteArray(Charsets.UTF_8)
        conn.outputStream.use { it.write(bytes) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        conn.disconnect()

        Log.d(TAG, "POST $path -> $code | ${text.take(300)}")
        return try { JSONObject(text) } catch (_: Exception) {
            JSONObject().apply { put("success", false); put("message", "Плохой ответ сервера: $text") }
        }
    }
}
