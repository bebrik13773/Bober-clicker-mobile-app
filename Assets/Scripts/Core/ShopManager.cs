using UnityEngine;
using System;
using System.Collections.Generic;

[Serializable]
public class ShopItem
{
    public string id;
    public string displayName;
    public string description;
    public double baseCost;
    public int clickPowerBonus;      // бонус к клику
    public double passiveIncomeBonus; // бонус к пассивному доходу
    public int maxLevel;
    public int currentLevel;
    public Sprite icon;

    public double GetCost() => Math.Floor(baseCost * Math.Pow(1.15, currentLevel));
    public bool CanBuy() => currentLevel < maxLevel && GameManager.Instance.Score >= GetCost();
}

public class ShopManager : MonoBehaviour
{
    public static ShopManager Instance { get; private set; }

    public event Action OnShopUpdated;

    [Header("Список товаров (настраивается в Inspector)")]
    public List<ShopItem> items = new List<ShopItem>();

    void Awake()
    {
        if (Instance != null && Instance != this) { Destroy(gameObject); return; }
        Instance = this;
        DontDestroyOnLoad(gameObject);
        InitDefaultItems();
    }

    void InitDefaultItems()
    {
        if (items.Count > 0) return;

        items = new List<ShopItem>
        {
            new ShopItem { id="sharp_claws",    displayName="Острые когти",    description="+1 к клику",           baseCost=10,    clickPowerBonus=1,  maxLevel=50 },
            new ShopItem { id="beaver_friend",  displayName="Друг-бобёр",      description="+0.5 бобров/сек",      baseCost=50,    passiveIncomeBonus=0.5, maxLevel=30 },
            new ShopItem { id="dam",            displayName="Плотина",          description="+3 бобров/сек",        baseCost=200,   passiveIncomeBonus=3,   maxLevel=20 },
            new ShopItem { id="beaver_school",  displayName="Школа бобров",    description="+2 к клику",           baseCost=500,   clickPowerBonus=2,  maxLevel=20 },
            new ShopItem { id="beaver_factory", displayName="Фабрика бобров",  description="+10 бобров/сек",       baseCost=2000,  passiveIncomeBonus=10,  maxLevel=15 },
            new ShopItem { id="mega_dam",       displayName="Мега-плотина",    description="+25 бобров/сек",       baseCost=10000, passiveIncomeBonus=25,  maxLevel=10 },
            new ShopItem { id="beaver_city",    displayName="Город бобров",    description="+5 к клику, +50/сек",  baseCost=50000, clickPowerBonus=5, passiveIncomeBonus=50, maxLevel=5 },
        };
    }

    public bool BuyItem(string id)
    {
        var item = items.Find(i => i.id == id);
        if (item == null || !item.CanBuy()) return false;

        double cost = item.GetCost();
        if (!GameManager.Instance.SpendScore(cost)) return false;

        item.currentLevel++;
        if (item.clickPowerBonus > 0)
            GameManager.Instance.AddClickPower(item.clickPowerBonus);
        if (item.passiveIncomeBonus > 0)
            GameManager.Instance.AddPassiveIncome(item.passiveIncomeBonus);

        QuestManager.Instance?.OnPurchase(id);
        AchievementManager.Instance?.CheckShopAchievements();
        OnShopUpdated?.Invoke();
        return true;
    }

    public ShopItemSaveData[] GetSaveData()
    {
        var result = new ShopItemSaveData[items.Count];
        for (int i = 0; i < items.Count; i++)
            result[i] = new ShopItemSaveData { id = items[i].id, level = items[i].currentLevel };
        return result;
    }

    public void LoadData(ShopItemSaveData[] data)
    {
        if (data == null) return;
        foreach (var saved in data)
        {
            var item = items.Find(i => i.id == saved.id);
            if (item == null) continue;

            int levelDiff = saved.level - item.currentLevel;
            item.currentLevel = saved.level;

            // Восстанавливаем бонусы
            if (levelDiff > 0)
            {
                if (item.clickPowerBonus > 0)
                    GameManager.Instance.AddClickPower(item.clickPowerBonus * levelDiff);
                if (item.passiveIncomeBonus > 0)
                    GameManager.Instance.AddPassiveIncome(item.passiveIncomeBonus * levelDiff);
            }
        }
        OnShopUpdated?.Invoke();
    }
}
