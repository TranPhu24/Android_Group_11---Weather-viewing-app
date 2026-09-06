# Implementation Plan - Favorite Locations Feature

This plan outlines the addition of a "Favorite Locations" feature, allowing users to save, manage, and quickly access weather information for their most-visited cities.

## Proposed Changes

### Data Layer

#### [NEW] [FavoriteLocationStore.kt](file:///C:/Nhan/Android_Group_11---Weather-viewing-app/Code/app/src/main/kotlin/vn/edu/student/weatherviewingapp/data/FavoriteLocationStore.kt)
Create a new class to persist the list of favorite locations using `SharedPreferences` and `kotlinx.serialization`.

#### [MODIFY] [WeatherRepository.kt](file:///C:/Nhan/Android_Group_11---Weather-viewing-app/Code/app/src/main/kotlin/vn/edu/student/weatherviewingapp/repository/WeatherRepository.kt)
Add methods to handle favorite locations by interacting with `FavoriteLocationStore`.

### Business Logic

#### [MODIFY] [WeatherViewModel.kt](file:///C:/Nhan/Android_Group_11---Weather-viewing-app/Code/app/src/main/kotlin/vn/edu/student/weatherviewingapp/viewmodel/WeatherViewModel.kt)
- Add a `StateFlow` for the list of favorite locations.
- Implement `toggleFavorite(location: LocationResult)` to add or remove a city.
- Add logic to check if the currently displayed city is in the favorites list.

### UI Layer

#### [MODIFY] [WeatherScreen.kt](file:///C:/Nhan/Android_Group_11---Weather-viewing-app/Code/app/src/main/kotlin/vn/edu/student/weatherviewingapp/ui/screens/WeatherScreen.kt)
- **Top Bar**: Add a "Star" icon to the current city's title to allow users to favorite/unfavorite the current location.
- **Top Bar**: Add a "List" or "Bookmark" icon to open a list of favorite cities.
- **Favorites List**: Implement a `ModalBottomSheet` that displays the list of favorite cities. Tapping a city will fetch its weather and close the sheet.
- **Search Suggestions**: (Optional) Highlight cities that are already in favorites.

## Verification Plan

### Manual Verification
1. **Add Favorite**: Search for a city, view its weather, and tap the Star icon. Verify the icon changes to "filled".
2. **Remove Favorite**: Tap the Star icon again. Verify it changes back to "outline".
3. **Quick Access**: Tap the Favorites icon in the top bar. Verify the `ModalBottomSheet` opens with the list of saved cities.
4. **Navigation**: Tap a city in the favorites list. Verify the app fetches and displays weather for that city.
5. **Persistence**: Close the app completely and reopen it. Verify that the favorites list is preserved.
6. **GPS Location**: Check if the Star icon works correctly for locations found via GPS.

## User Review Required

> [!NOTE]
> The UI will use a `ModalBottomSheet` for the favorites list to keep the main screen clean while providing quick access. Do you prefer this over a side navigation drawer?
