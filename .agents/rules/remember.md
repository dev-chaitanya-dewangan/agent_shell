---
trigger: always_on
---

whatever things you learn should be saved in the proper form in the rules section where other ai doesnt makes mistakes C:\Users\Chaitanya\Documents\BRAINSTROMING\019-app_agent_shell\.agents\rules\remember.md
in this file which relative path is this .agents\rules\remember.md 

## AI System Learnings:
- **Android App Queries**: On Android 11+, you must declare `<queries>` block or request `android.permission.QUERY_ALL_PACKAGES` in `AndroidManifest.xml` for `PackageManager.getPackageInfo` to accurately list installed apps. Otherwise, third-party apps will incorrectly show as uninstalled.
- **Compose Navigation**: Whenever creating a new screen that takes the user away from a main flow (like detail screens), always pass an `onBack: () -> Unit` parameter and render a prominent Back button in the UI, following the back navigation gesture/breadcrumb UX pattern.
- **Hilt in Compose**: ViewModels in standalone UI components or Scaffold Overlays should be injected using `hiltViewModel()` to maintain scope lifecycle.