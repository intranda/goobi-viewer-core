# Viewer Core JS + CSS Abhängigkeiten

## CONTENTS
* [JS](#js)
  * [Bootstrap](#bootstrap)
  * [Clipboard](#clipboard)
  * [hc-sticky](#hc-sticky)
  * [jqPlot](#jqplot)
  * [jqueryUI](#jqueryui)
  * [Leaflet](#leaflet)
  * [Mapbox](#mapbox)
  * [Masonry-Layout](#masonry-layout)
  * [Mirador](#mirador)
  * [Q-Promises](#q-promises)
  * [PdfJS](#pdfjs)
  * [rxjs (reactiveX)](#rxjs-(reactivex))
  * [Riot](#riot)
  * [Swagger-ui-dist](#swagger-ui-dist)
  * [SWEETALERT](#sweetalert)
  * [ThreeJS](#threejs)
  * [TINYMCE](#tinymce)
  * [x3dom](#x3dom)

* [CSS](#css)
  * [CSS: Bootstrap](#css:-bootstrap)
  * [CSS: Font Awesome](#css:-font-awesome)
  * [CSS: JQueryUI](#css:-jqueryui)
  * [CSS: Leaflet](#css:-leaflet)
  * [CSS: Mapbox](#css:-mapbox)
  * [CSS: swagger](#css:-swagger)
  * [CSS: sweetalert](#css:-sweetalert)
  * [CSS: Swiper](#css:-swiper)

## JS

### Bootstrap
- *bootstrap.bundle.min.js*
- Wird in allen Template Dateien eingebunden

#### Zweck
- Bootstrap-Abhängigkeit

#### Test
- Aufruf z.B. Startseite, prüfe, ob die Datei geladen wird:
- [Startseite](http://localhost:8080/viewer/admin/)

### Clipboard
- *clipboard.min.js*
- Wird in allen Template Dateien eingebunden

#### Zweck
- Inhalte ins clipboard kopieren
  - z.B. über das drücken eine copy buttons

#### Test
- Aufruf z.B. Startseite, prüfe, ob die Datei geladen wird:
  - [Startseite](http://localhost:8080/viewer/admin/)

### hc-sticky
- *hc-sticky.js*
- Wird in folgenden Templates geladen:
  - templateAdmin.html
  - templateArchives.html

#### Zweck
- DOM-Elemente fixieren
- z.B. um nur eine sidebar zu scrollen

#### Test
- wird u.a. im Backend eingesetzt:
  - [http://localhost:8080/viewer/admin/](http://localhost:8080/viewer/admin/)
  - das blaue Menu ist fixiert
  - während die Sidebar + main scrollt

### jqPlot
- *jquery.jqplot.min.js*

#### Besonderheiten
- css liegt mit im JS-Ordner
  - *jquery.jqplot.min.css*
- es wird eine Vielzahl von *Plugins* genutzt
  - nicht via NPM aktualisierbar!
  - manueller Download hier: [http://www.jqplot.com/download/](http://www.jqplot.com/download/)

#### Zweck
- Diagramme rendern

#### Test
- wird im Statistics View geladen:
- [http://localhost:8080/viewer/statistics/](http://localhost:8080/viewer/statistics/)

### jqueryUI
- v1.11.0
- v1.12.0

#### Besonderheiten
- v1.11.0 stammt aus dem Juni 2014
- jqueryUI sollte auf den neusten Stand gebracht werden
- es bedarf weiterer Recherche, um zu klären, wo jqueryUI überall eingesetzt wird

#### Zweck

#### Test

### Leaflet
```
leaflet
├── LICENSE
├── draw
│   └── leaflet.draw.js
├── extra-markers
│   ├── LICENSE
│   ├── leaflet.extra-markers.js
│   ├── leaflet.extra-markers.js.map
│   └── leaflet.extra-markers.min.js
├── leaflet.js
├── leaflet.js.map
└── markercluster
    ├── MIT-LICENCE.txt
    ├── leaflet.markercluster.js
    └── leaflet.markercluster.js.map
```

#### Besonderheiten

#### Zweck
- Rendern von Kartenmaterial

#### Test
- wird in Templates eingebunden
- prüfe, ob die Dateien geladen werden
- prüfe, ob Karten richtig gerendert werden

### Mapbox
- *geocoder/mapbox-gl-geocoder.min.js*
- *mapbox-gl.js*

#### Besonderheiten

#### Zweck

#### Test
- wird in template.html + templateCrowdsourcing.html eingebunden
- außerdem: die Kartensuche testen [http://localhost:8080/viewer/searchgeomap/](http://localhost:8080/viewer/searchgeomap/)

### Masonry-Layout
- *masonry.pkdg.min.js*

#### Besonderheiten
- benötigt *imagesloaded.pkgd.min.js*

#### Zweck
- Masonry-Layout im cms_template_11_masonry.xhtml

#### Test
- Masonry Template testen
- z.B.: [http://localhost:8080/viewer/index/)](http://localhost:8080/viewer/index/)

### Mirador
- *mirador.min.js*
- *mirador.min.js.LICENSE.txt*
- *mirador.min.js.map*

#### Besonderheiten

#### Zweck
- Mirador wird für die Vergleichsansichten benutzt:

#### Test
- füge 2 Werke der Reading-List hinzu
- klicke "Vergleichsansicht" in den Session-Bookmarks

### Q-Promises
- *q.min.js*

#### Besonderheiten

#### Zweck
* Promise Library
* stammt aus der Zeit als native Promises noch nicht production ready waren
- Genutzt wird es u.a. bei der Bildanzeige

#### Test
- Wird in allen Templates eingebunden
* prüfe, ob die Bildanzeige funktioniert
- [Beispiel_Bildanzeige](http://localhost:8080/viewer/image/PPN407465633d27352e322e27_40636c6173736e756d3d2733322e27_407369673d2733322f303827/1/LOG_0000/)

### openseadragon

#### Besonderheiten
* der viewer benutzt eine Version, die mit den offiziellen Releases nicht kompatibel ist
* daher wird die Bibliothek nicht mit NPM verwaltet

#### Zweck
* web-basierter viewer
* wird für die Bildanzeige gentutz

#### Test
* prüfe, ob die Bildanzeige funktioniert
- [Beispiel_Bildanzeige](http://localhost:8080/viewer/image/PPN407465633d27352e322e27_40636c6173736e756d3d2733322e27_407369673d2733322f303827/1/LOG_0000/)

### PdfJS
- *pdf.js*
- *pdf.js.map*
- *pdf.min.js*
- *pdf.worker.js*
- *pdf.worker.min.js*

#### Besonderheiten

#### Zweck
- PDFs auf cms Seiten darstellen

#### Test
- cms Seite anlegen und ein PDF einbetten

### rxjs (reactiveX)
- *rxjs.umd.min.js*
- *rxjs.umd.min.js.map*

#### Besonderheiten

#### Zweck
- wird in der Bildanzeige genutzt 

#### Test
- Wird in allen Templates eingebunden
- Genutzt wird es u.a. bei der Bildanzeige
  - [Beispiel_Bildanzeige](http://localhost:8080/viewer/image/PPN407465633d27352e322e27_40636c6173736e756d3d2733322e27_407369673d2733322f303827/1/LOG_0000/)

### Riot
- *riot.min.js*
- *riot+compiler.min.js*

#### Besonderheiten

#### Zweck
- UI Bibliothek
- dynamisches Rendern von HTML
- Beispiel ist die Bookmarklist (Merkliste)

#### Test
- wird in den meisten Templates eingebunden
- wenn Templates geladen werden, überprüfe, ob die Bookmarklist gerendert wird

### Swagger-ui-dist
- *swagger-ui-bundle.js*
- *swagger-ui-bundle.js.map*

#### Besonderheiten

#### Zweck
- eingebunden in restApi.xhtml
- wird genutzt, um swagger apis zu visualisieren
- [https://viewer.goobi.io/api/swagger/](https://viewer.goobi.io/api/swagger/) 

#### Test
- rufe [https://viewer.goobi.io/api/swagger/](https://viewer.goobi.io/api/swagger/) auf
- wird die lib geladen? Funktioniert die visuelle Darstellung?

### SWEETALERT
* *sweetalert2.min.js*

#### Besonderheiten
- in viewerJS.notifications.js wird geprüft, welche notification lib vorhanden ist
- overhang wird nicht mehr genutzt.

#### Zweck
- Alerts anzeigen
- eingebunden in template.html, templateAdmin.html + templateCrowdsourcing.html
- im Crowdsourcing-Modul z.B. genutzt für Infos beim Speichern von Änderungen

#### Test
- crowdsourcing modul:
  - werden CSS + JS geladen?
  - funktioniert die Anzeige von Alerts?

### Swiper
- *swiper-bundel.min.js*

#### Besonderheiten

#### Zweck
- Darstellen von Slidern, die mit der Swiper Api gebaut werden

#### Test
- wird in template.html + templateAdmin.html eingebunden
- prüfe Styling des HeaderSliders auf der Startseite
- => [http://localhost:8080/viewer/](http://localhost:8080/viewer/)

### ThreeJS
```
three
|-- controls
    |-- OrbitControls.js 
|-- dependencies 
    |-- draco
        |-- gltf
          |-- draco_decoder.js
          |-- draco_decoder.wasm
          |-- draco_encoder.js
          |-- draco_wasm_wrapper.js
        |-- draco_decoder.js
        |-- draco_decoder.wasm
        |-- draco_encoder.js
        |-- draco_wasm_wrapper.js
        |-- README.md 
    |-- inflate_LICENCE
    |-- inflate.min.js
|-- loaders 
    |-- DRACOLoader.js 
    |-- FBXLoader.js 
    |-- GLTFLoader.js 
    |-- MTLLoader.js 
    |-- OBJLoader.js 
    |-- PLYLoader.js 
    |-- STLLoader.js 
    |-- TDSLoader.js 
|-- LICENSE 
|-- three.min.js 

```
#### Besonderheiten
- Für den Loader 'FBXLoader' wird eine externe Bibliothek benötigt:
  - Inflate.min.js 
  - Sie muss manuell von hier zugefügt werden: [https://github.com/imaya/zlib.js](https://github.com/imaya/zlib.js)

#### Zweck
- zum Rendern von 3D Objekte
- Wird unter anderem beim Bonenberger eingesetzt: [http://localhost:8080/viewer/image/1547556475000/5](http://localhost:8080/viewer/image/1547556475000/5)

#### Test
- Checken, ob three.js + dependencies geladen werden
  - z.B. beim Bonenberger
- Visuell überprüfen, ob die 3D Darstellung funktioniert

### TINYMCE
```
├── icons
├── jquery.tinymce.min.js
├── langs
├── license.txt
├── plugins
├── skins
├── themes
└── tinymce.min.js
```

#### Besonderheiten
- es müssen Spracheinstellungen zur Verfügung gestellt werden
- diese befinden sich im Verzeichnis 'langs'

#### Zweck
- Texteditor Autorenbackend

#### Test

### x3dom
- *x3dom.js*

#### Besonderheiten

#### Zweck
* wird vermutlich auch zum Rendern von 3D-Objekten eingesetzt
* Beispiel?

#### Test
???

---

## CSS

### CSS: Bootstrap
- *bootstrap.custom.css*

#### Besonderheiten
- Goobi viewer nutzt eine (manuell) angepasste Variante vom BS4 CSS
- Daher kann das BS CSS nicht durch einen package manager verwaltet werden

#### Zweck
- wird in allen Templates eingebunden
- Grundständiges Styling + Layout

#### Test
- überprüfe, ob die Datei geladen wird

### CSS: Font Awesome
* *font-awesome.min.css* 

#### Besonderheiten
- Goobi viewer nutzt v.4.7.0

#### Zweck
- stellt icons im Frontend dar

#### Test
- sollte in allen Templates geladen werden
- prüfe außerdem, ob die Lupe im Facets-Widget angezeigt wird

### CSS: JQueryUI

#### Besonderheiten
- Goobi viewer nutzt v1.11.0 + v1.12.1
- v1.11.0 wird aus legacy Gründen noch weiter mitgeführt
- die meisten Themes sollten aber bereits v1.12.1 nutzen

#### Zweck
- eingebunden in Templates
- Wird für bestimmte UI-Elemente eingesetzt
- Bsp: Timematrix-Slider: [http://localhost:8080/viewer/timematrix/](http://localhost:8080/viewer/timematrix/)

#### Test
- überprüfe, ob Dateien geladen werden
- überprüfe Timematix-Slider visuell

### CSS: Leaflet
```
draw
├── draw
│   ├── images
│   │   ├── layers-2x.png
│   │   ├── layers.png
│   │   ├── marker-icon-2x.png
│   │   ├── marker-icon.png
│   │   ├── marker-shadow.png
│   │   ├── spritesheet-2x.png
│   │   ├── spritesheet.png
│   │   └── spritesheet.svg
│   └── leaflet.draw.css
├── extra-markers
│   └── leaflet.extra-markers.min.css
├── images
│   ├── layers-2x.png
│   ├── layers.png
│   ├── marker-icon-2x.png
│   ├── marker-icon.png
│   └── marker-shadow.png
├── img
│   ├── markers_default.png
│   ├── markers_default@2x.png
│   ├── markers_shadow.png
│   └── markers_shadow@2x.png
├── leaflet.css
└── markercluster
    └── MarkerCluster.css
```

#### Besonderheiten

#### Zweck
- rendern von Karten

#### Test
- Wird in allen Templates eingebunden
- prüfe, ob Dateien gefunden werden
- prüfe, ob Karten gerendert werden

### CSS: Mapbox
- *geocoder/mapbox-gl-geocoder.css*
- *mapbox-gl.css*

#### Besonderheiten

#### Zweck

#### Test

### CSS: swagger

#### Besonderheiten

#### Zweck
- Styling Rest Api Doku

#### Test
- prüfe ob es geladen wird
- prüfe Styling
- => [https://viewer.goobi.io/api/swagger/](https://viewer.goobi.io/api/swagger/) 

### CSS: sweetalert
* *sweetalert2.min.css*

#### Besonderheiten

#### Zweck
- Alerts anzeigen
- eingebunden in template.html, templateAdmin.html + templateCrowdsourcing.html
- im Crowdsourcing-Modul z.B. genutzt für Infos beim Speichern von Änderungen

#### Test
- crowdsourcing modul:
  - werden CSS + JS geladen?
  - funktioniert die Anzeige/Styling von Alerts?

### CSS: Swiper
- *swiper-bundel.min.css*

#### Besonderheiten

#### Zweck
- Styling von Slidern, die mit der Swiper Api gebaut werden

#### Test
- wird in template.html + templateAdmin.html eingebunden
- prüfe Styling des HeaderSliders auf der Startseite
- => [http://localhost:8080/viewer/](http://localhost:8080/viewer/)

