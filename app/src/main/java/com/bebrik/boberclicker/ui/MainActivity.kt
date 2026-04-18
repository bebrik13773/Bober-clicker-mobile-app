package com.bebrik.boberclicker.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bebrik.boberclicker.R
import com.bebrik.boberclicker.data.*
import com.bebrik.boberclicker.databinding.ActivityMainBinding
import com.bebrik.boberclicker.game.GameViewModel
import com.bebrik.boberclicker.minigame.MiniGameActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var vm: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this)[GameViewModel::class.java]
        setupClick()
        setupNav()
        observe()
    }

    private fun setupClick() {
        val scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_down)
        val scaleUp   = AnimationUtils.loadAnimation(this, R.anim.scale_up)

        binding.btnBober.setOnClickListener {
            val ok = vm.onBoberClick()
            if (ok) {
                it.startAnimation(scaleDown)
                scaleDown.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(a: android.view.animation.Animation?) {}
                    override fun onAnimationRepeat(a: android.view.animation.Animation?) {}
                    override fun onAnimationEnd(a: android.view.animation.Animation?) { it.startAnimation(scaleUp) }
                })
            } else {
                // Тряска — нет энергии
                it.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
            }
        }
    }

    private fun setupNav() {
        binding.btnShop.setOnClickListener         { showPanel(binding.panelShop) }
        binding.btnSkins.setOnClickListener        { showPanel(binding.panelSkins) }
        binding.btnAchievements.setOnClickListener { showPanel(binding.panelAchievements) }
        binding.btnFly.setOnClickListener          { startActivity(Intent(this, MiniGameActivity::class.java)) }

        binding.btnCloseShop.setOnClickListener         { hideAll() }
        binding.btnCloseSkins.setOnClickListener        { hideAll() }
        binding.btnCloseAchievements.setOnClickListener { hideAll() }
    }

    private fun observe() {
        vm.state.observe(this) { s ->
            // Счёт
            binding.tvScore.text = s.score.fmt()

            // Энергия
            val energyPct = (s.energy / s.energyMax).coerceIn(0.0, 1.0)
            binding.progressEnergy.progress = (energyPct * 1000).toInt()
            binding.tvEnergy.text = "${s.energy.fmt()} / ${s.energyMax.fmt()}"
            binding.tvPlus.text = "+${s.plus.fmt()} / 🔋${s.regenPerSec.fmt()}/сек"

            // Картинка бобра — выбранный скин
            loadBoberSkin(s.equippedSkin)

            // Блокировка кнопки по энергии
            binding.btnBober.alpha = if (s.energy >= s.plus) 1f else 0.5f

            // Магазин улучшений
            ShopUpgradeAdapter.update(binding.listShop, ALL_UPGRADES, s.upgrades, s.score) {
                vm.buyUpgrade(it)
            }

            // Магазин скинов
            ShopSkinAdapter.update(binding.listSkins, ALL_SKINS, s.ownedSkinIds, s.equippedSkin, s.score,
                onBuy = { vm.buySkin(it) },
                onEquip = { vm.equipSkin(it) }
            )

            // Достижения
            AchievementAdapter.update(binding.listAchievements, s.achievements)

            // Тост нового достижения
            s.newAchievement?.let {
                showToast("${it.icon} ${it.title}")
                vm.clearNewAchievement()
            }
        }
    }

    private fun loadBoberSkin(skinId: String) {
        val skin = ALL_SKINS.find { it.id == skinId } ?: ALL_SKINS.first()
        try {
            assets.open("skins/${skin.assetFile}").use { stream ->
                binding.btnBober.setImageBitmap(BitmapFactory.decodeStream(stream))
            }
        } catch (e: Exception) {
            binding.btnBober.setImageResource(android.R.drawable.sym_def_app_icon)
        }
    }

    private fun showPanel(panel: View) { hideAll(); panel.visibility = View.VISIBLE }

    private fun hideAll() {
        binding.panelShop.visibility         = View.GONE
        binding.panelSkins.visibility        = View.GONE
        binding.panelAchievements.visibility = View.GONE
    }

    private fun showToast(text: String) {
        binding.tvToast.text = text
        binding.tvToast.visibility = View.VISIBLE
        binding.tvToast.alpha = 1f
        binding.tvToast.animate().alpha(0f).setStartDelay(2200).setDuration(500)
            .withEndAction { binding.tvToast.visibility = View.GONE }.start()
    }

    override fun onPause() { super.onPause(); vm.saveGame() }
    override fun onStop()  { super.onStop();  vm.saveGame() }
}
