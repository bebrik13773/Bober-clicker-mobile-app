using UnityEngine;
using UnityEngine.UI;
using TMPro;

/// <summary>
/// Контроллер главного экрана.
/// Привяжи элементы в Inspector.
/// </summary>
public class MainUIController : MonoBehaviour
{
    [Header("Основной UI")]
    public TextMeshProUGUI scoreText;
    public TextMeshProUGUI perSecText;
    public TextMeshProUGUI clickPowerText;

    [Header("Кнопка бобра")]
    public Button boberButton;

    [Header("Навигация")]
    public GameObject mainPanel;
    public GameObject shopPanel;
    public GameObject questsPanel;
    public GameObject achievementsPanel;

    [Header("Уведомление об ачивке")]
    public GameObject achievementPopup;
    public TextMeshProUGUI achievementPopupText;

    void Start()
    {
        // Подписываемся на события
        GameManager.Instance.OnScoreChanged += UpdateScoreUI;
        GameManager.Instance.OnPassiveIncomeChanged += UpdatePassiveUI;
        GameManager.Instance.OnClickPowerChanged += UpdateClickPowerUI;
        AchievementManager.Instance.OnAchievementUnlocked += ShowAchievementPopup;

        boberButton.onClick.AddListener(OnBoberClick);

        // Инициализируем UI
        UpdateScoreUI(GameManager.Instance.Score);
        UpdatePassiveUI(GameManager.Instance.PassiveIncome);
        UpdateClickPowerUI(GameManager.Instance.ClickPower);

        ShowPanel(mainPanel);
    }

    void OnDestroy()
    {
        if (GameManager.Instance != null)
        {
            GameManager.Instance.OnScoreChanged -= UpdateScoreUI;
            GameManager.Instance.OnPassiveIncomeChanged -= UpdatePassiveUI;
            GameManager.Instance.OnClickPowerChanged -= UpdateClickPowerUI;
        }
        if (AchievementManager.Instance != null)
            AchievementManager.Instance.OnAchievementUnlocked -= ShowAchievementPopup;
    }

    // --- Клик по бобру ---
    void OnBoberClick()
    {
        GameManager.Instance.OnBoberClicked();
        AchievementManager.Instance?.CheckClickAchievements();

        // Анимация нажатия (опционально)
        LeanTween.cancel(boberButton.gameObject);
        LeanTween.scale(boberButton.gameObject, Vector3.one * 0.9f, 0.05f)
                 .setEaseOutQuad()
                 .setOnComplete(() =>
                     LeanTween.scale(boberButton.gameObject, Vector3.one, 0.1f).setEaseOutBack());
    }

    // --- Обновление UI ---
    void UpdateScoreUI(double score)
    {
        if (scoreText) scoreText.text = NumberFormatter.Format(score) + " 🦫";
        AchievementManager.Instance?.CheckScoreAchievements();
        QuestManager.Instance?.CheckScoreQuests();
    }

    void UpdatePassiveUI(double passive)
    {
        if (perSecText) perSecText.text = NumberFormatter.FormatPerSec(passive);
        AchievementManager.Instance?.CheckPassiveAchievements();
    }

    void UpdateClickPowerUI(int power)
    {
        if (clickPowerText) clickPowerText.text = "+" + power + " за клик";
    }

    // --- Навигация между панелями ---
    public void ShowMain()       => ShowPanel(mainPanel);
    public void ShowShop()       => ShowPanel(shopPanel);
    public void ShowQuests()     => ShowPanel(questsPanel);
    public void ShowAchievements() => ShowPanel(achievementsPanel);

    void ShowPanel(GameObject panel)
    {
        mainPanel?.SetActive(panel == mainPanel);
        shopPanel?.SetActive(panel == shopPanel);
        questsPanel?.SetActive(panel == questsPanel);
        achievementsPanel?.SetActive(panel == achievementsPanel);
    }

    // --- Попап ачивки ---
    async void ShowAchievementPopup(Achievement ach)
    {
        if (!achievementPopup || !achievementPopupText) return;
        achievementPopupText.text = "🏆 " + ach.displayName + "\n" + ach.description;
        achievementPopup.SetActive(true);
        await System.Threading.Tasks.Task.Delay(3000);
        achievementPopup?.SetActive(false);
    }
}
