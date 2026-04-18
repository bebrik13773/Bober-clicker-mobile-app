using UnityEngine;
using UnityEngine.UI;
using TMPro;

/// <summary>
/// UI панели квестов. Динамически создаёт строки квестов.
/// Привяжи questItemPrefab и contentParent в Inspector.
/// </summary>
public class QuestUIController : MonoBehaviour
{
    [Header("Префаб строки квеста")]
    public GameObject questItemPrefab;
    public Transform contentParent;

    void OnEnable()
    {
        RefreshQuests();
        QuestManager.Instance.OnQuestsUpdated += RefreshQuests;
    }

    void OnDisable()
    {
        if (QuestManager.Instance) QuestManager.Instance.OnQuestsUpdated -= RefreshQuests;
    }

    void RefreshQuests()
    {
        foreach (Transform child in contentParent)
            Destroy(child.gameObject);

        foreach (var quest in QuestManager.Instance.quests)
        {
            var go = Instantiate(questItemPrefab, contentParent);
            go.name = quest.id;

            var titleText    = go.transform.Find("TitleText")?.GetComponent<TextMeshProUGUI>();
            var descText     = go.transform.Find("DescText")?.GetComponent<TextMeshProUGUI>();
            var progressBar  = go.transform.Find("ProgressBar")?.GetComponent<Slider>();
            var progressText = go.transform.Find("ProgressText")?.GetComponent<TextMeshProUGUI>();
            var rewardText   = go.transform.Find("RewardText")?.GetComponent<TextMeshProUGUI>();
            var claimButton  = go.transform.Find("ClaimButton")?.GetComponent<Button>();

            if (titleText)    titleText.text    = quest.displayName;
            if (descText)     descText.text      = quest.description;
            if (progressBar)  progressBar.value  = quest.ProgressPercent;
            if (rewardText)   rewardText.text    = $"+{NumberFormatter.Format(quest.rewardScore)} 🦫";

            if (progressText)
            {
                if (quest.completed)
                    progressText.text = "✅ Выполнено!";
                else
                    progressText.text = $"{NumberFormatter.Format(quest.progress)} / {NumberFormatter.Format(quest.target)}";
            }

            if (claimButton)
            {
                claimButton.gameObject.SetActive(quest.completed && !quest.claimed);
                string capturedId = quest.id;
                claimButton.onClick.AddListener(() =>
                {
                    QuestManager.Instance.ClaimReward(capturedId);
                    AchievementManager.Instance?.CheckQuestAchievements();
                });
            }

            // Затемняем выполненные/забранные
            if (quest.claimed)
            {
                var group = go.GetComponent<CanvasGroup>() ?? go.AddComponent<CanvasGroup>();
                group.alpha = 0.5f;
            }
        }
    }
}
