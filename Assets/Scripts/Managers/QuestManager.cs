using UnityEngine;
using System;
using System.Collections.Generic;

public enum QuestType { TotalClicks, TotalEarned, BuyUpgrade, ReachScore }

[Serializable]
public class QuestDefinition
{
    public string id;
    public string title;
    public string description;
    public QuestType type;
    public double targetValue;
    public double reward;
    public string requiredUpgradeId; // для BuyUpgrade
}

[Serializable]
public class Quest
{
    public QuestDefinition def;
    public double progress;
    public bool completed;
    public bool claimed;
}

public class QuestManager : MonoBehaviour
{
    public static QuestManager Instance { get; private set; }

    public event Action OnQuestsUpdated;

    private List<Quest> _quests = new List<Quest>();

    void Awake()
    {
        if (Instance != null && Instance != this) { Destroy(gameObject); return; }
        Instance = this;
        DontDestroyOnLoad(gameObject);
        InitQuests();
    }

    void InitQuests()
    {
        var defs = new List<QuestDefinition>
        {
            new QuestDefinition { id="click_10",       title="Первые шаги",       description="Кликни 10 раз",             type=QuestType.TotalClicks,  targetValue=10,      reward=50 },
            new QuestDefinition { id="click_100",      title="Кликер",            description="Кликни 100 раз",            type=QuestType.TotalClicks,  targetValue=100,     reward=200 },
            new QuestDefinition { id="click_1000",     title="Про-кликер",        description="Кликни 1000 раз",           type=QuestType.TotalClicks,  targetValue=1000,    reward=2000 },
            new QuestDefinition { id="click_10000",    title="Маньяк кликера",    description="Кликни 10 000 раз",         type=QuestType.TotalClicks,  targetValue=10000,   reward=20000 },
            new QuestDefinition { id="earn_100",       title="Копейка рубль...",  description="Заработай 100 бобров",      type=QuestType.TotalEarned,  targetValue=100,     reward=100 },
            new QuestDefinition { id="earn_1000",      title="Тысячник",          description="Заработай 1 000 бобров",    type=QuestType.TotalEarned,  targetValue=1000,    reward=500 },
            new QuestDefinition { id="earn_100000",    title="Богач",             description="Заработай 100 000 бобров",  type=QuestType.TotalEarned,  targetValue=100000,  reward=50000 },
            new QuestDefinition { id="earn_1000000",   title="Миллионер",         description="Заработай 1 000 000 бобров",type=QuestType.TotalEarned,  targetValue=1000000, reward=500000 },
            new QuestDefinition { id="buy_claws",      title="Вооружён",          description="Купи Острые когти",         type=QuestType.BuyUpgrade,   targetValue=1,       reward=100,  requiredUpgradeId="sharp_claws" },
            new QuestDefinition { id="buy_dam",        title="Строитель",         description="Купи Плотину",              type=QuestType.BuyUpgrade,   targetValue=1,       reward=300,  requiredUpgradeId="dam" },
            new QuestDefinition { id="reach_1000",     title="На старте",         description="Набери 1 000 очков",        type=QuestType.ReachScore,   targetValue=1000,    reward=500 },
            new QuestDefinition { id="reach_1000000",  title="Бобёр-магнат",      description="Набери 1 000 000 очков",    type=QuestType.ReachScore,   targetValue=1000000, reward=1000000 },
        };

        foreach (var def in defs)
            _quests.Add(new Quest { def = def });
    }

    public List<Quest> GetAllQuests() => _quests;

    public void OnClick()
    {
        double totalClicks = GameManager.Instance.TotalClicks;
        foreach (var q in _quests)
        {
            if (q.completed || q.def.type != QuestType.TotalClicks) continue;
            q.progress = totalClicks;
            if (q.progress >= q.def.targetValue) CompleteQuest(q);
        }
        OnQuestsUpdated?.Invoke();
    }

    public void OnScoreEarned(double totalEarned, double currentScore)
    {
        foreach (var q in _quests)
        {
            if (q.completed) continue;
            if (q.def.type == QuestType.TotalEarned)
            {
                q.progress = totalEarned;
                if (q.progress >= q.def.targetValue) CompleteQuest(q);
            }
            else if (q.def.type == QuestType.ReachScore)
            {
                q.progress = currentScore;
                if (q.progress >= q.def.targetValue) CompleteQuest(q);
            }
        }
        OnQuestsUpdated?.Invoke();
    }

    public void OnPurchase(string upgradeId)
    {
        foreach (var q in _quests)
        {
            if (q.completed || q.def.type != QuestType.BuyUpgrade) continue;
            if (q.def.requiredUpgradeId == upgradeId)
            {
                q.progress = 1;
                CompleteQuest(q);
            }
        }
        OnQuestsUpdated?.Invoke();
    }

    private void CompleteQuest(Quest q)
    {
        q.completed = true;
        Debug.Log($"[QuestManager] Квест выполнен: {q.def.title}");
    }

    public bool ClaimReward(string questId)
    {
        var q = _quests.Find(x => x.def.id == questId);
        if (q == null || !q.completed || q.claimed) return false;
        q.claimed = true;
        GameManager.Instance.AddScore(q.def.reward);
        OnQuestsUpdated?.Invoke();
        Debug.Log($"[QuestManager] Награда получена: +{q.def.reward}");
        return true;
    }

    public QuestSaveData[] GetSaveData()
    {
        var result = new QuestSaveData[_quests.Count];
        for (int i = 0; i < _quests.Count; i++)
            result[i] = new QuestSaveData
            {
                id = _quests[i].def.id,
                progress = _quests[i].progress,
                completed = _quests[i].completed,
                claimed = _quests[i].claimed
            };
        return result;
    }

    public void LoadData(QuestSaveData[] data)
    {
        if (data == null) return;
        foreach (var saved in data)
        {
            var q = _quests.Find(x => x.def.id == saved.id);
            if (q == null) continue;
            q.progress = saved.progress;
            q.completed = saved.completed;
            q.claimed = saved.claimed;
        }
        OnQuestsUpdated?.Invoke();
    }
}
