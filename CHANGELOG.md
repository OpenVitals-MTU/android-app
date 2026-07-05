# Changelog

## 1.7.7 - 2026-07-05

### English

- Add Body Energy explainability, breaking down which factors drive your daily score over time with a dedicated timeline chart.
- Clean up and harden the Apple Health importer with clearer category handling, geo-distance helpers, and more consistent activity/route/workout conversions.
- Improve the quick-beverage home-screen widget with better drink ordering and configuration handling.
- Refresh docs for the Apple Health import and privacy/support/diagnostics pages.

### Español

- Añade explicabilidad de Energía corporal, desglosando qué factores determinan tu puntuación diaria con un gráfico de línea temporal dedicado.
- Limpia y refuerza el importador de Apple Health con un manejo de categorías más claro, ayudantes de distancia geográfica y conversiones de actividad/ruta/entrenamiento más consistentes.
- Mejora el widget de bebida rápida en la pantalla de inicio con mejor ordenación y configuración de bebidas.
- Actualiza la documentación de importación de Apple Health y de privacidad/soporte/diagnóstico.

### Deutsch

- Fügt Body-Energy-Erklärungen hinzu, die mit einem eigenen Zeitachsendiagramm aufschlüsseln, welche Faktoren deinen täglichen Wert über die Zeit beeinflussen.
- Bereinigt und härtet den Apple-Health-Importer mit klarerer Kategorienverarbeitung, Geo-Distanz-Hilfsfunktionen und konsistenteren Aktivitäts-/Routen-/Trainingskonvertierungen.
- Verbessert das Schnellgetränk-Widget auf dem Startbildschirm mit besserer Getränkereihenfolge und Konfigurationsverarbeitung.
- Aktualisiert die Dokumentation zum Apple-Health-Import und zu Datenschutz/Support/Diagnose.

### Italiano

- Aggiunge la spiegabilità di Energia corporea, che scompone quali fattori determinano il punteggio giornaliero nel tempo con un grafico temporale dedicato.
- Ripulisce e rafforza l'importer di Apple Health con gestione delle categorie più chiara, funzioni di distanza geografica e conversioni di attività/percorsi/allenamenti più coerenti.
- Migliora il widget bevanda rapida nella schermata home con un ordinamento e una configurazione delle bevande migliori.
- Aggiorna la documentazione per l'importazione Apple Health e per privacy/supporto/diagnostica.

## 1.7.6 - 2026-07-04

### English

- Add a configurable quick beverage home-screen widget so saved containers can log hydration, caffeine, and nutrition faster.
- Improve beverage entry with tap-to-save containers, better daily-goal context, custom amount and category handling, and persisted nutrition defaults.
- Upgrade activity import with richer FIT activity/course/workout parsing, route-less FIT support, imported calories, duration and title inference, and clearer import errors.
- Split Heart and Vitals into clearer metric destinations, add a blood-pressure vitals view, and streamline Settings and navigation flows.
- Refresh docs, README screenshots, and Play Store screenshots, including the reorganized app guide, feature guide, how-to pages, and offline maps notes.

### Español

- Añade un widget configurable de bebida rápida en la pantalla de inicio para registrar hidratación, cafeína y nutrición más rápido desde recipientes guardados.
- Mejora la entrada de bebidas con recipientes de guardado rápido, más contexto del objetivo diario, manejo de cantidades y categorías personalizadas, y valores nutricionales persistentes.
- Refuerza la importación de actividades con análisis FIT más completo para actividades, recorridos y entrenamientos, soporte para FIT sin ruta, calorías importadas, inferencia de duración y título, y errores más claros.
- Separa Corazón y Vitales en destinos de métricas más claros, añade una vista de presión arterial y simplifica Ajustes y la navegación.
- Actualiza la documentación, las capturas del README y las capturas de Play Store, incluida la guía de la app, la guía de funciones, los how-to y las notas de mapas sin conexión.

### Deutsch

- Fügt ein konfigurierbares Schnellgetränk-Widget für den Startbildschirm hinzu, damit gespeicherte Behälter Flüssigkeit, Koffein und Nährwerte schneller protokollieren.
- Verbessert die Getränkeerfassung mit Tipp-zum-Speichern-Behältern, besserem Tagesziel-Kontext, eigener Mengen- und Kategorienverwaltung sowie gespeicherten Nährwertvorgaben.
- Erweitert den Aktivitätsimport mit vollständigerem FIT-Parsing für Aktivitäten, Strecken und Trainings, FIT-Unterstützung ohne Route, importierten Kalorien, Dauer- und Titelableitung sowie klareren Importfehlern.
- Trennt Herz und Vitalwerte in klarere Metrikziele, ergänzt eine Blutdruckansicht und vereinfacht Einstellungen und Navigation.
- Aktualisiert Dokumentation, README-Screenshots und Play-Store-Screenshots, einschließlich neu geordneter App-Anleitung, Feature-Guide, How-to-Seiten und Offline-Kartenhinweise.

### Italiano

- Aggiunge un widget configurabile per bevande rapide nella schermata home, così i contenitori salvati possono registrare idratazione, caffeina e nutrienti più velocemente.
- Migliora l'inserimento delle bevande con contenitori salva-subito, contesto dell'obiettivo giornaliero più chiaro, gestione di quantità e categorie personalizzate e valori nutrizionali persistenti.
- Potenzia l'importazione attività con parsing FIT più completo per attività, percorsi e allenamenti, supporto ai FIT senza traccia, calorie importate, inferenza di durata e titolo ed errori più chiari.
- Separa Cuore e Parametri vitali in destinazioni metriche più chiare, aggiunge una vista per la pressione arteriosa e semplifica Impostazioni e navigazione.
- Aggiorna documentazione, screenshot del README e screenshot per Play Store, incluse guida app, guida funzioni, pagine how-to e note sulle mappe offline riorganizzate.

## 1.7.5 - 2026-07-03

### English

- Add a dedicated Caffeine detail flow with active-caffeine modeling, source and time-of-day insights, bedtime guidance, dashboard support, and configurable sensitivity and limits.
- Expand beverage logging with a Room-backed drink catalog, 215 preset drinks, editable categories, custom drinks, and nutrition defaults while keeping Health Connect records as the source of truth.
- Save richer beverage entries by pairing effective hydration with caffeine and nutrition values, and preserve custom drink ordering and categories across app launches.
- Backfill activity detail data from related Health Connect records so historical workouts can show more complete sessions and metrics.
- Improve support and release stability with crash-report email drafts, database migration coverage, Zulip links, and cleaner Gradle/Woodpecker release steps.

### Español

- Añade un flujo de detalle de cafeína con modelo de cafeína activa, análisis por fuente y hora del día, guía para la hora de dormir, soporte en el panel y sensibilidad y límites configurables.
- Amplía el registro de bebidas con un catálogo respaldado por Room, 215 bebidas predefinidas, categorías editables, bebidas personalizadas y valores nutricionales por defecto, manteniendo Health Connect como fuente de verdad.
- Guarda entradas de bebidas más completas combinando hidratación efectiva con valores de cafeína y nutrición, y conserva el orden y las categorías de bebidas personalizadas entre inicios de la app.
- Rellena datos de detalle de actividades desde registros relacionados de Health Connect para que los entrenamientos históricos puedan mostrar sesiones y métricas más completas.
- Mejora el soporte y la estabilidad de publicación con borradores de informes de fallo por email, cobertura de migración de base de datos, enlaces de Zulip y pasos de Gradle/Woodpecker más limpios.

### Deutsch

- Fügt einen eigenen Koffein-Detailfluss mit Aktiv-Koffein-Modell, Auswertungen nach Quelle und Tageszeit, Schlafenszeit-Hinweisen, Dashboard-Unterstützung sowie konfigurierbarer Empfindlichkeit und Grenzwerten hinzu.
- Erweitert die Getränkeerfassung um einen Room-gestützten Getränkekatalog, 215 Presets, bearbeitbare Kategorien, eigene Getränke und Standard-Nährwerte, während Health Connect die Quelle der Wahrheit bleibt.
- Speichert reichere Getränkeeinträge, indem effektive Flüssigkeit mit Koffein- und Nährwerten kombiniert wird, und erhält Reihenfolge und Kategorien eigener Getränke über App-Starts hinweg.
- Füllt Aktivitätsdetails aus verwandten Health-Connect-Datensätzen auf, damit historische Trainings vollständigere Sitzungen und Messwerte zeigen können.
- Verbessert Support und Release-Stabilität mit E-Mail-Entwürfen für Absturzberichte, Datenbank-Migrationsabdeckung, Zulip-Links und saubereren Gradle/Woodpecker-Schritten.

### Italiano

