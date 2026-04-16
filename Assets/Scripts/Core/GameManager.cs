using UnityEngine;
using System;

/// <summary>
/// Главный менеджер игры. Синглтон.
/// Хранит все игровые данные и управляет логикой.
/// </summary>
public class GameManager : MonoBehaviour
{
    public static GameManager Instance { get; private set; }

    // События для UI
    public event Action<double> OnScoreChanged;
    public event Action<double> OnPassiveIncomeChanged;
    public event Action<int> OnClickPowerChanged;

    // Игровые данные
    public double Score { get; private set; }
    public double TotalClicks { get; private set; }
    public double TotalEarned { get; private set; }
    public int ClickPower { get; private set; } = 1;
    public double PassiveIncome { get; private set; } = 0;

    // Пассивный доход тикает каждую секунду
    private float _passiveTimer = 0f;
    private const float PASSIVE_INTERVAL = 1f;

    void Awake()
    {
        if (Instance != null && Instance != this)
        {
            Destroy(gameObject);
            return;
        }
        Instance = this;
        DontDestroyOnLoad(gameObject);
    }

    void Start()
    {
        SaveSystem.Load();
        OnScoreChanged?.Invoke(Score);
        OnPassiveIncomeChanged?.Invoke(PassiveIncome);
    }

    void Update()
    {
        // Пассивный доход
        if (PassiveIncome > 0)
        {
            _passiveTimer += Time.deltaTime;
            if (_passiveTimer >= PASSIVE_INTERVAL)
            {
                _passiveTimer -= PASSIVE_INTERVAL;
                AddScore(PassiveIncome);
            }
        }
    }

    // Клик по бобру
    public void OnBoberClicked()
    {
        AddScore(ClickPower);
        TotalClicks++;
        QuestManager.Instance?.OnClick();
    }

    public void AddScore(double amount)
    {
        Score += amount;
        TotalEarned += amount;
        OnScoreChanged?.Invoke(Score);
    }

    public bool SpendScore(double amount)
    {
        if (Score < amount) return false;
        Score -= amount;
        OnScoreChanged?.Invoke(Score);
        return true;
    }

    public void AddClickPower(int amount)
    {
        ClickPower += amount;
        OnClickPowerChanged?.Invoke(ClickPower);
    }

    public void AddPassiveIncome(double amount)
    {
        PassiveIncome += amount;
        OnPassiveIncomeChanged?.Invoke(PassiveIncome);
    }

    // Сохранение при сворачивании/закрытии
    void OnApplicationPause(bool pause) { if (pause) SaveSystem.Save(); }
    void OnApplicationQuit() { SaveSystem.Save(); }

    // Для SaveSystem
    public void LoadData(GameData data)
    {
        Score = data.score;
        TotalClicks = data.totalClicks;
        TotalEarned = data.totalEarned;
        ClickPower = data.clickPower;
        PassiveIncome = data.passiveIncome;
    }

    public GameData GetData()
    {
        return new GameData
        {
            score = Score,
            totalClicks = TotalClicks,
            totalEarned = TotalEarned,
            clickPower = ClickPower,
            passiveIncome = PassiveIncome
        };
    }
}
