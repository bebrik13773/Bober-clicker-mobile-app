package com.bebrik.boberclicker.minigame

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bebrik.boberclicker.data.formatScore
import com.bebrik.boberclicker.databinding.ActivityMinigameBinding
import com.bebrik.boberclicker.game.GameViewModel
import kotlin.math.abs
import kotlin.random.Random

class MiniGameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMinigameBinding
    private lateinit var vm: GameViewModel
    private lateinit var gameView: FlyingBeaverView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMinigameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vm = ViewModelProvider(this)[GameViewModel::class.java]

        gameView = FlyingBeaverView(this) { score, reward ->
            runOnUiThread {
                vm.addScoreFromMiniGame(reward)
                vm.unlockMiniGameAchievement()
                binding.tvGameOverScore.text = "Счёт: $score\n+${reward.formatScore()} 🦫"
                binding.panelGameOver.visibility = View.VISIBLE
            }
        }
        binding.gameContainer.addView(gameView)

        binding.btnStart.setOnClickListener {
            binding.panelStart.visibility = View.GONE
            gameView.startGame()
        }
        binding.btnRestart.setOnClickListener {
            binding.panelGameOver.visibility = View.GONE
            gameView.startGame()
        }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnBackGameOver.setOnClickListener { finish() }
    }

    override fun onPause()  { super.onPause();  gameView.pause() }
    override fun onResume() { super.onResume(); gameView.resume() }
}

// ─── Игровое View ────────────────────────────────────────────────

class FlyingBeaverView(
    context: Context,
    private val onGameOver: (score: Int, reward: Double) -> Unit
) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var thread: Thread? = null
    private var running = false
    private var playing = false

    // Бобёр
    private var beaverY = 0f
    private var velocity = 0f
    private val gravity = 1800f
    private val flapForce = -600f
    private val beaverX get() = width * 0.2f
    private val beaverSize = 80f

    // Препятствия
    private val obstacles = mutableListOf<Obstacle>()
    private val obstacleSpeed = 400f
    private val gapSize = 280f
    private var spawnTimer = 0f
    private val spawnInterval = 2.2f

    // Счёт
    private var score = 0
    private var lastTime = 0L

    // Краски
    private val paintBg    = Paint().apply { color = Color.parseColor("#0c1a2e") }
    private val paintBvr   = Paint().apply { color = Color.parseColor("#8B4513"); isAntiAlias = true }
    private val paintObs   = Paint().apply { color = Color.parseColor("#2d6a4f") }
    private val paintObsH  = Paint().apply { color = Color.parseColor("#40916c") }
    private val paintScore = Paint().apply { color = Color.WHITE; textSize = 72f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
    private val paintEye   = Paint().apply { color = Color.WHITE; isAntiAlias = true }
    private val paintPupil = Paint().apply { color = Color.BLACK; isAntiAlias = true }

    init { holder.addCallback(this) }

    fun startGame() {
        beaverY = height / 2f
        velocity = 0f
        obstacles.clear()
        spawnTimer = 0f
        score = 0
        lastTime = System.nanoTime()
        playing = true
    }

    fun pause()  { running = false }
    fun resume() { if (thread?.isAlive == false || thread == null) { running = true; thread = Thread(this); thread?.start() } }

    override fun surfaceCreated(h: SurfaceHolder)  { running = true; thread = Thread(this); thread?.start() }
    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, ht: Int) { beaverY = ht / 2f }
    override fun surfaceDestroyed(h: SurfaceHolder) { running = false; thread?.join() }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN && playing) velocity = flapForce
        return true
    }

    override fun run() {
        while (running) {
            val now = System.nanoTime()
            val dt = ((now - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
            lastTime = now

            if (playing) update(dt)
            draw()
            Thread.sleep(16)
        }
    }

    private fun update(dt: Float) {
        // Физика
        velocity += gravity * dt
        beaverY += velocity * dt

        // Стены
        if (beaverY < 0 || beaverY > height) { gameOver(); return }

        // Спавн препятствий
        spawnTimer += dt
        if (spawnTimer >= spawnInterval) { spawnTimer = 0f; spawnObstacle() }

        // Двигаем / убираем / считаем очки
        val iter = obstacles.iterator()
        while (iter.hasNext()) {
            val obs = iter.next()
            obs.x -= obstacleSpeed * dt
            if (!obs.passed && obs.x + 60f < beaverX) { obs.passed = true; score++ }
            if (obs.x < -120f) iter.remove()

            // Коллизия
            val bRect = RectF(beaverX - beaverSize/2, beaverY - beaverSize/2, beaverX + beaverSize/2, beaverY + beaverSize/2)
            val topRect = RectF(obs.x - 60f, 0f, obs.x + 60f, obs.gapTop)
            val botRect = RectF(obs.x - 60f, obs.gapTop + gapSize, obs.x + 60f, height.toFloat())
            if (RectF.intersects(bRect, topRect) || RectF.intersects(bRect, botRect)) { gameOver(); return }
        }
    }

    private fun spawnObstacle() {
        val margin = height * 0.15f
        val gapTop = Random.nextFloat() * (height - gapSize - margin * 2) + margin
        obstacles.add(Obstacle(width + 120f, gapTop))
    }

    private fun gameOver() {
        playing = false
        val reward = score * 10.0 + 10.0
        onGameOver(score, reward)
    }

    private fun draw() {
        val canvas = holder.lockCanvas() ?: return
        try {
            // Фон
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBg)

            // Препятствия
            for (obs in obstacles) {
                canvas.drawRoundRect(obs.x - 55f, 0f, obs.x + 55f, obs.gapTop, 20f, 20f, paintObs)
                canvas.drawRoundRect(obs.x - 55f, obs.gapTop - 30f, obs.x + 55f, obs.gapTop, 20f, 20f, paintObsH)
                canvas.drawRoundRect(obs.x - 55f, obs.gapTop + gapSize, obs.x + 55f, height.toFloat(), 20f, 20f, paintObs)
                canvas.drawRoundRect(obs.x - 55f, obs.gapTop + gapSize, obs.x + 55f, obs.gapTop + gapSize + 30f, 20f, 20f, paintObsH)
            }

            // Бобёр (тело)
            canvas.drawRoundRect(beaverX - beaverSize/2, beaverY - beaverSize/2, beaverX + beaverSize/2, beaverY + beaverSize/2, 20f, 20f, paintBvr)
            // Глаз
            canvas.drawCircle(beaverX + 18f, beaverY - 12f, 14f, paintEye)
            canvas.drawCircle(beaverX + 21f, beaverY - 10f, 7f, paintPupil)
            // Хвост
            val tailPaint = Paint().apply { color = Color.parseColor("#5D3A1A"); isAntiAlias = true }
            canvas.drawOval(beaverX - beaverSize/2 - 25f, beaverY, beaverX - beaverSize/2 + 10f, beaverY + 35f, tailPaint)

            // Счёт
            canvas.drawText("$score", 40f, 100f, paintScore)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }
}

data class Obstacle(var x: Float, val gapTop: Float, var passed: Boolean = false)
