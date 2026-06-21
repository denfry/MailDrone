# MailDrone — Почта России на дронах 🚁📦

[![Build](https://github.com/denfry/MailDrone/actions/workflows/build.yml/badge.svg)](https://github.com/denfry/MailDrone/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/denfry/MailDrone?sort=semver)](https://github.com/denfry/MailDrone/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x%20%7C%2026.x-brightgreen.svg)](#архитектура)

Плагин для Minecraft, который доставляет посылки между игроками с помощью
летающего дрона, оформленного в духе Почты России: трек-номера `ПР…RU`,
статусы доставки, случайные задержки и «сортировочные центры».

Работает на **Paper, Folia и всех форках** (Purpur, Leaf, Leaves, Pufferfish,
Canvas, ShreddedPaper …) и на **двух линейках версий**: классической `1.21.x`
и новой `26.x`. Один универсальный `.jar`.

## Возможности

- `/post send <ник>` — меню упаковки: кладёте предметы, платите, отправляете.
- Дрон (BlockDisplay-корпус + 4 крутящихся ItemDisplay-винта) физически летит
  от отделения к почтомату с плавной интерполяцией.
- Трек-номера формата `ПР123456789RU` и статусы: *принята → в пути →
  (на сортировке) → прибыла → вручена*.
- Случайные задержки доставки и шанс «застрять на сортировке» (мем-уровень
  настраивается, по умолчанию умеренный, без потери груза).
- Почтоматы для получения (`/post box` или ПКМ по блоку-почтомату).
- Возврат отправителю: посылку, которую долго не забирают, дрон «возвращает» —
  отправитель забирает её сам через `/post box` (срок настраивается).
- Уведомления получателю (онлайн — сообщение + заголовок + звук; офлайн — при
  входе).
- Отделения и почтоматы — это назначаемые блоки (команды администратора).

## Команды

| Команда | Право | Описание |
|---|---|---|
| `/post send <ник>` | `maildrone.send` | Открыть упаковку и отправить посылку |
| `/post box` | `maildrone.use` | Открыть почтомат со своими посылками |
| `/post track <номер>` | `maildrone.use` | Отследить посылку |
| `/post list` | `maildrone.use` | Список своих активных посылок |
| `/post admin office` | `maildrone.admin` | Назначить/снять отделение (блок в прицеле) |
| `/post admin postomat` | `maildrone.admin` | Назначить/снять почтомат (блок в прицеле) |
| `/post admin list` | `maildrone.admin` | Список почтовых точек |
| `/post admin reload` | `maildrone.admin` | Перечитать конфиг |

Алиасы команды: `/pochta`, `/mail`.

### Права по умолчанию
- `maildrone.use`, `maildrone.send` — всем (`true`)
- `maildrone.admin` — операторам (`op`)

## Скачать

Готовый плагин — на странице [**Releases**](https://github.com/denfry/MailDrone/releases/latest)
(файл `MailDrone-<версия>.jar`). Сборки каждого коммита доступны как артефакт
`MailDrone-jar` во вкладке [Actions](https://github.com/denfry/MailDrone/actions/workflows/build.yml).

## Установка

1. Скачайте `MailDrone-<версия>.jar` из [Releases](https://github.com/denfry/MailDrone/releases/latest)
   или соберите сами (см. [Сборку](#сборка)).
2. Положите в папку `plugins/` сервера Paper/Folia (1.21.x или 26.x).
3. Запустите сервер — создадутся `config.yml` и файлы данных.
4. (Опционально) назначьте отделения и почтоматы: посмотрите на блок и выполните
   `/post admin office` / `/post admin postomat`.

> Без настроенных отделений/почтоматов плагин тоже работает: отправка идёт из
> точки игрока, а получение — через `/post box`.

## Сборка

Требуется JDK 21+. Gradle ставить не обязательно — есть wrapper.

```bash
./gradlew build          # Linux/macOS
.\gradlew.bat build      # Windows
```

Готовый плагин: `plugin/build/libs/MailDrone-<версия>.jar`.

## Конфигурация (`config.yml`)

Основные параметры: время доставки (`delivery.min/max-seconds`), шанс и время
сортировки, стоимость отправки (`cost.type`: `NONE` / `XP_LEVELS` / `ITEM`),
число рядов в упаковке, требование отделения рядом, внешний вид дрона
(материалы корпуса/винтов/носа, масштаб, число винтов, поворот по курсу,
частицы, звук), срок хранения вручённых посылок (`storage.delivered-retention-days`),
срок ожидания получения до возврата отправителю (`storage.pickup-timeout-days`),
проверку обновлений (`update-check`) и метрики bStats (`metrics`). Тексты можно
переопределить файлом `messages.yml` (формат MiniMessage).

## Архитектура

Многомодульный Gradle-проект с версионными адаптерами; сборка — один `.jar`
через `com.gradleup.shadow`.

```
adapter-api/  интерфейс PlatformAdapter + абстракция частиц
core/         вся логика: посылки, доставка, дрон, GUI, хранилище, команды
v1_21/        адаптер линейки 1.21.x
v26_1/        адаптер линейки 26.x
plugin/       JavaPlugin-bootstrap: выбор адаптера по версии + shadowJar
```

**Совместимость с Folia.** Вся работа идёт через Folia-совместимые планировщики
(entity / region / global / async), которые присутствуют и в Paper; на серверах
без регионов используется обычный `BukkitScheduler` (детект Folia в рантайме).
Доступ к сущностям/миру/инвентарю/частицам выполняется на потоке владеющего
региона; перемещение дрона — через `teleportAsync`. В манифесте указано
`folia-supported: true`.

**Совместимость версий.** Код компилируется против baseline `paper-api:1.21.x`
(байткод Java 21) и использует API, общее для обеих линеек; различия (например,
частицы) изолированы в версионных адаптерах. Один jar грузится и на Java 21
(1.21.x), и на Java 25 (26.x). В `plugin.yml` указано `api-version: '1.21'`,
которое принимают обе линейки.

## Тестирование

Юнит-тесты чистой логики (трек-номера, статусы, форматирование, сравнение
версий, фазы полёта `FlightMath`) — `./gradlew test` (15 тестов). Для
интеграционных тестов через MockBukkit добавьте в `core/build.gradle.kts`:

```kotlin
testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:<версия>")
```

(резолвится из Maven Central; покрывает команды/инвентари, но Folia-планировщики
и Display-сущности не эмулирует).

## Публикация и инфраструктура

- **CI:** GitHub Actions (`.github/workflows/build.yml`) собирает проект и
  прогоняет тесты на каждый push/PR; готовый jar — артефакт `MailDrone-jar`.
- **Метрики:** bStats (релоцирован в jar, по умолчанию выключен). Зарегистрируйте
  плагин на <https://bstats.org> и впишите `metrics.plugin-id` в `config.yml`.
- **Обновления:** при запуске проверяется последний релиз `denfry/MailDrone`.
- **Hangar/Modrinth:** заявляйте поддержку версий `1.21.x` и `26.1.x`; для
  автопубликации добавьте `io.papermc.hangar-publish` / `com.modrinth.minotaur`
  в `plugin/build.gradle.kts`.

## Релизы

Релиз выпускается автоматически по тегу:

```bash
git tag v0.2.0
git push origin v0.2.0
```

Workflow [`release.yml`](.github/workflows/release.yml) собирает `jar` с версией
из тега и публикует GitHub-релиз с вложением и авто-сгенерированными заметками.

## Лицензия

[MIT](LICENSE) © denfry
