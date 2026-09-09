# Convention Plugins

From [NowInAndroid](https://github.com/android/nowinandroid)

`commit 15cc536a44df88c2f04a2bde7484485c6c0015c9 'Upgrade to spotless 8.3.0'`

The `build-logic` folder defines project-specific convention plugins, used to keep a single
source of truth for common module configurations.

Current list of convention plugins:

- [`tblite.android.application`](convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt),
  [`tblite.android.library`](convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt),
  [`tblite.android.test`](convention/src/main/kotlin/AndroidTestConventionPlugin.kt):
  Configures common Android and Kotlin options.
- [`tblite.android.application.compose`](convention/src/main/kotlin/AndroidApplicationComposeConventionPlugin.kt),
  [`tblite.android.library.compose`](convention/src/main/kotlin/AndroidLibraryComposeConventionPlugin.kt):
  Configures Jetpack Compose options
