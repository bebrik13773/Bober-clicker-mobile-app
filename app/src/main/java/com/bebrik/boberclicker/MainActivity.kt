package com.bebrik.boberclicker

import android.animation.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bebrik.boberclicker.data.*
import com.bebrik.boberclicker.data.ApiClient
import com.bebrik.boberclicker.ui.LoginActivity
import com.bebrik.boberclicker.ui.ShopActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvScore:      TextView
    private lateinit var tvPlus:       TextView
    private lateinit var tvEnergy:     TextView
    private lateinit var tvEnergyMax:  TextView
    private lateinit var progressEnergy: ProgressBar
    private lateinit var ivBober:      ImageView
    private lateinit var tvSyncStatus: TextView
    private lateinit var tvLb1:        TextView
    private lateinit var tvLb2:        TextView
    private lateinit var tvLb3:        TextView

    private val handler = Handler(Looper.getMainLooper())
    private val energyRunnable = object : Runnable {
        override fun run() {
            GameRepository.tickEnergy()
            updateEnergyUI()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false; isAppearanceLightNavigationBars = false
        }
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // Route: if not logged in → login screen
        if (!GameRepository.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            return
        }

        ApiClient.activityContext = this
        buildUI()
        setupCallbacks()
        handler.post(energyRunnable)

        // Try restore session from cookie
        GameRepository.tryRestoreSession(this) { /* handled via callbacks */ }
        GameRepository.startPeriodicSync(this)
    }

    // =============== UI BUILD ===============

        buildUI() {
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
            setPadding(dp(16), dp(52), dp(16), dp(32))
        }

        // --- Sync status bar ---
        tvSyncStatus = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.parseColor("#7EF1FF"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = dp(6)
            }
            text = "🔄 Синхронизация…"
        }
        content.addView(tvSyncStatus)

        // --- Title ---
        content.addView(TextView(this).apply {
            text = "БОБЁР 🦫 КЛИКЕР 2"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = glassCard("#22FFFFFF", "#557EF1FF", 22)
            setPadding(dp(20), dp(14), dp(20), dp(14))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        })
        content.addView(spacer(10))

        // --- Score card ---
        val scoreCard = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            background = glassCard("#1E0A44", "#337EF1FF", 22)
            setPadding(dp(20), dp(14), dp(20), dp(14))
        }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        tvScore = textView("Счёт: 0", 30f, Color.WHITE, Typeface.DEFAULT_BOLD, Gravity.CENTER)
        tvPlus  = textView("+1 за тап", 14f, Color.parseColor("#9FEEFF"), Typeface.DEFAULT, Gravity.CENTER)
        val userRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(6) }
        }
        userRow.addView(textView("👤 ${GameRepository.login}", 13f, Color.parseColor("#88CCDD")))
        userRow.addView(textView("   ", 13f, Color.TRANSPARENT))
        val logoutBtn = textView("Выйти", 12f, Color.parseColor("#FF8888"))
        logoutBtn.setOnClickListener {
            GameRepository.logout(this)
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
        userRow.addView(logoutBtn)
        col.addView(tvScore); col.addView(tvPlus); col.addView(userRow)
        scoreCard.addView(col); content.addView(scoreCard)
        content.addView(spacer(20))

        // --- Bober button ---
        ivBober = ImageView(this).apply {
            val s = dp(280)
            layoutParams = LinearLayout.LayoutParams(s, s).apply { gravity = Gravity.CENTER_HORIZONTAL }
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1A0A35"))
                setStroke(dp(3), Color.parseColor("#447EF1FF"))
            }
            clipToOutline = true; outlineProvider = ViewOutlineProvider.BACKGROUND
            elevation = dp(10).toFloat()
            setOnClickListener { handleTap(this) }
        }
        content.addView(ivBober)
        content.addView(spacer(20))

        // --- Energy ---
        val ep = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            background = glassCard("#1A0A35", "#22FFFFFF", 20)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val er = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        er.addView(textView("⚡", 24f, Color.WHITE))
        tvEnergy = textView("5000", 22f, Color.parseColor("#FFE600"), Typeface.DEFAULT_BOLD)
        tvEnergy.setPadding(dp(6), 0, 0, 0)
        tvEnergyMax = textView(" / 5000", 14f, Color.parseColor("#7EF1FF"))
        er.addView(tvEnergy); er.addView(tvEnergyMax)
        ep.addView(er); ep.addView(spacer(8))
        progressEnergy = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(8))
            progressDrawable = LayerDrawable(arrayOf(
                GradientDrawable().apply { cornerRadius = dp(4).toFloat(); setColor(Color.parseColor("#22FFFFFF")) },
                ClipDrawable(GradientDrawable().apply { cornerRadius = dp(4).toFloat(); setColor(Color.parseColor("#7EF1FF")) }, Gravity.LEFT, ClipDrawable.HORIZONTAL)
            )).apply { setId(0, android.R.id.background); setId(1, android.R.id.progress) }
            max = 5000; progress = 5000
        }
        ep.addView(progressEnergy); content.addView(ep)
        content.addView(spacer(28))

        // --- Buttons ---
        content.addView(gradBtn("🏪  Магазин улучшений и скинов", "#5088FF", "#2859C2", dp(56), 16f, Color.WHITE) {
            startActivity(Intent(this, ShopActivity::class.java))
        })
        content.addView(spacer(10))
        content.addView(gradBtn("🪽  Летающий бобёр", "#7EF1FF", "#27A4FF", dp(60), 16f, Color.parseColor("#062035")) {
            Toast.makeText(this, "Летающий бобёр скоро! 🦫", Toast.LENGTH_SHORT).show()
        })
        content.addView(spacer(28))

        // --- Leaderboard ---
        content.addView(makeLeaderboard())

        scroll.addView(content); root.addView(scroll); setContentView(root)
        updateBoberImage()
        updateUI()
    }

    private fun makeLeaderboard(): LinearLayout {
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
        card.addView(textView("🏆 Топ-3 игроков", 22f, Color.parseColor("#C9FFFF"), Typeface.DEFAULT_BOLD, Gravity.CENTER).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        })
        card.addView(spacer(16))

        val ranks = listOf("🥇", "🥈", "🥉")
        tvLb1 = makeLbRow(ranks[0]); tvLb2 = makeLbRow(ranks[1]); tvLb3 = makeLbRow(ranks[2])
        card.addView(tvLb1); card.addView(spacer(8))
        card.addView(tvLb2); card.addView(spacer(8))
        card.addView(tvLb3)
        updateLeaderboardUI()
        return card
    }

    private fun makeLbRow(rank: String): TextView = TextView(this).apply {
        text = "$rank  —"
        textSize = 16f
        setTextColor(Color.parseColor("#DDFBFF"))
        background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(Color.parseColor("#221B143D"))
            setStroke(dp(1), Color.parseColor("#2233C5C7"))
        }
        setPadding(dp(14), dp(10), dp(14), dp(10))
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }

    // =============== GAME ===============

    private fun handleTap(v: View) {
        val ok = GameRepository.tap()
        AnimatorSet().apply {
            playSequentially(
                anim(v, 0.92f, 70), anim(v, 1.06f, 110), anim(v, 1.0f, 80)
            )
            start()
        }
        if (ok) {
            vibrate(16); updateScoreUI()
            updateEnergyUI()
        } else {
            ObjectAnimator.ofFloat(v, "translationX", 0f, -18f, 18f, -12f, 12f, -6f, 6f, 0f).apply { duration = 360; start() }
            showToast("⚡ Нет энергии!")
        }
    }

    private fun anim(v: View, scale: Float, dur: Long) = AnimatorSet().apply {
        playTogether(ObjectAnimator.ofFloat(v, "scaleX", scale), ObjectAnimator.ofFloat(v, "scaleY", scale))
        duration = dur
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

    // =============== CALLBACKS ===============

    private fun setupCallbacks() {
        GameRepository.onStateChanged = { runOnUiThread { updateUI() } }
        GameRepository.onLeaderboardChanged = { runOnUiThread { updateLeaderboardUI() } }
        GameRepository.onSyncStatusChanged = { online, msg ->
            runOnUiThread {
                GameRepository.isOnline = online
                tvSyncStatus.text = if (online) "✅ $msg" else "⚠️ Нет сети — $msg"
                tvSyncStatus.setTextColor(if (online) Color.parseColor("#7EF1FF") else Color.parseColor("#FFAA44"))
            }
        }
    }

    // =============== UI UPDATE ===============

    private fun updateUI() {
        updateScoreUI(); updateEnergyUI(); updateBoberImage()
    }

    private fun updateScoreUI() {
        tvScore.text = "Счёт: ${formatScore(GameRepository.score)}"
        tvPlus.text  = "+${GameRepository.plus} за тап"
    }

    private fun updateEnergyUI() {
        val e = GameRepository.energy
        val m = GameRepository.energyMax
        tvEnergy.text    = formatScore(e.toLong())
        tvEnergyMax.text = " / ${formatScore(m.toLong())}"
        progressEnergy.max      = m
        progressEnergy.progress = e.toInt().coerceAtLeast(0)
    }

    private fun updateBoberImage() {
        val id  = GameRepository.skin.equippedSkinId
        val def = SKIN_DEFINITIONS.find { it.id == id } ?: SKIN_DEFINITIONS.first()
        try {
            val bmp = assets.open("skins/${def.assetName}").use { BitmapFactory.decodeStream(it) }
            ivBober.setImageBitmap(bmp)
        } catch (_: Exception) {}
    }

    private fun updateLeaderboardUI() {
        val lb = GameRepository.leaderboard
        val ranks = listOf("🥇", "🥈", "🥉")
        listOf(tvLb1, tvLb2, tvLb3).forEachIndexed { i, tv ->
            val e = lb.getOrNull(i)
            tv.text = if (e != null) "${ranks[i]}  ${e.login}  —  ${formatScore(e.score)}"
                      else "${ranks[i]}  —"
        }
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    // =============== HELPERS ===============

    private fun glassCard(bg: String, border: String, r: Int) = GradientDrawable().apply {
        cornerRadius = dp(r).toFloat()
        setColor(Color.parseColor(bg))
        setStroke(dp(1), Color.parseColor(border))
    }

    private fun gradBtn(text: String, c1: String, c2: String, h: Int, sz: Float, tc: Int, action: () -> Unit) =
        Button(this).apply {
            this.text = text; textSize = sz; typeface = Typeface.DEFAULT_BOLD
            setTextColor(tc)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, h)
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor(c1), Color.parseColor(c2))).apply { cornerRadius = dp(18).toFloat() }
            setOnClickListener { action() }
        }

    private fun textView(t: String, sz: Float, color: Int, tf: Typeface = Typeface.DEFAULT, g: Int = Gravity.START) =
        TextView(this).apply { text = t; textSize = sz; typeface = tf; setTextColor(color); gravity = g }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun spacer(v: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(v)) }
    private val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP_CONTENT  = ViewGroup.LayoutParams.WRAP_CONTENT

    override fun onResume() {
        super.onResume()
        if (::ivBober.isInitialized) { updateUI(); GameRepository.syncNow(this) }
    }

    override fun onStop() {
        super.onStop()
        GameRepository.tickEnergy()
        GameRepository.saveLocal(this)
    }

    override fun onDestroy() {
        handler.removeCallbacks(energyRunnable)
        GameRepository.onStateChanged = null
        GameRepository.onLeaderboardChanged = null
        GameRepository.onSyncStatusChanged = null
        super.onDestroy()
    }
}
