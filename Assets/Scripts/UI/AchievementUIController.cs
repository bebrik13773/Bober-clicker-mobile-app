using UnityEngine;
using UnityEngine.UI;
using TMPro;

/// <summary>
/// UI панели достижений.
/// Привяжи achievementItemPrefab и contentParent в Inspector.
/// </summary>
public class AchievementUIController : MonoBehaviour
{
    [Header("Префаб карточки достижения")]
    public GameObject achievementItemPrefab;
    public Transform contentParent;

    [Header("Счётчик")]
    public TextMeshProUGUI counterText;

    void OnEnable()
    {
        RefreshAchievements();
        AchievementManager.Instance.OnAchievementUnlocked += _ => RefreshAchievements();
    }

    void OnDisable()
    {
        if (AchievementManager.Instance)
            AchievementManager.Instance.OnAchievementUnlocked -= _ => RefreshAchievements();
    }

    void RefreshAchievements()
    {
        foreach (Transform child in contentParent)
            Destroy(child.gameObject);

        var all = AchievementManager.Instance.achievements;
        int unlocked = 0;

        foreach (var ach in all)
        {
            var go = Instantiate(achievementItemPrefab, contentParent);

            var nameText = go.transform.Find("NameText")?.GetComponent<TextMeshProUGUI>();
            var descText = go.transform.Find("DescText")?.GetComponent<TextMeshProUGUI>();
            var icon     = go.transform.Find("Icon")?.GetComponent<Image>();
            var lockIcon = go.transform.Find("LockIcon");

            if (nameText) nameText.text = ach.unlocked ? ach.displayName : "???";
            if (descText) descText.text = ach.unlocked ? ach.description : "Не разблокировано";
            if (icon && ach.icon) icon.sprite = ach.icon;

            // Показываем замок если не разблокировано
            lockIcon?.gameObject.SetActive(!ach.unlocked);

            // Затемняем заблокированные
            var group = go.GetComponent<CanvasGroup>() ?? go.AddComponent<CanvasGroup>();
            group.alpha = ach.unlocked ? 1f : 0.4f;

            if (ach.unlocked) unlocked++;
        }

        if (counterText)
            counterText.text = $"{unlocked} / {all.Count}";
    }
}
