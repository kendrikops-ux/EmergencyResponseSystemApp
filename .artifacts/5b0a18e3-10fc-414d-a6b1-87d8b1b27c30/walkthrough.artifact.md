# Emergency Response System - Walkthrough

We have successfully updated the **Emergency Response System** application to request runtime location permissions with a system popup dialog (`Allow` / `Deny`) when accessing device GPS.

## 🚀 Key Updates Implemented

### 1. Runtime Location Permission Popup (`ReporterDashboard.kt`)
- **System Permission Prompt**: Upon opening the Resident Portal, the app automatically triggers the Android runtime permission dialog asking the user to **Allow** or **Deny** location access.
- **Permission Handlers**:
  - If **Allowed**: Automatically queries real device GPS and reverse geocodes the exact street address onto the Google Maps style card.
  - If **Denied**: Displays a prompt on the map card with a "Grant GPS" button so the user can grant permission at any time.

### 2. Google Maps Style Visual Card & Camera Capture
- Road grid background, red center map pin, search bar, and bottom info card showing live device street address.
- Direct camera photo capture (`TakePicturePreview()`).

### 3. Core Portals & Dashboards
- **Admin Dashboard**: Combined responder/vehicle unit assignment, telemetry, finished reports, and separated user directory.
- **Responder Dashboard**: Top bar formatted as `[Full Name] - [Role]` with high-priority alarm volume override (`AudioManager.STREAM_ALARM`).

## 🛠️ Verification & Build Status
- **Build Result**: Successfully compiled and built (`app:assembleDebug`) with 0 errors.
- **Architecture**: Kotlin Coroutines, StateFlow reactive state management, Material 3 UI design.
