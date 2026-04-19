package com.bebrik.boberclicker.data

data class UpgradeCounts(
    val tapSmall: Int = 0,
    val tapBig: Int = 0,
    val energy: Int = 0,
    val tapHuge: Int = 0,
    val regenBoost: Int = 0,
    val energyHuge: Int = 0
)

data class SkinState(
    val equippedSkinId: String = "classic",
    val ownedSkinIds: List<String> = listOf("classic", "standard")
)

data class GameState(
    val score: Long = 0L,
    val plus: Int = 1,
    val energyMax: Int = 5000,
    val energy: Float = 5000f,
    val lastEnergyUpdate: Long = System.currentTimeMillis(),
    val upgrades: UpgradeCounts = UpgradeCounts(),
    val skin: SkinState = SkinState()
)

data class UpgradeDef(
    val type: String,
    val title: String,
    val meta: String,
    val badge: String,
    val baseCost: Long,
    val colorClass: String
)

data class SkinDef(
    val id: String,
    val name: String,
    val assetName: String,
    val price: Long,
    val rarity: String,
    val category: String,
    val defaultOwned: Boolean = false
)

val UPGRADE_DEFINITIONS = listOf(
    UpgradeDef("tapSmall",   "Усилить тап",     "Каждое нажатие приносит на 1 коин больше.",      "+1",    5_000L,       "tap_small"),
    UpgradeDef("tapBig",     "Сильный тап",     "Мощное усиление для быстрого роста счёта.",       "+5",    20_000L,      "tap_big"),
    UpgradeDef("energy",     "Запас энергии",   "Увеличивает максимум энергии на 1000.",            "⚡",    10_000L,      "energy"),
    UpgradeDef("regenBoost", "Быстрая зарядка", "Ускоряет восполнение энергии на 2 ед./сек.",      "⟳",    35_000L,      "regen_boost"),
    UpgradeDef("tapHuge",    "Мега-тап",        "Каждое нажатие даёт на 100 коинов больше.",       "+100",  2_500_000L,   "tap_huge"),
    UpgradeDef("energyHuge", "Сверхзапас",      "Увеличивает максимум энергии на 10000.",           "+10K",  90_000L,      "energy_huge")
)

val SKIN_DEFINITIONS = listOf(
    SkinDef("classic",    "Просто бобёр",     "bober.png",             0L,       "common",    "classic",  true),
    SkinDef("standard",   "Стандартный бобёр","matvey-new-bober.jpg",  0L,       "common",    "classic",  true),
    SkinDef("paper",      "Бумажный бобёр",   "bumazny-bober.jpg",     5_000L,   "common",    "fun"),
    SkinDef("strawberry", "Клубничный бобёр", "klub-smz-bober.jpg",    15_000L,  "uncommon",  "food"),
    SkinDef("sock",       "Носок бобёр",      "nosok-bober.jpg",       30_000L,  "epic",      "fun"),
    SkinDef("chocolate",  "Шоколад бобёр",    "Shok-upok-bober.jpg",   50_000L,  "rare",      "food"),
    SkinDef("strange",    "Странный бобёр",   "strany-bober.jpg",      30_000L,  "rare",      "mystic"),
    SkinDef("dev",        "Dev бобёр",        "dev.png",               90_000L,  "admin",     "admin")
)

fun calcPlus(u: UpgradeCounts): Int =
    1 + u.tapSmall * 1 + u.tapBig * 5 + u.tapHuge * 100

fun calcEnergyMax(u: UpgradeCounts): Int =
    5000 + u.energy * 1000 + u.energyHuge * 10000

fun calcRegenPerMs(u: UpgradeCounts, max: Int): Float {
    val basePerMs = max.toFloat() / (15f * 60f * 1000f)
    val boostPerMs = u.regenBoost * 2f / 1000f
    return basePerMs + boostPerMs
}

fun formatScore(v: Long): String {
    if (v >= 1_000_000_000L) return "%.1fB".format(v / 1_000_000_000.0)
    if (v >= 1_000_000L)     return "%.1fM".format(v / 1_000_000.0)
    if (v >= 1_000L)         return "%.1fK".format(v / 1_000.0)
    return v.toString()
}
