package com.bebrik.boberclicker.game

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bebrik.boberclicker.data.*

data class GameState(
    val score: Double = 0.0,
    val totalClicks: Long = 0,
    val totalEarned: Double = 0.0,
    val clickPower: Double = 1.0,
    val passiveIncome: Double = 0.0,
    val upgradeLevels: Map<String, Int> = emptyMap(),
    val quests: List<QuestState> = emptyList(),
    val achievements: Set<String> = emptySet(),
    val newAchievement: AchievementDef? = null
)

data class QuestState(
    val def: QuestDef,
    val progress: Double,
    val completed: Boolean,
    val claimed: Boolean
)

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableLiveData(GameState())
    val state: LiveData<GameState> = _state

    private var save = GameSave()
    private val handler = Handler(Looper.getMainLooper())

    private val passiveTick = object : Runnable {
        override fun run() {
            if (save.passiveIncome > 0) {
                save.score += save.passiveIncome
                save.totalEarned += save.passiveIncome
                checkQuestsOnEarn()
                checkAchievementsOnScore()
                emitState()
            }
            handler.postDelayed(this, 1000)
        }
    }

    private val autoSaveTick = object : Runnable {
        override fun run() {
            saveGame()
            handler.postDelayed(this, 30_000)
        }
    }

    init {
        loadGame()
        handler.postDelayed(passiveTick, 1000)
        handler.postDelayed(autoSaveTick, 30_000)
    }

    private fun loadGame() {
        save = SaveManager.load(getApplication())
        emitState()
    }

    fun saveGame() {
        SaveManager.save(getApplication(), save)
    }

    fun onBoberClick() {
        save.score += save.clickPower
        save.totalEarned += save.clickPower
        save.totalClicks++
        checkQuestsOnClick()
        checkAchievementsOnClick()
        emitState()
    }

    fun buyUpgrade(id: String): Boolean {
        val def = ALL_UPGRADES.find { it.id == id } ?: return false
        val level = save.upgrades[id] ?: 0
        if (level >= def.maxLevel) return false
        val cost = def.costAt(level)
        if (save.score < cost) return false

        save.score -= cost
        save.upgrades[id] = level + 1
        recalcStats()
        checkQuestsOnPurchase(id)
        checkAchievementsOnShop()
        emitState()
        return true
    }

    private fun recalcStats() {
        var click = 1.0
        var passive = 0.0
        for (def in ALL_UPGRADES) {
            val lvl = save.upgrades[def.id] ?: 0
            click += def.clickBonus * lvl
            passive += def.passiveBonus * lvl
        }
        save.clickPower = click
        save.passiveIncome = passive
    }

    private fun checkQuestsOnClick() {
        for (q in ALL_QUESTS) {
            if (q.type != QuestType.TOTAL_CLICKS) continue
            val s = save.quests.getOrPut(q.id) { QuestSave() }
            if (s.completed) continue
            s.progress = save.totalClicks.toDouble()
            if (s.progress >= q.target) s.completed = true
        }
    }

    private fun checkQuestsOnEarn() {
        for (q in ALL_QUESTS) {
            val s = save.quests.getOrPut(q.id) { QuestSave() }
            if (s.completed) continue
            when (q.type) {
                QuestType.TOTAL_EARNED   -> { s.progress = save.totalEarned;    if (s.progress >= q.target) s.completed = true }
                QuestType.REACH_SCORE    -> { s.progress = save.score;          if (s.progress >= q.target) s.completed = true }
                QuestType.PASSIVE_INCOME -> { s.progress = save.passiveIncome;  if (s.progress >= q.target) s.completed = true }
                else -> {}
            }
        }
    }

    private fun checkQuestsOnPurchase(upgradeId: String) {
        for (q in ALL_QUESTS) {
            if (q.type != QuestType.BUY_UPGRADE || q.requiredUpgrade != upgradeId) continue
            val s = save.quests.getOrPut(q.id) { QuestSave() }
            if (!s.completed) { s.progress = 1.0; s.completed = true }
        }
    }

    fun claimQuestReward(questId: String) {
        val q = ALL_QUESTS.find { it.id == questId } ?: return
        val s = save.quests[questId] ?: return
        if (!s.completed || s.claimed) return
        s.claimed = true
        save.score += q.reward
        save.totalEarned += q.reward
        if (save.quests.values.all { it.claimed }) unlockAchievement("all_quests")
        emitState()
    }

    private fun checkAchievementsOnClick() {
        if (save.totalClicks >= 1)       unlockAchievement("first_click")
        if (save.totalClicks >= 100)     unlockAchievement("clicks_100")
        if (save.totalClicks >= 10000)   unlockAchievement("clicks_10k")
        if (save.totalClicks >= 1000000) unlockAchievement("clicks_1m")
    }

    private fun checkAchievementsOnScore() {
        if (save.score >= 1_000)         unlockAchievement("score_1k")
        if (save.score >= 1_000_000)     unlockAchievement("score_1m")
        if (save.score >= 1_000_000_000) unlockAchievement("score_1b")
        if (save.passiveIncome >= 100)   unlockAchievement("passive_100")
    }

    private fun checkAchievementsOnShop() {
        unlockAchievement("first_upgrade")
        if (ALL_UPGRADES.all { (save.upgrades[it.id] ?: 0) > 0 }) unlockAchievement("all_upgrades")
    }

    fun unlockMiniGameAchievement() {
        unlockAchievement("minigame_win")
        emitState()
    }

    private fun unlockAchievement(id: String) {
        if (save.achievements.contains(id)) return
        save.achievements.add(id)
        val def = ALL_ACHIEVEMENTS.find { it.id == id }
        _state.value = _state.value!!.copy(newAchievement = def)
    }

    fun clearNewAchievement() {
        _state.value = _state.value!!.copy(newAchievement = null)
    }

    fun addScoreFromMiniGame(amount: Double) {
        save.score += amount
        save.totalEarned += amount
        emitState()
    }

    private fun emitState() {
        _state.value = GameState(
            score = save.score,
            totalClicks = save.totalClicks,
            totalEarned = save.totalEarned,
            clickPower = save.clickPower,
            passiveIncome = save.passiveIncome,
            upgradeLevels = save.upgrades.toMap(),
            quests = ALL_QUESTS.map { q ->
                val s = save.quests[q.id] ?: QuestSave()
                QuestState(q, s.progress, s.completed, s.claimed)
            },
            achievements = save.achievements.toSet(),
            newAchievement = _state.value?.newAchievement
        )
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacks(passiveTick)
        handler.removeCallbacks(autoSaveTick)
        saveGame()
    }
}