- Aggiunge un flusso di dettaglio Caffeina con modello di caffeina attiva, analisi per fonte e fascia oraria, indicazioni per l'ora di andare a dormire, supporto nella dashboard e sensibilità e limiti configurabili.
- Estende la registrazione delle bevande con un catalogo basato su Room, 215 bevande predefinite, categorie modificabili, bevande personalizzate e valori nutrizionali predefiniti, mantenendo Health Connect come fonte di verità.
- Salva voci bevanda più complete unendo idratazione effettiva, caffeina e valori nutrizionali, e conserva ordine e categorie delle bevande personalizzate tra gli avvii dell'app.
- Recupera dettagli attività da record Health Connect collegati, così gli allenamenti storici possono mostrare sessioni e metriche più complete.
- Migliora supporto e stabilità di rilascio con bozze email per i report di crash, copertura delle migrazioni database, link Zulip e passaggi Gradle/Woodpecker più puliti.

## 1.7.4 - 2026-07-02

### English

- Add a dedicated Body Energy detail flow with calibration controls, timeline loading, dashboard support, and widget data.
- Show saved Bluetooth LE sensor connection status and battery levels in dashboard and recording surfaces.
- Add a rest-timer bell for repetition activity recordings.
- Use fuller raw samples in day metric views so same-day charts and detail data stay more accurate.
- Keep the local app internet-free by removing inherited network access, while improving diagnostics, Apple Health import logging, and release automation.

### Español

- Añade un flujo de detalle de Body Energy con controles de calibración, carga de línea temporal, soporte en el panel y datos para widgets.
- Muestra el estado de conexión y batería de sensores Bluetooth LE guardados en el panel y en las superficies de grabación.
- Añade una campana de temporizador de descanso para grabaciones de actividad por repeticiones.
- Usa muestras sin procesar más completas en las vistas diarias de métricas para que los gráficos y detalles del mismo día sean más precisos.
- Mantiene la app local sin internet al eliminar acceso de red heredado, con mejores diagnósticos, registros de importación de Apple Health y automatización de publicación.

### Deutsch

- Fügt einen eigenen Body-Energy-Detailfluss mit Kalibrierung, Zeitachsenladung, Dashboard-Unterstützung und Widget-Daten hinzu.
- Zeigt Verbindungsstatus und Akkustand gespeicherter Bluetooth-LE-Sensoren im Dashboard und in Aufnahmeansichten.
- Ergänzt einen Signalton für den Pausentimer bei Wiederholungs-Aktivitätsaufnahmen.
- Nutzt vollständigere Rohdaten in Tagesansichten, damit Diagramme und Detaildaten für denselben Tag genauer bleiben.
- Hält die lokale App internetfrei, indem geerbter Netzwerkzugriff entfernt wird, und verbessert Diagnosen, Apple-Health-Importprotokolle und Release-Automatisierung.

### Italiano

- Aggiunge un flusso di dettaglio Body Energy con controlli di calibrazione, caricamento della timeline, supporto nella dashboard e dati per i widget.
- Mostra stato di connessione e batteria dei sensori Bluetooth LE salvati nella dashboard e nelle schermate di registrazione.
- Aggiunge un campanello per il timer di riposo nelle registrazioni di attività a ripetizioni.
- Usa campioni grezzi più completi nelle viste giornaliere delle metriche, così grafici e dettagli dello stesso giorno restano più accurati.
- Mantiene l'app locale senza accesso a internet rimuovendo l'accesso di rete ereditato, con diagnostica, log di importazione Apple Health e automazione di rilascio migliori.

## 1.7.3 - 2026-06-30

### English

- Remove the local dashboard summary cache and related warmup controls so metric refreshes read directly from Health Connect with less stale state.
- Simplify dashboard refresh loading and repository queries after cache removal.
- Improve sleep handling by merging overlapping sessions and surfacing sleep summary data more consistently.
- Fix weekly activity progress markers so past days without activity are not drawn as completed.
- Fix data source attribution text fitting for long provider and app names, with regression coverage.

### Español

- Elimina la cache local de resúmenes del panel y sus controles de precarga para que las actualizaciones lean directamente desde Health Connect con menos estado obsoleto.
- Simplifica la carga de actualización del panel y las consultas de repositorios tras retirar la cache.
- Mejora el manejo del sueño fusionando sesiones solapadas y mostrando los resúmenes de sueño de forma más consistente.
- Corrige los marcadores de progreso semanal de actividad para que los días pasados sin actividad no aparezcan completados.
- Corrige el ajuste del texto de atribución de fuente de datos para nombres largos de proveedores y apps, con cobertura de regresión.

### Deutsch

- Entfernt den lokalen Cache für Dashboard-Zusammenfassungen und die zugehörigen Vorwärm-Steuerelemente, sodass Aktualisierungen direkter aus Health Connect lesen und weniger veralteten Zustand behalten.
- Vereinfacht Dashboard-Aktualisierungen und Repository-Abfragen nach dem Entfernen des Caches.
- Verbessert die Schlafverarbeitung, indem überlappende Sitzungen zusammengeführt und Schlafzusammenfassungen konsistenter angezeigt werden.
- Korrigiert Wochenaktivitäts-Markierungen, damit vergangene Tage ohne Aktivität nicht als abgeschlossen dargestellt werden.
- Korrigiert die Textanpassung der Datenquellen-Zuordnung für lange Anbieter- und App-Namen, inklusive Regressionstest.

### Italiano

- Rimuove la cache locale dei riepiloghi della dashboard e i relativi controlli di preriscaldamento, così gli aggiornamenti leggono direttamente da Health Connect con meno stato obsoleto.
- Semplifica il caricamento degli aggiornamenti della dashboard e le query dei repository dopo la rimozione della cache.
- Migliora la gestione del sonno unendo le sessioni sovrapposte e mostrando i riepiloghi del sonno in modo più coerente.
- Corregge i marcatori di avanzamento settimanale delle attività, così i giorni passati senza attività non risultano completati.
- Corregge l'adattamento del testo di attribuzione della fonte dati per nomi lunghi di provider e app, con copertura di regressione.

## 1.7.2 - 2026-06-30

### English

- Add a sleep-stage time graph so overnight sessions are easier to scan by time of night.
- Improve drag-and-drop mechanics for reorderable dashboard widgets and metric detail sections.
- Fix dashboard carousel behavior after widget and layout changes.
- Fix weekly activity visuals when a day has no activity.
- Expand connected-flow and visual-regression coverage for dashboard, hydration, manual entry, settings, and shared Material components.

### Español

- Añade un gráfico temporal de fases del sueño para revisar mejor las sesiones por hora de la noche.
- Mejora la mecánica de arrastrar y soltar para widgets del panel y secciones de detalle de métricas reordenables.
- Corrige el comportamiento del carrusel del panel tras cambios en widgets y diseño.
- Corrige las visualizaciones semanales de actividad cuando un día no contiene actividad.
- Amplía la cobertura de pruebas connected-flow y visual-regression para panel, hidratación, entrada manual, ajustes y componentes Material compartidos.

### Deutsch

- Ergänzt ein Zeitdiagramm für Schlafphasen, damit Nachtsitzungen nach Uhrzeit leichter zu erfassen sind.
- Verbessert Drag-and-drop für sortierbare Dashboard-Widgets und Metrikdetail-Abschnitte.
- Korrigiert das Verhalten des Dashboard-Karussells nach Widget- und Layoutänderungen.
- Korrigiert Wochenaktivitätsansichten, wenn ein Tag keine Aktivität enthält.
- Erweitert Connected-Flow- und Visual-Regression-Abdeckung für Dashboard, Hydration, manuelle Eingabe, Einstellungen und gemeinsame Material-Komponenten.

### Italiano

- Aggiunge un grafico temporale delle fasi del sonno per leggere più facilmente le sessioni durante la notte.
- Migliora la meccanica di trascinamento per widget della dashboard e sezioni metriche riordinabili.
- Corregge il comportamento del carosello della dashboard dopo le modifiche a widget e layout.
- Corregge le visualizzazioni settimanali dell'attività quando un giorno non contiene attività.
- Estende la copertura dei test connected-flow e visual-regression per dashboard, idratazione, inserimento manuale, impostazioni e componenti Material condivisi.

## 1.7.1 - 2026-06-30

### English

- Extend reorderable metric detail sections across activities, calories, hydration, nutrition, heart/vitals, sleep, and body screens.
- Improve Apple Health import diagnostics with clearer error/report copy actions and more detailed import logging.
- Fix heart data loading for days with more than 1,000 samples by paging reads before chart aggregation.
- Refine dashboard and metric internals with feature-owned repositories, presentation mappers, and smaller screen components for steadier refreshes.
- Stabilize release/debug build signing and add coverage for weekly sleep and hydration layouts.

### Español

