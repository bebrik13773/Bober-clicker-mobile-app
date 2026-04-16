using UnityEngine;
using UnityEngine.UI;
using TMPro;
using System.Collections;

/// <summary>
/// Мини-игра Flying Beaver.
/// Бобёр летит, нужно уворачиваться от препятствий.
/// Привяжи все элементы в Inspector.
/// </summary>
public class FlyingBeaverGame : MonoBehaviour
{
    [Header("Бобёр")]
    public RectTransform beaverRect;
    public float flapForce = 500f;
    public float gravity = -800f;

    [Header("Препятствия")]
    public GameObject obstaclePrefab;
    public Transform obstacleParent;
    public float obstacleSpeed = 300f;
    public float obstacleSpawnInterval = 2f;
    public float gapSize = 250f;

    [Header("UI")]
    public TextMeshProUGUI scoreText;
    public TextMeshProUGUI rewardText;
    public GameObject gameOverPanel;
    public GameObject startPanel;
    public Button startButton;
    public Button restartButton;

    [Header("Награда")]
    public double rewardMultiplier = 10.0; // очков = счёт * PassiveIncome * multiplier

    private float _velocity;
    private int _score;
    private bool _isPlaying;
    private float _spawnTimer;
    private float _canvasHeight;
    private float _canvasWidth;

    void Awake()
    {
        var canvas = GetComponentInParent<Canvas>();
        var canvasRect = canvas.GetComponent<RectTransform>();
        _canvasHeight = canvasRect.rect.height;
        _canvasWidth = canvasRect.rect.width;
    }

    void Start()
    {
        startButton.onClick.AddListener(StartGame);
        restartButton.onClick.AddListener(StartGame);
        gameOverPanel.SetActive(false);
        startPanel.SetActive(true);
    }

    void Update()
    {
        if (!_isPlaying) return;

        // Гравитация
        _velocity += gravity * Time.deltaTime;
        beaverRect.anchoredPosition += Vector2.up * _velocity * Time.deltaTime;

        // Флап по тапу/клику
        if (Input.GetMouseButtonDown(0) || (Input.touchCount > 0 && Input.GetTouch(0).phase == TouchPhase.Began))
        {
            _velocity = flapForce;
        }

        // Границы экрана
        float halfH = _canvasHeight / 2f;
        if (beaverRect.anchoredPosition.y <= -halfH + 50f || beaverRect.anchoredPosition.y >= halfH - 50f)
        {
            GameOver();
            return;
        }

        // Спавн препятствий
        _spawnTimer += Time.deltaTime;
        if (_spawnTimer >= obstacleSpawnInterval)
        {
            _spawnTimer = 0;
            SpawnObstacle();
        }

        // Двигаем препятствия
        foreach (Transform obs in obstacleParent)
        {
            obs.GetComponent<RectTransform>().anchoredPosition +=
                Vector2.left * obstacleSpeed * Time.deltaTime;

            // Подсчёт очков — прошли препятствие
            var tracker = obs.GetComponent<ObstacleTracker>();
            if (tracker && !tracker.passed && obs.GetComponent<RectTransform>().anchoredPosition.x < beaverRect.anchoredPosition.x)
            {
                tracker.passed = true;
                _score++;
                if (scoreText) scoreText.text = "Счёт: " + _score;
            }

            // Удаляем за экраном
            if (obs.GetComponent<RectTransform>().anchoredPosition.x < -_canvasWidth)
                Destroy(obs.gameObject);
        }

        // Коллизия (простая, через Rect)
        CheckCollision();
    }

    void StartGame()
    {
        _isPlaying = true;
        _score = 0;
        _velocity = 0;
        _spawnTimer = 0;

        beaverRect.anchoredPosition = Vector2.zero;

        foreach (Transform obs in obstacleParent)
            Destroy(obs.gameObject);

        if (scoreText) scoreText.text = "Счёт: 0";
        gameOverPanel.SetActive(false);
        startPanel.SetActive(false);
    }

    void SpawnObstacle()
    {
        float randomY = Random.Range(-_canvasHeight * 0.25f, _canvasHeight * 0.25f);
        float spawnX = _canvasWidth / 2f + 100f;

        // Верхняя часть
        var top = Instantiate(obstaclePrefab, obstacleParent);
        var topRect = top.GetComponent<RectTransform>();
        topRect.anchoredPosition = new Vector2(spawnX, randomY + gapSize / 2f + topRect.rect.height / 2f);
        top.AddComponent<ObstacleTracker>();

        // Нижняя часть
        var bot = Instantiate(obstaclePrefab, obstacleParent);
        var botRect = bot.GetComponent<RectTransform>();
        botRect.anchoredPosition = new Vector2(spawnX, randomY - gapSize / 2f - botRect.rect.height / 2f);
        botRect.localScale = new Vector3(1, -1, 1);
    }

    void CheckCollision()
    {
        Rect beaverRect2D = new Rect(
            beaverRect.anchoredPosition.x - 30,
            beaverRect.anchoredPosition.y - 30,
            60, 60);

        foreach (Transform obs in obstacleParent)
        {
            var r = obs.GetComponent<RectTransform>();
            Rect obsRect = new Rect(
                r.anchoredPosition.x - r.rect.width / 2f,
                r.anchoredPosition.y - r.rect.height / 2f,
                r.rect.width, r.rect.height);

            if (beaverRect2D.Overlaps(obsRect))
            {
                GameOver();
                return;
            }
        }
    }

    void GameOver()
    {
        _isPlaying = false;

        // Считаем награду
        double reward = _score * GameManager.Instance.PassiveIncome * rewardMultiplier;
        if (reward < 10) reward = 10; // минимальная награда

        GameManager.Instance.AddScore(reward);
        AchievementManager.Instance?.UnlockMiniGameWin();

        if (gameOverPanel) gameOverPanel.SetActive(true);
        if (rewardText) rewardText.text = $"Счёт: {_score}\nНаграда: +{NumberFormatter.Format(reward)} 🦫";
    }
}

// Вспомогательный компонент
public class ObstacleTracker : MonoBehaviour
{
    public bool passed = false;
}
