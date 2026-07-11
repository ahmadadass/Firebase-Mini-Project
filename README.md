# Recipe Book

Recipe Book is an Android Firebase application that allows users to share, discover, search, save, edit, and delete cooking recipes.

## App Description

Recipe Book is a social-style recipe sharing app. Users can register, log in, add their own recipes with images, browse recipes from other users, search recipes by name, filter recipes by category, and view full recipe details.

## Features

- User registration using Firebase Authentication
- User login using Firebase Authentication
- Remember Me option
- Save user profile data in Firestore
- View all recipes in the home screen
- Search recipes by name
- Filter recipes by category using TabLayout
- Add new recipes
- Edit recipes created by the logged-in user
- Delete recipes created by the logged-in user
- View recipe details
- View recipe ingredients and steps
- Open recipe YouTube video link
- View user profile information
- View recipes created by the logged-in user
- Logout from the account
- Light and dark theme support
- Toast messages for user actions

## Screens

- Login Screen
- Register Screen
- Home Screen
- Add Recipe Screen
- Edit Recipe Screen
- Recipe Details Screen
- Profile Screen

## Technologies Used

- Java
- Android Studio
- Firebase Authentication
- Firebase Firestore
- Material Design Components
- Recycler/List views
- SharedPreferences

## Firebase Collections

### users

Stores user information such as:

- name
- email
- country
- profile image

### recipes

Stores recipe information such as:

- recipe name
- description
- ingredients
- steps
- category
- video URL
- image
- creator user ID

## How to Run

1. Open the project in Android Studio.
2. Connect the project to Firebase.
3. Make sure `google-services.json` is inside the `app` folder.
4. Sync the Gradle files.
5. Run the app on an emulator or Android device.

## Notes

This project was created as a Firebase mini project for a Recipe Book Android application.
