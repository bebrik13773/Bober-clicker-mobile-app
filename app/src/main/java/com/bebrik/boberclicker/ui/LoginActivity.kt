package com.bebrik.boberclicker.ui

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.*
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bebrik.boberclicker.MainActivity
import com.bebrik.boberclicker.R
import com.bebrik.boberclicker.data.ApiClient
import com.bebrik.boberclicker.data.GameRepository

class LoginActivity : AppCompatActivity() {

    private lateinit var etLogin: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvStatus: TextView
    private var activeTab = "login" // "login" | "register"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        ApiClient.activityContext = this
        buildUI()
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
            setPadding(dp(24), dp(80), dp(24), dp(40))
        }

        // Logo / emoji
        content.addView(TextView(this).apply {
            text = "🦫"
            textSize = 72f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        })
        content.addView(spacer(8))
        content.addView(TextView(this).apply {
            text = "Бобёр Кликер 2"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        })
        content.addView(spacer(32))

        // Card
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(28).toFloat()
                setColor(Color.parseColor("#1A1040"))
                setStroke(dp(1), Color.parseColor("#447EF1FF"))
            }
            setPadding(dp(24), dp(24), dp(24), dp(24))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        // Tabs
        val tabRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(46))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.parseColor("#22FFFFFF"))
            }
        }
        val tabLogin = makeTab("Войти", true)
        val tabReg   = makeTab("Регистрация", false)
        tabRow.addView(tabLogin)
        tabRow.addView(tabReg)
        card.addView(tabRow)
        card.addView(spacer(18))

        // Fields
        val kicker = TextView(this).apply {
            text = "Облачный аккаунт"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#7EF1FF"))
            background = GradientDrawable().apply {
                cornerRadius = dp(999).toFloat()
                setColor(Color.parseColor("#167EF1FF"))
                setStroke(dp(1), Color.parseColor("#227EF1FF"))
            }
            setPadding(dp(10), dp(6), dp(10), dp(6))
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        }
        card.addView(kicker)
        card.addView(spacer(12))

        etLogin = makeField("Логин", false)
        card.addView(etLogin)
        card.addView(spacer(10))

        etPassword = makeField("Пароль", true)
        card.addView(etPassword)
        card.addView(spacer(20))

        tvStatus = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#FF8888"))
            gravity = Gravity.CENTER
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = dp(12)
            }
        }
        card.addView(tvStatus)

        btnLogin = Button(this).apply {
            text = "Войти"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#062035"))
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(52))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#7EF1FF"), Color.parseColor("#27A4FF"))
            ).apply { cornerRadius = dp(16).toFloat() }
            setOnClickListener { doAction() }
        }
        card.addView(btnLogin)

        content.addView(card)
        content.addView(spacer(20))
        content.addView(TextView(this).apply {
            text = "Аккаунт даёт облачное сохранение,\nмагазин и таблицу лидеров"
            textSize = 13f
            setTextColor(Color.parseColor("#88AABB"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        })

        // Tab click handlers
        tabLogin.setOnClickListener {
            activeTab = "login"
            btnLogin.text = "Войти"
            updateTabs(tabLogin, tabReg)
        }
        tabReg.setOnClickListener {
            activeTab = "register"
            btnLogin.text = "Зарегистрироваться"
            updateTabs(tabReg, tabLogin)
        }

        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
    }

    private fun makeTab(label: String, active: Boolean): TextView = TextView(this).apply {
        text = label
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f)
        setTextColor(if (active) Color.parseColor("#062035") else Color.parseColor("#88FFFFFF"))
        if (active) {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#5088FF"), Color.parseColor("#2859C2"))
            ).apply { cornerRadius = dp(14).toFloat() }
        }
    }

    private fun updateTabs(active: TextView, inactive: TextView) {
        active.setTextColor(Color.parseColor("#062035"))
        active.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#5088FF"), Color.parseColor("#2859C2"))
        ).apply { cornerRadius = dp(14).toFloat() }
        inactive.setTextColor(Color.parseColor("#88FFFFFF"))
        inactive.background = null
    }

    private fun makeField(hint: String, isPassword: Boolean): EditText = EditText(this).apply {
        this.hint = hint
        textSize = 16f
        setTextColor(Color.WHITE)
        setHintTextColor(Color.parseColor("#66FFFFFF"))
        background = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(Color.parseColor("#11FFFFFF"))
            setStroke(dp(1), Color.parseColor("#337EF1FF"))
        }
        setPadding(dp(16), dp(14), dp(16), dp(14))
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        if (isPassword) inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        else inputType = android.text.InputType.TYPE_CLASS_TEXT
    }

    private fun doAction() {
        val loginVal = etLogin.text.toString().trim()
        val pass     = etPassword.text.toString()
        if (loginVal.isEmpty() || pass.isEmpty()) {
            showStatus("Введите логин и пароль", false)
            return
        }
        btnLogin.isEnabled = false
        btnLogin.text = "Подождите…"
        tvStatus.visibility = View.GONE

        GameRepository.login(this, loginVal, pass) { success, message ->
            btnLogin.isEnabled = true
            btnLogin.text = if (activeTab == "login") "Войти" else "Зарегистрироваться"
            if (success) {
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            } else {
                showStatus(message, false)
            }
        }
    }

    private fun showStatus(msg: String, ok: Boolean) {
        tvStatus.text = msg
        tvStatus.setTextColor(if (ok) Color.parseColor("#88EE88") else Color.parseColor("#FF8888"))
        tvStatus.visibility = View.VISIBLE
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun spacer(v: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(v)) }
    private val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
}
