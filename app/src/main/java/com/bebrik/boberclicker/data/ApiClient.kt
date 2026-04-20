package com.bebrik.boberclicker.data

import android.content.Context
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
    private const val TAG     = "BoberApi"
    const val BASE            = "https://bober-api.gt.tc"
    private const val TIMEOUT = 14_000

    // java.net cookie jar — shared with ChallengeResolver
    val cookieManager = CookieManager(null, CookiePolicy.ACCEPT_ALL).also {
        java.net.CookieHandler.setDefault(it)
    }

    // Activity context for WebView challenge — set by BoberApp / activities
    var activityContext: Context? = null

    // ──────────────────────────────────────────────────────────────
    //  Public API
    // ──────────────────────────────────────────────────────────────

    data class LoginResult(
        val success: Boolean, val message: String,
        val userId: Int = 0, val login: String = "",
        val score: Long = 0, val plus: Int = 1,
        val energyMax: Int = 5000, val energy: Float = 5000f,
        val lastEnergyUpdate: Long = System.currentTimeMillis(),
        val upgrades: UpgradeCounts = UpgradeCounts(),
        val skin: SkinState = SkinState()
    )

    data class SyncResult(
        val success: Boolean, val message: String = "",
        val score: Long = 0, val plus: Int = 1,
        val energyMax: Int = 5000, val energy: Float = 5000f,
        val lastEnergyUpdate: Long = System.currentTimeMillis(),
        val upgrades: UpgradeCounts = UpgradeCounts(),
        val skin: SkinState = SkinState(),
        val leaderboard: List<LeaderboardEntry> = emptyList()
    )

    fun login(login: String, password: String): LoginResult {
        return try {
            val body = JSONObject().apply { put("login", login); put("password", password) }
            val resp = post("/api/auth/login.php", body)
            if (!resp.optBoolean("success"))
                return LoginResult(false, resp.optString("message", "Ошибка входа"))
            LoginResult(
                success  = true,
                message  = resp.optString("message", ""),
                userId   = resp.optInt("userId"),
                login    = resp.optString("login", login),
                score    = resp.optLong("score"),
                plus     = resp.optInt("plus", 1),
                energyMax        = resp.optInt("ENERGY_MAX", 5000),
                energy           = resp.optDouble("energy", 5000.0).toFloat(),
                lastEnergyUpdate = resp.optLong("lastEnergyUpdate", System.currentTimeMillis()),
                upgrades = parseUpgrades(resp.optJSONObject("upgradePurchases")),
                skin     = parseSkin(resp.optString("skin"))
            )
        } catch (e: Exception) {
            Log.e(TAG, "login error", e)
            LoginResult(false, "Ошибка сети: ${e.message}")
        }
    }

    fun sync(userId: Int, state: GameState): SyncResult {
        return try {
            val resp = post("/api/state/sync.php", buildSavePayload(userId, state))
            val acc  = resp.optJSONObject("account") ?: resp
            SyncResult(
                success   = resp.optBoolean("success", true),
                score     = acc.optLong("score", state.score),
                plus      = acc.optInt("plus", state.plus),
                energyMax = acc.optInt("ENERGY_MAX", state.energyMax),
                energy    = acc.optDouble("energy", state.energy.toDouble()).toFloat(),
                lastEnergyUpdate = acc.optLong("lastEnergyUpdate", System.currentTimeMillis()),
                upgrades  = parseUpgrades(acc.optJSONObject("upgradePurchases")),
                skin      = parseSkin(acc.optString("skin", "")),
                leaderboard = parseLeaderboard(resp.optJSONObject("leaderboards"))
            )
        } catch (e: Exception) {
            Log.e(TAG, "sync error", e)
            SyncResult(false, "Нет сети: ${e.message}")
        }
    }

    fun restoreSession(): SyncResult {
        return try {
            val resp = post("/api/state/sync.php", JSONObject())
            val acc  = resp.optJSONObject("account")
            if (acc == null || acc.optInt("userId") == 0)
                return SyncResult(false, "Сессия истекла")
            SyncResult(
                success   = true,
                score     = acc.optLong("score"),
                plus      = acc.optInt("plus", 1),
                energyMax = acc.optInt("ENERGY_MAX", 5000),
                energy    = acc.optDouble("energy", 5000.0).toFloat(),
                lastEnergyUpdate = acc.optLong("lastEnergyUpdate", System.currentTimeMillis()),
                upgrades  = parseUpgrades(acc.optJSONObject("upgradePurchases")),
                skin      = parseSkin(acc.optString("skin", "")),
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
            raw.split(";").map { it.trim() }.filter { it.contains("=") }.forEach { part ->
                val name  = part.substringBefore("=").trim()
                val value = part.substringAfter("=").trim()
                if (name.isNotEmpty()) {
                    val c = HttpCookie(name, value).apply { path = "/"; domain = "bober-api.gt.tc" }
                    cookieManager.cookieStore.add(uri, c)
                }
            }
        } catch (e: Exception) { Log.w(TAG, "restoreCookies: $e") }
    }

    fun exportCookies(): String = try {
        val uri = URL(BASE).toURI()
        cookieManager.cookieStore.get(uri).joinToString("; ") { "${it.name}=${it.value}" }
    } catch (_: Exception) { "" }

    // ──────────────────────────────────────────────────────────────
    //  HTTP core with WebView challenge auto-solve
    // ──────────────────────────────────────────────────────────────

    /**
     * POST endpoint.
     * Если сервер вернул HTML-challenge — запускаем ChallengeResolver (WebView),
     * ждём куку _test, потом повторяем запрос.
     */
    private fun post(path: String, body: JSONObject): JSONObject {
        var text = rawPost("$BASE$path", body)

        if (isChallengePage(text)) {
            Log.d(TAG, "Challenge detected on $path, launching WebView resolver…")
            val ctx = activityContext
            if (ctx != null) {
                val solved = ChallengeResolver.resolve(ctx, cookieManager, BASE)
                if (solved) {
                    // Retry — challenge should be cleared
                    val retryPath = if (path.contains("?")) "$path&i=1" else "$path?i=1"
                    text = rawPost("$BASE$retryPath", body)
                    Log.d(TAG, "Retry after challenge: ${text.take(100)}")
                }
            } else {
                Log.e(TAG, "activityContext is null — can't resolve challenge")
            }
        }

        if (isChallengePage(text)) {
            Log.e(TAG, "Still HTML after challenge resolution")
            return JSONObject().apply {
                put("success", false)
                put("message", "Сервер не ответил. Попробуйте позже.")
            }
        }

        return try {
            JSONObject(text)
        } catch (_: Exception) {
            Log.e(TAG, "Bad JSON: ${text.take(200)}")
            JSONObject().apply { put("success", false); put("message", "Ошибка ответа сервера") }
        }
    }

    private fun isChallengePage(text: String): Boolean {
        val t = text.trimStart()
        return t.startsWith("<") && (
            t.contains("slowAES") ||
            t.contains("_test=") ||
            t.contains("aes.js")
        )
    }

    private fun rawPost(url: String, body: JSONObject): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.apply {
            requestMethod  = "POST"
            connectTimeout = TIMEOUT
            readTimeout    = TIMEOUT
            doOutput       = true
            setRequestProperty("Content-Type",      "application/json")
            setRequestProperty("Accept",            "application/json, text/html")
            setRequestProperty("User-Agent",        "BoberClickerApp/4.0 Android")
            setRequestProperty("X-Requested-With",  "XMLHttpRequest")
        }
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code   = conn.responseCode
        val stream = if (code in 200..399) conn.inputStream else (conn.errorStream ?: conn.inputStream)
        val text   = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        conn.disconnect()
        Log.d(TAG, "POST $url → $code | ${text.take(160)}")
        return text
    }

    // ──────────────────────────────────────────────────────────────
    //  Parsers
    // ──────────────────────────────────────────────────────────────

    private fun buildSavePayload(userId: Int, s: GameState) = JSONObject().apply {
        val u = s.upgrades
        put("userId", userId); put("score", s.score); put("plus", s.plus)
        put("skin", JSONObject().apply {
            put("equippedSkinId", s.skin.equippedSkinId)
            put("ownedSkinIds", JSONArray(s.skin.ownedSkinIds))
        }.toString())
        put("energy", s.energy.toInt()); put("lastEnergyUpdate", s.lastEnergyUpdate)
        put("ENERGY_MAX", s.energyMax)
        put("upgradePurchases", JSONObject().apply {
            put("tapSmall",   u.tapSmall);   put("tapBig",     u.tapBig)
            put("energy",     u.energy);     put("tapHuge",    u.tapHuge)
            put("regenBoost", u.regenBoost); put("energyHuge", u.energyHuge)
        })
    }

    private fun parseUpgrades(j: JSONObject?) = if (j == null) UpgradeCounts() else UpgradeCounts(
        tapSmall   = j.optInt("tapSmall"),   tapBig     = j.optInt("tapBig"),
        energy     = j.optInt("energy"),     tapHuge    = j.optInt("tapHuge"),
        regenBoost = j.optInt("regenBoost"), energyHuge = j.optInt("energyHuge")
    )

    private fun parseSkin(raw: String): SkinState {
        if (raw.isBlank() || raw == "null") return SkinState()
        return try {
            val j   = JSONObject(raw)
            val arr = j.optJSONArray("ownedSkinIds")
            val owned = mutableListOf<String>()
            if (arr != null) for (i in 0 until arr.length()) owned.add(arr.getString(i))
            if (owned.isEmpty()) owned.addAll(listOf("classic", "standard"))
            SkinState(equippedSkinId = j.optString("equippedSkinId", "classic"), ownedSkinIds = owned)
        } catch (_: Exception) { SkinState() }
    }

    private fun parseLeaderboard(j: JSONObject?): List<LeaderboardEntry> {
        val arr = j?.optJSONArray("main") ?: return emptyList()
        return (0 until minOf(arr.length(), 3)).map { i ->
            arr.getJSONObject(i).let { e ->
                LeaderboardEntry(e.optString("login", "?"), e.optLong("score"), e.optInt("userId"))
            }
        }
    }
}
