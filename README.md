# OLED Tastiera IT

Un'app Android (Kotlin) che installa una tastiera di sistema:

- Layout **QWERTY italiano** con lettere accentate (à è é ì ò ù …) su pressione lunga
- **Nessun suggerimento / correzione automatica**
- **Tema OLED**: grigio chiaro su nero totale, oppure bianco su nero, con pulsante toggle nelle impostazioni
- Icone/tasti disegnati via Canvas, nessuna dipendenza esterna

## Installazione

Scarica l'APK più recente dal flusso di lavoro GitHub (Actions → Artifact) oppure
dalla release taggata `v*`.

1. Installa l'APK
2. Impostazioni → Sistema → Lingue e immissione → Tastiere → abilita **OLED Tastiera IT**
3. Selezionala come tastiera predefinita
4. Apri l'app per cambiare il tema (grigio su nero / bianco su nero)

## Funzioni

| Tasto | Azione |
| --- | --- |
| `⇧` | Maiuscole; doppio tap o pressione lunga = Bloc maiuscole |
| Pressione lunga su vocale | Lettere accentate italiane |
| Pressione lunga su `⌫` | Cancellazione ripetuta |
| `?123` | Cifre e simboli (`€` incluso) |

## Build

```bash
./gradlew assembleDebug
```

L'APK si trova in `app/build/outputs/apk/debug/`.

## GitHub Actions

`.github/workflows/build-apk.yml` compila l'APK a ogni push su `main` e pubblica
l'artefatto; con un tag `v*` crea anche una Release GitHub con l'APK.
