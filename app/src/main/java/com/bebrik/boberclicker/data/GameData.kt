package com.bebrik.boberclicker.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlin.math.log10
import kotlin.math.max

// ─── Сохранение ──────────────────────────────────────────────────

data class GameSave(
    @SerializedName("score")           var score: Double = 0.0,
    @SerializedName("energy")          var energy: Double = 5000.0,
    @SerializedName("lastSaveTime")    var lastSaveTime: Long = 0,
    @SerializedName("upgrades")        var upgrades: UpgradeCounts = UpgradeCounts(),
    @SerializedName("ownedSkinIds")    var ownedSkinIds: MutableSet<String> = mutableSetOf("classic", "standard"),
    @SerializedName("equippedSkin")    var equippedSkin: String = "classic",
    @SerializedName("achievements")    var achievements: MutableSet<String> = mutableSetOf(),
    @SerializedName("flyBestScore")    var flyBestScore: Int = 0,
    @SerializedName("flyGamesPlayed")  var flyGamesPlayed: Int = 0
)

data class UpgradeCounts(
    @SerializedName("tapSmall")    var tapSmall: Int = 0,    // +1 к тапу, база 5 000
    @SerializedName("tapBig")      var tapBig: Int = 0,      // +5 к тапу, база 20 000
    @SerializedName("tapHuge")     var tapHuge: Int = 0,     // +100 к тапу, база 2 500 000
    @SerializedName("energy")      var energy: Int = 0,      // +1000 макс.энергии, база 10 000
    @SerializedName("regenBoost")  var regenBoost: Int = 0,  // +2/сек регена, база 35 000
    @SerializedName("energyHuge")  var energyHuge: Int = 0   // +10000 макс.энергии, база 90 000
)

// ─── Вычисляемые параметры ────────────────────────────────────────

fun UpgradeCounts.calcPlus(): Int =
    1 + tapSmall + tapBig * 5 + tapHuge * 100

fun UpgradeCounts.calcEnergyMax(): Double =
    5000.0 + energy * 1000.0 + energyHuge * 10000.0

fun UpgradeCounts.calcRegenPerSecond(): Double =
    1.0 + regenBoost * 2.0

fun UpgradeCounts.totalPurchases(): Int =
    tapSmall + tapBig + tapHuge + energy + regenBoost + energyHuge

// ─── Апгрейды ────────────────────────────────────────────────────

data class UpgradeDef(
    val id: String,
    val name: String,
    val description: String,
    val baseCost: Double,
    val emoji: String
)

val ALL_UPGRADES = listOf(
    UpgradeDef("tapSmall",   "Маленький тап",    "+1 к тапу",                    5_000.0,     "🐾"),
    UpgradeDef("tapBig",     "Большой тап",      "+5 к тапу",                    20_000.0,    "💥"),
    UpgradeDef("energy",     "Запас энергии",    "+1 000 к макс. энергии",       10_000.0,    "🔋"),
    UpgradeDef("regenBoost", "Разгон ритма",     "+2 к регену в сек.",           35_000.0,    "⚡"),
    UpgradeDef("energyHuge", "Огромный запас",   "+10 000 к макс. энергии",      90_000.0,    "🛢️"),
    UpgradeDef("tapHuge",    "Огромный тап",     "+100 к тапу",                  2_500_000.0, "🪵")
)

fun UpgradeDef.getLevel(counts: UpgradeCounts): Int = when (id) {
    "tapSmall"   -> counts.tapSmall
    "tapBig"     -> counts.tapBig
    "tapHuge"    -> counts.tapHuge
    "energy"     -> counts.energy
    "regenBoost" -> counts.regenBoost
    "energyHuge" -> counts.energyHuge
    else -> 0
}

// ─── Скины ───────────────────────────────────────────────────────

data class SkinDef(
    val id: String,
    val name: String,
    val price: Double,
    val assetFile: String, // имя файла в assets/skins/
    val rarity: String,
    val grantOnly: Boolean = false
)

val ALL_SKINS = listOf(
    SkinDef("classic",   "Просто бобер",            0.0,       "bober.png",            "common"),
    SkinDef("standard",  "Стандартный бобер",        0.0,       "matvey-new-bober.jpg", "common"),
    SkinDef("paper",     "Бумажный бобер",           5_000.0,   "bumazny-bober.jpg",    "common"),
    SkinDef("strawberry","Клубничный йогурт бобер",  15_000.0,  "klub-smz-bober.jpg",   "uncommon"),
    SkinDef("sock",      "Носок бобер",              30_000.0,  "nosok-bober.jpg",      "epic"),
    SkinDef("strange",   "Странный бобер",           30_000.0,  "strany-bober.jpg",     "rare"),
    SkinDef("chocolate", "Шоколад бобер",            50_000.0,  "Shok-upok-bober.jpg",  "rare"),
    SkinDef("dev",       "Dev бобер",                0.0,       "dev.png",              "admin", grantOnly = true)
)

// ─── Достижения ──────────────────────────────────────────────────

data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val secret: Boolean = false
)

