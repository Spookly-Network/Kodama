# Hytale Server Orchestrator – ausführliche Spezifikation

## 1. Zweck und Ziele

Das System soll für dich **vollautomatisch Hytale-Server-Instanzen** erstellen, starten, überwachen und wieder entsorgen.  
Dafür wird die komplette Logik in drei Ebenen aufgeteilt:

1. **Brain (Control Plane)**  
   Zentrale Steuerung, verwaltet alles und redet mit den Nodes und dem Webpanel.

2. **Nodes (Compute Layer)**  
   Maschinen, auf denen Docker läuft und die die eigentliche Arbeit machen: Templates holen, mergen, Server starten.

3. **Gameserver (Runtime Layer)**  
   Der eigentliche Hytale-Server-Prozess im Docker-Container.

Wichtige Ziele:

- Mehrere Gamemodes und Maps über ein **mehrschichtiges Template-System** abbilden.
- Beliebig viele Templates pro Instanz (nicht nur Master / Gamemode / Map).
- **S3 als zentraler Template-Speicher**, Nodes haben nur Cache.
- **Nodes bleiben dumm und austauschbar**, Brain hat den Überblick.
- Webpanel für Verwaltung ohne direkte SSH-Logins.

Nicht-Ziele (zumindest erstmal):

- Kein vollautomatisches Auto-Scaling über Cloud-APIs.
- Keine superkomplexen In-Game-Features, Fokus ist auf Orchestrierung.
- Keine Mod-/Plugin-Verwaltung auf Dateiebene im ersten Wurf.

---

## 2. Hohe Ebene: Architektur

### 2.1 Gesamtbild

```text
                    +------------------------------+
                    |             Brain            |
                    |      (Control Plane)         |
                    +-------+-------------+--------+
                            |             |
                    REST / gRPC / MQ      |
                            |             |
            +---------------+---+         |
            |                   |         |
     +------+--------+   +------+--------+
     |    Node A     |   |    Node B     |   ...
     | (Agent +      |   | (Agent +      |
     |  Docker)      |   |  Docker)      |
     +------+--------+   +------+--------+
            |                   |
            | Docker            | Docker
            v                   v
      +-----+------+       +----+-------+
      | Gameserver |       | Gameserver |
      +------------+       +------------+

                    +------------------------------+
                    |             S3               |
                    |       (Template Storage)     |
                    +------------------------------+

                    +------------------------------+
                    |           Webpanel           |
                    |        (Nuxt Frontend)       |
                    +------------------------------+
```

### 2.2 Wichtige Domänenbegriffe

- **Template**  
  Paket (Tarball) mit Dateien und Template-Metadaten, die einen Teil der Serverwelt beschreiben  
  Beispiele: „Base-Hytale“, „Lobby-Gamemode“, „Map-Template-XYZ“.

- **Template-Layer**  
  Ein Template in einer konkreten Reihenfolge zu einer Instanz.

- **Instance (Gameserver-Instanz)**  
  Konkreter Server, der aus einer **geordneten Liste von Templates** erzeugt wird.

- **Node**  
  Maschine mit Docker, auf der mehrere Instanzen laufen.

- **Brain**  
  Zentrale Steuerung: weiß, welche Instanz wo läuft, welche Templates es gibt und wie sie zusammengesetzt werden.

---

## 3. Template-System im Detail

### 3.1 Template-Lebenszyklus

1. Template wird erstellt (lokal, zentral, egal wo)  
2. Template wird als **Tarball** gepackt  
3. Tarball wird in **S3** hochgeladen  
4. Brain bekommt einen Datensatz:  
   - Template-ID  
   - Version  
   - Checksumme  
   - S3-Key  
   - Beschreibung, Typ, evtl. Tags  

5. Instanz-Definition nutzt eine **sortierte Liste** von Template-IDs/Versionen  
6. Node lädt die Templates bei Bedarf aus S3 und cached sie.

### 3.2 Datenfluss Template

```text
[Brain] --(Metadaten)--> [DB: Template-Tabelle]

[Brain] speichert:
  - template_id
  - version
  - checksum
  - s3_key
  - type (master, gamemode, map, custom)
  - meta (Beschreibung, Tags, etc.)

Tarball selbst:
  liegt in S3 unter s3_key
```

Beim Erzeugen einer Instanz:

```text
[Brain] holt Template-Metadaten aus DB
[Brain] baut sortierte Liste:
  [master v1, gamemode v4, map v2, extra v1, ...]
[Brain] -> [Node]: "prepare_instance" mit Liste der Template-IDs/Versionen
```

