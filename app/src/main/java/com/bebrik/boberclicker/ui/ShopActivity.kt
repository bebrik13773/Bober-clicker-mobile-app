package com.bebrik.boberclicker.ui

import android.animation.ObjectAnimator
import android.graphics.*
import android.graphics.drawable.*
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bebrik.boberclicker.data.*

class ShopActivity : AppCompatActivity() {

    private lateinit var tvBalance: TextView
    private lateinit var upgradeContainer: LinearLayout
    private lateinit var skinContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false; isAppearanceLightNavigationBars = false
        }
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // Subscribe to state changes
        GameRepository.onStateChanged = { runOnUiThread { refreshAll() } }
        buildUI()
    }

    private fun buildUI() {
        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#6B11D7"), Color.parseColor("#45118A"), Color.parseColor("#1C0F42"))
            )
        }
        val scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(52), dp(16), dp(32))
        }

        // Header
        val header = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(52)) }
        header.addView(TextView(this).apply {
            text = "← Назад"; textSize = 15f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#7EF1FF"))
            background = GradientDrawable().apply { cornerRadius = dp(12).toFloat(); setColor(Color.parseColor("#22FFFFFF")) }
            setPadding(dp(14), dp(8), dp(14), dp(8))
            layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { gravity = Gravity.CENTER_VERTICAL or Gravity.START }
            setOnClickListener { finish() }
        })
        header.addView(TextView(this).apply {
            text = "Магазин"; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { gravity = Gravity.CENTER }
        })
        content.addView(header); content.addView(spacer(12))

        // Balance
        val balRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            background = GradientDrawable().apply { cornerRadius = dp(18).toFloat(); setColor(Color.parseColor("#1E0A44")); setStroke(dp(1), Color.parseColor("#337EF1FF")) }
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        balRow.addView(tv("💰 Баланс: ", 22f, Color.parseColor("#C9FFFF")))
        tvBalance = tv("0", 22f, Color.WHITE, Typeface.DEFAULT_BOLD)
        balRow.addView(tvBalance)
        content.addView(balRow); content.addView(spacer(20))

        // Upgrades
        content.addView(tv("⬆️  Улучшения", 22f, Color.parseColor("#E9FFFF"), Typeface.DEFAULT_BOLD).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        })
        content.addView(spacer(10))
        upgradeContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT) }
        content.addView(upgradeContainer)
        content.addView(spacer(24))

        // Skins
        content.addView(tv("🎨  Скины", 22f, Color.parseColor("#E9FFFF"), Typeface.DEFAULT_BOLD).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        })
        content.addView(spacer(10))
        skinContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT) }
        content.addView(skinContainer)

        scroll.addView(content); root.addView(scroll); setContentView(root)
        refreshAll()
    }

    private fun refreshAll() {
        tvBalance.text = formatScore(GameRepository.score)
        buildUpgradeCards()
        buildSkinCards()
    }

    // ---- UPGRADES ----

    private fun buildUpgradeCards() {
        upgradeContainer.removeAllViews()
        val ups = GameRepository.upgrades
        UPGRADE_DEFINITIONS.forEach { def ->
            val count = when (def.type) {
                "tapSmall"   -> ups.tapSmall
                "tapBig"     -> ups.tapBig
                "energy"     -> ups.energy
                "tapHuge"    -> ups.tapHuge
                "regenBoost" -> ups.regenBoost
                "energyHuge" -> ups.energyHuge
                else -> 0
            }
            upgradeContainer.addView(makeUpgradeCard(def, count))
            upgradeContainer.addView(spacer(12))
        }
    }

    private fun makeUpgradeCard(def: UpgradeDef, count: Int): FrameLayout {
        val canAfford = GameRepository.score >= def.baseCost
        val (c1, c2) = upgradeColors(def.colorClass)
        val card = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(c1, c2)).apply {
                cornerRadius = dp(20).toFloat()
                setStroke(dp(2), if (canAfford) Color.parseColor("#907EF1FF") else Color.parseColor("#33FFFFFF"))
            }
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        // Badge
        row.addView(TextView(this).apply {
            text = def.badge; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
            background = GradientDrawable().apply { cornerRadius = dp(999).toFloat(); setColor(Color.parseColor("#33FFFFFF")); setStroke(dp(1), Color.parseColor("#22FFFFFF")) }
            setPadding(dp(12), dp(8), dp(12), dp(8)); gravity = Gravity.CENTER; minWidth = dp(52)
        })
        // Info
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(12); rightMargin = dp(12) }
        }
        info.addView(tv(def.title, 20f, Color.WHITE, Typeface.DEFAULT_BOLD))
        info.addView(tv(def.meta, 13f, Color.parseColor("#CCE2FFE4")))
        info.addView(tv("Куплено: $count", 12f, Color.parseColor("#887EF1FF")).apply { setPadding(0, dp(4), 0, 0) })
        // Buy btn
        val btn = Button(this).apply {
            text = "${formatScore(def.baseCost)} 💰"; textSize = 13f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (canAfford) Color.parseColor("#062035") else Color.parseColor("#AAFFFFFF"))
            layoutParams = LinearLayout.LayoutParams(dp(110), dp(44))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                if (canAfford) intArrayOf(Color.parseColor("#7EF1FF"), Color.parseColor("#27A4C8"))
                else intArrayOf(Color.parseColor("#33FFFFFF"), Color.parseColor("#22FFFFFF"))
            ).apply { cornerRadius = dp(14).toFloat() }
            setOnClickListener {
                val err = GameRepository.buyUpgrade(def.type)
                if (err == null) {
                    animatePulse(it)
                    GameRepository.saveLocal(this@ShopActivity)
                    GameRepository.syncNow(this@ShopActivity)
                    refreshAll()
                } else Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
            }
        }
        row.addView(info); row.addView(btn); card.addView(row)
        return card
    }

    private fun upgradeColors(cls: String): Pair<Int, Int> = when (cls) {
        "tap_small"   -> Color.parseColor("#183468") to Color.parseColor("#081531")
        "tap_big"     -> Color.parseColor("#4E1C68") to Color.parseColor("#190A30")
        "energy"      -> Color.parseColor("#4A350A") to Color.parseColor("#221505")
        "regen_boost" -> Color.parseColor("#165A3A") to Color.parseColor("#082618")
        "tap_huge"    -> Color.parseColor("#6E2512") to Color.parseColor("#2E0F07")
        "energy_huge" -> Color.parseColor("#183C6E") to Color.parseColor("#08142F")
        else          -> Color.parseColor("#1E1040") to Color.parseColor("#0A081E")
    }

    // ---- SKINS ----

    private fun buildSkinCards() {
        skinContainer.removeAllViews()
        val skinState = GameRepository.skin
        var row: LinearLayout? = null
        SKIN_DEFINITIONS.forEachIndexed { index, def ->
            if (index % 2 == 0) {
                row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                }
                skinContainer.addView(row)
                skinContainer.addView(spacer(12))
            }
            val isOwned    = skinState.ownedSkinIds.contains(def.id)
            val isEquipped = skinState.equippedSkinId == def.id
            row!!.addView(makeSkinCard(def, isOwned, isEquipped))
            if (index % 2 == 0) row!!.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(12), MATCH_PARENT)
            })
        }
    }

    private fun makeSkinCard(def: SkinDef, owned: Boolean, equipped: Boolean): LinearLayout {
        val border = when { equipped -> Color.parseColor("#7EF1FF"); owned -> Color.parseColor("#6FF1E3"); else -> Color.parseColor("#33FFFFFF") }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#5F38D4CC"), Color.parseColor("#33C5C7BB"))).apply {
                cornerRadius = dp(18).toFloat(); setStroke(dp(2), border)
            }
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        val img = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(140))
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(Color.parseColor("#22000000")) }
            clipToOutline = true; outlineProvider = ViewOutlineProvider.BACKGROUND
        }
        try {
            val bmp = assets.open("skins/${def.assetName}").use { android.graphics.BitmapFactory.decodeStream(it) }
            img.setImageBitmap(bmp)
        } catch (_: Exception) {}
        card.addView(img); card.addView(spacer(8))
        card.addView(tv(def.name, 14f, Color.WHITE, Typeface.DEFAULT_BOLD, Gravity.CENTER).apply {
            maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END
        })
        card.addView(spacer(4))
        card.addView(rarityChip(def.rarity)); card.addView(spacer(8))

        val btnText  = if (equipped) "✓ Надет" else if (owned) "Установить" else "${formatScore(def.price)} 💰"
        val btnColors = if (equipped) intArrayOf(Color.parseColor("#33C5C7"), Color.parseColor("#22A8B7"))
                        else if (owned) intArrayOf(Color.parseColor("#5088FF"), Color.parseColor("#2859C2"))
                        else intArrayOf(Color.parseColor("#7EF1FF"), Color.parseColor("#27A4FF"))
        val btn = Button(this).apply {
            text = btnText; textSize = 13f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (equipped || owned) Color.WHITE else Color.parseColor("#062035"))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(40))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, btnColors).apply { cornerRadius = dp(12).toFloat() }
            isEnabled = !equipped
            setOnClickListener {
                val err = GameRepository.buySkin(def.id)
                if (err == null) {
                    GameRepository.saveLocal(this@ShopActivity)
                    GameRepository.syncNow(this@ShopActivity)
                    refreshAll()
                } else Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
            }
        }
        card.addView(btn)
        return card
    }

    private fun rarityChip(r: String): TextView {
        val (label, bg) = when (r) {
            "uncommon"  -> "Необычный" to "#28139FCA28"
            "rare"      -> "Редкий"    to "#2874E7FF"
            "epic"      -> "Эпический" to "#28C791FF"
            "legendary" -> "Легенд."   to "#28FFC657"
            "admin"     -> "Админ."    to "#28FF6EB9"
            else        -> "Обычный"   to "#22FFFFFF"
        }
        return tv(label, 11f, Color.WHITE, Typeface.DEFAULT_BOLD, Gravity.CENTER).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(999).toFloat()
                setColor(Color.parseColor(bg))
                setStroke(dp(1), Color.parseColor("#22FFFFFF"))
            }
            setPadding(dp(8), dp(4), dp(8), dp(4))
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { gravity = Gravity.CENTER_HORIZONTAL }
        }
    }

    private fun animatePulse(v: View) {
        ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.88f, 1f).apply { duration = 200; start() }
        ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.88f, 1f).apply { duration = 200; start() }
    }

    private fun tv(t: String, sz: Float, c: Int, tf: Typeface = Typeface.DEFAULT, g: Int = Gravity.START) =
        TextView(this).apply { text = t; textSize = sz; typeface = tf; setTextColor(c); gravity = g }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun spacer(v: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(v)) }
    private val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP_CONTENT  = ViewGroup.LayoutParams.WRAP_CONTENT

    override fun onStop() {
        super.onStop()
        GameRepository.saveLocal(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        GameRepository.onStateChanged = null
    }
}
