package com.bebrik.boberclicker.ui

import android.animation.ObjectAnimator
import android.content.Context
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

class ShopActivity : AppCompatActivity() {

    private lateinit var vm: GameViewModel
    private lateinit var tvBalance: TextView
    private lateinit var upgradeContainer: LinearLayout
    private lateinit var skinContainer: LinearLayout

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

        buildUI()
        setupObservers()
    }

    private fun buildUI() {
        val root = FrameLayout(this).apply {
            background = createPurpleGradient()
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

        // Back button + Title
        val header = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(52))
        }
        val btnBack = TextView(this).apply {
            text = "← Назад"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#7EF1FF"))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#22FFFFFF"))
            }
            setPadding(dp(14), dp(8), dp(14), dp(8))
            layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
            }
            setOnClickListener { finish() }
        }
        header.addView(btnBack)
        val titleTv = TextView(this).apply {
            text = "Магазин"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
        }
        header.addView(titleTv)
        content.addView(header)
        content.addView(spacer(12))

        // Balance card
        val balCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(Color.parseColor("#1E0A44"))
                setStroke(dp(1), Color.parseColor("#337EF1FF"))
            }
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        tvBalance = TextView(this).apply {
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        balCard.addView(TextView(this).apply {
            text = "💰 Баланс: "
            textSize = 22f
            setTextColor(Color.parseColor("#C9FFFF"))
        })
        balCard.addView(tvBalance)
        content.addView(balCard)
        content.addView(spacer(20))

        // Upgrades section
        content.addView(makeSectionTitle("⬆️  Улучшения"))
        content.addView(spacer(10))
        upgradeContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        buildUpgradeCards()
        content.addView(upgradeContainer)
        content.addView(spacer(24))

        // Skins section
        content.addView(makeSectionTitle("🎨  Скины"))
        content.addView(spacer(10))
        skinContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        buildSkinCards()
        content.addView(skinContainer)

        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)

        tvBalance.text = formatScore(vm.getScore())
    }

    // ---- UPGRADE CARDS ----

    private fun buildUpgradeCards() {
        upgradeContainer.removeAllViews()
        val ups = vm.getUpgrades()
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
        val canAfford = vm.getScore() >= def.baseCost
        val bg = getUpgradeCardColors(def.colorClass)
        val borderColor = if (canAfford) Color.parseColor("#907EF1FF") else Color.parseColor("#33FFFFFF")

        val card = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(bg.first, bg.second)
            ).apply {
                cornerRadius = dp(20).toFloat()
                setStroke(dp(2), borderColor)
            }
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Badge
        val badge = TextView(this).apply {
            text = def.badge
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(999).toFloat()
                setColor(Color.parseColor("#33FFFFFF"))
                setStroke(dp(1), Color.parseColor("#22FFFFFF"))
            }
            setPadding(dp(12), dp(8), dp(12), dp(8))
            gravity = Gravity.CENTER
            minWidth = dp(52)
        }

        // Info column
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                leftMargin = dp(12)
                rightMargin = dp(12)
            }
        }
        info.addView(TextView(this).apply {
            text = def.title
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })
        info.addView(TextView(this).apply {
            text = def.meta
            textSize = 13f
            setTextColor(Color.parseColor("#CCE2FFE4"))
            setPadding(0, dp(2), 0, 0)
        })
        info.addView(TextView(this).apply {
            text = "Куплено: $count"
            textSize = 12f
            setTextColor(Color.parseColor("#887EF1FF"))
            setPadding(0, dp(4), 0, 0)
        })

        // Buy button
        val btn = Button(this).apply {
            text = "${formatScore(def.baseCost)} 💰"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (canAfford) Color.parseColor("#062035") else Color.parseColor("#AAFFFFFF"))
            layoutParams = LinearLayout.LayoutParams(dp(110), dp(44))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                if (canAfford)
                    intArrayOf(Color.parseColor("#7EF1FF"), Color.parseColor("#27A4C8"))
                else
                    intArrayOf(Color.parseColor("#33FFFFFF"), Color.parseColor("#22FFFFFF"))
            ).apply { cornerRadius = dp(14).toFloat() }
            setOnClickListener {
                if (vm.buyUpgrade(def.type, this@ShopActivity)) {
                    animateBuySuccess(it)
                    buildUpgradeCards()
                    buildSkinCards()
                }
            }
        }

        row.addView(badge)
        row.addView(info)
        row.addView(btn)
        card.addView(row)
        return card
    }

    private fun getUpgradeCardColors(cls: String): Pair<Int, Int> = when (cls) {
        "tap_small"   -> Pair(Color.parseColor("#183468"), Color.parseColor("#081531"))
        "tap_big"     -> Pair(Color.parseColor("#4E1C68"), Color.parseColor("#190A30"))
        "energy"      -> Pair(Color.parseColor("#4A350A"), Color.parseColor("#221505"))
        "regen_boost" -> Pair(Color.parseColor("#165A3A"), Color.parseColor("#082618"))
        "tap_huge"    -> Pair(Color.parseColor("#6E2512"), Color.parseColor("#2E0F07"))
        "energy_huge" -> Pair(Color.parseColor("#183C6E"), Color.parseColor("#08142F"))
        else          -> Pair(Color.parseColor("#1E1040"), Color.parseColor("#0A081E"))
    }

    // ---- SKIN CARDS ----

    private fun buildSkinCards() {
        skinContainer.removeAllViews()
        val skinState = vm.getSkin()
        // 2 per row
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
            if (index % 2 == 0) {
                row!!.addView(spacer(12).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(12), MATCH_PARENT)
                })
            }
        }
    }

    private fun makeSkinCard(def: SkinDef, isOwned: Boolean, isEquipped: Boolean): LinearLayout {
        val borderColor = when {
            isEquipped -> Color.parseColor("#7EF1FF")
            isOwned    -> Color.parseColor("#6FF1E3")
            else       -> Color.parseColor("#33FFFFFF")
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#5F38D4CC"), Color.parseColor("#33C5C7BB"))
            ).apply {
                cornerRadius = dp(18).toFloat()
                setStroke(dp(2), borderColor)
            }
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        // Skin image
        val img = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(140))
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.parseColor("#22000000"))
            }
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
        }
        try {
            val bmp = assets.open("skins/${def.assetName}").use {
                android.graphics.BitmapFactory.decodeStream(it)
            }
            img.setImageBitmap(bmp)
        } catch (e: Exception) {
            img.setBackgroundColor(Color.parseColor("#33FFFFFF"))
        }
        card.addView(img)
        card.addView(spacer(8))

        // Name
        card.addView(TextView(this).apply {
            text = def.name
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_HORIZONTAL
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        card.addView(spacer(4))

        // Rarity chip
        card.addView(makeRarityChip(def.rarity))
        card.addView(spacer(8))

        // Action button
        val btnText = when {
            isEquipped -> "✓ Надет"
            isOwned    -> "Установить"
            else       -> "${formatScore(def.price)} 💰"
        }
        val btnBg = when {
            isEquipped -> intArrayOf(Color.parseColor("#33C5C7"), Color.parseColor("#22A8B7"))
            isOwned    -> intArrayOf(Color.parseColor("#5088FF"), Color.parseColor("#2859C2"))
            else       -> intArrayOf(Color.parseColor("#7EF1FF"), Color.parseColor("#27A4FF"))
        }
        val btn = Button(this).apply {
            text = btnText
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (isEquipped || isOwned) Color.WHITE else Color.parseColor("#062035"))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(40))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, btnBg).apply {
                cornerRadius = dp(12).toFloat()
            }
            isEnabled = !isEquipped
            setOnClickListener {
                vm.buySkin(def.id, this@ShopActivity)
                buildSkinCards()
                buildUpgradeCards()
            }
        }
        card.addView(btn)
        return card
    }

    private fun makeRarityChip(rarity: String): TextView {
        val (label, bg) = when (rarity) {
            "uncommon"  -> "Необычный" to "#28139FCA28"
            "rare"      -> "Редкий"    to "#2874E7FF"
            "epic"      -> "Эпический" to "#28C791FF"
            "legendary" -> "Легенд."   to "#28FFC657"
            "admin"     -> "Админ."    to "#28FF6EB9"
            else        -> "Обычный"   to "#22FFFFFF"
        }
        return TextView(this).apply {
            text = label
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = dp(999).toFloat()
                setColor(Color.parseColor(bg))
                setStroke(dp(1), Color.parseColor("#22FFFFFF"))
            }
            setPadding(dp(8), dp(4), dp(8), dp(4))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
    }

    private fun makeSectionTitle(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 22f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.parseColor("#E9FFFF"))
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }

    // ---- OBSERVERS ----

    private fun setupObservers() {
        vm.score.observe(this) { s ->
            tvBalance.text = formatScore(s)
        }
        vm.toastMsg.observe(this) { msg ->
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                vm.toastMsg.value = null
            }
        }
    }

    private fun animateBuySuccess(v: View) {
        ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.88f, 1f).apply { duration = 200; start() }
        ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.88f, 1f).apply { duration = 200; start() }
    }

    // ---- HELPERS ----

    private fun createPurpleGradient() = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(Color.parseColor("#6B11D7"), Color.parseColor("#45118A"), Color.parseColor("#1C0F42"))
    )

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun spacer(v: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(v))
    }
    private val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
}
