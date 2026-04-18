package com.bebrik.boberclicker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.bebrik.boberclicker.R
import com.bebrik.boberclicker.data.*
import com.bebrik.boberclicker.game.QuestState

// ─── Магазин ─────────────────────────────────────────────────────

object ShopAdapter {
    fun update(
        container: LinearLayout,
        upgrades: List<UpgradeDef>,
        levels: Map<String, Int>,
        score: Double,
        onBuy: (String) -> Unit
    ) {
        if (container.childCount != upgrades.size) {
            container.removeAllViews()
            val inflater = LayoutInflater.from(container.context)
            for (def in upgrades) {
                val row = inflater.inflate(R.layout.item_shop, container, false)
                row.tag = def.id
                container.addView(row)
            }
        }
        for (i in upgrades.indices) {
            val def = upgrades[i]
            val row = container.getChildAt(i)
            val level = levels[def.id] ?: 0
            val cost = def.costAt(level)
            val maxed = level >= def.maxLevel

            row.findViewById<TextView>(R.id.tvName).text    = "${def.emoji} ${def.name}"
            row.findViewById<TextView>(R.id.tvDesc).text    = def.description
            row.findViewById<TextView>(R.id.tvLevel).text   = if (maxed) "МАКС" else "Ур.$level/${def.maxLevel}"
            val btn = row.findViewById<Button>(R.id.btnBuy)
            btn.text = if (maxed) "Макс" else "💰 ${cost.formatScore()}"
            btn.isEnabled = !maxed && score >= cost
            btn.setOnClickListener { onBuy(def.id) }
        }
    }
}

// ─── Квесты ──────────────────────────────────────────────────────

object QuestAdapter {
    fun update(
        container: LinearLayout,
        quests: List<QuestState>,
        onClaim: (String) -> Unit
    ) {
        if (container.childCount != quests.size) {
            container.removeAllViews()
            val inflater = LayoutInflater.from(container.context)
            for (q in quests) {
                val row = inflater.inflate(R.layout.item_quest, container, false)
                row.tag = q.def.id
                container.addView(row)
            }
        }
        for (i in quests.indices) {
            val q = quests[i]
            val row = container.getChildAt(i)

            row.findViewById<TextView>(R.id.tvTitle).text    = q.def.title
            row.findViewById<TextView>(R.id.tvDesc).text     = q.def.description
            row.findViewById<TextView>(R.id.tvReward).text   = "+${q.def.reward.formatScore()} 🦫"

            val progressBar = row.findViewById<android.widget.ProgressBar>(R.id.progressBar)
            val tvProgress  = row.findViewById<TextView>(R.id.tvProgress)
            val btnClaim    = row.findViewById<Button>(R.id.btnClaim)

            val pct = (q.progress / q.def.target).coerceIn(0.0, 1.0)
            progressBar.progress = (pct * 100).toInt()

            tvProgress.text = if (q.completed) "✅ Выполнено!" else
                "${q.progress.formatScore()} / ${q.def.target.formatScore()}"

            btnClaim.visibility = if (q.completed && !q.claimed) View.VISIBLE else View.GONE
            btnClaim.setOnClickListener { onClaim(q.def.id) }

            row.alpha = if (q.claimed) 0.5f else 1f
        }
    }
}

// ─── Достижения ──────────────────────────────────────────────────

object AchievementAdapter {
    fun update(container: LinearLayout, unlocked: Set<String>) {
        if (container.childCount != ALL_ACHIEVEMENTS.size) {
            container.removeAllViews()
            val inflater = LayoutInflater.from(container.context)
            for (ach in ALL_ACHIEVEMENTS) {
                val row = inflater.inflate(R.layout.item_achievement, container, false)
                container.addView(row)
            }
        }
        for (i in ALL_ACHIEVEMENTS.indices) {
            val ach = ALL_ACHIEVEMENTS[i]
            val row = container.getChildAt(i)
            val isUnlocked = unlocked.contains(ach.id)

            row.findViewById<TextView>(R.id.tvEmoji).text = ach.emoji
            row.findViewById<TextView>(R.id.tvTitle).text = if (isUnlocked) ach.title else "???"
            row.findViewById<TextView>(R.id.tvDesc).text  = if (isUnlocked) ach.description else "Не разблокировано"
            row.alpha = if (isUnlocked) 1f else 0.4f
        }
    }
}