### 3.3 Template-Merge-Regeln

- Templates werden in **fixer Reihenfolge** angewendet, z. B.:

  1. Base / Master  
  2. Gamemode  
  3. Map  
  4. Optional: Events, Specials, Overrides  

- Regel: **„Last layer wins“**  
  Wenn Datei in mehreren Templates existiert, zählt die aus dem zuletzt angewendeten Template.

- Verzeichnisse werden rekursiv gemerged.  
- Überflüssige Dateien werden nicht automatisch gelöscht, es sei denn, es gibt separate Regeln dafür (z. B. Clean-Template ganz am Anfang).

Schematisches Merge:

```text
MergedFolder = leer

for template in templates_in_order:
    entpacke template in temp_folder
    kopiere alle Dateien auf MergedFolder
      → existiert Datei schon? überschreiben

    optional: Template-spezifische Hooks ausführen (z. B. kleine Meta-Files)
```

### 3.4 Variable Substitution

Nach dem Mergen:

- Platzhalter wie `${INSTANCE_ID}`, `${PORT}`, `${SERVER_NAME}`, `${SEED}`, `${LOCALE}` usw.
- Node kennt alle Konfig-Werte, Brain gibt die Werte beim „prepare_instance“ mit.

Beispielhafte Werte (nur Konzept, kein Code):

- INSTANCE_ID: eindeutige ID  
- NODE_NAME: Name der Node  
- PORT: zugewiesener Port  
- REGION: z. B. `eu-central-1`  
- ENVIRONMENT: `dev`, `stage`, `prod`  

---

## 4. Node: Agent und Ablauf

### 4.1 Node-Überblick

Die Node ist ein Hintergrunddienst. Aufgaben:

- Beim Brain registrieren  
- Heartbeats senden  
- Befehle ausführen (Templates holen, Instanzen starten/stoppen)  
- Template-Cache pflegen  
- Docker-Container verwalten

Startablauf Node:

```text
[Node Agent Start]
  |
  v
liest lokale Config (Brain-URL, Node-ID, Token, etc.)
  |
  v
verbindet sich mit Brain, sendet "register_node"
  |
  v
geht in "ready" Zustand, wartet auf Befehle
  |
  v
periodische Heartbeats (Status, Kapazität, Dev-Mode)
```

### 4.2 Template-Cache auf der Node

Struktur (nur konzeptionell):

- Cache-Verzeichnis: z. B. `/var/cache/hytale-templates`
- Unterordner nach Template-ID und Version  
- Zusätzlich eine Datei mit der Checksumme

Ablauf beim Zugriff auf Template:

```text
Node erhält Template {id, version, checksum}.

1. Prüfe, ob Cache-Verzeichnis für diese Kombination existiert.
2. Wenn nicht:
      lade Tarball von S3, speichere im Cache, entpacke.
   Wenn doch:
      vergleiche gespeicherte Checksumme mit erwarteter.
3. Wenn Checksumme abweicht:
      neu von S3 laden und Cache aktualisieren.
4. Liefere Pfad zum cached Template zurück.
```

### 4.3 Dev-Mode und Cache-Purge

- **Dev-Mode**:  
  - Node ignoriert den Cache und lädt jedes Template neu aus S3.  
  - Sinnvoll für Template-Entwicklung oder Debugging.  

- **Cache-Purge**:  
  - Brain sendet `purge_cache` an Node.  
  - Node löscht kompletten Template-Cache oder bestimmte Templates.  

Flow:

```text
[Brain] -> [Node]: "set_dev_mode(true/false)"
[Node] passt Verhalten an.

[Brain] -> [Node]: "purge_cache"
[Node] löscht Cache-Verzeichnisse.
```

### 4.4 Instanz erstellen auf Node

```text
[Brain] -> [Node]: "prepare_instance" mit:
    - instance_id
    - template_layers (Liste in Reihenfolge)
    - variable_map (Port, Name, etc.)

[Node]:
  1. Für jedes Template: Cache prüfen / S3 laden
  2. Leeres Instanzverzeichnis anlegen
  3. Templates nacheinander in das Instanzverzeichnis mergen
  4. Variablen ersetzen
  5. Interne Konsistenz prüfen (z. B. wichtige Files vorhanden)
  6. Status „prepared“ an Brain melden
```

Danach:

