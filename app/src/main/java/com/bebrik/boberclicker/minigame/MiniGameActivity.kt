package com.bebrik.boberclicker.minigame

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bebrik.boberclicker.data.fmt
import com.bebrik.boberclicker.databinding.ActivityMinigameBinding
import com.bebrik.boberclicker.game.GameViewModel
import kotlin.math.roundToInt
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

        gameView = FlyingBeaverView(this) { score ->
            runOnUiThread {
                vm.onFlyBeaverGameOver(score)
                val coins = score * 500.0
                binding.tvGameOverScore.text = "Счёт: $score\n+${coins.fmt()} коинов 🦫"
                binding.panelGameOver.visibility = View.VISIBLE
            }
        }
        binding.gameContainer.addView(gameView)

        binding.btnStart.setOnClickListener   { binding.panelStart.visibility = View.GONE; gameView.startGame() }
        binding.btnRestart.setOnClickListener { binding.panelGameOver.visibility = View.GONE; gameView.startGame() }
        binding.btnBack.setOnClickListener    { finish() }
        binding.btnBackGameOver.setOnClickListener { finish() }
    }

    override fun onPause()  { super.onPause();  gameView.pause() }
    override fun onResume() { super.onResume(); gameView.resume() }
}

class FlyingBeaverView(context: Context, private val onGameOver: (Int) -> Unit)
    : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var thread: Thread? = null
    private var running = false
    private var playing = false

    private var beaverY = 0f
    private var velocity = 0f
    private val gravity = 1800f
    private val flapForce = -580f
    private val beaverX get() = width * 0.22f
    private val beaverR = 38f

    private val obstacles = mutableListOf<Obstacle>()
    private val obstacleSpeed = 420f
    private val gapSize = 260f
    private var spawnTimer = 0f
    private val spawnInterval = 2.0f
    private var score = 0
    private var lastTime = 0L

    private val paintBg   = Paint().apply { color = Color.parseColor("#14072f") }
    private val paintBvr  = Paint().apply { color = Color.parseColor("#8B4513"); isAntiAlias = true }
    private val paintLog  = Paint().apply { color = Color.parseColor("#5D4037"); isAntiAlias = true }
    private val paintLogH = Paint().apply { color = Color.parseColor("#795548"); isAntiAlias = true }
    private val paintEye  = Paint().apply { color = Color.WHITE; isAntiAlias = true }
    private val paintPup  = Paint().apply { color = Color.parseColor("#1a1a1a"); isAntiAlias = true }
    private val paintScore = Paint().apply {
        color = Color.WHITE; textSize = 72f
        typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
    }

    init { holder.addCallback(this) }

    fun startGame() {
        beaverY = height / 2f; velocity = 0f
        obstacles.clear(); spawnTimer = 0f; score = 0
        lastTime = System.nanoTime(); playing = true
    }

    fun pause()  { running = false }
    fun resume() {
        if (thread?.isAlive != true) {
            running = true; thread = Thread(this); thread!!.start()
        }
    }

    override fun surfaceCreated(h: SurfaceHolder)    { running = true; thread = Thread(this); thread!!.start() }
    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, ht: Int) { beaverY = ht / 2f }
    override fun surfaceDestroyed(h: SurfaceHolder)  { running = false; thread?.join() }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN && playing) velocity = flapForce
        return true
    }

    override fun run() {
        while (running) {
            val now = System.nanoTime()
            val dt = ((now - lastTime) / 1e9f).coerceAtMost(0.05f)
            lastTime = now
            if (playing) update(dt)
            draw()
            Thread.sleep(16)
        }
    }

    private fun update(dt: Float) {
        velocity += gravity * dt
        beaverY  += velocity * dt

        if (beaverY < beaverR || beaverY > height - beaverR) { gameOver(); return }

        spawnTimer += dt
        if (spawnTimer >= spawnInterval) { spawnTimer = 0f; spawnObstacle() }

        val iter = obstacles.iterator()
        while (iter.hasNext()) {
            val obs = iter.next()
            obs.x -= obstacleSpeed * dt
            if (!obs.passed && obs.x + 55f < beaverX) { obs.passed = true; score++ }
            if (obs.x < -120f) { iter.remove(); continue }

            val bRect = RectF(beaverX - beaverR, beaverY - beaverR, beaverX + beaverR, beaverY + beaverR)
            val top = RectF(obs.x - 55f, 0f, obs.x + 55f, obs.gapTop)
            val bot = RectF(obs.x - 55f, obs.gapTop + gapSize, obs.x + 55f, height.toFloat())
            if (RectF.intersects(bRect, top) || RectF.intersects(bRect, bot)) { gameOver(); return }
        }
    }

    private fun spawnObstacle() {
        val margin = height * 0.18f
        val gapTop = Random.nextFloat() * (height - gapSize - margin * 2) + margin
        obstacles.add(Obstacle(width + 120f, gapTop))
    }

    private fun gameOver() { playing = false; onGameOver(score) }

    private fun draw() {
        val c = holder.lockCanvas() ?: return
        try {
            c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBg)
            for (obs in obstacles) {
                c.drawRoundRect(obs.x - 50f, 0f,              obs.x + 50f, obs.gapTop,             16f, 16f, paintLog)
                c.drawRoundRect(obs.x - 50f, obs.gapTop - 24f, obs.x + 50f, obs.gapTop,             16f, 16f, paintLogH)
                c.drawRoundRect(obs.x - 50f, obs.gapTop + gapSize, obs.x + 50f, height.toFloat(),   16f, 16f, paintLog)
                c.drawRoundRect(obs.x - 50f, obs.gapTop + gapSize, obs.x + 50f, obs.gapTop + gapSize + 24f, 16f, 16f, paintLogH)
            }
            // Тело
            c.drawCircle(beaverX, beaverY, beaverR, paintBvr)
            // Глаз
            c.drawCircle(beaverX + 14f, beaverY - 10f, 10f, paintEye)
            c.drawCircle(beaverX + 16f, beaverY - 9f, 5f, paintPup)
            // Счёт
            c.drawText("$score", 40f, 90f, paintScore)
        } finally { holder.unlockCanvasAndPost(c) }
    }
}

data class Obstacle(var x: Float, val gapTop: Float, var passed: Boolean = false)
