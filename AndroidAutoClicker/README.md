# Автокликер для Android

Простой автокликер: красный прицел на экране и плавающая панель
(▶/■ — старт/стоп, ✥ — перетащить панель, ⚙ — настройки, ✕ — скрыть).
Нажимает в центре прицела с заданным интервалом; можно ограничить число кликов.

**Скачать APK:** https://github.com/ivugar/PowershellScripts/releases/download/autoclicker-latest/autoclicker.apk

Проверено: устанавливается и работает на телефоне Xiaomi (HyperOS).

## Установка

1. Скачать `autoclicker.apk` на телефон и установить (разрешить установку из неизвестных источников).
2. Открыть «Автокликер» → «Включить в Спец. возможностях» → включить «Автокликер».
3. Если на Android 13+ переключатель серый: Настройки → Приложения → Автокликер → ⋮ →
   «Разрешить доступ к настройкам ограниченного доступа», затем повторить шаг 2.

## Как устроено

- `app/src/main/java/com/ivugar/autoclicker/ClickService.java` — сервис спец. возможностей:
  рисует прицел и панель (`TYPE_ACCESSIBILITY_OVERLAY`, отдельного разрешения на оверлей не нужно)
  и нажимает через `dispatchGesture`. Пока идут клики, прицел становится «прозрачным» для касаний.
- `app/src/main/java/com/ivugar/autoclicker/MainActivity.java` — экран настроек
  (интервал в мс, минимум 20; число кликов, 0 — бесконечно), хранится в `SharedPreferences`.
  Шапка своя, отступы берутся из `WindowInsets`: на Android 15 окно рисуется под системными панелями.
- Java, без AndroidX, minSdk 24, targetSdk 35, AGP 8.7.3, Gradle 8.14.3 (без wrapper —
  Gradle ставит workflow).

## Сборка

APK собирается в GitHub Actions (`.github/workflows/android-autoclicker.yml`) при каждом push,
затрагивающем `AndroidAutoClicker/`, и выкладывается в Release `autoclicker-latest`
(файл перезаписывается). Можно запустить и вручную: Actions → Build AutoClicker APK → Run workflow.

Собирается **release**-сборка: debug-APK на Xiaomi не устанавливался («App not installed»).
Ключ подписи создаётся заново при каждой сборке и в репозиторий не попадает, поэтому новую версию
нельзя поставить поверх старой — сначала удалить старую. Чтобы обновлять без удаления, нужно
положить постоянный keystore в секреты репозитория и брать его в workflow вместо `keytool -genkeypair`.

При изменениях увеличивать `versionCode`/`versionName` в `app/build.gradle`.