```text
[Brain] -> [Node]: "start_instance" mit instance_id

[Node]:
  1. Docker-Container mit Instanzverzeichnis als Volume starten
  2. Ports mappen
  3. Environment setzen (z. B. INSTANCE_ID)
  4. Status „running“ an Brain melden
```

Stoppen/Destroy:

```text
[Brain] -> [Node]: "stop_instance"
[Node] stoppt Container, meldet „stopped“.

[Brain] -> [Node]: "destroy_instance"
[Node] löscht Instanz-Verzeichnis, meldet „destroyed“.
```

---

## 5. Brain: Control Plane im Detail

### 5.1 Verantwortungen

- Templates verwalten  
- Nodes verwalten (Registrierung, Online/Offline, Dev-Mode)  
- Instanzen verwalten (Definition, Zustand, Logs, History)  
- Scheduling: auf welcher Node soll eine Instanz laufen?  
- API für Webpanel bereitstellen  
- Sicherheitslogik: Auth, Rechte, Tokens

### 5.2 Instanz-Scheduling

Kriterien:

- Node ist online  
- Node hat genügend freie Slots/CPU/RAM (eigene Kapazitätslogik)  
- Node-Eigenschaften (Region, Tags) passen zur Anforderung  
- Optional: möglichst gleichmäßige Verteilung

Ablauf:

```text
1. Webpanel/externes System schickt "create_instance_request" ans Brain.
2. Brain legt Instanz-Datensatz an: Zustand "REQUESTED".
3. Brain wählt eine passende Node.
4. Brain sendet "prepare_instance" an diese Node.
5. Node führt Template-Schritte aus.
6. Node meldet "prepared".
7. Brain sendet "start_instance".
8. Node meldet "running".
9. Brain aktualisiert Zustand und zeigt ihn im Panel an.
```

### 5.3 Zustandsmaschine Instanz

```text
+------------+      +-----------+      +-----------+
| REQUESTED  | ---> | PREPARING | ---> | STARTING  |
+------------+      +-----------+      +-----------+
      |                                          |
      | Fehler                                   |
      v                                          v
+------------+                              +-----------+
|   FAILED   | <--------------------------- | RUNNING   |
+------------+                              +-----------+
                                                 |
                                                 v
                                           +-----------+
                                           | STOPPING  |
                                           +-----------+
                                                 |
                                                 v
                                           +-----------+
                                           | DESTROYED |
                                           +-----------+
```

---

## 6. Gameserver-Ebene

### 6.1 Runtime

- Hytale-Server läuft im Container mit:

  - Merged Instanz-Verzeichnis als Volume  
  - Konfiguration mit bereits ersetzten Variablen  
  - Ports und Ressourcen begrenzt durch Docker

- Node beobachtet:

  - Container-Status (running, exited)  
  - Exit-Code  
  - optional Logs  

### 6.2 Crash-Handling (später ausbaubar)

- Wenn Container „exited“ ist:

  - Node meldet an Brain, dass Instanz „stopped“ oder „failed“ ist (abhängig vom Exit-Code).  
  - Später: Auto-Restart-Strategie definieren (z. B. max. N Restarts in Zeit X).

---

## 7. Webpanel (Nuxt) – Funktionen

### 7.1 Übersicht

- Login / Auth  
- Dashboard:
  - Anzahl Nodes, Instanzen, Templates  
  - Status-Übersicht (wie viele running, failed, preparing)

### 7.2 Node-Ansicht

- Node-Liste mit Status (Online/Offline, Dev-Mode)  
- Detailseite:
  - Name, ID, Specs (CPU, RAM, Node-Typ)  
  - Dev-Mode-Schalter  
  - Button „Cache leeren“  
  - Liste laufender Instanzen  
  - Basis-Metriken (CPU/RAM-Auslastung, Slots)

### 7.3 Instanz-Ansicht

- Instanzliste:

  - ID, Name, Gamemode, Node, Zustand, Uptime  

- Detailseite:

  - Template-Layers (in Reihenfolge)  
  - Aktueller Zustand (inkl. Historie / Events)  
  - Node-Info  
  - Logs (optional in der ersten Version nur Basis)  
  - Aktionen: Start, Stop, Destroy  

### 7.4 Template-Ansicht

- Liste aller Templates:

  - Name, ID, Version, Typ, in wie vielen Instanzen verwendet  
  - Metadaten (Beschreibung, Tags)

- Detailseite:

  - Übersicht der Versionen  
  - Zuordnung zu Instanzen  
  - S3-Key-Anzeige (nur Info)

---

## 8. Sicherheit und Rechte

### 8.1 Auth / AuthZ

