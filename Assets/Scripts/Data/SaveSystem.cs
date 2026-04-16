using UnityEngine;
using System;
using System.IO;

[Serializable]
public class GameData
{
    public double score;
    public double totalClicks;
    public double totalEarned;
    public int clickPower;
    public double passiveIncome;
    public ShopItemSaveData[] shopItems;
    public QuestSaveData[] quests;
    public bool[] achievements;
    public long lastSaveTime;
}

[Serializable]
public class ShopItemSaveData
{
    public string id;
    public int level;
}

[Serializable]
public class QuestSaveData
{
    public string id;
    public double progress;
    public bool completed;
    public bool claimed;
}

public static class SaveSystem
{
    private static string SavePath => Path.Combine(Application.persistentDataPath, "save.json");

    public static void Save()
    {
        try
        {
            GameData data = GameManager.Instance.GetData();
            data.shopItems = ShopManager.Instance?.GetSaveData();
            data.quests = QuestManager.Instance?.GetSaveData();
            data.achievements = AchievementManager.Instance?.GetSaveData();
            data.lastSaveTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();

            string json = JsonUtility.ToJson(data, true);
            File.WriteAllText(SavePath, json);
            Debug.Log($"[SaveSystem] Сохранено в {SavePath}");
        }
        catch (Exception e)
        {
            Debug.LogError($"[SaveSystem] Ошибка сохранения: {e.Message}");
        }
    }

    public static void Load()
    {
        try
        {
            if (!File.Exists(SavePath))
            {
                Debug.Log("[SaveSystem] Сохранение не найдено, новая игра");
                return;
            }

            string json = File.ReadAllText(SavePath);
            GameData data = JsonUtility.FromJson<GameData>(json);

            GameManager.Instance?.LoadData(data);
            ShopManager.Instance?.LoadData(data.shopItems);
            QuestManager.Instance?.LoadData(data.quests);
            AchievementManager.Instance?.LoadData(data.achievements);

            Debug.Log("[SaveSystem] Загружено успешно");
        }
        catch (Exception e)
        {
            Debug.LogError($"[SaveSystem] Ошибка загрузки: {e.Message}");
        }
    }

    public static void DeleteSave()
    {
        if (File.Exists(SavePath))
            File.Delete(SavePath);
        Debug.Log("[SaveSystem] Сохранение удалено");
    }
}
