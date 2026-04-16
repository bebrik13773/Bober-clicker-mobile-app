using UnityEngine;
using System;
using System.Collections.Generic;

[Serializable]
public class Achievement
{
    public string id;
    public string displayName;
    public string description;
    public bool unlocked;
    public Sprite icon;
}

public class AchievementManager : MonoBehaviour
{
    public static AchievementManager Instance { get; private set; }

    public event Action<Achievement> OnAchievementUnlocked;

    public List<Achievement> achievements = new List<Achievement>();

    void Awake()
    {
        if (Instance != null && Instance != this) { Destroy(gameObject); return; }
        Instance = this;
        DontDestroyOnLoad(gameObject);
        InitDefaultAchievements();
    }

    void InitDefaultAchievements()
    {
        achievements = new List<Achievement>
        {
            new Achievement { id="first_click",     displayName="Первый клик",          description="Кликни первый раз" },
            new Achievement { id="clicks_100",       displayName="100 кликов",           description="Сделай 100 кликов" },
            new Achievement { id="clicks_10000",     displayName="10 000 кликов",        description="Сделай 10 000 кликов" },
            new Achievement { id="clicks_1m",        displayName="Миллион кликов",       description="Сделай 1 000 000 кликов" },
            new Achievement { id="score_1000",       displayName="Первая тысяча",        description="Накопи 1 000 бобров" },
            new Achievement { id="score_1m",         displayName="Миллионер",            description="Накопи 1 000 000 бобров" },
            new Achievement { id="shop_first",       displayName="Первая покупка",       description="Купи первый апгрейд" },
            new Achievement { id="shop_all",         displayName="Коллекционер",         description="Купи все апгрейды хотя бы раз" },
            new Achievement { id="passive_100",      displayName="Пассивщик",            description="Получай 100 бобров/сек" },
            new Achievement { id="minigame_win",     displayName="Лётчик-бобёр",         description="Выиграй мини-игру Flying Beaver" },
            new Achievement { id="all_quests",       displayName="Квестоман",            description="Выполни все квесты" },
        };
    }

    public void CheckClickAchievements()
    {
        double clicks = GameManager.Instance.TotalClicks;
        if (clicks >= 1)       Unlock("first_click");
        if (clicks >= 100)     Unlock("clicks_100");
        if (clicks >= 10000)   Unlock("clicks_10000");
        if (clicks >= 1000000) Unlock("clicks_1m");
    }

    public void CheckScoreAchievements()
    {
        double score = GameManager.Instance.Score;
        if (score >= 1000)    Unlock("score_1000");
        if (score >= 1000000) Unlock("score_1m");
    }

    public void CheckShopAchievements()
    {
        Unlock("shop_first");

        var shop = ShopManager.Instance;
        if (shop == null) return;
        bool allBought = shop.items.TrueForAll(i => i.currentLevel > 0);
        if (allBought) Unlock("shop_all");
    }

    public void CheckPassiveAchievements()
    {
        if (GameManager.Instance.PassiveIncome >= 100)
            Unlock("passive_100");
    }

    public void CheckQuestAchievements()
    {
        var qm = QuestManager.Instance;
        if (qm == null) return;
        if (qm.quests.TrueForAll(q => q.claimed))
            Unlock("all_quests");
    }

    public void UnlockMiniGameWin() => Unlock("minigame_win");

    void Unlock(string id)
    {
        var ach = achievements.Find(a => a.id == id);
        if (ach == null || ach.unlocked) return;
        ach.unlocked = true;
        OnAchievementUnlocked?.Invoke(ach);
        Debug.Log($"[Achievement] Разблокировано: {ach.displayName}");
    }

    public bool[] GetSaveData()
    {
        var result = new bool[achievements.Count];
        for (int i = 0; i < achievements.Count; i++)
            result[i] = achievements[i].unlocked;
        return result;
    }

    public void LoadData(bool[] data)
    {
        if (data == null) return;
        for (int i = 0; i < Math.Min(data.Length, achievements.Count); i++)
            achievements[i].unlocked = data[i];
    }
}