- Extiende secciones reordenables de detalle de métricas a pantallas de actividades, calorías, hidratación, nutrición, corazón/constantes, sueño y cuerpo.
- Mejora diagnósticos de importación de Apple Health con acciones más claras para copiar errores/informes y registros de importación más detallados.
- Corrige la carga de datos cardíacos en días con más de 1.000 muestras paginando lecturas antes de agregar gráficos.
- Refina internos del panel y las métricas con repositorios por función, mapeadores de presentación y componentes de pantalla más pequeños para actualizaciones más estables.
- Estabiliza la firma de builds release/debug y añade cobertura para diseños semanales de sueño e hidratación.

### Deutsch

- Erweitert sortierbare Metrikdetail-Abschnitte auf Aktivitäten, Kalorien, Hydration, Ernährung, Herz/Vitalwerte, Schlaf und Körper.
- Verbessert Apple-Health-Importdiagnosen mit klareren Aktionen zum Kopieren von Fehlern/Berichten und detaillierter Importprotokollierung.
- Korrigiert das Laden von Herzdaten an Tagen mit mehr als 1.000 Samples, indem Lesevorgänge vor der Diagrammaggregation paginiert werden.
- Verfeinert Dashboard- und Metrik-Interna mit featureeigenen Repositories, Presentation-Mappern und kleineren Screen-Komponenten für stabilere Aktualisierungen.
- Stabilisiert Release-/Debug-Build-Signierung und ergänzt Tests für Wochenlayouts von Schlaf und Hydration.

### Italiano

- Estende le sezioni riordinabili dei dettagli metriche alle schermate attività, calorie, idratazione, nutrizione, cuore/vitali, sonno e corpo.
- Migliora la diagnostica delle importazioni Apple Health con azioni più chiare per copiare errori/report e log di importazione più dettagliati.
- Corregge il caricamento dei dati cardiaci nei giorni con più di 1.000 campioni paginando le letture prima dell'aggregazione dei grafici.
- Rifinisce gli interni di dashboard e metriche con repository per funzionalità, mapper di presentazione e componenti più piccoli per aggiornamenti più stabili.
- Stabilizza la firma delle build release/debug e aggiunge copertura per i layout settimanali di sonno e idratazione.

## 1.7.0 - 2026-06-29

### English

- Add controls for reordering metric detail sections so charts, statistics, entries, and guidance can match your workflow.
- Add a high-contrast outdoor recording theme and improve widget-edit scrolling while recording.
- Add post-activity speed and cadence charts, and trim duplicated heart-rate sensor samples during recording.
- Make Apple Health imports safer for large exports with targeted lookups and time-window chunking.
- Fix Health Connect permission handling and reduce oversized heart/chart reads that could trigger CursorWindow errors.

### Español

- Añade controles para reordenar secciones de detalle de métricas, para adaptar gráficos, estadísticas, entradas y guías a tu flujo.
- Añade un tema de alto contraste para grabar al aire libre y mejora el desplazamiento al editar widgets durante una grabación.
- Añade gráficos de velocidad y cadencia después de la actividad, y recorta muestras duplicadas de frecuencia cardiaca de sensores.
- Hace más seguras las importaciones grandes de Apple Health con búsquedas específicas y procesamiento por ventanas de tiempo.
- Corrige permisos de Health Connect y reduce lecturas grandes de corazón/gráficos que podían causar errores CursorWindow.

### Deutsch

- Ergänzt Steuerelemente zum Sortieren von Metrikdetail-Abschnitten, damit Diagramme, Statistiken, Einträge und Hinweise besser zum eigenen Ablauf passen.
- Ergänzt einen kontrastreichen Outdoor-Aufzeichnungsmodus und verbessert das Scrollen beim Bearbeiten von Widgets während einer Aufzeichnung.
- Ergänzt Geschwindigkeits- und Kadenzdiagramme nach Aktivitäten und entfernt doppelte Herzfrequenz-Sensorsamples während der Aufzeichnung.
- Macht große Apple-Health-Importe sicherer durch gezielte Lookups und Verarbeitung in Zeitfenstern.
- Korrigiert Health-Connect-Berechtigungen und reduziert sehr große Herz-/Diagrammabfragen, die CursorWindow-Fehler auslösen konnten.

### Italiano

- Aggiunge controlli per riordinare le sezioni dei dettagli metriche, così grafici, statistiche, voci e guide seguono meglio il tuo flusso.
- Aggiunge un tema ad alto contrasto per le registrazioni outdoor e migliora lo scorrimento durante la modifica dei widget in registrazione.
- Aggiunge grafici di velocità e cadenza dopo l'attività e rimuove campioni duplicati della frequenza cardiaca dai sensori.
- Rende più sicure le importazioni Apple Health grandi con ricerche mirate ed elaborazione a finestre temporali.
- Corregge permessi Health Connect e riduce letture cuore/grafici troppo grandi che potevano causare errori CursorWindow.

## 1.6.3 - 2026-06-28

### English

- Add a manual carbohydrate entry flow that writes total-carbohydrate NutritionRecords to Health Connect.
- Add offline activity maps by importing PMTiles or Mapsforge map packs from Settings.
- Show imported offline maps while recording activities and previewing saved or imported routes, with map recentering and background import progress.

### Español

- Añade un flujo de entrada manual de carbohidratos que escribe NutritionRecords de carbohidratos totales en Health Connect.
- Añade mapas de actividad sin conexión mediante paquetes PMTiles o Mapsforge importados desde Ajustes.
- Muestra mapas sin conexión importados al grabar actividades y previsualizar rutas guardadas o importadas, con recentrado del mapa y progreso de importación en segundo plano.

### Deutsch

- Ergänzt einen manuellen Kohlenhydrateintrag, der Gesamt-Kohlenhydrate als NutritionRecords in Health Connect schreibt.
- Ergänzt Offline-Aktivitätskarten über aus den Einstellungen importierte PMTiles- oder Mapsforge-Kartenpakete.
- Zeigt importierte Offline-Karten beim Aufzeichnen von Aktivitäten und in gespeicherten oder importierten Routenvorschauen, mit Neuzentrierung und Importfortschritt im Hintergrund.

### Italiano

- Aggiunge un flusso di inserimento manuale dei carboidrati che scrive NutritionRecord con carboidrati totali in Health Connect.
- Aggiunge mappe attività offline tramite pacchetti PMTiles o Mapsforge importati dalle Impostazioni.
- Mostra le mappe offline importate durante la registrazione attività e nelle anteprime di percorsi salvati o importati, con ricentratura e avanzamento import in background.

## 1.6.2 - 2026-06-27

### English

- Add a configurable activity recording dashboard with Focus mode for a cleaner in-recording view.
- Add strength training recording with heart-rate monitoring and richer repetition training heart-rate stats.
- Keep the screen awake during activity recording when enabled and make recording setup and review flows cleaner.
- Improve Bluetooth LE sensor timeout handling so stale sensor values drop out more reliably.
- Fix daily HRV loading and defer heavier dashboard widget reads to improve dashboard responsiveness.
- Fix release automation so the signed Android App Bundle is found reliably during publishing.

### Español

- Añade un panel configurable de grabación de actividad con modo Enfoque para una vista de grabación más limpia.
- Añade grabación de entrenamiento de fuerza con monitorización de frecuencia cardiaca y estadísticas de frecuencia cardiaca más completas para entrenamientos de repeticiones.
- Mantiene la pantalla activa durante la grabación de actividades cuando está habilitado y mejora los flujos de configuración y revisión.
- Mejora el manejo de tiempos de espera de sensores Bluetooth LE para descartar con más fiabilidad valores obsoletos.
- Corrige la carga diaria de HRV y difiere lecturas más pesadas de widgets del panel para mejorar la capacidad de respuesta.
- Corrige la automatización de release para encontrar de forma fiable el Android App Bundle firmado durante la publicación.

### Deutsch

- Ergänzt ein konfigurierbares Dashboard für Aktivitätsaufzeichnungen mit Fokusmodus für eine übersichtlichere Aufzeichnungsansicht.
- Ergänzt Krafttraining-Aufzeichnungen mit Herzfrequenzmessung und ausführlicheren Herzfrequenzwerten für Wiederholungstraining.
- Hält den Bildschirm während Aktivitätsaufzeichnungen wach, wenn die Option aktiviert ist, und verbessert Einrichtungs- und Prüfabläufe.
- Verbessert das Timeout-Handling von Bluetooth-LE-Sensoren, sodass veraltete Sensorwerte zuverlässiger verschwinden.
- Korrigiert das tägliche HRV-Laden und verschiebt schwerere Dashboard-Widget-Abfragen, um die Reaktionsfähigkeit zu verbessern.
- Korrigiert die Release-Automation, damit das signierte Android App Bundle bei der Veröffentlichung zuverlässig gefunden wird.

### Italiano

