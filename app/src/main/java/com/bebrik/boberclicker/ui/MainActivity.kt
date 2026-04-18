package com.bebrik.boberclicker.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bebrik.boberclicker.R
import com.bebrik.boberclicker.data.ALL_UPGRADES
import com.bebrik.boberclicker.data.formatScore
import com.bebrik.boberclicker.databinding.ActivityMainBinding
import com.bebrik.boberclicker.game.GameViewModel
import com.bebrik.boberclicker.minigame.MiniGameActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickButton()
        setupNavigation()
        observeState()
    }

    private fun setupClickButton() {
        val scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_down)
        val scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up)

        binding.btnBober.setOnClickListener {
            vm.onBoberClick()
            it.startAnimation(scaleDown)
            scaleDown.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(a: android.view.animation.Animation?) {}
                override fun onAnimationRepeat(a: android.view.animation.Animation?) {}
                override fun onAnimationEnd(a: android.view.animation.Animation?) { it.startAnimation(scaleUp) }
            })
        }
    }

    private fun setupNavigation() {
        binding.btnShop.setOnClickListener         { showPanel(binding.panelShop) }
        binding.btnQuests.setOnClickListener       { showPanel(binding.panelQuests) }
        binding.btnAchievements.setOnClickListener { showPanel(binding.panelAchievements) }
        binding.btnMinigame.setOnClickListener {
            startActivity(Intent(this, MiniGameActivity::class.java))
        }
        binding.btnCloseShop.setOnClickListener         { hideAllPanels() }
        binding.btnCloseQuests.setOnClickListener       { hideAllPanels() }
        binding.btnCloseAchievements.setOnClickListener { hideAllPanels() }
    }

    private fun observeState() {
        lifecycleScope.launch {
            vm.state.collect { state ->
                // Основная статистика
                binding.tvScore.text = state.score.formatScore() + " 🦫"
                binding.tvPassive.text = "+${state.passiveIncome.formatScore()}/сек"
                binding.tvClickPower.text = "+${state.clickPower.formatScore()} за клик"

                // Апгрейды
                ShopAdapter.update(binding.listShop, ALL_UPGRADES, state.upgradeLevels, state.score) { id ->
                    vm.buyUpgrade(id)
                }

                // Квесты
                QuestAdapter.update(binding.listQuests, state.quests) { id ->
                    vm.claimQuestReward(id)
                }

                // Достижения
                AchievementAdapter.update(binding.listAchievements, state.achievements)

                // Toast о новом достижении
                state.newAchievement?.let { ach ->
                    showAchievementToast("${ach.emoji} ${ach.title}")
                    vm.clearNewAchievement()
                }
            }
        }
    }

    private fun showPanel(panel: View) {
        hideAllPanels()
        panel.visibility = View.VISIBLE
    }

    private fun hideAllPanels() {
        binding.panelShop.visibility         = View.GONE
        binding.panelQuests.visibility       = View.GONE
        binding.panelAchievements.visibility = View.GONE
    }

    private fun showAchievementToast(text: String) {
        binding.tvAchievementToast.text = text
        binding.tvAchievementToast.visibility = View.VISIBLE
        binding.tvAchievementToast.animate()
            .alpha(1f).setDuration(300)
            .withEndAction {
                binding.tvAchievementToast.animate()
                    .alpha(0f).setStartDelay(2000).setDuration(500)
                    .withEndAction { binding.tvAchievementToast.visibility = View.GONE }
                    .start()
            }.start()
    }

    override fun onPause()  { super.onPause();  vm.saveGame() }
    override fun onStop()   { super.onStop();   vm.saveGame() }
}
