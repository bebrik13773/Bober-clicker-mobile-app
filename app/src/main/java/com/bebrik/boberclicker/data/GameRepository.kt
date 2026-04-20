package com.bebrik.boberclicker.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Singleton — единственный источник истины для всего приложения.
 * Обе Activity работают с этим объектом напрямую.
 */
object GameRepository {
    private const val TAG = "BoberRepo"
    private const val PREF = "bober_state_v3"

    // ---- Live state (updated from anywhere) ----
    var score: Long = 0L
    var plus: Int = 1
    var energyMax: Int = 5000
    var energy: Float = 5000f
    var lastEnergyUpdate: Long = System.currentTimeMillis()
    var upgrades: UpgradeCounts = UpgradeCounts()
    var skin: SkinState = SkinState()

    var userId: Int = 0
    var login: String = ""
    var isLoggedIn: Boolean = false

    var leaderboard: List<LeaderboardEntry> = emptyList()

    var isOnline: Boolean = true
    var lastSyncTime: Long = 0L
    var lastSyncMessage: String = ""

    // Callbacks to notify UI
    var onStateChanged: (() -> Unit)? = null
    var onLeaderboardChanged: (() -> Unit)? = null
    var onSyncStatusChanged: ((Boolean, String) -> Unit)? = null  // isOnline, message

    private var syncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ---- Init ----

    fun init(ctx: Context) {
        loadLocal(ctx)
        if (isLoggedIn) {
            ApiClient.restoreCookies(loadCookies(ctx))
        }
    }

