# Fjordvær

A small Android app that shows wind, current, wave and rain forecasts for the Oslofjord on a
map. Pick a layer, set how strong the conditions need to be, and the map shows
where that's the case right now.

## Features

- **Four forecast layers:** wind, current, waves and rain, from MET Norway's GRIB forecast for
  the Oslofjord.
- **Arrows and dots on the map:** wind and current are shown as arrows pointing the way they
  flow, and waves and rain as dots. Each marker shows its value.
- **Your own thresholds:** each layer has a slider. Only spots above it stay on the map, and
  they're coloured by how far above it they are (just above, well above or far above). Your
  thresholds are remembered between launches.
- **Strongest value now:** the bottom sheet shows the strongest value right now anywhere in
  the fjord, and how many spots are on the map. Tap the handle to see all four layers at once.
- **Light and dark theme:** the map follows the system theme.

The forecast is downloaded once and cached, so moving a slider updates the map instantly
without fetching anything again. When the hour changes, the app switches to that hour's
forecast the next time it's opened.

## How it works

1. `GribDataSource` downloads the three GRIB files (weather, current, waves) from
   [api.met.no](https://api.met.no/weatherapi/gribfiles/1.1/documentation) and keeps them on
   disk for an hour.
2. `GribParser` reads them with NetCDF-Java and picks the forecast step for the current hour.
3. `GribOverlayCalculator` turns the grids into map markers: it keeps the points above your
   threshold and spaces them out so they don't overlap, keeping the strongest ones.
4. `GribMarkers` draws those markers on a [MapLibre](https://maplibre.org) map, using
   [OpenFreeMap](https://openfreemap.org) tiles recoloured to match the app.

## Project structure

```
model/          Data classes for the forecast, map markers and thresholds
data/grib/      Downloading, parsing and caching the GRIB files
ui/grib/        GribOverlayCalculator and GribViewModel
ui/map/         The map, its colours, and the weather markers
ui/components/  Header, layer buttons, bottom sheet, layer icons
ui/theme/       Colours and typography (Fraunces + IBM Plex Sans)
di/             A small container that wires the app together
```

## Running it

Open the project in Android Studio and press Run. There are no API keys to set up: the map
tiles are free and keyless, and MET Norway only asks for an identifying User-Agent.

Unit tests are in `app/src/test` and run on the plain JVM:

```
./gradlew testDebugUnitTest
```

## Data

Weather data from [MET Norway](https://api.met.no), licensed under
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Map data © OpenStreetMap
contributors, served by OpenFreeMap.