- Brain hat ein eigenes Auth-System (z. B. Tokens oder OAuth / OIDC später).  
- Webpanel nutzt dieses System, um API-Anfragen zu stellen.  
- Rollen:

  - Admin: alles  
  - Operator: Instanzen & Nodes steuern, aber keine globalen Einstellungen  
  - Viewer: nur lesend

### 8.2 Kommunikation

- Brain <-> Webpanel: HTTPS  
- Brain <-> Node: ebenfalls TLS (am besten mTLS mit Zertifikaten)  
- Node <-> S3: Zugang über eingeschränkte Credentials (nur Lesen, nur bestimmter Bucket / Pfad)

---

## 9. Betrieb, Logging, Monitoring

### 9.1 Logging

- Brain:

  - Requests (auditfähig)  
  - State-Changes von Instanzen und Nodes  
  - Fehler (S3, Node nicht erreichbar, etc.)

- Node:

  - Kommandos vom Brain  
  - Template-Cache-Aktivitäten  
  - Docker-Events (start/stop)  
  - Fehler bei Merge/Variablen-Ersatz

### 9.2 Monitoring

- Metriken:

  - Anzahl Instanzen pro Node und gesamt  
  - Statusverteilung (running, failed, preparing)  
  - Template-Cache-Hit-Rate  
  - S3-Traffic / Requests  
  - Node-Online-Status  
  - Antwortzeiten Brain-API

### 9.3 Backup / Restore

- Brain-DB regelmäßig sichern  
- S3 übernimmt ohnehin redundante Speicherung  
- Im Desasterfall:

  - Brain aus Backup wiederherstellen  
  - Nodes neu verbinden  
  - Instanz-Status neu aufbauen (ggf. neu ausrollen)

---

## 10. Roadmap (detaillierter)

### Phase 1 – Core Orchestrator

**Ziel:** Alles läuft technisch, auch wenn UI noch rudimentär ist.

- Brain:
  - Node-Registry
  - Instanz-Domänenmodell
  - Template-Metadaten
  - einfache REST-API für Node-Kommunikation
- Node:
  - Registrierung + Heartbeats
  - Template-Cache + S3-Laden
  - Merge-Logik
  - Variablen-Ersetzung
  - Docker-Start/Stop für Instanzen
- Basis-Error-Handling
- Minimaler Status (Text/JSON)

### Phase 2 – Webpanel (Admin-UI)

**Ziel:** Administrierbar ohne direkte API-Calls.

- UI:
  - Login
  - Node-Liste + Detail
  - Instanz-Liste + Detail
  - Template-Liste + Detail
- Aktionen:
  - Instanz erstellen/stoppen/zerstören
  - Node-Dev-Mode umschalten
  - Cache-Purge auslösen
- Bessere Fehleranzeigen im Panel

### Phase 3 – Stabilität & Komfort

**Ziel:** System wird robust und angenehm zu nutzen.

- Verbesserte Fehlerbehandlung / Retry-Mechanismen  
- Auto-Restart bei Crashes (konfigurierbar)  
- Besseres Monitoring:
  - einfache Charts (Instanzen, Status, etc.)  
- Version-Locking:
  - Instanz bleibt auf einer Template-Version, auch wenn neue dazukommen  
- Rechte-System im Panel (Admin / Operator / Viewer)

### Phase 4 – Advanced Features

**Ziel:** Nicer QoL und Skalierung.

- Auto-Scaling-Regeln (z. B. basierend auf Slots/Spielern, später)  
- Multi-Region-Unterstützung (Nodes mit Regions-Tags)  
- Backup-/Snapshot-Funktionen für Instanzen  
- Automatisches Mod-/Plugin-Handling im Template-System  
- Export/Import von Template-Konfigurationen

---

## 11. Kurze Zusammenfassung in einem Flow

Erstellung einer Instanz end-to-end:

```text
User (Panel)
   |
   v
[Brain] "create_instance"
   |
   | wählt Node
   v
[Brain] -> [Node]: "prepare_instance"
   |
   v
[Node]:
  - Templates aus Cache/S3 holen
  - Mergen
  - Variablen ersetzen
  - Instanzverzeichnis fertigstellen
   |
   v
[Node] -> [Brain]: "prepared"
   |
   v
[Brain] -> [Node]: "start_instance"
   |
   v
[Node]:
  - Docker-Container starten
  - Status verfolgen
   |
   v
[Node] -> [Brain]: "running"
   |
   v
Panel zeigt Instanz als "running"
```