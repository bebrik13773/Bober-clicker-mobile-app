# Bober Clicker Mobile — Unity Setup Guide

## Структура скриптов

```
Assets/Scripts/
├── Core/
│   ├── GameManager.cs       — главный менеджер, синглтон
│   ├── ShopManager.cs       — магазин апгрейдов
│   ├── QuestManager.cs      — квесты
│   ├── AchievementManager.cs — ачивки
│   └── NumberFormatter.cs   — форматирование чисел
├── Data/
│   └── SaveSystem.cs        — сохранение/загрузка JSON
├── UI/
│   ├── MainUIController.cs  — главный экран
│   └── ShopUIController.cs  — экран магазина
└── MiniGame/
    └── FlyingBeaverGame.cs  — мини-игра
```

---

## Этап 1 — Сцена и менеджеры

1. Создай пустой GameObject → назови **"Managers"**
2. Добавь на него компоненты:
   - `GameManager`
   - `ShopManager`
   - `QuestManager`
   - `AchievementManager`

---

## Этап 2 — Главный экран (Canvas)

### Иерархия UI:
```
Canvas (Screen Space - Overlay)
├── MainPanel
│   ├── ScoreText (TextMeshPro)        ← "0 🦫"
│   ├── PerSecText (TextMeshPro)       ← "0/сек"
│   ├── ClickPowerText (TextMeshPro)   ← "+1 за клик"
│   ├── BoberButton (Button)           ← большая кнопка с бобром
│   └── BottomNav
│       ├── ShopButton   → вызывает ShowShop()
│       ├── QuestsButton → вызывает ShowQuests()
│       └── AchButton    → вызывает ShowAchievements()
├── ShopPanel
│   └── ScrollView → Viewport → Content   ← сюда ShopUIController
├── QuestsPanel
├── AchievementsPanel
└── AchievementPopup (скрытый по умолчанию)
    └── PopupText (TextMeshPro)
```

3. Добавь `MainUIController` на **Canvas**
4. Привяжи все поля в Inspector

---

## Этап 3 — Магазин

1. Создай префаб `ShopItemPrefab`:
```
ShopItemPrefab (вертикальный layout)
├── Icon (Image)
├── NameText (TextMeshPro)
├── DescText (TextMeshPro)
├── LevelText (TextMeshPro)
├── CostText (TextMeshPro)
└── BuyButton (Button)
    └── ButtonText (TextMeshPro)
```

2. Добавь `ShopUIController` на ShopPanel
3. Привяжи `shopItemPrefab` и `contentParent` (Content внутри ScrollView)

---

## Этап 4 — Мини-игра Flying Beaver

Создай отдельную **сцену FlyingBeaver** или панель:
```
FlyingBeaverPanel
├── BeaverImage (RectTransform) ← двигается скриптом
├── ObstacleParent (пустой контейнер)
├── ScoreText
├── StartPanel
│   └── StartButton
└── GameOverPanel
    ├── RewardText
    └── RestartButton
```

Создай префаб `ObstaclePrefab` — вертикальный прямоугольник (Image).

---

## Android Build

1. **File → Build Settings → Android**
2. **Player Settings:**
   - Package Name: `com.bebrik.boberclicker`
   - Minimum API: 26
   - Target API: 34
   - Scripting Backend: **IL2CPP**
   - Target Architectures: ✅ ARM64
3. **Build → .aab** для Google Play

---

## Зависимости

- **TextMeshPro** — встроен в Unity (Window → Package Manager → TextMeshPro)
- **LeanTween** — для анимации кнопки (Asset Store, бесплатно)
  - Или замени LeanTween в MainUIController на простой DOTween/собственную анимацию

---

## Сохранения

Файл сохранения: `Application.persistentDataPath/save.json`  
На Android: `/Android/data/com.bebrik.boberclicker/files/save.json`
