# SpeedSound

> Automatically adjusts your media volume based on GPS speed — stay propelled by the music when you ride.

[![Build](https://github.com/<YOUR_USERNAME>/speedsound/actions/workflows/build.yml/badge.svg)](https://github.com/<YOUR_USERNAME>/speedsound/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/<YOUR_USERNAME>/speedsound)](https://github.com/<YOUR_USERNAME>/speedsound/releases/latest)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen)](https://developer.android.com/about/versions/oreo)

---

## How it works

SpeedSound runs as a **foreground service** and reads your GPS speed every 500 ms.  
Media volume follows a linear curve between your minimum speed (0 km/h) and the configured reference speed.  
When you accelerate, volume rises instantly. When you slow down, it fades smoothly.

```
Speed   0 km/h  →  Min volume  (e.g. 40 %)
Speed  50 km/h  →  Max volume  (e.g. 100 %)
```

## Features

- **Background operation** — persistent foreground service with speed + volume notification
- **Asymmetric ramp** — instant volume rise, smooth fade (~12 s for full range)
- **GPS smoothing** — 2-reading rolling average to absorb GPS jitter
- **Smart min-volume default** — auto-set on first launch (current volume − 20 %)
- **Fully configurable** — sensitivity, reference speed, min/max volume
- **Material You design** — supports Android 8.0+ (API 26)

## Screenshots

> *(coming soon)*

## Installation

### From GitHub Releases

1. Download the latest `.apk` from the [Releases page](https://github.com/<YOUR_USERNAME>/speedsound/releases)
2. Allow unknown sources on your device: **Settings → Security → Unknown sources**
3. Install the `.apk` file

### From Android Studio

```bash
git clone https://github.com/<YOUR_USERNAME>/speedsound.git
```

Open the project in Android Studio and click **Run ▶**.

**Requirements:** Android Studio Hedgehog or newer, JDK 17+.

## Permissions

| Permission | Purpose |
|---|---|
| `ACCESS_FINE_LOCATION` | Read GPS speed |
| `POST_NOTIFICATIONS` | Show the persistent status notification (Android 13+) |
| `FOREGROUND_SERVICE` | Run as a background service |
| `FOREGROUND_SERVICE_LOCATION` | Service type declaration (Android 14+) |

## Architecture

```
app/src/main/java/com/speedsound/
├── MainActivity.kt          # UI — sliders, toggle, live stats
├── SpeedVolumeService.kt    # Foreground Service — GPS → AudioManager
├── VolumeMapper.kt          # Pure logic: speed → volume fraction
└── SettingsRepository.kt    # SharedPreferences wrapper
```

## Generating a signed APK

1. In Android Studio: **Build → Generate Signed Bundle / APK → APK**
2. Create or select a keystore (`.jks` file) and fill in the alias / passwords
3. Choose **release** build variant and click **Finish**

To enable automatic signed builds in CI, add the following secrets to your GitHub repository  
(**Settings → Secrets and variables → Actions**):

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 your-keystore.jks` |
| `STORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

The workflow will automatically build a **signed release APK** when these secrets are present,  
and fall back to a debug APK otherwise.

## Contributing

Commits must follow **[Conventional Commits](https://www.conventionalcommits.org/)** to trigger the correct version bump:

| Prefix | Effect |
|---|---|
| `fix: ...` | Patch → `1.0.x` |
| `feat: ...` | Minor → `1.x.0` |
| `feat!: ...` or `BREAKING CHANGE` | Major → `x.0.0` |

## License

[MIT](LICENSE)


> Automatiquement ajuste le volume de ta musique selon ta vitesse GPS — pour rester propulsé par le son quand tu pédales.

[![Build](https://github.com/<YOUR_USERNAME>/speedsound/actions/workflows/build.yml/badge.svg)](https://github.com/<YOUR_USERNAME>/speedsound/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/<YOUR_USERNAME>/speedsound)](https://github.com/<YOUR_USERNAME>/speedsound/releases/latest)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen)](https://developer.android.com/about/versions/oreo)

---

## Fonctionnement

SpeedSound tourne en **service de premier plan** et lit la vitesse GPS toutes les 500 ms.  
Le volume multimédia suit une courbe linéaire entre ta vitesse minimale (0 km/h) et la vitesse maximale configurée.  
Quand tu accélères, le volume monte instantanément. Quand tu freines, il redescend doucement.

```
Vitesse 0 km/h  →  Volume min  (ex : 40 %)
Vitesse 50 km/h →  Volume max  (ex : 100 %)
```

## Fonctionnalités

- **Fond d'écran** — service foreground avec notification persistante vitesse + volume
- **Réactivité asymétrique** — montée de volume immédiate, descente douce (~12 s plage complète)
- **Lissage GPS** — moyenne glissante sur 2 mesures pour absorber les sautes GPS
- **Volume min intelligent** — initialisé automatiquement au premier lancement (volume actuel − 20 %)
- **Tout configurable** : sensibilité, vitesse max, volume min/max
- **Design Material You** — compatible Android 8.0+ (API 26)

## Captures d'écran

> _(à venir)_

## Installation

### Depuis les Releases GitHub

1. Télécharge le dernier `.apk` depuis la [page Releases](https://github.com/<YOUR_USERNAME>/speedsound/releases)
2. Autorise les sources inconnues sur ton téléphone : **Paramètres → Sécurité → Sources inconnues**
3. Installe le fichier `.apk`

### Depuis Android Studio

```bash
git clone https://github.com/<YOUR_USERNAME>/speedsound.git
```

Ouvre le projet dans Android Studio et clique sur **Run ▶**.

**Prérequis :** Android Studio Hedgehog ou plus récent, JDK 17+.

## Permissions requises

| Permission                    | Utilisation                   |
| ----------------------------- | ----------------------------- |
| `ACCESS_FINE_LOCATION`        | Lecture de la vitesse GPS     |
| `FOREGROUND_SERVICE`          | Service en arrière-plan       |
| `FOREGROUND_SERVICE_LOCATION` | Type du service (Android 14+) |

## Architecture

```
app/src/main/java/com/speedsound/
├── MainActivity.kt          # UI — sliders, toggle, stats live
├── SpeedVolumeService.kt    # Foreground Service — GPS → AudioManager
├── VolumeMapper.kt          # Logique pure : vitesse → fraction de volume
└── SettingsRepository.kt    # SharedPreferences wrapper
```

## Contribution

Les commits doivent suivre la convention **[Conventional Commits](https://www.conventionalcommits.org/)** pour déclencher le bon bump de version :

| Préfixe                           | Effet           |
| --------------------------------- | --------------- |
| `fix: ...`                        | Patch → `1.0.x` |
| `feat: ...`                       | Minor → `1.x.0` |
| `feat!: ...` ou `BREAKING CHANGE` | Major → `x.0.0` |

## Licence

[MIT](LICENSE)
