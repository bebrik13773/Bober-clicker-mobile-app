package com.bebrik.boberclicker.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

// ─── Модели данных ───────────────────────────────────────────────

data class GameSave(
    @SerializedName("score")          var score: Double = 0.0,
    @SerializedName("totalClicks")    var totalClicks: Long = 0,
    @SerializedName("totalEarned")    var totalEarned: Double = 0.0,
    @SerializedName("clickPower")     var clickPower: Double = 1.0,
    @SerializedName("passiveIncome")  var passiveIncome: Double = 0.0,
    @SerializedName("upgrades")       var upgrades: MutableMap<String, Int> = mutableMapOf(),
    @SerializedName("quests")         var quests: MutableMap<String, QuestSave> = mutableMapOf(),
    @SerializedName("achievements")   var achievements: MutableSet<String> = mutableSetOf(),
    @SerializedName("lastSaveTime")   var lastSaveTime: Long = 0
)

data class QuestSave(
    @SerializedName("progress")  var progress: Double = 0.0,
    @SerializedName("completed") var completed: Boolean = false,
    @SerializedName("claimed")   var claimed: Boolean = false
)

// ─── Апгрейды ────────────────────────────────────────────────────

data class UpgradeDef(
    val id: String,
    val name: String,
    val description: String,
    val baseCost: Double,
    val clickBonus: Double = 0.0,
    val passiveBonus: Double = 0.0,
    val maxLevel: Int = 50,
    val emoji: String = "🦫"
) {
    fun costAt(level: Int): Double = Math.floor(baseCost * Math.pow(1.15, level.toDouble()))
}

val ALL_UPGRADES = listOf(
    UpgradeDef("sharp_claws",    "Острые когти",     "+1 к клику",           10.0,     clickBonus=1.0,   emoji="🦷"),
    UpgradeDef("beaver_friend",  "Друг-бобёр",       "+0.5 бобров/сек",      50.0,     passiveBonus=0.5, emoji="🦫"),
    UpgradeDef("dam",            "Плотина",           "+3 бобров/сек",        200.0,    passiveBonus=3.0, emoji="🪵"),
    UpgradeDef("beaver_school",  "Школа бобров",     "+2 к клику",           500.0,    clickBonus=2.0,   emoji="🏫"),
    UpgradeDef("beaver_factory", "Фабрика бобров",   "+10 бобров/сек",       2000.0,   passiveBonus=10.0,emoji="🏭"),
    UpgradeDef("mega_dam",       "Мега-плотина",     "+25 бобров/сек",       10000.0,  passiveBonus=25.0,emoji="🌊"),
    UpgradeDef("beaver_city",    "Город бобров",     "+5 клик, +50/сек",     50000.0,  clickBonus=5.0, passiveBonus=50.0, emoji="🏙️", maxLevel=10)
)

// ─── Квесты ──────────────────────────────────────────────────────

enum class QuestType { TOTAL_CLICKS, TOTAL_EARNED, REACH_SCORE, BUY_UPGRADE, PASSIVE_INCOME }

data class QuestDef(
    val id: String,
    val title: String,
    val description: String,
    val type: QuestType,
    val target: Double,
    val reward: Double,
    val requiredUpgrade: String? = null
)

val ALL_QUESTS = listOf(
    QuestDef("click_100",    "Первые шаги",      "Кликни 100 раз",              QuestType.TOTAL_CLICKS,  100.0,      50.0),
    QuestDef("click_1000",   "Кликер",           "Кликни 1 000 раз",            QuestType.TOTAL_CLICKS,  1000.0,     500.0),
    QuestDef("click_10000",  "Про-кликер",       "Кликни 10 000 раз",           QuestType.TOTAL_CLICKS,  10000.0,    5000.0),
    QuestDef("earn_500",     "Первые бобры",     "Заработай 500 бобров",        QuestType.TOTAL_EARNED,  500.0,      100.0),
    QuestDef("earn_10000",   "Богатый бобёр",    "Заработай 10 000 бобров",     QuestType.TOTAL_EARNED,  10000.0,    2000.0),
    QuestDef("earn_1m",      "Миллионер",        "Заработай 1 000 000 бобров",  QuestType.TOTAL_EARNED,  1000000.0,  100000.0),
    QuestDef("buy_dam",      "Строитель",        "Купи плотину",                QuestType.BUY_UPGRADE,   1.0,        300.0,  "dam"),
    QuestDef("passive_10",   "Пассивный доход",  "Получай 10 бобров/сек",       QuestType.PASSIVE_INCOME,10.0,       1000.0),
    QuestDef("reach_100k",   "Богач",            "Накопи 100 000 бобров",       QuestType.REACH_SCORE,   100000.0,   10000.0)
)

// ─── Достижения ──────────────────────────────────────────────────

data class AchievementDef(val id: String, val title: String, val description: String, val emoji: String)

val ALL_ACHIEVEMENTS = listOf(
    AchievementDef("first_click",   "Первый клик!",     "Кликни впервые",               "👆"),
    AchievementDef("clicks_100",    "100 кликов",       "Сделай 100 кликов",            "🖱️"),
    AchievementDef("clicks_10k",    "10 000 кликов",    "Сделай 10 000 кликов",         "⚡"),
    AchievementDef("clicks_1m",     "Миллион кликов",   "Сделай 1 000 000 кликов",      "💥"),
    AchievementDef("score_1k",      "Первая тысяча",    "Накопи 1 000 бобров",          "💰"),
    AchievementDef("score_1m",      "Миллионер",        "Накопи 1 000 000 бобров",      "💎"),
    AchievementDef("score_1b",      "Миллиардер",       "Накопи 1 000 000 000 бобров",  "👑"),
    AchievementDef("first_upgrade", "Первый апгрейд",   "Купи первый апгрейд",          "🛒"),
    AchievementDef("all_upgrades",  "Коллекционер",     "Купи все виды апгрейдов",      "🏆"),
    AchievementDef("passive_100",   "Машина денег",     "Получай 100 бобров/сек",       "⚙️"),
    AchievementDef("minigame_win",  "Лётчик-бобёр",     "Выиграй мини-игру",            "✈️"),
    AchievementDef("all_quests",    "Квестоман",        "Выполни все квесты",           "📜")
)

// ─── Утилита форматирования ───────────────────────────────────────

fun Double.formatScore(): String = when {
    this < 1_000        -> "%.0f".format(this)
    this < 1_000_000    -> "%.1fK".format(this / 1_000)
    this < 1_000_000_000 -> "%.1fM".format(this / 1_000_000)
    this < 1e12         -> "%.1fB".format(this / 1_000_000_000)
    this < 1e15         -> "%.1fT".format(this / 1e12)
    else                -> "%.1fQa".format(this / 1e15)
}
