package com.bebrik.boberclicker.ui

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bebrik.boberclicker.R
import com.bebrik.boberclicker.data.*

// ─── Магазин улучшений ───────────────────────────────────────────

object ShopUpgradeAdapter {
    fun update(
        container: LinearLayout,
        defs: List<UpgradeDef>,
        counts: UpgradeCounts,
        score: Double,
        onBuy: (String) -> Unit
    ) {
        if (container.childCount != defs.size) {
            container.removeAllViews()
            val inf = LayoutInflater.from(container.context)
            defs.forEach { container.addView(inf.inflate(R.layout.item_upgrade, container, false)) }
        }
        defs.forEachIndexed { i, def ->
            val row   = container.getChildAt(i)
            val level = def.getLevel(counts)
            row.findViewById<TextView>(R.id.tvEmoji).text   = def.emoji
            row.findViewById<TextView>(R.id.tvName).text    = def.name
            row.findViewById<TextView>(R.id.tvDesc).text    = def.description
            row.findViewById<TextView>(R.id.tvLevel).text   = "Ур. $level"
            val btn = row.findViewById<Button>(R.id.btnBuy)
            btn.text = "💰 ${def.baseCost.fmt()}"
            btn.isEnabled = score >= def.baseCost
            btn.setOnClickListener { onBuy(def.id) }
        }
    }
}

// ─── Магазин скинов ──────────────────────────────────────────────

object ShopSkinAdapter {
    fun update(
        container: LinearLayout,
        skins: List<SkinDef>,
        owned: Set<String>,
        equipped: String,
        score: Double,
        onBuy: (String) -> Unit,
        onEquip: (String) -> Unit
    ) {
        val visible = skins.filter { !it.grantOnly }
        if (container.childCount != visible.size) {
            container.removeAllViews()
            val inf = LayoutInflater.from(container.context)
            visible.forEach { container.addView(inf.inflate(R.layout.item_skin, container, false)) }
        }
        visible.forEachIndexed { i, skin ->
            val row = container.getChildAt(i)
            val isOwned    = owned.contains(skin.id)
            val isEquipped = skin.id == equipped

            row.findViewById<TextView>(R.id.tvSkinName).text   = skin.name
            row.findViewById<TextView>(R.id.tvSkinRarity).text = rarityLabel(skin.rarity)
            row.findViewById<TextView>(R.id.tvSkinRarity).setTextColor(rarityColor(skin.rarity, row))

            // Картинка
            val img = row.findViewById<ImageView>(R.id.imgSkin)
            try {
                row.context.assets.open("skins/${skin.assetFile}").use { stream ->
                    img.setImageBitmap(BitmapFactory.decodeStream(stream))
                }
            } catch (e: Exception) { /* оставить пустым */ }

            val btn = row.findViewById<Button>(R.id.btnSkinAction)
            when {
                isEquipped -> { btn.text = "✓ Надет"; btn.isEnabled = false }
                isOwned    -> { btn.text = "Надеть";  btn.isEnabled = true; btn.setOnClickListener { onEquip(skin.id) } }
                skin.price == 0.0 -> { btn.text = "Получить"; btn.isEnabled = true; btn.setOnClickListener { onBuy(skin.id) } }
                else       -> {
                    btn.text = "💰 ${skin.price.fmt()}"
                    btn.isEnabled = score >= skin.price
                    btn.setOnClickListener { onBuy(skin.id) }
                }
            }
        }
    }

    private fun rarityLabel(r: String) = when(r) {
        "common"   -> "Обычный"
        "uncommon" -> "Необычный"
        "rare"     -> "Редкий"
        "epic"     -> "Эпик"
        "legendary"-> "Легендарный"
        "admin"    -> "Admin"
        else -> r
    }

    private fun rarityColor(r: String, v: View): Int {
        val ctx = v.context
        return when(r) {
            "common"    -> ctx.getColor(android.R.color.darker_gray)
            "uncommon"  -> 0xFF4CAF50.toInt()
            "rare"      -> 0xFF2196F3.toInt()
            "epic"      -> 0xFF9C27B0.toInt()
            "legendary" -> 0xFFFFD700.toInt()
            "admin"     -> 0xFFFF5722.toInt()
            else        -> ctx.getColor(android.R.color.darker_gray)
        }
    }
}

// ─── Достижения ──────────────────────────────────────────────────

object AchievementAdapter {
    fun update(container: LinearLayout, unlocked: Set<String>) {
        if (container.childCount != ALL_ACHIEVEMENTS.size) {
            container.removeAllViews()
            val inf = LayoutInflater.from(container.context)
            ALL_ACHIEVEMENTS.forEach { container.addView(inf.inflate(R.layout.item_achievement, container, false)) }
        }
        ALL_ACHIEVEMENTS.forEachIndexed { i, ach ->
            val row = container.getChildAt(i)
            val isUnlocked = unlocked.contains(ach.id)
            row.findViewById<TextView>(R.id.tvEmoji).text = if (isUnlocked) ach.icon else "🔒"
            row.findViewById<TextView>(R.id.tvTitle).text = if (isUnlocked) ach.title else "???"
            row.findViewById<TextView>(R.id.tvDesc).text  = if (isUnlocked) ach.description else "Не разблокировано"
            row.alpha = if (isUnlocked) 1f else 0.35f
        }
    }
}
