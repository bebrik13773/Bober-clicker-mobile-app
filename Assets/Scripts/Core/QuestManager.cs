using UnityEngine;
using System;
using System.Collections.Generic;

public enum QuestType { TotalClicks, TotalEarned, ReachScore, BuyItem, PassiveIncome }

[Serializable]
public class Quest
{
    public string id;
    public string displayName;
    public string description;
    public QuestType type;
    public double target;
    public double rewardScore;
    public string requiredItemId; // для BuyItem квеста
    public double progress;
    public bool completed;
    public bool claimed;

    public float ProgressPercent => (float)Math.Min(1.0, progress / target);
}

public class QuestManager : MonoBehaviour
{
    public static QuestManager Instance { get; private set; }

    public event Action OnQuestsUpdated;

    public List<Quest> quests = new List<Quest>();

    void Awake()
    {
        if (Instance != null && Instance != this) { Destroy(gameObject); return; }
        Instance = this;
        DontDestroyOnLoad(gameObject);
        InitDefaultQuests();
    }

    void InitDefaultQuests()
    {
        quests = new List<Quest>
        {
            new Quest { id="clicks_100",    displayName="Первые шаги",      description="Кликни 100 раз",               type=QuestType.TotalClicks,  target=100,     rewardScore=50 },
            new Quest { id="clicks_1000",   displayName="Кликер-новичок",   description="Кликни 1000 раз",              type=QuestType.TotalClicks,  target=1000,    rewardScore=500 },
            new Quest { id="clicks_10000",  displayName="Кликер-про",       description="Кликни 10 000 раз",            type=QuestType.TotalClicks,  target=10000,   rewardScore=5000 },
            new Quest { id="earn_500",      displayName="Первые бобры",     description="Заработай 500 бобров",         type=QuestType.TotalEarned,  target=500,     rewardScore=100 },
            new Quest { id="earn_10000",    displayName="Богатый бобёр",    description="Заработай 10 000 бобров",      type=QuestType.TotalEarned,  target=10000,   rewardScore=2000 },
            new Quest { id="earn_1m",       displayName="Миллионер",        description="Заработай 1 000 000 бобров",   type=QuestType.TotalEarned,  target=1000000, rewardScore=100000 },
            new Quest { id="buy_dam",       displayName="Строитель",        description="Купи плотину",                 type=QuestType.BuyItem,      target=1, requiredItemId="dam", rewardScore=300 },
            new Quest { id="passive_10",    displayName="Пассивный доход",  description="Получай 10 бобров/сек",        type=QuestType.PassiveIncome, target=10,     rewardScore=1000 },
            new Quest { id="reach_100k",    displayName="Богач",            description="Накопи 100 000 бобров",        type=QuestType.ReachScore,   target=100000,  rewardScore=10000 },
        };
    }

    public void OnClick()
    {
        double totalClicks = GameManager.Instance.TotalClicks;
        foreach (var q in quests)
        {
            if (q.completed || q.type != QuestType.TotalClicks) continue;
            q.progress = totalClicks;
            CheckComplete(q);
        }
        OnQuestsUpdated?.Invoke();
    }

    public void OnPurchase(string itemId)
    {
        foreach (var q in quests)
        {
            if (q.completed || q.type != QuestType.BuyItem) continue;
            if (q.requiredItemId == itemId) { q.progress++; CheckComplete(q); }
        }

        // Проверяем пассивный доход
        CheckPassiveQuests();
        OnQuestsUpdated?.Invoke();
    }

    public void CheckScoreQuests()
    {
        double score = GameManager.Instance.Score;
        double totalEarned = GameManager.Instance.TotalEarned;
        foreach (var q in quests)
        {
            if (q.completed) continue;
            if (q.type == QuestType.ReachScore) { q.progress = score; CheckComplete(q); }
            if (q.type == QuestType.TotalEarned) { q.progress = totalEarned; CheckComplete(q); }
        }
        OnQuestsUpdated?.Invoke();
    }

    void CheckPassiveQuests()
    {
        double passive = GameManager.Instance.PassiveIncome;
        foreach (var q in quests)
        {
            if (q.completed || q.type != QuestType.PassiveIncome) continue;
            q.progress = passive;
            CheckComplete(q);
        }
    }

    void CheckComplete(Quest q)
    {
        if (q.progress >= q.target) q.completed = true;
    }

    public bool ClaimReward(string questId)
    {
        var q = quests.Find(x => x.id == questId);
        if (q == null || !q.completed || q.claimed) return false;
        q.claimed = true;
        GameManager.Instance.AddScore(q.rewardScore);
        OnQuestsUpdated?.Invoke();
        return true;
    }

    public QuestSaveData[] GetSaveData()
    {
        var result = new QuestSaveData[quests.Count];
        for (int i = 0; i < quests.Count; i++)
            result[i] = new QuestSaveData { id = quests[i].id, progress = quests[i].progress, completed = quests[i].completed, claimed = quests[i].claimed };
        return result;
    }

    public void LoadData(QuestSaveData[] data)
    {
        if (data == null) return;
        foreach (var saved in data)
        {
            var q = quests.Find(x => x.id == saved.id);
            if (q == null) continue;
            q.progress = saved.progress;
            q.completed = saved.completed;
            q.claimed = saved.claimed;
        }
        OnQuestsUpdated?.Invoke();
    }
}
