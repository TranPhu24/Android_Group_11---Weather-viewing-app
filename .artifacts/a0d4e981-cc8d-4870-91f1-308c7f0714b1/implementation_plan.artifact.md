# Implementation Plan - Advanced Location Search (Districts & GPS)

Enhance the app to support searching for specific districts and wards in Vietnam, and add support for fetching weather based on the user's current GPS location.

## User Review Required

> [!IMPORTANT]
> To support searching for specific districts (like "Quận 9" or "Quận Hoàn Kiếm"), we will use OpenWeather's Geocoding API. For the most accurate district-level weather, the app will also request **Location Permissions** to use GPS.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/kotlin/gradle/libs.versions.toml)
- Add `play-services-location` for GPS support.

#### [MODIFY] [build.gradle.kts (app)](file:///C:/kotlin/app/build.gradle.kts)
- Add the location dependency.

### Data Layer

#### [MODIFY] [WeatherData.kt](file:///C:/kotlin/app/src/main/java/vn/edu/student/weatherviewingapp/data/WeatherData.kt)
- Add `LocationResult` model for Geocoding API responses (includes name, lat, lon, country, and local names in Vietnamese).

#### [MODIFY] [WeatherApi.kt](file:///C:/kotlin/app/src/main/java/vn/edu/student/weatherviewingapp/data/WeatherApi.kt)
- Add `@GET("geo/1.0/direct")` to search for locations by name.
- Add `@GET("geo/1.0/reverse")` to find the name of a location from GPS coordinates.

#### [MODIFY] [WeatherRepository.kt](file:///C:/kotlin/app/src/main/java/vn/edu/student/weatherviewingapp/data/WeatherRepository.kt)
- Add methods to search for locations and perform reverse geocoding.

### UI Layer

#### [MODIFY] [AndroidManifest.xml](file:///C:/kotlin/app/src/main/AndroidManifest.xml)
- Add `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` permissions.

#### [MODIFY] [WeatherViewModel.kt](file:///C:/kotlin/app/src/main/java/vn/edu/student/weatherviewingapp/WeatherViewModel.kt)
- Implement `searchLocations(query)` to provide suggestions as the user types.
- Implement `fetchWeatherForLocation(lat, lon, name)` to fetch data for a specific coordinates.
- Add logic to request and handle current GPS location.

#### [MODIFY] [WeatherScreen.kt](file:///C:/kotlin/app/src/main/java/vn/edu/student/weatherviewingapp/WeatherScreen.kt)
- Update search bar to show a list of location results (e.g., "Quận 9, Ho Chi Minh City, VN").
- Add a "My Location" button (GPS icon) to automatically fetch weather for the current district.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure compilation.

### Manual Verification
- Deploy to a device.
- Test searching for specific Vietnamese districts (e.g., "Quan 1", "Ba Dinh").
- Click the GPS icon and verify it asks for permission and fetches local weather.
