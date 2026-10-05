# Design System: The Editorial Habit Tracker (Redesign)

## 1. Overview & Creative North Star: "The Fluid Architect"
The vision for this design system is **The Fluid Architect**. We are moving away from the rigid, boxed-in nature of traditional utility apps and toward an editorial, data-forward experience that feels both authoritative and breathable. 

The system rejects the "template" look by utilizing intentional asymmetry, expansive negative space, and deep tonal layering. Instead of overwhelming the user with a grid of checkboxes, we treat habit tracking as a premium journaling experience. Every data point is elevated; every interaction is a soft, tactile response. By blending the precision of "Plus Jakarta Sans" with the soft, organic "Teal" and "Indigo" palette, we create an environment that is focused, encouraging, and impeccably organized.

---

## 2. Colors & Tonal Depth
This system relies on color to define structure, not lines. We use a sophisticated palette of Indigos (`Primary`) and Teals (`Secondary`) to guide the eye without adding visual noise.

### The "No-Line" Rule
**Explicit Instruction:** Designers are prohibited from using 1px solid borders to section content. Boundaries must be defined solely through background color shifts or subtle tonal transitions.
*   *Implementation:* A card (`surface-container-lowest`) sits on a background (`surface`) naturally. Use vertical rhythm to separate ideas, never a horizontal rule.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers, like stacked sheets of fine paper.
*   **Base:** `surface` (#fcf8ff)
*   **Sectioning:** `surface-container-low` (#f5f2ff)
*   **Primary Interaction Surface:** `surface-container-lowest` (#ffffff)
*   **Elevated/Contextual Surface:** `surface-container-highest` (#e4e1ee)

### The "Glass & Gradient" Rule
To move beyond "out-of-the-box" Android, use **Glassmorphism** for floating action buttons (FABs) and navigation overlays.
*   **Glass Specs:** Use `surface` at 70% opacity with a `24px` backdrop-blur. 
*   **Signature Textures:** Apply a linear gradient from `primary` (#3525cd) to `primary-container` (#4f46e5) for high-impact hero moments or habit completion states to provide "soul" and professional polish.

---

## 3. Typography: The Editorial Voice
We utilize **Plus Jakarta Sans** for its modern, geometric clarity and **Inter** for functional, high-legibility labeling.

*   **Display (Display-LG/MD/SM):** Reserved for "Momentum Numbers." When a user sees their 30-day streak, it should feel like a headline in a high-end magazine.
*   **Headlines (Headline-LG/MD/SM):** Used for habit titles. These are the anchors of the page.
*   **Body (Body-LG/MD):** All-purpose content. We favor `body-lg` for habit descriptions to ensure the UI feels "roomy" and premium.
*   **Labels (Label-MD/SM):** Set in **Inter** to differentiate functional data (timestamps, category tags) from editorial content.

---

## 4. Elevation & Depth: Tonal Layering
Traditional shadows are a last resort. We communicate hierarchy through "Tonal Stacking."

### The Layering Principle
Depth is achieved by "stacking" container tiers. Place a `surface-container-lowest` card on top of a `surface-container-low` section to create a soft, natural lift.

### Ambient Shadows
When a "floating" effect is required (e.g., a FAB or a modal), use an **Extra-Diffused Ambient Shadow**:
*   **Blur:** 32px to 48px.
*   **Opacity:** 6%.
*   **Color:** Tint the shadow with `on-surface` (#1b1b24) rather than pure black to keep the light-forward aesthetic clean.

### The "Ghost Border" Fallback
If a container lacks sufficient contrast against its parent, use a **Ghost Border**:
*   **Token:** `outline-variant` (#c7c4d8) at 15% opacity. Never use 100% opacity.

---

## 5. Components & Primitive Styling

### Cards & Lists
*   **The Rule:** Forbid divider lines.
*   **Execution:** Use `xl` (3rem) or `lg` (2rem) corner radii. Separate items in a list using `12px` of vertical whitespace. If grouping is needed, wrap the group in a `surface-container-low` background with a `md` radius.

### Buttons (The Momentum Drivers)
*   **Primary:** Indigo gradient (`primary` to `primary-container`). `full` (pill) radius. White text (`on-primary`). 
*   **Secondary:** `secondary-container` (#86f2e4) with `on-secondary-container` (#006f66) text. High-contrast, teal-forward for secondary actions like "Edit Habit."
*   **Tertiary:** No background. Text-only using `primary`. Use for "Dismiss" or "Back."

### Data Visualization: "The Teal Pulse"
*   **Progress Rings:** Use `secondary` (#006a61) for completed progress and `secondary-container` for the track.
*   **Heatmaps:** Transition from `surface-container-highest` (0%) to `primary` (100%).

### Input Fields
*   **Style:** Minimalist. No bottom line. Use a `surface-container-lowest` background with a `sm` (0.5rem) radius and a `Ghost Border`.
*   **Active State:** The border transitions to `primary` at 40% opacity.

---

## 6. Do's and Don'ts

### Do
*   **DO** use white space as a structural element. If a screen feels cluttered, increase the padding to the `xl` scale.
*   **DO** use `Display-LG` for personal bests. Make the user's data feel like an achievement.
*   **DO** utilize the `teal` (Secondary) accent for "Success" or "Completion" states to evoke a sense of calm and growth.

### Don't
*   **DON'T** use `error` (#ba1a1a) for "habit not completed." Use `surface-variant` instead. Habit tracking should be encouraging, not punishing. Reserved `error` only for system-level failures.
*   **DON'T** use standard Android "CardView" default shadows. They are too heavy for this system’s "Fluid" North Star.
*   **DON'T** use hard 90-degree corners. Everything in this system should feel approachable and soft to the touch.
