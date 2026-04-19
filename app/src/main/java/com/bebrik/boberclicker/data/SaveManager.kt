package com.bebrik.boberclicker.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object SaveManager {
    private const val PREF = "bober_save"

    fun save(ctx: Context, state: GameState) {
        val u = state.upgrades
        val s = state.skin
        val json = JSONObject().apply {
            put("score", state.score)
            put("tapSmall", u.tapSmall)
            put("tapBig", u.tapBig)
            put("energy", u.energy)
            put("tapHuge", u.tapHuge)
            put("regenBoost", u.regenBoost)
            put("energyHuge", u.energyHuge)
            put("equippedSkinId", s.equippedSkinId)
            put("ownedSkins", JSONArray(s.ownedSkinIds))
            put("currentEnergy", state.energy)
            put("lastEnergyUpdate", state.lastEnergyUpdate)
        }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString("v2", json.toString()).apply()
    }

    fun load(ctx: Context): GameState {
        val raw = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("v2", null) ?: return GameState()
        return try {
            val j = JSONObject(raw)
            val u = UpgradeCounts(
                tapSmall   = j.optInt("tapSmall"),
                tapBig     = j.optInt("tapBig"),
                energy     = j.optInt("energy"),
                tapHuge    = j.optInt("tapHuge"),
                regenBoost = j.optInt("regenBoost"),
                energyHuge = j.optInt("energyHuge")
            )
            val ownedArr = j.optJSONArray("ownedSkins")
            val owned = mutableListOf<String>()
            if (ownedArr != null) {
                for (i in 0 until ownedArr.length()) owned.add(ownedArr.getString(i))
            }
            if (owned.isEmpty()) owned.addAll(listOf("classic", "standard"))
            val skin = SkinState(
                equippedSkinId = j.optString("equippedSkinId", "classic"),
                ownedSkinIds   = owned
            )
            val max = calcEnergyMax(u)
            val savedEnergy = j.optDouble("currentEnergy", max.toDouble()).toFloat()
            val lastUpdate  = j.optLong("lastEnergyUpdate", System.currentTimeMillis())
            GameState(
                score           = j.optLong("score"),
                plus            = calcPlus(u),
                energyMax       = max,
                energy          = savedEnergy.coerceIn(0f, max.toFloat()),
                lastEnergyUpdate = lastUpdate,
                upgrades        = u,
                skin            = skin
            )
        } catch (e: Exception) {
            GameState()
        }
    }
}
