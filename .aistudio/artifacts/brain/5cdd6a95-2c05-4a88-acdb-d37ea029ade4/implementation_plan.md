# Kube Architecture & UI Layout Revision Plan

Architectural blueprint to resolve UI navigation and layout issues by establishing a persistent **MainWrapper Scaffold** with bottom navigation, an interactive swipeable **PageView Onboarding Carousel** with dot indicators, a keyboard-safe scrollable **Auth Form**, and a centered **Splash Screen**.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> **Summary of Confirmed Architectural Decisions (Phase 1 Answers)**:
> 1. **Onboarding Direct Transition**: Tapping "Continue" on the Onboarding slider will transition directly to the **Login & Sign Up Screen** (`pushReplacement` semantics, disposing onboarding state).
> 2. **3-Slide Swipeable Onboarding**: The onboarding carousel will feature 3 distinct slides with touch-swipe gestures (`HorizontalPager` / `PageView`) and dynamic dot indicators.
> 3. **Persistent Bottom Navigation Bar**: Anchored in the `bottomNavigationBar` slot of `MainWrapper`, preserving all tab states (scroll positions, filters, active forms) across all 5 core tabs (*Home, Delivery, Map, Wallet, Profile*) via an `IndexedStack` equivalent.

- **Confirmed Decision 1 (Bottom Nav Architecture)**: Dedicated `MainWrapper` scaffold where changing tabs only updates `currentIndex` (0 to 4), keeping all 5 tabs alive in an indexed stack rather than pushing new routes.
- **Confirmed Decision 2 (Auth Form Responsiveness)**: Full `SafeArea` + `SingleChildScrollView` container with 24dp horizontal padding and standard spacer heights to completely eliminate soft-keyboard overflow.
- **Confirmed Decision 3 (Splash Centering)**: Symmetrical `Center` + `Column(Arrangement.Center)` layout with official Kube logo, smooth entrance, and clean replacement transition upon completion.

---

## 1. Overview & Core Concept

This revision restructures the primary navigation and layout hierarchy of **Kube** to ensure seamless state retention and responsive screen adaptation:

- **State Retention**: Couriers navigating between their delivery map, jobdesk queue, and wallet will never lose their scrolled position, active filters, or ongoing inputs.
- **Keyboard Safety**: Couriers entering credentials on various device screen sizes and aspect ratios will experience zero UI clipping or pixel overflow when the virtual keyboard expands.
- **Intuitive Onboarding**: New users can freely swipe between value propositions with clear visual feedback via animated dot indicators.

---

## 2. User Experience & Visual Design

### 1. Centered Splash Screen
- **Composition**: Centered layout using `Box(contentAlignment = Alignment.Center)` and `Column(verticalArrangement = Arrangement.Center)`.
- **Branding**: Official Kube ice crystal logo (`logo_kube.png`) rendered with `BoxFit.contain` (`ContentScale.Fit`) and a 180dp proportional size.
- **Transition**: Single-run 1.8s progress bar; once completed, invokes route replacement directly to Onboarding without lingering background tasks.

### 2. Swipeable Onboarding Carousel (`PageView` / `HorizontalPager`)
- **Gestures**: Horizontal drag/swipe with momentum and spring snapping between 3 slides:
  1. *Cold Chain Delivery*: Temperature-controlled ice transport.
  2. *Hygienic Ice Quality*: Certified pure ice blocks and crystal cubes.
  3. *Fast, Safe, and Always On Time*: Rapid delivery to cafes, warungs, and restaurants.
- **Indicator**: Active elongated pill indicator alongside subtle round dots synchronized with the current page index.
- **Action**: Floating bottom bar with dark "Continue" button and slide indicator. On slide 3 or button click, transitions cleanly via route replacement to the Auth screen.

### 3. Keyboard-Safe Auth Screen (Login & Sign Up)
- **Container Structure**: `SafeArea` -> `SingleChildScrollView` with `imePadding()` and `24dp` horizontal content padding.
- **Input Visibility**: Black87 (`Color(0xDE000000)`) text style on all input fields for maximum readability against white cards.
- **Consistent Rhythm**: Uniform `SizedBox(height = 16.dp)` / `Spacer` spacing between fields to prevent layout jumping when switching between Login and Sign Up tabs.

### 4. MainWrapper & Persistent Bottom Navigation
- **Architecture**: A root scaffold housing:
  - `body`: `IndexedStack` hosting `[DashboardScreen, DeliveryListScreen, DeliveryMapScreen, WalletScreen, ProfileScreen]`.
  - `bottomNavigationBar`: Floating glass bottom navigation bar updating `currentIndex` on tab selection.
- **State Integrity**: Filter chips in Delivery, scroll positions in Dashboard, and active map tracking remain 100% intact while switching tabs.

---

## 3. Technical Architecture & Data Strategy

### Navigation & Layout Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                          MainActivity Root                             │
└────────────────────────────────────────────────────────────────────────┘
                                    │
           ┌────────────────────────┼────────────────────────┐
           ▼                        ▼                        ▼
┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────────┐
│     SplashScreen     │ │   OnboardingScreen   │ │      AuthScreen      │
│  (Centered Branding) │ │ (3-Slide PageView &  │ │  (SafeArea & Scroll  │
│  [pushReplacement]   │ │    Dot Indicator)    │ │   Black87 Contrast)  │
└──────────────────────┘ └──────────────────────┘ └──────────────────────┘
           │                        │                        │
           └────────────────────────┴────────────────────────┘
                                    │ (Auth Success)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                              MainWrapper                               │
│              Scaffold(bottomNavigationBar = KubeBottomNav)             │
├────────────────────────────────────────────────────────────────────────┤
│                           IndexedStack                                 │
│  ┌──────────────┬──────────────┬──────────────┬──────────────┬──────┐  │
│  │ Index 0:     │ Index 1:     │ Index 2:     │ Index 3:     │Index4│  │
│  │ Dashboard    │ Delivery     │ DeliveryMap  │ Wallet       │Profil│  │
│  │ (Jobdesk)    │ (Queue/Chip) │ (Vector Map) │ (Earnings)   │(User)│  │
│  └──────────────┴──────────────┴──────────────┴──────────────┴──────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Verification Plan

1. **Compilation Verification**: Execute `compile_applet` to confirm zero syntax or layout errors.
2. **Navigation Stack Verification**: Confirm that switching through all 5 tabs preserves state without re-creating views or losing active filters.
3. **Onboarding Swipe Verification**: Test 3-slide horizontal swipe gesture, dot indicator synchronization, and direct transition to Auth.
4. **Keyboard Inset Verification**: Verify that virtual keyboard activation on the Auth form scrolls smoothly without any layout overflow.
5. **Automated Unit Testing**: Run `gradle :app:testDebugUnitTest` to guarantee all existing test cases pass.
