package com.bebrik.boberclicker

import android.animation.*
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.bebrik.boberclicker.data.*
import com.bebrik.boberclicker.game.GameViewModel
import com.bebrik.boberclicker.ui.ShopActivity

class MainActivity : AppCompatActivity() {

    private lateinit var vm: GameViewModel
    private lateinit var ivBober: ImageView
    private lateinit var tvScore: TextView
    private lateinit var tvEnergy: TextView
    private lateinit var tvEnergyMax: TextView
    private lateinit var tvPlus: TextView
    private lateinit var progressEnergy: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        vm = ViewModelProvider(this)[GameViewModel::class.java]
        vm.init(this)

        buildUI()
        setupObservers()
    }

    private fun buildUI() {
        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#6B11D7"), Color.parseColor("#45118A"), Color.parseColor("#1C0F42"))
            )
        }

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(56), dp(16), dp(32))
        }

        content.addView(makeTitle())
        content.addView(spacer(12))
        content.addView(makeScoreCard())
        content.addView(spacer(20))
        ivBober = makeBoberButton().also { content.addView(it) }
        content.addView(spacer(20))
        content.addView(makeEnergyPanel())
        content.addView(spacer(28))
        content.addView(makeShopBtn())
        content.addView(spacer(10))
        content.addView(makeFlyBtn())
        content.addView(spacer(28))
        content.addView(makeLeaderboardCard())

        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
        updateBoberImage(vm.getSkin().equippedSkinId)
    }

    private fun makeTitle() = TextView(this).apply {
        text = "БОБЁР 🦫 КЛИКЕР 2"
        textSize = 20f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            cornerRadius = dp(22).toFloat()
            setColor(Color.parseColor("#22FFFFFF"))
            setStroke(dp(1), Color.parseColor("#557EF1FF"))
        }
        setPadding(dp(20), dp(16), dp(20), dp(16))
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }

    private fun makeScoreCard(): FrameLayout {
        val card = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            background = GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(Color.parseColor("#1E0A44"))
                setStroke(dp(1), Color.parseColor("#337EF1FF"))
            }
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        tvScore = TextView(this).apply {
            text = "Счёт: 0"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        tvPlus = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#9FEEFF"))
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        }
        col.addView(tvScore)
        col.addView(tvPlus)
        card.addView(col)
        return card
    }

    private fun makeBoberButton(): ImageView {
        val s = dp(280)
        return ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(s, s).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1A0A35"))
                setStroke(dp(3), Color.parseColor("#447EF1FF"))
            }
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
            elevation = dp(10).toFloat()
            setOnClickListener { handleTap(this) }
        }
    }

    private fun makeEnergyPanel(): LinearLayout {
        val p = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(Color.parseColor("#1A0A35"))
                setStroke(dp(1), Color.parseColor("#22FFFFFF"))
            }
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(TextView(this).apply { text = "⚡"; textSize = 24f })
        tvEnergy = TextView(this).apply {
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#FFE600"))
            setPadding(dp(6), 0, 0, 0)
        }
        tvEnergyMax = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#7EF1FF"))
            setPadding(dp(4), 0, 0, 0)
        }
        row.addView(tvEnergy)
        row.addView(tvEnergyMax)
        p.addView(row)
        p.addView(spacer(8))
        progressEnergy = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(8))
            progressDrawable = LayerDrawable(arrayOf(
                GradientDrawable().apply { cornerRadius = dp(4).toFloat(); setColor(Color.parseColor("#22FFFFFF")) },
                ClipDrawable(GradientDrawable().apply { cornerRadius = dp(4).toFloat(); setColor(Color.parseColor("#7EF1FF")) }, Gravity.LEFT, ClipDrawable.HORIZONTAL)
            )).apply { setId(0, android.R.id.background); setId(1, android.R.id.progress) }
            max = 5000; progress = 5000
        }
        p.addView(progressEnergy)
        return p
    }

    private fun makeShopBtn() = Button(this).apply {
        text = "🏪  Магазин улучшений и скинов"
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(56))
        background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#5088FF"), Color.parseColor("#2859C2"))).apply {
            cornerRadius = dp(16).toFloat()
        }
        setOnClickListener { startActivity(Intent(this@MainActivity, ShopActivity::class.java)) }
    }

    private fun makeFlyBtn() = Button(this).apply {
        text = "🪽  Летающий бобёр"
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.parseColor("#062035"))
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(60))
        background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#7EF1FF"), Color.parseColor("#27A4FF"))).apply {
            cornerRadius = dp(20).toFloat()
        }
        setOnClickListener { Toast.makeText(context, "Летающий бобёр скоро! 🦫", Toast.LENGTH_SHORT).show() }
    }

    private fun makeLeaderboardCard(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(Color.parseColor("#22084266"))
                setStroke(dp(2), Color.parseColor("#9933C5C7"))
            }
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        card.addView(TextView(this).apply {
            text = "🏆 Топ-3 игроков"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#C9FFFF"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        })
        card.addView(spacer(8))
        card.addView(TextView(this).apply {
            text = "Облачный рейтинг доступен на сайте\nbober-api.gt.tc"
            textSize = 13f
            setTextColor(Color.parseColor("#88CCDD"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        })
        return card
    }

    private fun handleTap(v: View) {
        // Animate: scale down → up → normal
        AnimatorSet().apply {
            playSequentially(
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.92f),
                        ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.92f)
                    ); duration = 70
                },
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(v, "scaleX", 0.92f, 1.06f),
                        ObjectAnimator.ofFloat(v, "scaleY", 0.92f, 1.06f)
                    ); duration = 110
                },
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(v, "scaleX", 1.06f, 1f),
                        ObjectAnimator.ofFloat(v, "scaleY", 1.06f, 1f)
                    ); duration = 80
                }
            )
            start()
        }
        vibrate(16)
        vm.tap()
    }

    private fun vibrate(ms: Long) {
        try {
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            else @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") vib.vibrate(ms)
        } catch (_: Exception) {}
    }

    private fun setupObservers() {
        vm.score.observe(this) { tvScore.text = "Счёт: ${formatScore(it)}" }
        vm.plus.observe(this) { tvPlus.text = "+$it за тап" }
        vm.energy.observe(this) { e ->
            val max = vm.energyMax.value ?: 5000
            tvEnergy.text = formatScore(e.toLong())
            progressEnergy.max = max
            progressEnergy.progress = e.toInt().coerceAtLeast(0)
        }
        vm.energyMax.observe(this) { max ->
            tvEnergyMax.text = " / ${formatScore(max.toLong())}"
            progressEnergy.max = max
        }
        vm.skin.observe(this) { updateBoberImage(it.equippedSkinId) }
        vm.toastMsg.observe(this) { msg ->
            if (msg != null) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); vm.toastMsg.value = null }
        }
        vm.noEnergy.observe(this) { no ->
            if (no == true) {
                ObjectAnimator.ofFloat(ivBober, "translationX", 0f, -18f, 18f, -12f, 12f, -6f, 6f, 0f).apply { duration = 360; start() }
                Toast.makeText(this, "⚡ Нет энергии!", Toast.LENGTH_SHORT).show()
                vm.noEnergy.value = false
            }
        }
    }

    private fun updateBoberImage(id: String) {
        val def = SKIN_DEFINITIONS.find { it.id == id } ?: SKIN_DEFINITIONS.first()
        try {
            val bmp = assets.open("skins/${def.assetName}").use { BitmapFactory.decodeStream(it) }
            ivBober.setImageBitmap(bmp)
        } catch (_: Exception) {}
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun spacer(v: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(v)) }

    override fun onStop() { super.onStop(); vm.save(this) }

    companion object {
        private const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
        private const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
