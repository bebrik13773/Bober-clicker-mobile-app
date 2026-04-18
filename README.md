# 🦫 Bober Clicker 2 — Mobile (Unity)

Нативная Android-версия игры Bober Clicker 2. Работает полностью офлайн, без браузера.

## Структура проекта

```
Assets/Scripts/
├── Core/
│   ├── GameManager.cs         # Главный менеджер, счёт, клики, пассивный доход
│   ├── ShopManager.cs         # Магазин апгрейдов
│   ├── QuestManager.cs        # Квесты
│   ├── AchievementManager.cs  # Достижения
│   └── NumberFormatter.cs     # Форматирование чисел (1.5K, 2M...)
├── Data/
│   └── SaveSystem.cs          # Сохранение/загрузка JSON
├── UI/
│   ├── MainUIController.cs    # Главный экран, кнопка клика, навигация
│   ├── ShopUIController.cs    # UI магазина
│   ├── QuestUIController.cs   # UI квестов
│   └── AchievementUIController.cs
└── MiniGame/
    └── FlyingBeaverGame.cs    # Мини-игра Flying Beaver
```

## Как собрать APK

### Требования
- Unity 2022 LTS + Android Build Support (через Unity Hub)
- Android SDK + JDK (устанавливается вместе с Android Build Support)

### Шаги

1. Открой проект в Unity
2. **File → Build Settings → Android → Switch Platform**
3. **Player Settings:**
   - `Package Name` → `com.bebrik.boberclicker2`
   - `Minimum API Level` → Android 6.0 (API 23)
   - `Target API Level` → API 34 (обязательно для Google Play)
4. **Build** → получишь `.apk`

### Для Google Play

В Build Settings включи **Build App Bundle (Google Play)** → получишь `.aab`.

> ⚠️ Создай Keystore в Player Settings → Publishing Settings.  
> Сохрани `.keystore` файл — без него не обновить приложение в Play!

## Зависимости

- **TextMeshPro** — `Window → TextMeshPro → Import TMP Essential Resources`
- **LeanTween** — бесплатно в Asset Store (для анимаций кнопок)

## Сохранения

Хранятся в `Application.persistentDataPath/save.json`.  
На Android: `/Android/data/com.bebrik.boberclicker2/files/save.json`

