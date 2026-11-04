# FinanzTracker – Android-Projekt

## APK über GitHub bauen
1. ZIP entpacken.
2. Den **gesamten Inhalt** dieses Ordners in das Stammverzeichnis deines GitHub-Repositories hochladen – nicht nur die ZIP-Datei.
3. In GitHub **Actions** öffnen.
4. **Android APK bauen** auswählen und **Run workflow** drücken (oder nach einem Push den automatischen Lauf öffnen).
5. Nach erfolgreichem Lauf unter **Artifacts** `FinanzTracker-debug-apk` herunterladen und entpacken. Darin liegt `app-debug.apk`.

## In dieser ersten Version
- lokale Speicherung auf dem Smartphone
- Konten hinzufügen und löschen
- Buchungen: Einnahme/Ausgabe, Datum, Zweck und Betrag in CHF
- Tages-, Wochen-, Monats- und Jahresfilter; Monat ist Standard
- Summen und Saldo des gewählten Kontos und Zeitraums
- CSV-Export zum Teilen; CSV lässt sich mit Excel öffnen
- Münzsymbol als App-Symbol

## Noch nicht umgesetzt
PIN-Schutz, PDF-Export sowie Google-Drive-/OneDrive-Backup sind noch nicht enthalten. Der Export ist CSV und keine native XLSX-Datei. Dies ist eine erste Projektversion und sollte vor produktiver Finanzverwaltung sorgfältig getestet werden.

Wichtig: `settings.gradle`, `build.gradle`, `app/` und `.github/workflows/android.yml` müssen direkt im Repository-Stamm liegen.
