using UnityEngine;
using UnityEngine.UI;
using TMPro;

/// <summary>
/// UI магазина. Динамически создаёт карточки товаров.
/// Привяжи shopItemPrefab и contentParent в Inspector.
/// </summary>
public class ShopUIController : MonoBehaviour
{
    [Header("Префаб карточки товара")]
    public GameObject shopItemPrefab;
    public Transform contentParent;

    void OnEnable()
    {
        RefreshShop();
        ShopManager.Instance.OnShopUpdated += RefreshShop;
        GameManager.Instance.OnScoreChanged += OnScoreChanged;
    }

    void OnDisable()
    {
        if (ShopManager.Instance) ShopManager.Instance.OnShopUpdated -= RefreshShop;
        if (GameManager.Instance) GameManager.Instance.OnScoreChanged -= OnScoreChanged;
    }

    void OnScoreChanged(double _) => RefreshButtons();

    void RefreshShop()
    {
        // Очищаем старые карточки
        foreach (Transform child in contentParent)
            Destroy(child.gameObject);

        // Создаём новые
        foreach (var item in ShopManager.Instance.items)
        {
            var go = Instantiate(shopItemPrefab, contentParent);
            go.name = item.id;

            var nameText    = go.transform.Find("NameText")?.GetComponent<TextMeshProUGUI>();
            var descText    = go.transform.Find("DescText")?.GetComponent<TextMeshProUGUI>();
            var costText    = go.transform.Find("CostText")?.GetComponent<TextMeshProUGUI>();
            var levelText   = go.transform.Find("LevelText")?.GetComponent<TextMeshProUGUI>();
            var buyButton   = go.transform.Find("BuyButton")?.GetComponent<Button>();
            var iconImage   = go.transform.Find("Icon")?.GetComponent<Image>();

            if (nameText)  nameText.text  = item.displayName;
            if (descText)  descText.text  = item.description;
            if (costText)  costText.text  = NumberFormatter.Format(item.GetCost()) + " 🦫";
            if (levelText) levelText.text = item.currentLevel >= item.maxLevel
                                            ? "МАКС" : $"Ур. {item.currentLevel}/{item.maxLevel}";
            if (iconImage && item.icon) iconImage.sprite = item.icon;

            if (buyButton)
            {
                bool maxed = item.currentLevel >= item.maxLevel;
                buyButton.interactable = !maxed && item.CanBuy();
                buyButton.GetComponentInChildren<TextMeshProUGUI>().text = maxed ? "Макс" : "Купить";

                string capturedId = item.id;
                buyButton.onClick.AddListener(() => OnBuyClicked(capturedId));
            }
        }
    }

    void RefreshButtons()
    {
        foreach (var item in ShopManager.Instance.items)
        {
            var go = contentParent.Find(item.id);
            if (!go) continue;
            var btn = go.Find("BuyButton")?.GetComponent<Button>();
            if (btn) btn.interactable = item.CanBuy();
        }
    }

    void OnBuyClicked(string id)
    {
        ShopManager.Instance.BuyItem(id);
    }
}
