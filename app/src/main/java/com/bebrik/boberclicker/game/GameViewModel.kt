package com.bebrik.boberclicker.game

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.bebrik.boberclicker.data.*

class GameViewModel(app: Application) : AndroidViewModel(app) {

    val score       = MutableLiveData(0L)
    val energy      = MutableLiveData(5000f)
    val energyMax   = MutableLiveData(5000)
    val plus        = MutableLiveData(1)
    val upgrades    = MutableLiveData(UpgradeCounts())
    val skin        = MutableLiveData(SkinState())
    val toastMsg    = MutableLiveData<String?>()
    val noEnergy    = MutableLiveData(false)

    private var _score: Long = 0L
    private var _energy: Float = 5000f
    private var _energyMax: Int = 5000
    private var _plus: Int = 1
    private var _upgrades: UpgradeCounts = UpgradeCounts()
    private var _skin: SkinState = SkinState()
    private var lastEnergyUpdate: Long = System.currentTimeMillis()
    private var regenPerMs: Float = calcRegenPerMs(UpgradeCounts(), 5000)

    private val handler = Handler(Looper.getMainLooper())
    private val regenRunnable = object : Runnable {
        override fun run() {
            tickRegen()
            handler.postDelayed(this, 500)
        }
    }

    fun init(ctx: android.content.Context) {
        val state = SaveManager.load(ctx)
        _score     = state.score
        _upgrades  = state.upgrades
        _skin      = state.skin
        _energyMax = state.energyMax
        _plus      = state.plus
        lastEnergyUpdate = state.lastEnergyUpdate
        regenPerMs = calcRegenPerMs(_upgrades, _energyMax)

        // Восстанавливаем энергию за оффлайн-время
        val offlineMs = System.currentTimeMillis() - lastEnergyUpdate
        _energy = (state.energy + offlineMs * regenPerMs).coerceAtMost(_energyMax.toFloat())
        lastEnergyUpdate = System.currentTimeMillis()

        pushAll()
        handler.postDelayed(regenRunnable, 500)
    }

    private fun tickRegen() {
        val now = System.currentTimeMillis()
        val delta = now - lastEnergyUpdate
        lastEnergyUpdate = now
        _energy = (_energy + delta * regenPerMs).coerceAtMost(_energyMax.toFloat())
        energy.value = _energy
    }

    fun tap() {
        if (_energy < _plus) {
            noEnergy.value = true
            return
        }
        _score  += _plus
        _energy -= _plus
        score.value  = _score
        energy.value = _energy
    }

    fun buyUpgrade(type: String, ctx: android.content.Context): Boolean {
        val def = UPGRADE_DEFINITIONS.find { it.type == type } ?: return false
        val cost = def.baseCost
        if (_score < cost) {
            toastMsg.value = "Недостаточно коинов. Нужно: ${formatScore(cost)}"
            return false
        }
        _score -= cost
        _upgrades = when (type) {
            "tapSmall"   -> _upgrades.copy(tapSmall   = _upgrades.tapSmall   + 1)
            "tapBig"     -> _upgrades.copy(tapBig     = _upgrades.tapBig     + 1)
            "energy"     -> _upgrades.copy(energy     = _upgrades.energy     + 1)
            "tapHuge"    -> _upgrades.copy(tapHuge    = _upgrades.tapHuge    + 1)
            "regenBoost" -> _upgrades.copy(regenBoost = _upgrades.regenBoost + 1)
            "energyHuge" -> _upgrades.copy(energyHuge = _upgrades.energyHuge + 1)
            else -> _upgrades
        }
        _plus      = calcPlus(_upgrades)
        _energyMax = calcEnergyMax(_upgrades)
        regenPerMs = calcRegenPerMs(_upgrades, _energyMax)
        pushAll()
        save(ctx)
        toastMsg.value = "Улучшение куплено!"
        return true
    }

    fun buySkin(skinId: String, ctx: android.content.Context): Boolean {
        val def = SKIN_DEFINITIONS.find { it.id == skinId } ?: return false
        if (_skin.ownedSkinIds.contains(skinId)) {
            // Already owned — equip
            _skin = _skin.copy(equippedSkinId = skinId)
            skin.value = _skin
            save(ctx)
            toastMsg.value = "Скин установлен!"
            return true
        }
        if (_score < def.price) {
            toastMsg.value = "Недостаточно коинов. Нужно: ${formatScore(def.price)}"
            return false
        }
        _score -= def.price
        _skin = _skin.copy(
            equippedSkinId = skinId,
            ownedSkinIds   = _skin.ownedSkinIds + skinId
        )
        pushAll()
        save(ctx)
        toastMsg.value = "Скин куплен и установлен!"
        return true
    }

    fun save(ctx: android.content.Context) {
        SaveManager.save(ctx, GameState(
            score            = _score,
            plus             = _plus,
            energyMax        = _energyMax,
            energy           = _energy,
            lastEnergyUpdate = lastEnergyUpdate,
            upgrades         = _upgrades,
            skin             = _skin
        ))
    }

    fun getScore()     = _score
    fun getUpgrades()  = _upgrades
    fun getSkin()      = _skin
    fun getEnergyMax() = _energyMax

    private fun pushAll() {
        score.value     = _score
        energy.value    = _energy
        energyMax.value = _energyMax
        plus.value      = _plus
        upgrades.value  = _upgrades
        skin.value      = _skin
    }

    override fun onCleared() {
        handler.removeCallbacks(regenRunnable)
        super.onCleared()
    }
}
