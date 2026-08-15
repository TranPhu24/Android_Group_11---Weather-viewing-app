# Tasks - Advanced Location Search (Districts & GPS)

- [x] **Phase 1: Dependencies & Permissions**
    - [x] Add `play-services-location` to `libs.versions.toml` and `app/build.gradle.kts`
    - [x] Add location permissions to `AndroidManifest.xml`
    - [x] Run Gradle Sync

- [x] **Phase 2: Data Models & API**
    - [x] Update `WeatherData.kt` with `LocationResult`
    - [x] Update `WeatherApi.kt` with Geocoding endpoints
    - [x] Update `WeatherRepository.kt` with search and reverse geocoding logic

- [x] **Phase 3: ViewModel Enhancements**
    - [x] Add location search logic and suggestions state
    - [x] Add GPS location fetching logic
    - [x] Update `fetchWeather` to support coordinate-based fetching

- [x] **Phase 4: UI Enhancements**
    - [x] Update search bar to show suggestions dropdown
    - [x] Add "My Location" button to the header
    - [x] Handle permission requests in UI

- [x] **Phase 5: Verification**
    - [x] Build and run
    - [x] Test district search (e.g., "Quan 9")
    - [x] Test GPS location fetching
