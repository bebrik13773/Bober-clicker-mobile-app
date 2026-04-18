package com.bebrik.boberclicker.game

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bebrik.boberclicker.data.*
import kotlin.math.min
import kotlin.math.max

data class UiState(
    val score: Double = 0.0,
    val energy: Double = 5000.0,
    val energyMax: Double = 5000.0,
    val plus: Int = 1,
    val regenPerSec: Double = 1.0,
    val upgrades: UpgradeCounts = UpgradeCounts(),
    val ownedSkinIds: Set<String> = setOf("classic", "standard"),
    val equippedSkin: String = "classic",
    val achievements: Set<String> = emptySet(),
    val flyBestScore: Int = 0,
    val flyGamesPlayed: Int = 0,
    val newAchievement: AchievementDef? = null
)

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableLiveData(UiState())
    val state: LiveData<UiState> = _state

    private var save = GameSave()
    private val handler = Handler(Looper.getMainLooper())

    // Тикает каждые 100мс — плавная регенерация энергии
    private val energyTick = object : Runnable {
        override fun run() {
            val energyMax = save.upgrades.calcEnergyMax()
            if (save.energy < energyMax) {
                val regen = save.upgrades.calcRegenPerSecond() * 0.1 // 100мс = 0.1 сек
                save.energy = min(energyMax, save.energy + regen)
                emitState()
            }
            handler.postDelayed(this, 100)
        }
    }

    private val autoSaveTick = object : Runnable {
        override fun run() { saveGame(); handler.postDelayed(this, 30_000) }
    }

    init {
        save = SaveManager.load(getApplication())
        emitState()
        handler.postDelayed(energyTick, 100)
        handler.postDelayed(autoSaveTick, 30_000)
    }

    // ─── Клик ────────────────────────────────────────────────────

    fun onBoberClick(): Boolean {
        val plus = save.upgrades.calcPlus()
        if (save.energy < plus) return false  // нет энергии

        save.score += plus
        save.energy = max(0.0, save.energy - plus)
        checkAchievements()
        emitState()
        return true
    }

    // ─── Магазин улучшений ───────────────────────────────────────

    fun buyUpgrade(id: String): Boolean {
        val def = ALL_UPGRADES.find { it.id == id } ?: return false
        if (save.score < def.baseCost) return false

        save.score -= def.baseCost
        when (id) {
            "tapSmall"   -> save.upgrades.tapSmall++
            "tapBig"     -> save.upgrades.tapBig++
            "tapHuge"    -> save.upgrades.tapHuge++
            "energy"     -> save.upgrades.energy++
            "regenBoost" -> save.upgrades.regenBoost++
            "energyHuge" -> save.upgrades.energyHuge++
        }
        checkAchievements()
        emitState()
        return true
    }

    // ─── Магазин скинов ──────────────────────────────────────────

    fun buySkin(id: String): Boolean {
        val skin = ALL_SKINS.find { it.id == id } ?: return false
        if (skin.grantOnly) return false
        if (save.ownedSkinIds.contains(id)) { equipSkin(id); return true }
        if (save.score < skin.price) return false

        save.score -= skin.price
        save.ownedSkinIds.add(id)
        equipSkin(id)
        checkAchievements()
        emitState()
        return true
    }

    fun equipSkin(id: String) {
        if (!save.ownedSkinIds.contains(id)) return
        save.equippedSkin = id
        emitState()
    }

    // ─── Мини-игра ───────────────────────────────────────────────

    fun onFlyBeaverGameOver(score: Int) {
        save.flyGamesPlayed++
        val coinsEarned = score * 500.0
        save.score += coinsEarned
        if (score > save.flyBestScore) save.flyBestScore = score
        checkAchievements()
        emitState()
    }

    // ─── Достижения ──────────────────────────────────────────────

    private fun checkAchievements() {
        val u = save.upgrades
        val s = save.score
        val plus = u.calcPlus()
        val energyMax = u.calcEnergyMax()
        val total = u.totalPurchases()
        val owned = save.ownedSkinIds.size
        val fly = save.flyBestScore
        val games = save.flyGamesPlayed

        // Коины
        if (s >= 10_000)       unlock("clicker_10k")
        if (s >= 50_000)       unlock("clicker_50k")
        if (s >= 100_000)      unlock("clicker_100k")
        if (s >= 500_000)      unlock("clicker_500k")
        if (s >= 1_000_000)    unlock("clicker_1m")
        if (s >= 5_000_000)    unlock("clicker_5m")
        if (s >= 10_000_000)   unlock("clicker_10m")
        if (s >= 50_000_000)   unlock("clicker_50m")
        if (s >= 100_000_000)  unlock("clicker_100m")
        if (s >= 1_000_000_000) unlock("clicker_1b")

        // Сила тапа
        if (plus >= 100)  unlock("plus_100")
        if (plus >= 500)  unlock("plus_500")
        if (plus >= 1000) unlock("plus_1000")

        // Энергия
        if (energyMax >= 25_000)  unlock("energy_25k")
        if (energyMax >= 100_000) unlock("energy_100k")
        if (energyMax >= 250_000) unlock("energy_250k")
        if (energyMax >= 500_000) unlock("energy_500k")

        // Улучшения суммарно
        if (total >= 10)  unlock("upgrades_10")
        if (total >= 25)  unlock("upgrades_25")
        if (total >= 50)  unlock("upgrades_50")
        if (total >= 100) unlock("upgrades_100")

        // Все улучшения хотя бы раз
        if (u.tapSmall >= 1 && u.tapBig >= 1 && u.tapHuge >= 1 &&
            u.energy >= 1 && u.regenBoost >= 1 && u.energyHuge >= 1)
            unlock("every_upgrade")

        // Уровни конкретных улучшений
        if (u.tapSmall >= 25)   unlock("tap_small_25")
        if (u.tapBig >= 25)     unlock("tap_big_25")
        if (u.energy >= 25)     unlock("energy_up_25")
        if (u.tapHuge >= 25)    unlock("tap_huge_25")
        if (u.regenBoost >= 25) unlock("regen_25")
        if (u.energyHuge >= 10) unlock("energy_huge_10")

        // Скины
        if (owned >= 1) unlock("collector_1")
        if (owned >= 3) unlock("collector_3")
        if (owned >= 5) unlock("collector_5")

        // Flying Beaver — рекорд
        if (fly >= 10)  unlock("fly_best_10")
        if (fly >= 25)  unlock("fly_best_25")
        if (fly >= 50)  unlock("fly_best_50")
        if (fly >= 100) unlock("fly_best_100")

        // Flying Beaver — количество игр
        if (games >= 10)  unlock("fly_games_10")
        if (games >= 50)  unlock("fly_games_50")
        if (games >= 100) unlock("fly_games_100")
    }

    private fun unlock(id: String) {
        if (save.achievements.contains(id)) return
        save.achievements.add(id)
        val def = ALL_ACHIEVEMENTS.find { it.id == id }
        _state.value = _state.value!!.copy(newAchievement = def)
    }

    fun clearNewAchievement() {
        _state.value = _state.value!!.copy(newAchievement = null)
    }

    // ─── Утилиты ─────────────────────────────────────────────────

    fun saveGame() = SaveManager.save(getApplication(), save)

    private fun emitState() {
        val u = save.upgrades
        _state.value = UiState(
            score = save.score,
            energy = save.energy,
            energyMax = u.calcEnergyMax(),
            plus = u.calcPlus(),
            regenPerSec = u.calcRegenPerSecond(),
            upgrades = u.copy(),
            ownedSkinIds = save.ownedSkinIds.toSet(),
            equippedSkin = save.equippedSkin,
            achievements = save.achievements.toSet(),
            flyBestScore = save.flyBestScore,
            flyGamesPlayed = save.flyGamesPlayed,
            newAchievement = _state.value?.newAchievement
        )
    }

    override fun onCleared() {
        handler.removeCallbacksAndMessages(null)
        saveGame()
    }
}
