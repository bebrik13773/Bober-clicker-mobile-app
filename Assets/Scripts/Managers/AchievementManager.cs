using UnityEngine;
using System;
using System.Collections.Generic;

[Serializable]
public class Achievement
{
    public string id;
    public string title;
    public string description;
    public string icon; // имя спрайта в Resources
    public bool unlocked;
}

public class AchievementManager : MonoBehaviour
{
    public static AchievementManager Instance { get; private set; }

    public event Action<Achievement> OnAchievementUnlocked;

    private List<Achievement> _achievements = new List<Achievement>();

    void Awake()
    {
        if (Instance != null && Instance != this) { Destroy(gameObject); return; }
        Instance = this;
        DontDestroyOnLoad(gameObject);
        InitAchievements();
    }

    void InitAchievements()
    {
        _achievements = new List<Achievement>
        {
            new Achievement { id="first_click",       title="Первый клик!",          description="Кликни впервые",                       icon="ach_click" },
            new Achievement { id="clicks_100",        title="100 кликов",            description="Сделай 100 кликов",                    icon="ach_click" },
            new Achievement { id="clicks_10000",      title="10 000 кликов",         description="Сделай 10 000 кликов",                 icon="ach_click" },
            new Achievement { id="clicks_1000000",    title="Миллион кликов",        description="Сделай 1 000 000 кликов",              icon="ach_click" },
            new Achievement { id="score_1000",        title="Первая тысяча",         description="Набери 1 000 очков",                   icon="ach_score" },
            new Achievement { id="score_1000000",     title="Миллионер",             description="Набери 1 000 000 очков",               icon="ach_score" },
            new Achievement { id="score_1000000000",  title="Миллиардер",            description="Набери 1 000 000 000 очков",           icon="ach_score" },
            new Achievement { id="first_upgrade",     title="Первый апгрейд",        description="Купи первый апгрейд в магазине",        icon="ach_shop" },
            new Achievement { id="all_upgrades",      title="Коллекционер",          description="Купи хотя бы 1 каждого апгрейда",      icon="ach_shop" },
            new Achievement { id="passive_10",        title="Пассивный доход",       description="Получи 10 бобров/сек",                 icon="ach_passive" },
            new Achievement { id="passive_1000",      title="Машина денег",          description="Получи 1 000 бобров/сек",              icon="ach_passive" },
            new Achievement { id="minigame_win",      title="Лётчик-бобёр",          description="Выиграй мини-игру Flying Beaver",      icon="ach_minigame" },
            new Achievement { id="all_quests",        title="Квестоман",             description="Выполни все квесты",                   icon="ach_quest" },
        };
    }

    public List<Achievement> GetAll() => _achievements;

    private void Unlock(string id)
    {
        var ach = _achievements.Find(a => a.id == id);
        if (ach == null || ach.unlocked) return;
        ach.unlocked = true;
        OnAchievementUnlocked?.Invoke(ach);
        Debug.Log($"[Achievement] Разблокировано: {ach.title}");
    }

    public void CheckClickAchievements(double totalClicks)
    {
        if (totalClicks >= 1)       Unlock("first_click");
        if (totalClicks >= 100)     Unlock("clicks_100");
        if (totalClicks >= 10000)   Unlock("clicks_10000");
        if (totalClicks >= 1000000) Unlock("clicks_1000000");
    }

    public void CheckScoreAchievements(double score)
    {
        if (score >= 1000)       Unlock("score_1000");
        if (score >= 1000000)    Unlock("score_1000000");
        if (score >= 1000000000) Unlock("score_1000000000");
    }

    public void CheckPassiveAchievements(double passive)
    {
        if (passive >= 10)   Unlock("passive_10");
        if (passive >= 1000) Unlock("passive_1000");
    }

    public void CheckShopAchievements()
    {
        var shop = ShopManager.Instance;
        if (shop == null) return;

        bool anyBought = shop.items.Exists(i => i.currentLevel > 0);
        if (anyBought) Unlock("first_upgrade");

        bool allBought = shop.items.TrueForAll(i => i.currentLevel > 0);
        if (allBought) Unlock("all_upgrades");
    }

    public void CheckMinigameWin()
    {
        Unlock("minigame_win");
    }

    public void CheckAllQuests()
    {
        var quests = QuestManager.Instance?.GetAllQuests();
        if (quests == null) return;
        bool allDone = quests.TrueForAll(q => q.completed);
        if (allDone) Unlock("all_quests");
    }

    public bool[] GetSaveData()
    {
        var result = new bool[_achievements.Count];
        for (int i = 0; i < _achievements.Count; i++)
            result[i] = _achievements[i].unlocked;
        return result;
    }

    public void LoadData(bool[] data)
    {
        if (data == null) return;
        for (int i = 0; i < Math.Min(data.Length, _achievements.Count); i++)
            _achievements[i].unlocked = data[i];
    }
}
