# Walkthrough - Favorite Locations Feature

I have successfully implemented the **Favorite Locations** feature. This allows users to save their frequently visited cities and access their weather information quickly.

## Changes Made

### Data & Persistence
- Created [FavoriteLocationStore.kt](file:///C:/Nhan/Android_Group_11---Weather-viewing-app/Code/app/src/main/kotlin/vn/edu/student/weatherviewingapp/data/FavoriteLocationStore.kt) to persist the favorites list using `SharedPreferences` and `kotlinx.serialization`.

### Business Logic
- Updated [WeatherViewModel.kt](file:///C:/Nhan/Android_Group_11---Weather-viewing-app/Code/app/src/main/kotlin/vn/edu/student/weatherviewingapp/viewmodel/WeatherViewModel.kt) to:
    - Maintain a reactive `StateFlow` of favorites.
    - Provide `toggleFavorite()` to add or remove cities.
    - Provide `isFavorite()` to check the current city's status.

### UI Improvements
- Updated [WeatherScreen.kt](file:///C:/Nhan/Android_Group_11---Weather-viewing-app/Code/app/src/main/kotlin/vn/edu/student/weatherviewingapp/ui/screens/WeatherScreen.kt):
    - **Header**: Added a **Star icon** next to the city name. Yellow indicates the city is favorited.
    - **Header**: Added a **Bookmark icon** to open the favorites list.
    - **Favorites Sheet**: Implemented a `ModalBottomSheet` displaying all saved locations with a delete option.
    - **Navigation**: Tapping a favorite location instantly fetches its weather data.

## How to use

1.  **Search for a city** or use GPS to get weather data.
2.  Tap the **Star icon** in the top bar to save the location.
3.  Tap the **Bookmark icon** to view your list of favorites.
4.  Tap any location in the list to switch to its weather view.
5.  Use the **Trash icon** in the favorites list to remove a location.

## Verification Results
- [x] **Build**: Successfully compiled using `gradle assembleDebug`.
- [x] **Persistence**: Favorites are saved and loaded correctly across app restarts.
- [x] **UI**: Responsive transitions and intuitive icons added to the header.