- Aggiunge una dashboard configurabile per la registrazione attività con modalità Focus per una vista di registrazione più pulita.
- Aggiunge la registrazione dell'allenamento di forza con monitoraggio della frequenza cardiaca e statistiche più ricche per gli allenamenti a ripetizioni.
- Mantiene lo schermo attivo durante la registrazione delle attività quando l'opzione è abilitata e migliora i flussi di configurazione e revisione.
- Migliora la gestione dei timeout dei sensori Bluetooth LE, così i valori obsoleti vengono rimossi con maggiore affidabilità.
- Corregge il caricamento giornaliero dell'HRV e rimanda le letture più pesanti dei widget della dashboard per migliorare la reattività.
- Corregge l'automazione di release in modo che l'Android App Bundle firmato venga trovato in modo affidabile durante la pubblicazione.

## 1.6.1 - 2026-06-27

### English

- Fix activity tracking notifications so tapping the notification reopens the active recording screen.
- Improve dashboard and background metric loading performance, including coalesced refreshes and more efficient summary reads.
- Add Italian translations and make Italian available in the in-app language selector.

### Español

- Corrige las notificaciones de grabación de actividad para que al tocarlas se abra de nuevo la pantalla de grabación activa.
- Mejora el rendimiento de carga de métricas del panel y en segundo plano, incluidas actualizaciones agrupadas y lecturas de resúmenes más eficientes.
- Añade traducciones al italiano y permite elegir italiano en el selector de idioma de la app.

### Deutsch

- Korrigiert Aktivitätsaufzeichnungs-Benachrichtigungen, sodass Antippen wieder den aktiven Aufzeichnungsbildschirm öffnet.
- Verbessert die Ladeleistung für Dashboard- und Hintergrundmetriken, einschließlich zusammengefasster Aktualisierungen und effizienterer Zusammenfassungsabfragen.
- Fügt italienische Übersetzungen hinzu und macht Italienisch in der Sprachauswahl der App verfügbar.

### Italiano

- Corregge le notifiche del tracciamento attività in modo che un tocco riapra la schermata della registrazione attiva.
- Migliora le prestazioni di caricamento delle metriche nella dashboard e in background, incluse le sincronizzazioni raggruppate e letture dei riepiloghi più efficienti.
- Aggiunge le traduzioni italiane e rende l'italiano disponibile nel selettore lingua dell'app.

## 1.6.0 - 2026-06-27

### English

- Add a dedicated debug version that can be installed alongside production builds for safer troubleshooting.
- Automatically hide hydration reminder notifications after a hydration entry is saved.
- Add Fat-Free Mass Index (FFMI) to body composition insights when weight, height, and body fat data are available.
- Add experimental Bluetooth LE sensor integration for activity recording.
- Implement a refreshed UI/UX across the app with clearer navigation, metric screens, and entry flows.

### Español

- Añade una versión debug dedicada que puede instalarse junto a las versiones de producción para diagnósticos más seguros.
- Oculta automáticamente las notificaciones de recordatorio de hidratación después de guardar una entrada de hidratación.
- Añade el Índice de Masa Libre de Grasa (FFMI) a los análisis de composición corporal cuando hay peso, altura y grasa corporal disponibles.
- Añade integración experimental con sensores Bluetooth LE para la grabación de actividades.
- Implementa una UI/UX renovada en toda la app con navegación, pantallas de métricas y flujos de entrada más claros.

### Deutsch

- Fügt eine eigene Debug-Version hinzu, die für sicherere Fehlerdiagnosen neben Produktions-Builds installiert werden kann.
- Blendet Hydrationserinnerungen automatisch aus, nachdem ein Hydrationseintrag gespeichert wurde.
- Ergänzt den Fat-Free Mass Index (FFMI) in den Körperzusammensetzungs-Insights, wenn Gewicht, Größe und Körperfett verfügbar sind.
- Fügt eine experimentelle Bluetooth-LE-Sensorintegration für Aktivitätsaufzeichnungen hinzu.
- Implementiert eine überarbeitete UI/UX in der gesamten App mit klarerer Navigation, Metrikseiten und Eingabeflüssen.

## 1.5.1 - 2026-06-24

### English

- Add persistent derived metric storage for dashboard and home widget summaries so calculated metrics can be reused across refreshes.
- Fix Daily Readiness and metric home widgets so cached and freshly calculated values load more reliably.
- Let OpenVitals-owned activities be deleted directly from the activity summary flow with swipe-to-delete handling.
- Improve activity entry and recording flows with safer training-plan updates, corrected planned start times, clearer repetition stats, and a fix for repetitive activity recording crashes.
- Persist the last custom hydration amount more reliably and keep release automation aligned with the restored direct Google Play production upload path.

### Español

- Añade almacenamiento persistente de métricas derivadas para el panel y widgets de inicio, de modo que los cálculos puedan reutilizarse entre actualizaciones.
- Corrige los widgets de Preparación diaria y métricas para cargar con más fiabilidad valores en caché y recién calculados.
- Permite borrar actividades propias de OpenVitals directamente desde el flujo de resumen de actividad con deslizar para borrar.
- Mejora las entradas y grabaciones de actividad con actualizaciones de planes de entrenamiento más seguras, horas de inicio planificadas corregidas, estadísticas de repeticiones más claras y una corrección de cierres en actividades repetitivas.
- Persiste con más fiabilidad la última cantidad personalizada de hidratación y alinea la automatización de release con la ruta restaurada de subida directa a producción de Google Play.

### Deutsch

- Ergänzt persistente Speicherung abgeleiteter Metriken für Dashboard- und Startbildschirm-Widgets, damit berechnete Werte über Aktualisierungen hinweg wiederverwendet werden können.
- Korrigiert Daily-Readiness- und Metrik-Widgets, sodass gecachte und frisch berechnete Werte zuverlässiger geladen werden.
- Ermöglicht das Löschen OpenVitals-eigener Aktivitäten direkt im Aktivitätszusammenfassungsfluss per Wischgeste.
- Verbessert Aktivitätseinträge und -aufzeichnungen mit sichereren Trainingsplan-Updates, korrigierten geplanten Startzeiten, klareren Wiederholungsstatistiken und einem Fix für Abstürze bei repetitiven Aktivitäten.
- Speichert die letzte benutzerdefinierte Hydrationsmenge zuverlässiger und richtet die Release-Automation auf den wiederhergestellten direkten Google-Play-Produktionsupload aus.

## 1.5.0 - 2026-06-24

### English

- Add configurable Android home screen widgets for Daily Readiness, Body Energy, Today Vitals, and selected metric summaries.
- Improve GPS activity recording with split analysis, voice announcements, marker preferences, and cleaner non-GPS activity validation.
- Add set-based training timers and Health Connect training-plan support for activity entries and recordings.
- Let OpenVitals-owned hydration, body, vitals, and mindfulness entries edit their date and time as well as values.
- Add a cached metric summary layer and background warmup to make dashboard and period detail loads faster.
- Make large Apple Health imports safer with streaming conversions, narrower import repository boundaries, and clearer worker dependency handling.
- Restore Google Play internal testing uploads on tag builds and production promotion from the approved Woodpecker deployment.
- Refresh Health Connect permission guidance, remember the last custom hydration amount, update runtime/test dependencies with Gradle locks, add `Gemfile.lock`, and split large feature files.

### Español

- Añade widgets configurables de pantalla de inicio de Android para Preparación diaria, Energía corporal, Constantes de hoy y resúmenes de métricas seleccionadas.
- Mejora la grabación GPS de actividades con análisis de parciales, avisos de voz, preferencias de marcadores y validación más clara para actividades sin GPS.
- Añade temporizadores de entrenamiento por series y soporte de planes de entrenamiento de Health Connect para entradas y grabaciones de actividad.
- Permite editar la fecha y hora de entradas propias de OpenVitals de hidratación, cuerpo, constantes y mindfulness, además de sus valores.
- Añade una caché de resúmenes de métricas y calentamiento en segundo plano para acelerar el panel y detalles por periodo.
- Hace más seguras las importaciones grandes de Apple Health con conversiones en streaming, límites de repositorio más claros y mejor manejo de dependencias del worker.
- Restaura las subidas a pruebas internas de Google Play en builds etiquetadas y la promoción a producción desde el despliegue aprobado de Woodpecker.
- Renueva las indicaciones de permisos de Health Connect, recuerda el último valor personalizado de hidratación, actualiza dependencias con bloqueos de Gradle, añade `Gemfile.lock` y divide archivos grandes.

### Deutsch

