# VisiOnAir Radio – Android (player leggero)

Funzioni:
- Stream audio: https://live.s1.radiovisionair.net:8443/?ver=710800
- Play/Pausa
- Riproduzione in background (anche a schermo spento) con notifica di sistema
- Titolo + descrizione del programma in onda: letti dalla pagina **Programmazione** https://radiovisionair.net/programmazione/ e sincronizzati con ora/giorno (Europe/Rome)
- Logo Visionair Radio in home

---

## Come ottenere l'APK **senza** Android Studio (consigliato)

Questo metodo compila l'APK su GitHub Actions e poi te lo fa scaricare.

### 1) Crea un repository e carica i file
1. Su GitHub: **New repository** → scegli un nome → **Create repository**.
2. Nel repo appena creato: **Add file → Upload files**.
3. Dal tuo PC: apri lo zip, **seleziona e carica TUTTI i file** che trovi dentro (devi vedere nella root del repo: `app/`, `build.gradle.kts`, `settings.gradle.kts`, `build-apk.yml`, ecc.).

> Importante: non caricare solo una cartella “contenitore”. Nella root del repo devono comparire direttamente `app/`, `build.gradle.kts`, ecc.

### 2) Crea il workflow (senza caricare cartelle “nascoste”)
GitHub a volte blocca l'upload della cartella `.github` da browser. Per evitare il problema:
1. Nel repo: **Add file → Create new file**.
2. Nel campo nome file scrivi esattamente:
   
   `.github/workflows/build-apk.yml`
3. Apri il file `build-apk.yml` che hai caricato nella root del repo, copia tutto il contenuto e incollalo nel nuovo file.
4. In fondo: **Commit changes**.

### 3) Compila e scarica l'APK
1. Vai in **Actions**.
2. Apri il workflow **Build APK**.
3. Premi **Run workflow**.
4. Quando finisce, entra nella run e in basso trovi **Artifacts** → scarica **VisionairRadioPlayer-debug-apk**.
5. Dentro lo zip dell'artifact trovi `app-debug.apk`.

---

## Installazione su Android
- Copia `app-debug.apk` sul telefono.
- Abilita “Origini sconosciute”/“Installa app sconosciute” per il browser o file manager usato.
- Installa.

---

## Nota tecnica (perché prima uscivano errori)
Le versioni “troppo nuove” di Android Gradle Plugin/Gradle (tipo AGP 9 + Gradle 9.x) sono ancora instabili e spesso rompono il build su CI.
Qui ho fissato versioni più stabili (AGP 8.13.x + Gradle 8.13) e ho tolto la dipendenza da `gradlew` nel workflow.