    fun startPeriodicSync(ctx: Context) {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (isActive) {
                delay(30_000)
                doSync(ctx)
            }
        }
    }

    fun stopPeriodicSync() {
        syncJob?.cancel()
    }

    // ---- Auth ----

    fun login(ctx: Context, loginVal: String, password: String, callback: (Boolean, String) -> Unit) {
        scope.launch {
            val res = ApiClient.login(loginVal, password)
            if (res.success) {
                applyServerData(res.score, res.plus, res.energyMax, res.energy,
                    res.lastEnergyUpdate, res.upgrades, res.skin)
                userId     = res.userId
                login      = res.login
                isLoggedIn = true
                saveCookies(ctx, ApiClient.exportCookies())
                saveLocal(ctx)
                withContext(Dispatchers.Main) {
                    onStateChanged?.invoke()
                    callback(true, res.message)
                }
            } else {
                withContext(Dispatchers.Main) { callback(false, res.message) }
            }
        }
    }

    fun tryRestoreSession(ctx: Context, callback: (Boolean) -> Unit) {
        if (!isLoggedIn) { callback(false); return }
        scope.launch {
            val res = ApiClient.restoreSession()
            if (res.success) {
                applyServerData(res.score, res.plus, res.energyMax, res.energy,
                    res.lastEnergyUpdate, res.upgrades, res.skin)
                if (res.leaderboard.isNotEmpty()) leaderboard = res.leaderboard
                lastSyncTime = System.currentTimeMillis()
                isOnline = true
                saveLocal(ctx)
                withContext(Dispatchers.Main) {
                    onStateChanged?.invoke()
                    onLeaderboardChanged?.invoke()
                    onSyncStatusChanged?.invoke(true, "Синхронизировано")
                    callback(true)
                }
            } else {
                // Session expired or no network — use local data
                isOnline = res.message.contains("сети", ignoreCase = true) ||
                           res.message.contains("network", ignoreCase = true)
                withContext(Dispatchers.Main) {
                    onSyncStatusChanged?.invoke(isOnline, res.message)
                    callback(false)
                }
            }
        }
    }

    fun logout(ctx: Context) {
        scope.launch { ApiClient.logout() }
        userId = 0; login = ""; isLoggedIn = false
        score = 0; plus = 1; energyMax = 5000; energy = 5000f
        upgrades = UpgradeCounts(); skin = SkinState()
        leaderboard = emptyList()
        saveLocal(ctx)
        onStateChanged?.invoke()
    }

    // ---- Game actions ----

    fun tap(): Boolean {
        if (energy < plus) return false
        score += plus
        energy -= plus
        return true
    }

    fun buyUpgrade(type: String): String? {
        val def = UPGRADE_DEFINITIONS.find { it.type == type } ?: return "Улучшение не найдено"
        if (score < def.baseCost) return "Нужно ${formatScore(def.baseCost)} коинов"
        score -= def.baseCost
        upgrades = when (type) {
            "tapSmall"   -> upgrades.copy(tapSmall   = upgrades.tapSmall   + 1)
            "tapBig"     -> upgrades.copy(tapBig     = upgrades.tapBig     + 1)
            "energy"     -> upgrades.copy(energy     = upgrades.energy     + 1)
            "tapHuge"    -> upgrades.copy(tapHuge    = upgrades.tapHuge    + 1)
            "regenBoost" -> upgrades.copy(regenBoost = upgrades.regenBoost + 1)
            "energyHuge" -> upgrades.copy(energyHuge = upgrades.energyHuge + 1)
            else -> upgrades
        }
        plus      = calcPlus(upgrades)
        energyMax = calcEnergyMax(upgrades)
        return null
    }

    fun buySkin(skinId: String): String? {
        val def = SKIN_DEFINITIONS.find { it.id == skinId } ?: return "Скин не найден"
        if (skin.ownedSkinIds.contains(skinId)) {
            skin = skin.copy(equippedSkinId = skinId)
            return null
        }
        if (score < def.price) return "Нужно ${formatScore(def.price)} коинов"
        score -= def.price
        skin = skin.copy(equippedSkinId = skinId, ownedSkinIds = skin.ownedSkinIds + skinId)
        return null
    }

    fun tickEnergy() {
        val now = System.currentTimeMillis()
        val regen = calcRegenPerMs(upgrades, energyMax)
        energy = (energy + (now - lastEnergyUpdate) * regen).coerceAtMost(energyMax.toFloat())
        lastEnergyUpdate = now
    }

    // ---- Sync ----

    fun syncNow(ctx: Context) {
        scope.launch { doSync(ctx) }
    }

    private suspend fun doSync(ctx: Context) {
        if (!isLoggedIn) return
        try {
            val state = GameState(score, plus, energyMax, energy, lastEnergyUpdate, upgrades, skin)
            val res = ApiClient.sync(userId, state)
            if (res.success) {
                applyServerData(res.score, res.plus, res.energyMax, res.energy,
                    res.lastEnergyUpdate, res.upgrades, res.skin)
                if (res.leaderboard.isNotEmpty()) leaderboard = res.leaderboard
                lastSyncTime = System.currentTimeMillis()
                isOnline = true
                saveLocal(ctx)
                withContext(Dispatchers.Main) {
                    onStateChanged?.invoke()
                    onLeaderboardChanged?.invoke()
                    onSyncStatusChanged?.invoke(true, "Синхронизировано")
                }
            } else {
                isOnline = false
                withContext(Dispatchers.Main) {
                    onSyncStatusChanged?.invoke(false, "Нет сети — используем локальные данные")
                }
            }
        } catch (e: Exception) {
            isOnline = false
            Log.e(TAG, "sync failed", e)
            withContext(Dispatchers.Main) {
                onSyncStatusChanged?.invoke(false, "Нет сети — используем локальные данные")
            }
        }
    }

    // ---- Save / Load ----

    fun saveLocal(ctx: Context) {
        val u = upgrades; val s = skin
        val j = JSONObject().apply {
            put("score", score)
            put("plus", plus)
            put("energyMax", energyMax)
            put("energy", energy.toDouble())
            put("lastEnergyUpdate", lastEnergyUpdate)
            put("userId", userId)
            put("login", login)
            put("isLoggedIn", isLoggedIn)
            put("tapSmall",   u.tapSmall);  put("tapBig",     u.tapBig)
            put("energy_upg", u.energy);    put("tapHuge",    u.tapHuge)
            put("regenBoost", u.regenBoost); put("energyHuge", u.energyHuge)
            put("equippedSkin", s.equippedSkinId)
            put("ownedSkins", JSONArray(s.ownedSkinIds))
            // leaderboard cache
            val lb = JSONArray()
            leaderboard.forEach { e ->
                lb.put(JSONObject().apply { put("login", e.login); put("score", e.score); put("userId", e.userId) })
            }
            put("leaderboard", lb)
        }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString("state", j.toString()).apply()
    }

    private fun loadLocal(ctx: Context) {
        val raw = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("state", null) ?: return
        try {
            val j = JSONObject(raw)
            val u = UpgradeCounts(
                tapSmall   = j.optInt("tapSmall"),
                tapBig     = j.optInt("tapBig"),
                energy     = j.optInt("energy_upg"),
                tapHuge    = j.optInt("tapHuge"),
                regenBoost = j.optInt("regenBoost"),
                energyHuge = j.optInt("energyHuge")
            )
            val arr = j.optJSONArray("ownedSkins")
            val owned = mutableListOf<String>()
            if (arr != null) for (i in 0 until arr.length()) owned.add(arr.getString(i))
            if (owned.isEmpty()) owned.addAll(listOf("classic", "standard"))

            val savedMax    = j.optInt("energyMax", 5000)
            val savedEnergy = j.optDouble("energy", savedMax.toDouble()).toFloat()
            val savedUpdate = j.optLong("lastEnergyUpdate", System.currentTimeMillis())

            // Offline regen since last save
            val regen = calcRegenPerMs(u, savedMax)
            val offlineMs = System.currentTimeMillis() - savedUpdate
            val regenedEnergy = (savedEnergy + offlineMs * regen).coerceAtMost(savedMax.toFloat())

            score    = j.optLong("score")
            plus     = j.optInt("plus", 1)
            energyMax = savedMax
            energy   = regenedEnergy
            lastEnergyUpdate = System.currentTimeMillis()
            upgrades = u
            skin     = SkinState(j.optString("equippedSkin", "classic"), owned)
            userId   = j.optInt("userId")
            login    = j.optString("login", "")
            isLoggedIn = j.optBoolean("isLoggedIn", false)

            val lb = j.optJSONArray("leaderboard")
            if (lb != null) {
                val entries = mutableListOf<LeaderboardEntry>()
                for (i in 0 until lb.length()) {
                    val e = lb.getJSONObject(i)
                    entries.add(LeaderboardEntry(e.optString("login"), e.optLong("score"), e.optInt("userId")))
                }
                leaderboard = entries
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadLocal failed", e)
        }
    }

    private fun saveCookies(ctx: Context, raw: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString("cookies", raw).apply()
    }

    private fun loadCookies(ctx: Context): String =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("cookies", "") ?: ""

    private fun applyServerData(
        sc: Long, pl: Int, em: Int, en: Float, lu: Long,
        upg: UpgradeCounts, sk: SkinState
    ) {
        score = sc; plus = pl; energyMax = em
        // Add offline regen on top of server energy
        val regen = calcRegenPerMs(upg, em)
        val offlineMs = System.currentTimeMillis() - lu
        energy = (en + offlineMs * regen).coerceAtMost(em.toFloat())
        lastEnergyUpdate = System.currentTimeMillis()
        upgrades = upg; skin = sk
    }
}