- Ergänzt konfigurierbare Android-Startbildschirm-Widgets für Daily Readiness, Body Energy, heutige Vitalwerte und ausgewählte Metrikzusammenfassungen.
- Verbessert GPS-Aktivitätsaufzeichnungen mit Split-Analyse, Sprachansagen, Marker-Einstellungen und klarerer Validierung für Aktivitäten ohne GPS.
- Fügt satzbasierte Trainingstimer und Unterstützung für Health-Connect-Trainingspläne in Aktivitätseinträgen und Aufzeichnungen hinzu.
- Erlaubt bei OpenVitals-eigenen Einträgen für Hydration, Körper, Vitalwerte und Mindfulness das Bearbeiten von Datum und Uhrzeit sowie der Werte.
- Fügt einen Cache für Metrikzusammenfassungen und Hintergrund-Warmup hinzu, damit Dashboard und Periodendetails schneller laden.
- Macht große Apple-Health-Importe robuster durch Streaming-Konvertierungen, klarere Import-Repository-Grenzen und besseres Worker-Abhängigkeits-Handling.
- Aktiviert Google-Play-Uploads in den internen Testtrack für Tag-Builds und Produktions-Promotion aus dem freigegebenen Woodpecker-Deployment wieder.
- Aktualisiert Health-Connect-Berechtigungshinweise, merkt sich den letzten benutzerdefinierten Hydrationswert, aktualisiert Abhängigkeiten mit Gradle-Locks, ergänzt `Gemfile.lock` und teilt große Feature-Dateien auf.

## 1.4.1 - 2026-06-13

### English

- Fix metric hydration totals so small entries such as 150 ml display as `0.15 L` instead of rounding to `0.2 L`.
- Keep hydration preset taps writing the exact tapped container volume, with regression coverage for the 150 ml tea cup preset.
- Remove the selected highlight from hydration container presets in normal add mode because tapping a preset now saves immediately.
- Remove the redundant Today label above the hydration goal progress wave.

### Español

- Corrige los totales métricos de hidratación para que entradas pequeñas como 150 ml se muestren como `0,15 L` en vez de redondearse a `0,2 L`.
- Mantiene los toques en tamaños predefinidos de hidratación escribiendo el volumen exacto tocado, con cobertura de regresión para la taza de té de 150 ml.
- Quita el resaltado de selección de los tamaños predefinidos en el modo normal de añadir, porque tocar un tamaño ahora guarda al instante.
- Quita la etiqueta redundante Hoy sobre la onda de progreso del objetivo de hidratación.

### Deutsch

- Korrigiert metrische Hydration-Summen, sodass kleine Einträge wie 150 ml als `0,15 L` statt gerundet als `0,2 L` angezeigt werden.
- Stellt sicher, dass Tippen auf Hydration-Voreinstellungen exakt das angetippte Gefäßvolumen schreibt, mit Regressionstest für die 150-ml-Teetasse.
- Entfernt die Auswahlhervorhebung der Hydration-Gefäße im normalen Hinzufügen-Modus, weil Tippen jetzt sofort speichert.
- Entfernt die redundante Heute-Beschriftung über der Wellenanzeige des Hydrationsziels.

## 1.4.0 - 2026-06-13

### English

- Add Daily Readiness with local Body Energy, Training Readiness, HRV status, intensity minutes, physiological stress, adaptive goals, and explanation screens.
- Improve hydration logging so tapping a container size can save immediately, with the container controls shown before beverage type and better today progress feedback.
- Refresh hydration details with a wavy day trend, clearer week charts, and corrected totals based on the rounded values shown in the app.
- Let day-based detail screens move between days by swiping the date header, and refresh the dashboard automatically after saving manual entries.
- Move cycle tracking into the explicit Health Connect permission categories in onboarding and Settings, with clearer missing-permission handling.
- Fix the monochrome launcher icon and keep release CI publishing Codeberg artifacts while automated Google Play uploads/promotions remain disabled.

### Español

- Añade Preparación diaria con Energía corporal, Preparación para entrenar, estado de HRV, minutos de intensidad, estrés fisiológico, metas adaptativas y pantallas de explicación calculadas localmente.
- Mejora el registro de hidratación para que tocar un tamaño de recipiente pueda guardar al instante, con los controles de recipiente antes del tipo de bebida y mejor contexto del progreso de hoy.
- Renueva los detalles de hidratación con tendencia diaria ondulada, gráficos semanales más claros y totales corregidos basados en los valores redondeados mostrados en la app.
- Permite cambiar de día deslizando el encabezado de fecha en pantallas de detalle diario y actualiza el panel automáticamente después de guardar entradas manuales.
- Mueve el seguimiento del ciclo a las categorías explícitas de permisos de Health Connect en onboarding y Ajustes, con un manejo más claro de permisos faltantes.
- Corrige el icono monocromático del lanzador y mantiene CI publicando artefactos de Codeberg mientras las subidas/promociones automáticas de Google Play siguen deshabilitadas.

### Deutsch

- Ergänzt Daily Readiness mit lokal berechneter Body Energy, Training Readiness, HRV-Status, Intensitätsminuten, physiologischem Stress, adaptiven Zielen und Erklärseiten.
- Verbessert die Flüssigkeitserfassung, sodass Tippen auf eine Gefäßgröße direkt speichern kann, mit Gefäßauswahl vor Getränketyp und klarerem Fortschritt für heute.
- Überarbeitet die Hydration-Details mit wellenförmigem Tagestrend, klareren Wochendiagrammen und korrigierten Summen auf Basis der gerundeten App-Werte.
- Ermöglicht den Tageswechsel per Wischgeste über die Datumszeile in Tagesdetails und aktualisiert das Dashboard automatisch nach gespeicherten manuellen Einträgen.
- Verschiebt die Zyklusverfolgung in die expliziten Health-Connect-Berechtigungskategorien in Onboarding und Einstellungen, mit klarerer Behandlung fehlender Berechtigungen.
- Korrigiert das monochrome Launcher-Icon und lässt die Release-CI Codeberg-Artefakte veröffentlichen, während automatisierte Google-Play-Uploads/Promotions deaktiviert bleiben.

## 1.3.2 - 2026-06-10

### English

- Move Apple Health export imports to a WorkManager-backed background job so large `export.xml` or `export.zip` imports can continue after leaving Settings.
- Add live import progress, foreground notification text, and clearer parsed/imported/duplicate/unsupported/skipped/failed result counts.
- Stream Apple Health parsing and record writing to reduce memory pressure on large exports while preserving diagnostics and per-type summaries.
- Declare the data-sync foreground service path needed for reliable long-running Apple Health imports on newer Android versions.

### Español

- Mueve las importaciones de exportaciones de Apple Health a un trabajo en segundo plano con WorkManager para que importaciones grandes de `export.xml` o `export.zip` continúen al salir de Ajustes.
- Añade progreso de importación en vivo, texto de notificación en primer plano y recuentos más claros de analizados/importados/duplicados/no compatibles/omitidos/fallidos.
- Procesa en streaming el análisis de Apple Health y la escritura de registros para reducir el uso de memoria en exportaciones grandes, conservando diagnósticos y resúmenes por tipo.
- Declara la ruta de servicio en primer plano de sincronización de datos necesaria para importaciones largas y fiables de Apple Health en versiones recientes de Android.

### Deutsch

- Verschiebt Apple-Health-Exportimporte in einen WorkManager-gestützten Hintergrundauftrag, sodass große `export.xml`- oder `export.zip`-Importe nach dem Verlassen der Einstellungen weiterlaufen können.
- Ergänzt Live-Importfortschritt, Vordergrund-Benachrichtigungstexte und klarere Zählungen für gelesen/importiert/Duplikate/nicht unterstützt/übersprungen/fehlgeschlagen.
- Streamt Apple-Health-Parsing und Datensatzschreiben, um den Speicherbedarf bei großen Exporten zu senken und Diagnosen sowie Typzusammenfassungen zu behalten.
- Deklariert den Daten-Sync-Foreground-Service-Pfad, der für zuverlässige lange Apple-Health-Importe auf neueren Android-Versionen nötig ist.

## 1.3.1 - 2026-06-10

### English

- Update AndroidX Health Connect to 1.2.0-alpha04 and align activity recording with the newer activity-recognition, health foreground-service, and high-sampling sensor permissions.
- Expand recorded activity support with newer exercise types and repetition-set details where Health Connect provides them.
- Redesign the Apple Health import implementation into a dedicated importer package with clearer parser/converter tests and broader write-permission handling for supported records.
- Split dashboard, settings, manual entry, activity recording, route import, period helpers, and metric sections into smaller feature-owned files for safer future changes.
- Move app-local models, insights, and preferences into domain packages while keeping HealthRepository focused on availability, permissions, and dashboard aggregation.

### Español

- Actualiza AndroidX Health Connect a 1.2.0-alpha04 y alinea la grabación de actividades con los permisos más recientes de reconocimiento de actividad, servicio en primer plano de salud y sensores de alta frecuencia.
- Amplía el soporte de actividades grabadas con tipos de ejercicio más nuevos y detalles de series de repeticiones cuando Health Connect los proporciona.
- Rediseña la importación de Apple Health en un paquete de importación dedicado, con pruebas más claras de parser/conversión y manejo más amplio de permisos de escritura para registros compatibles.
- Divide panel, ajustes, entradas manuales, grabación de actividades, importación de rutas, helpers de periodo y secciones de métricas en archivos más pequeños y propios de cada feature para cambios futuros más seguros.
- Mueve modelos, insights y preferencias locales de la app a paquetes de dominio, manteniendo HealthRepository centrado en disponibilidad, permisos y agregación del panel.