val ALL_ACHIEVEMENTS = listOf(
    AchievementDef("clicker_10k",    "Разогрев",               "Набери 10 000 коинов",              "🔥"),
    AchievementDef("clicker_50k",    "Уверенный старт",        "Набери 50 000 коинов",              "🚀"),
    AchievementDef("clicker_100k",   "Первая сотня тысяч",     "Набери 100 000 коинов",             "💯"),
    AchievementDef("clicker_500k",   "Полмиллиона",            "Набери 500 000 коинов",             "💎"),
    AchievementDef("clicker_1m",     "Миллионер",              "Набери 1 000 000 коинов",           "🏦"),
    AchievementDef("clicker_5m",     "Пять миллионов",         "Набери 5 000 000 коинов",           "💼"),
    AchievementDef("clicker_10m",    "Десятимиллионный клуб",  "Набери 10 000 000 коинов",          "🪙"),
    AchievementDef("clicker_50m",    "Финансовый бобр",        "Набери 50 000 000 коинов",          "🏛️"),
    AchievementDef("clicker_100m",   "Девятизначный бобр",     "Набери 100 000 000 коинов",         "📈"),
    AchievementDef("clicker_1b",     "Бобровый миллиард",      "Набери 1 000 000 000 коинов",       "🌍"),
    AchievementDef("plus_100",       "Рука тяжелеет",          "Сила тапа +100",                    "✋"),
    AchievementDef("plus_500",       "Удар плотины",           "Сила тапа +500",                    "💥"),
    AchievementDef("plus_1000",      "Сверхудар",              "Сила тапа +1000",                   "🧨"),
    AchievementDef("energy_25k",     "Большой запас",          "Макс. энергия 25 000",              "🔋"),
    AchievementDef("energy_100k",    "Резервуар",              "Макс. энергия 100 000",             "⚡"),
    AchievementDef("energy_250k",    "Энергоблок",             "Макс. энергия 250 000",             "🔌"),
    AchievementDef("energy_500k",    "Энергостанция",          "Макс. энергия 500 000",             "⚙️"),
    AchievementDef("upgrades_10",    "Механик",                "Купи 10 улучшений суммарно",        "🛠️"),
    AchievementDef("upgrades_25",    "Инженер клика",          "Купи 25 улучшений суммарно",        "⚙️"),
    AchievementDef("upgrades_50",    "Архитектор фермы",       "Купи 50 улучшений суммарно",        "🏗️"),
    AchievementDef("upgrades_100",   "Главный конструктор",    "Купи 100 улучшений суммарно",       "🏭"),
    AchievementDef("every_upgrade",  "Полный стенд",           "Купи каждое улучшение хоть раз",   "🧰"),
    AchievementDef("tap_small_25",   "Точная лапа",            "Маленький тап до ур. 25",           "🐾"),
    AchievementDef("tap_big_25",     "Тяжёлый клик",           "Большой тап до ур. 25",             "🔨"),
    AchievementDef("energy_up_25",   "Энергетик",              "Запас энергии до ур. 25",           "🔋"),
    AchievementDef("tap_huge_25",    "Сокрушитель",            "Огромный тап до ур. 25",            "🪵"),
    AchievementDef("regen_25",       "Быстрая зарядка X25",    "Разгон ритма до ур. 25",            "⚡"),
    AchievementDef("energy_huge_10", "Сверхзапас X10",         "Огромный запас — 10 уровней",       "🛢️"),
    AchievementDef("collector_1",    "Первый скин",            "Получи первый скин",                "🧥"),
    AchievementDef("collector_3",    "Коллекционер",           "Собери 3 скина",                    "🎨"),
    AchievementDef("collector_5",    "Гардероб",               "Собери 5 скинов",                   "👕"),
    AchievementDef("fly_best_10",    "Первые крылья",          "Рекорд 10 в Flying Beaver",         "🪽"),
    AchievementDef("fly_best_25",    "Низкий пролет",          "Рекорд 25 в Flying Beaver",         "🌿"),
    AchievementDef("fly_best_50",    "Повелитель болота",      "Рекорд 50 в Flying Beaver",         "🌫️"),
    AchievementDef("fly_best_100",   "Сотня в полете",         "Рекорд 100 в Flying Beaver",        "🌤️"),
    AchievementDef("fly_games_10",   "Не сдаюсь",              "Сыграй 10 забегов",                 "🎮"),
    AchievementDef("fly_games_50",   "Ветеран полетов",        "Сыграй 50 забегов",                 "🧭"),
    AchievementDef("fly_games_100",  "Летная школа",           "Сыграй 100 забегов",                "🏅")
)

// ─── Форматирование чисел ─────────────────────────────────────────

fun Double.fmt(): String = when {
    this < 1_000        -> "%.0f".format(this)
    this < 1_000_000    -> "%.1fK".format(this / 1_000)
    this < 1_000_000_000 -> "%.1fM".format(this / 1_000_000)
    this < 1e12         -> "%.1fB".format(this / 1_000_000_000)
    this < 1e15         -> "%.1fT".format(this / 1e12)
    else                -> "%.1fQa".format(this / 1e15)
}

fun Int.fmt(): String = toDouble().fmt()
