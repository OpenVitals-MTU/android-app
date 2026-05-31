# Changelog

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