### Deutsch

- Aktualisiert AndroidX Health Connect auf 1.2.0-alpha04 und richtet die Aktivitätsaufzeichnung auf die neueren Berechtigungen für Aktivitätserkennung, Health-Foreground-Service und hochfrequente Sensoren aus.
- Erweitert aufgezeichnete Aktivitäten um neuere Trainingstypen und Wiederholungssatz-Details, sofern Health Connect sie bereitstellt.
- Überarbeitet den Apple-Health-Import in einem eigenen Importpaket mit klareren Parser-/Converter-Tests und breiterer Schreibberechtigungsbehandlung für unterstützte Datensätze.
- Teilt Dashboard, Einstellungen, manuelle Eingaben, Aktivitätsaufzeichnung, Routenimport, Zeitraumhelfer und Metrikbereiche in kleinere feature-eigene Dateien für sicherere künftige Änderungen.
- Verschiebt app-lokale Modelle, Insights und Einstellungen in Domain-Pakete und hält HealthRepository auf Verfügbarkeit, Berechtigungen und Dashboard-Aggregation fokussiert.

## 1.3.0 - 2026-06-09

### English

- Add Apple Health export import from Settings for supported activity, heart, body, hydration, and vitals records, writing imported records into Health Connect.
- Add FIT route file import alongside GPX/KML/KMZ, with parser tests and clearer handling for route metadata and large files.
- Add wheelchair activity support, wheelchair push summaries, charts, dashboard widgets, and Health Connect permission coverage.
- Expand Heart & Vitals with a combined overview screen, stronger charts, and high/low heart-rate check summaries.
- Unify Body and Nutrition detail screens with richer period overviews, body composition coverage, and meal/macro chart improvements.
- Rework Settings into grouped sections with data import permissions, clearer controls, and improved unit and chart formatting.

### Español

- Añade importación de exportaciones de Apple Health desde Ajustes para registros compatibles de actividad, corazón, cuerpo, hidratación y constantes, escribiéndolos en Health Connect.
- Añade importación de rutas FIT junto con GPX/KML/KMZ, con pruebas del parser y manejo más claro de metadatos de ruta y archivos grandes.
- Añade soporte para actividades en silla de ruedas, resúmenes de impulsos, gráficos, widgets del panel y cobertura de permisos de Health Connect.
- Amplía Corazón y Constantes con una pantalla general combinada, gráficos más sólidos y resúmenes de comprobaciones de frecuencia alta/baja.
- Unifica las pantallas de Cuerpo y Nutrición con vistas por periodo más completas, más cobertura de composición corporal y mejoras de gráficos de comidas y macros.
- Reorganiza Ajustes en secciones agrupadas con permisos de importación, controles más claros y mejor formato de unidades y gráficos.

### Deutsch

- Ergänzt in den Einstellungen den Import von Apple-Health-Exporten für unterstützte Aktivitäts-, Herz-, Körper-, Flüssigkeits- und Vitalwerte und schreibt sie in Health Connect.
- Ergänzt den FIT-Routenimport neben GPX/KML/KMZ, mit Parser-Tests und klarerer Verarbeitung von Routenmetadaten und großen Dateien.
- Fügt Rollstuhl-Aktivitäten, Rollstuhl-Schubzusammenfassungen, Diagramme, Dashboard-Widgets und passende Health-Connect-Berechtigungen hinzu.
- Erweitert Herz & Vitalwerte um eine kombinierte Übersicht, stärkere Diagramme und Zusammenfassungen hoher/niedriger Herzfrequenzwerte.
- Vereinheitlicht Körper- und Ernährungsdetails mit reicheren Zeitraumübersichten, mehr Körperzusammensetzung und verbesserten Mahlzeit-/Makrodiagrammen.
- Überarbeitet die Einstellungen in gruppierte Bereiche mit Importberechtigungen, klareren Bedienelementen und besserer Einheiten- und Diagrammformatierung.

## 1.2.3 - 2026-06-09

### English

- Add a Calories detail screen with period statistics, total and active calorie trends, BMR context, and day-level breakdown rows.
- Link dashboard and Activities calorie cards to the new Calories screen so calorie data has a full drill-down path.
- Clarify dashboard messaging when Health Connect has no total-calorie record or OpenVitals is estimating totals from active calories plus BMR.
- Improve hydration entry cup-size controls with better alignment and more readable saved container values.
- Add auto-resizing text to compact dashboard, metric, and chart cards so longer labels and values fit more reliably.
- Fix Activities today handling and update CI/build tooling for Android SDK 37, AGP 9.1.1, and the newer Material 3 library.

### Español

- Añade una pantalla de detalle de Calorías con estadísticas por periodo, tendencias de calorías totales y activas, contexto de BMR y desglose diario.
- Enlaza las tarjetas de calorías del panel y de Actividades con la nueva pantalla de Calorías para poder profundizar en los datos.
- Aclara los mensajes del panel cuando Health Connect no tiene un registro de calorías totales o cuando OpenVitals estima totales con calorías activas más BMR.
- Mejora los controles de tamaño de vaso o taza en hidratación, con mejor alineación y valores guardados más legibles.
- Añade texto autoajustable en tarjetas compactas del panel, métricas y gráficos para que etiquetas y valores largos encajen mejor.
- Corrige el manejo de hoy en Actividades y actualiza CI/compilación a Android SDK 37, AGP 9.1.1 y la biblioteca Material 3 más reciente.

### Deutsch

- Fügt eine Kalorien-Detailseite mit Zeitraumstatistiken, Trends für Gesamt- und aktive Kalorien, BMR-Kontext und Tagesaufschlüsselung hinzu.
- Verlinkt Kalorienkarten im Dashboard und in Aktivitäten mit der neuen Kalorienseite, damit Kaloriendaten vollständig aufgeschlüsselt werden können.
- Verdeutlicht Dashboard-Meldungen, wenn Health Connect keinen Gesamtkalorienwert liefert oder OpenVitals Werte aus aktiven Kalorien plus BMR schätzt.
- Verbessert die Bedienelemente für Trinkgefäßgrößen mit besserer Ausrichtung und lesbareren gespeicherten Werten.
- Ergänzt automatisch verkleinernden Text in kompakten Dashboard-, Metrik- und Diagrammkarten, damit längere Beschriftungen und Werte zuverlässiger passen.
- Behebt die Heute-Behandlung in Aktivitäten und aktualisiert CI/Build auf Android SDK 37, AGP 9.1.1 und die neuere Material-3-Bibliothek.

## 1.2.2 - 2026-06-09

### English

- Move the app toward a Summary-first flow by folding the old Activities and Sleep tab content into richer metric detail screens with overview cards and direct metric links.
- Add a total-calories preference that keeps Health Connect totals as the default and can optionally fill missing totals from active calories plus BMR.
- Make weekly cardio load respect the Activity week setting, so Last 7 days uses a rolling selected-date window while Mon-Sun remains fixed.
- Persist edited hydration container sizes per preset and give the controls more room so labels and values are not cut off.
- Add a discard action for unfinished GPS recording drafts before saving.
- Polish dashboard, activity, and sleep UI with clearer colors, denser widgets, and better text fitting.

### Español

- Orienta la app hacia un flujo centrado en Resumen integrando el contenido de las antiguas pestañas Actividades y Sueño en pantallas de detalle más completas con tarjetas de vista general y enlaces directos a métricas.
- Añade una preferencia de calorías totales que mantiene los totales de Health Connect por defecto y puede completar totales ausentes con calorías activas más BMR.
- Hace que la carga cardiovascular semanal respete el ajuste de semana de Actividades, usando una ventana móvil de últimos 7 días o una semana fija de lunes a domingo según la opción elegida.
- Guarda tamaños de recipientes de hidratación editados por cada preset y da más espacio a los controles para evitar textos cortados.
- Añade una acción para descartar borradores de grabación GPS antes de guardarlos.
- Pule la UI del panel, actividades y sueño con colores más claros, widgets más densos y mejor ajuste de texto.

### Deutsch

- Richtet die App stärker auf die Zusammenfassung aus, indem die bisherigen Inhalte der Aktivitäten- und Schlaf-Tabs in umfangreichere Detailseiten mit Übersichtskarten und direkten Metriklinks wandern.
- Fügt eine Einstellung für Gesamtkalorien hinzu, die standardmäßig Health-Connect-Werte nutzt und fehlende Werte optional aus aktiven Kalorien plus BMR ergänzt.
- Lässt die wöchentliche Cardio Load die Aktivitäten-Wocheneinstellung respektieren, mit rollierenden letzten 7 Tagen oder fester Montag-Sonntag-Woche.
- Speichert bearbeitete Trinkgefäßgrößen pro Vorgabe und gibt den Bedienelementen mehr Platz, damit Beschriftungen und Werte nicht abgeschnitten werden.
- Ergänzt eine Aktion zum Verwerfen unfertiger GPS-Aufzeichnungsentwürfe vor dem Speichern.
- Poliert Dashboard-, Aktivitäten- und Schlafoberflächen mit klareren Farben, dichteren Widgets und besser passendem Text.

## 1.2.1 - 2026-06-06

### English

- Remember the latest recorded activity type and preselect it for future activity entries.
- Add a Settings option to choose a favorite activity type that overrides the latest recorded activity default.
- Return to the dashboard after saving a new activity so users do not land back on the activity-entry screen.

### Español

- Recuerda el último tipo de actividad grabada y lo preselecciona en futuras entradas de actividad.
- Añade una opción en Ajustes para elegir un tipo de actividad favorito que sustituye al valor predeterminado de la última actividad grabada.
- Vuelve al panel después de guardar una actividad nueva para que la app no regrese a la pantalla de entrada de actividad.

### Deutsch

- Merkt sich den zuletzt aufgezeichneten Aktivitätstyp und wählt ihn für zukünftige Aktivitätseinträge vor.
- Fügt in den Einstellungen eine Lieblingsaktivität hinzu, die den Standardwert der zuletzt aufgezeichneten Aktivität überschreibt.
- Kehrt nach dem Speichern einer neuen Aktivität zum Dashboard zurück, damit Nutzer nicht wieder auf dem Aktivitätseintrag landen.

## 1.2.0 - 2026-06-06

### English

- Add System, Light, Dark, and AMOLED theme options, with AMOLED keeping Material You accent colors and pure black backgrounds.
- Add swipe-to-delete for OpenVitals-owned hydration, activity, mindfulness, body measurement, and vitals entries while keeping records from other apps read-only.
- Let users edit the default hydration container size so quick hydration logging can match their real bottle or glass.
- Add configurable mindfulness reminders alongside the existing hydration reminder support.
- Improve GPS activity recording by using the already locked GPS fix, keeping finished recordings recoverable after navigating back, and renaming the final action to Save activity.
- Compact the dashboard sleep widget and improve its contrast.

### Español

- Añade opciones de tema Sistema, Claro, Oscuro y AMOLED, con AMOLED manteniendo los colores Material You y fondos negro puro.
- Añade deslizar para borrar entradas de hidratación, actividad, mindfulness, medidas corporales y signos vitales creadas por OpenVitals, manteniendo solo lectura los registros de otras apps.
- Permite editar el tamaño predeterminado del recipiente de hidratación para que el registro rápido coincida con la botella o vaso real.
- Añade recordatorios configurables de mindfulness junto al soporte existente de recordatorios de hidratación.
- Mejora la grabación GPS usando la ubicación ya fijada, conservando grabaciones terminadas al volver atrás y renombrando la acción final a Guardar actividad.
- Compacta el widget de sueño del panel y mejora su contraste.

### Deutsch

- Fügt die Designoptionen System, Hell, Dunkel und AMOLED hinzu, wobei AMOLED Material-You-Akzentfarben und rein schwarze Hintergründe beibehält.
- Fügt Wischen zum Löschen von OpenVitals-eigenen Einträgen für Flüssigkeit, Aktivität, Achtsamkeit, Körpermessungen und Vitalwerte hinzu; Einträge anderer Apps bleiben schreibgeschützt.
- Ermöglicht das Bearbeiten der Standardgröße des Trinkgefäßes, damit schnelles Flüssigkeitstracking zur echten Flasche oder zum Glas passt.
- Fügt konfigurierbare Achtsamkeitserinnerungen zusätzlich zu den bestehenden Flüssigkeitserinnerungen hinzu.
- Verbessert die GPS-Aufzeichnung, indem der bereits fixierte GPS-Punkt verwendet wird, abgeschlossene Aufzeichnungen nach Zurück-Navigation wiederherstellbar bleiben und die Abschlussaktion in Aktivität speichern umbenannt wird.
- Komprimiert das Schlaf-Widget im Dashboard und verbessert dessen Kontrast.

## 1.1.1 - 2026-06-01

### English

- Show sleep score and its rating directly in the dashboard sleep widget using the shared sleep score calculation.
- Fix achievements history loading when Health Connect permissions are granted by allowing step-only history and chunking long activity-history reads.
- Keep average pace and average speed visible after GPS recording ends, both in the review form and in saved activity details.
- Use moving time for activity pace and speed when pause segments are available.

### Español

- Muestra la puntuación de sueño y su valoración directamente en el widget de sueño del panel usando el cálculo compartido.
- Corrige la carga del historial de logros cuando los permisos de Health Connect están concedidos, permitiendo historial solo de pasos y dividiendo lecturas largas de actividad.
- Mantiene el ritmo medio y la velocidad media visibles tras terminar una grabación GPS, tanto en el formulario de revisión como en el detalle guardado.
- Usa el tiempo en movimiento para el ritmo y la velocidad de actividad cuando hay segmentos de pausa disponibles.

### Deutsch

- Zeigt Schlafscore und Bewertung direkt im Schlaf-Widget des Dashboards mit der gemeinsamen Schlafscore-Berechnung.
- Behebt das Laden des Erfolgsverlaufs bei erteilten Health-Connect-Berechtigungen, indem reine Schrittverläufe erlaubt und lange Aktivitätsabfragen aufgeteilt werden.
- Hält durchschnittliches Tempo und Durchschnittsgeschwindigkeit nach dem Ende einer GPS-Aufzeichnung sichtbar, sowohl im Prüfungsformular als auch in gespeicherten Aktivitätsdetails.
- Verwendet Bewegungszeit für Tempo und Geschwindigkeit, wenn Pausensegmente verfügbar sind.

## 1.1.0 - 2026-06-01

### English

- Add Fitbit-inspired achievement badges for activity, distance, floors, workouts, hydration, sleep, and mindfulness, opened from the new top-bar ribbon button.
- Add opt-in hydration reminders with active hours, interval scheduling, notification permission handling, boot rescheduling, and automatic pause after the daily goal is reached.
- Add an Activities setting for either a fixed Monday-Sunday week or a rolling last 7 days, and keep every key-metric chart on a consistent seven-day range.
- Add one-tap onboarding for all requestable read, write, and additional Health Connect permissions while keeping cycle tracking explicitly opt-in and workout route access manual.
- Refactor hydration reminder scheduling around a controller and stricter alarm wrapper for more predictable reminder behavior.

### Español

- Añade insignias de logros inspiradas en Fitbit para actividad, distancia, pisos, entrenamientos, hidratación, sueño y mindfulness, abiertas desde el nuevo botón de cinta.
- Añade recordatorios de hidratación opcionales con horas activas, programación por intervalo, permiso de notificaciones, reprogramación tras reinicio y pausa automática al alcanzar el objetivo diario.
- Añade un ajuste de Actividades para usar una semana fija de lunes a domingo o los últimos 7 días, y mantiene todos los gráficos de métricas clave en un rango consistente de siete días.
- Añade onboarding de un toque para todos los permisos solicitables de lectura, escritura y acceso adicional de Health Connect, manteniendo ciclo como opt-in y rutas de entrenamiento como acceso manual.
- Refactoriza la programación de recordatorios de hidratación con un controlador y un wrapper de alarmas más estricto para un comportamiento más predecible.

### Deutsch

- Fügt Fitbit-inspirierte Erfolgsabzeichen für Aktivität, Distanz, Etagen, Trainings, Flüssigkeit, Schlaf und Achtsamkeit hinzu, erreichbar über die neue Ribbon-Schaltfläche.
- Fügt optionale Flüssigkeitserinnerungen mit aktiven Zeiten, Intervallplanung, Benachrichtigungsberechtigung, Neuplanung nach Neustart und automatischer Pause nach Erreichen des Tagesziels hinzu.
- Fügt eine Aktivitäten-Einstellung für eine feste Montag-Sonntag-Woche oder die rollierenden letzten 7 Tage hinzu und hält alle wichtigen Metrikdiagramme auf einem konsistenten Sieben-Tage-Bereich.
- Fügt Onboarding mit einem Schritt für alle anforderbaren Lese-, Schreib- und Zusatzberechtigungen von Health Connect hinzu, während Zyklusdaten explizit opt-in und Trainingsrouten manuell bleiben.
- Refaktoriert die Planung von Flüssigkeitserinnerungen mit einem Controller und strengerem Alarm-Wrapper für vorhersehbareres Verhalten.

## 1.0.0 - 2026-05-31

### English

- Revamp the dashboard with a denser widget grid, editable widget ordering, and clearer summary cards.
- Add a Recovery tab with sleep score, sleep efficiency, trend detail screens, confidence notes, and localized explanations.
- Add an Activities overview with cardio load, weekly progress, route-aware activity summaries, and a cardio load detail screen.
- Open saved GPS routes in external map apps, import GPX/KML/KMZ route files, and export activity routes as GPX/KMZ.
- Add high and low heart-rate threshold checks with adjustable settings in the Heart detail screen.
- Improve repository and Health Connect query performance with shared dispatcher wiring, query caching, and broader test coverage.

### Español

- Renueva el panel con una cuadrícula de widgets más densa, orden editable y tarjetas de resumen más claras.
- Añade una pestaña Recuperación con puntuación de sueño, eficiencia del sueño, pantallas de tendencia, notas de confianza y explicaciones localizadas.
- Añade una vista general de Actividades con carga cardiovascular, progreso semanal, resúmenes con rutas y detalle de carga cardiovascular.
- Abre rutas GPS guardadas en apps de mapas externas, importa archivos GPX/KML/KMZ y exporta rutas de actividad como GPX/KMZ.
- Añade comprobaciones de frecuencia cardiaca alta y baja con umbrales ajustables en el detalle de Corazón.
- Mejora el rendimiento de repositorios y consultas de Health Connect con dispatchers compartidos, caché de consultas y más cobertura de tests.

### Deutsch

- Überarbeitet das Dashboard mit dichterem Widget-Raster, bearbeitbarer Widget-Reihenfolge und klareren Übersichtskarten.
- Fügt einen Erholungs-Tab mit Schlafscore, Schlafeffizienz, Trenddetails, Vertrauenshinweisen und lokalisierten Erklärungen hinzu.
- Fügt eine Aktivitätenübersicht mit Cardio Load, Wochenfortschritt, routenbezogenen Aktivitätszusammenfassungen und Cardio-Load-Detailansicht hinzu.
- Öffnet gespeicherte GPS-Routen in externen Karten-Apps, importiert GPX/KML/KMZ-Routendateien und exportiert Aktivitätsrouten als GPX/KMZ.
- Ergänzt Prüfungen für hohe und niedrige Herzfrequenz mit einstellbaren Schwellenwerten in der Herz-Detailansicht.
- Verbessert Repository- und Health-Connect-Abfrageleistung mit gemeinsamem Dispatcher-Wiring, Abfrage-Cache und breiterer Testabdeckung.

## 0.7.1 - 2026-05-28

### English

- Edit OpenVitals-created hydration, activity, mindfulness, body measurement, and vitals entries from detail and browse lists.
- Keep records from other apps read-only; edit actions appear only for OpenVitals-owned records.
- Verify Health Connect ownership again before every update so third-party records cannot be modified through the edit routes.
- Prefill edit screens with existing values and save changes back to the original Health Connect record.
- Add localized English, Spanish, and German release notes and Play Store changelogs.

### Español

- Edita entradas de hidratación, actividad, mindfulness, medidas corporales y signos vitales creadas por OpenVitals desde listas de detalle y exploración.
- Las entradas de otras apps siguen siendo de solo lectura; la acción de edición solo aparece en registros propios de OpenVitals.
- Se vuelve a comprobar el origen en Health Connect antes de cada actualización para impedir cambios en registros de terceros.
- Las pantallas de edición se rellenan con los valores existentes y guardan los cambios en el registro original de Health Connect.
- Añade notas de versión y changelogs de Play Store en inglés, español y alemán.

### Deutsch

- Bearbeite von OpenVitals erstellte Einträge für Flüssigkeit, Aktivitäten, Achtsamkeit, Körpermessungen und Vitalwerte aus Detail- und Browse-Listen.
- Einträge anderer Apps bleiben schreibgeschützt; die Bearbeiten-Aktion erscheint nur für OpenVitals-eigene Datensätze.
- Die Health Connect-Herkunft wird vor jeder Aktualisierung erneut geprüft, damit Datensätze von Dritt-Apps nicht geändert werden.
- Bearbeitungsseiten werden mit vorhandenen Werten vorausgefüllt und speichern Änderungen im ursprünglichen Health Connect-Datensatz.
- Ergänzt Versionshinweise und Play Store-Changelogs auf Englisch, Spanisch und Deutsch.

## 0.7.0 - 2026-05-27

### English

- Add Activity entry support for Health Connect exercise sessions with optional route, distance, elevation gain, active calories, and total calories records.
- Import GPX, KML, and KMZ routes, preview them, infer activity details, retime untimestamped tracks, and save route-backed workouts to Health Connect.
- Record GPS activities in OpenVitals with pause, resume, discard, route preview, distance, elevation gain, moving time, and a persistent recording notification.
- Estimate active and total calories for imported routes and recorded activities while leaving fully manual activity entries blank.
- Update the release pipeline so tags publish beta releases to Codeberg and Google Play open testing, with approved promotion to Codeberg stable and Play production.

### Espanol

- Anade entradas de Actividad para sesiones de ejercicio de Health Connect con rutas, distancia, elevacion, calorias activas y calorias totales opcionales.
- Importa rutas GPX, KML y KMZ, las previsualiza, infiere detalles de actividad, ajusta rutas sin marcas de tiempo y guarda entrenamientos con ruta en Health Connect.
- Graba actividades GPS en OpenVitals con pausa, reanudacion, descarte, previsualizacion de ruta, distancia, elevacion, tiempo en movimiento y notificacion persistente.
- Estima calorias activas y totales para rutas importadas y actividades grabadas, dejando vacias las actividades completamente manuales.
- Actualiza el pipeline para publicar etiquetas como beta en Codeberg y pruebas abiertas de Google Play, con promocion aprobada a estable y produccion.

## 0.6.1 - 2026-05-26

### English

- Refresh the app shell with Material 3 adaptive navigation, updated theming, clearer dashboard cards, and scroll-aware detail screens.
- Move Add entry into a contextual create action while keeping Dashboard, Browse, and Settings as the main destinations.
- Improve manual-entry UX with cleaner controls, better spacing, stronger primary actions, and accessibility updates for widget editing.
- Update mindfulness entry with bell previews, looping background sounds, a circular animated timer, and a simplified minutes-only timer input.
- Add new Play screenshots and update third-party notices and thanks for bundled meditation sound assets.

### Español

- Se renovó la estructura visual con navegación adaptativa Material 3, tema actualizado, tarjetas de panel más claras y pantallas de detalle con barras superiores sensibles al desplazamiento.
- Añadir entrada pasa a ser una acción contextual de creación, manteniendo Panel, Explorar y Ajustes como destinos principales.
- Se mejoró la experiencia de entradas manuales con controles más claros, mejor espaciado, acciones principales más visibles y mejoras de accesibilidad al editar widgets.
- Mindfulness ahora incluye previsualización de campanas, sonidos de fondo en bucle, temporizador circular animado y entrada de duración simplificada solo en minutos.
- Se añadieron nuevas capturas para Play y se actualizaron los avisos y agradecimientos de sonidos de meditación incluidos.

## 0.6.0 - 2026-05-25

### English

- OpenVitals now has a dedicated Add entry area. The dashboard remains read-only, while manual entries are saved directly to Health Connect.
- Hydration entries can be added with drink and serving choices, including daily total context and clear write-permission messaging.
- Manual entries now cover weight, height, body fat percentage, blood pressure, blood oxygen, respiratory rate, and body temperature.
- Mindfulness entries now support a configurable sitting timer with bundled interval bells, plus manual minute entry and save/discard controls.
- The app architecture was modernized with Hilt, shared period queries, cached Health Connect reads, and CI/release improvements including the GitHub mirror.

### Español

- OpenVitals ahora tiene una zona dedicada para añadir entradas. El panel sigue siendo solo de lectura, y las entradas manuales se guardan directamente en Health Connect.
- La hidratación se puede registrar con tipo de bebida y tamaño de vaso, con el total diario y permisos de escritura explicados en el momento adecuado.
- Las entradas manuales ahora incluyen peso, altura, porcentaje de grasa corporal, presión arterial, oxígeno en sangre, frecuencia respiratoria y temperatura corporal.
- Mindfulness incorpora un temporizador configurable con campanas de intervalo incluidas, además de entrada manual de minutos y controles para guardar o descartar.
- La arquitectura se modernizó con Hilt, consultas por periodo compartidas, lecturas de Health Connect en caché y mejoras de CI/release, incluido el espejo en GitHub.

## 0.5.2 - 2026-05-24

- Show timeframe-scoped entry lists across metric detail screens with a paginated "Load 10 more" flow.
- Let week and month charts reveal a tapped day's entries, with a second tap clearing the selection.
- Simplify the dashboard workout widget so it highlights workout type and duration without extra source/date noise.
- Improve dashboard edit mode so dragging a carousel widget to the screen edge auto-scrolls pages.
- Fix carousel reordering so swapping widgets across pages does not accidentally target the fixed dashboard area.

## 0.5.1 - 2026-05-24

- Refresh the OpenVitals app branding with the new logo and launcher icons.
- Use distinct launcher icons for production and debug builds.
- Show the new logo during onboarding.
- Update README screenshots and project branding.
