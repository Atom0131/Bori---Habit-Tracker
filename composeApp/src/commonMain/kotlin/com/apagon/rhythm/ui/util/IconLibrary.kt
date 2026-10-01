package com.apagon.rhythm.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.unit.dp

/**
 * Desktop port of androidMain's `habitIconLibrary`/`todoIconList` (`ui/util/Extensions.kt`).
 * Verbatim port, not a curated subset: `compose.materialIconsExtended` (the JetBrains
 * Compose Multiplatform artifact — distinct from, and not the same dependency as, the AndroidX
 * `material-icons-extended` this project's `ref_notes` previously recorded as unreachable) resolves
 * and compiles cleanly here, confirmed by a direct build with `Icon(Icons.Filled.Star, ...)` before
 * writing this file — so the icon-availability blocker that justified a reduced icon set no longer
 * applies, and habit/to-do icon pickers can offer the same icons Android does.
 */

/** Port of androidMain's `TriathlonIcon` — composited from three stock glyphs rather than a
 * hand-drawn path, so it ports unchanged (`ImageVector.Builder` is pure Compose, no Android API). */
private val TriathlonIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Triathlon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Triangle: swimmer top-left, cyclist bottom-left, runner right-center
        val placements = listOf(
            Triple(Icons.Rounded.Pool,                       0f,  0f ),
            Triple(Icons.AutoMirrored.Filled.DirectionsBike, 0f,  12f),
            Triple(Icons.AutoMirrored.Filled.DirectionsRun,  12f, 6f ),
        )
        for ((icon, tx, ty) in placements) {
            val s = 12f / icon.viewportWidth
            addGroup(scaleX = s, scaleY = s, translationX = tx, translationY = ty)
            icon.root.addToBuilder(this)
            clearGroup()
        }
    }.build()
}

/** Expanded icon library for habit selection. Each entry is (label, icon). */
internal val habitIconLibrary: List<Pair<String, ImageVector>> = listOf(
    // ── Sports ──────────────────────────────────────────────────
    "Triathlon"      to TriathlonIcon,
    "Running"        to Icons.AutoMirrored.Filled.DirectionsRun,
    "Cycling"        to Icons.AutoMirrored.Filled.DirectionsBike,
    "Tennis"         to Icons.Default.SportsTennis,
    "Soccer"         to Icons.Rounded.SportsSoccer,
    "Football"       to Icons.Default.SportsFootball,
    "Swimming"       to Icons.Rounded.Pool,
    "Reading"        to Icons.AutoMirrored.Filled.MenuBook,
    "Hiking"         to Icons.Rounded.Hiking,
    "Yoga"           to Icons.Default.SelfImprovement,
    // ── Hobbies ─────────────────────────────────────────────────
    "Photography"    to Icons.Default.Camera,
    "Art"            to Icons.Default.Palette,
    "Cooking"        to Icons.Rounded.Restaurant,
    "Gaming"         to Icons.Default.SportsEsports,
    "Music"          to Icons.Default.MusicNote,
    "Recording"      to Icons.Rounded.Mic,
    "Science"        to Icons.Default.Science,
    "Crafts"         to Icons.Default.ContentCut,
    "Puzzle"         to Icons.Default.Extension,
    "Piano"          to Icons.Default.Piano,
    "Meditation"     to Icons.Default.Psychology,
    // ── Active & Social ─────────────────────────────────────────
    "Chat"           to Icons.AutoMirrored.Filled.Chat,
    "Language"       to Icons.Rounded.Translate,
    "Gym"            to Icons.Default.FitnessCenter,
    "Basketball"     to Icons.Rounded.SportsBasketball,
    "Volleyball"     to Icons.Rounded.SportsVolleyball,
    "Surfing"        to Icons.Default.Surfing,
    "Skiing"         to Icons.Rounded.DownhillSkiing,
    "Skating"        to Icons.Default.IceSkating,
    "Skateboarding"  to Icons.Default.Skateboarding,
    "Rowing"         to Icons.Default.Rowing,
    // ── Outdoors & Travel ────────────────────────────────────────
    "Camping"        to Icons.Default.Cabin,
    "Campfire"       to Icons.Default.LocalFireDepartment,
    "Travel"         to Icons.Default.Flight,
    "Globe"          to Icons.Rounded.Public,
    "Laptop"         to Icons.Default.LaptopMac,
    "Coffee"         to Icons.Rounded.EmojiFoodBeverage,
    "Hydration"      to Icons.Default.WaterDrop,
    "Garden"         to Icons.Default.LocalFlorist,
    "Nature"         to Icons.Default.Park,
    // ── Daily Health ─────────────────────────────────────────────
    "Sleep"          to Icons.Rounded.Bedtime,
    "Schedule"       to Icons.Rounded.Schedule,
    "Calendar"       to Icons.Rounded.CalendarMonth,
    "Checklist"      to Icons.Rounded.Checklist,
    "Savings"        to Icons.Default.Savings,
    "Eco"            to Icons.Rounded.Eco,
    "Pets"           to Icons.Rounded.Pets,
    "Nutrition"      to Icons.Default.LocalDining,
    "Medical"        to Icons.Rounded.MedicalServices,
    "Wellness"       to Icons.Rounded.Spa,
    // ── Nature ───────────────────────────────────────────────────
    "Sun"            to Icons.Rounded.WbSunny,
    "Moon"           to Icons.Rounded.DarkMode,
    "Cloud"          to Icons.Default.Cloud,
    "Rain"           to Icons.Default.Water,
    "Lightning"      to Icons.Rounded.Bolt,
    "Forest"         to Icons.Rounded.Forest,
    "Leaf"           to Icons.Rounded.EnergySavingsLeaf,
    "Snow"           to Icons.Default.AcUnit,
    "Mountain"       to Icons.Default.Terrain,
    "Grass"          to Icons.Rounded.Grass,
    // ── Lifestyle & Productivity ──────────────────────────────────
    "Heart"          to Icons.Default.Favorite,
    "Star"           to Icons.Default.Star,
    "Settings"       to Icons.Default.Settings,
    "Lightbulb"      to Icons.Rounded.Lightbulb,
    "TV"             to Icons.Rounded.Tv,
    "Study"          to Icons.Default.School,
    "Pencil"         to Icons.Default.Create,
    "Notes"          to Icons.Rounded.NoteAlt,
    "Print"          to Icons.Default.Print,
    "Shopping"       to Icons.Default.ShoppingCart,
    "Gift"           to Icons.Rounded.Redeem,
    "Party"          to Icons.Default.Celebration,
    // ── Home & Wellness ───────────────────────────────────────────
    "Theater"        to Icons.Default.TheaterComedy,
    "Shower"         to Icons.Default.Shower,
    "Recycle"        to Icons.Rounded.Recycling,
    "Cleaning"       to Icons.Default.CleaningServices,
    "Achievement"    to Icons.Default.EmojiEvents,
    "Streak"         to Icons.Rounded.Whatshot,
    // ── Transport ─────────────────────────────────────────────────
    "Car"            to Icons.Rounded.DirectionsCar,
    "Bus"            to Icons.Rounded.DirectionsBus,
    "Train"          to Icons.Default.Train,
    "Walking"        to Icons.AutoMirrored.Rounded.DirectionsWalk,
    "Motorcycle"     to Icons.Default.Motorcycle,
    "Explore"        to Icons.Default.Explore,
    // ── Goals & More ──────────────────────────────────────────────
    "Volunteer"      to Icons.Default.VolunteerActivism,
    "Tools"          to Icons.Default.Build,
    "Work"           to Icons.Default.Work,
    "Mood"           to Icons.Default.Mood,
    "Golf"           to Icons.Default.SportsGolf,
    "Martial Arts"   to Icons.Default.SportsMma,
    // ── More Sports & Outdoor ────────────────────────────────────────────
    "Snowboarding"   to Icons.Default.Snowboarding,
    "Handball"       to Icons.Default.SportsHandball,
    "Hockey"         to Icons.Default.SportsHockey,
    "Rugby"          to Icons.Default.SportsRugby,
    "Kayaking"       to Icons.Default.Kayaking,
    "Paragliding"    to Icons.Default.Paragliding,
    "Sailing"        to Icons.Default.Sailing,
    "Nordic Walk"    to Icons.Default.NordicWalking,
    "Snowshoeing"    to Icons.Default.Snowshoeing,
    "Sledding"       to Icons.Default.Sledding,
    "Cricket"        to Icons.Default.SportsCricket,
    // ── Making & DIY ────────────────────────────────────────────────────
    "3D View"        to Icons.Default.ViewInAr,
    "Measure"        to Icons.Default.SquareFoot,
    "Layers"         to Icons.Default.Layers,
    "Engineering"    to Icons.Default.Engineering,
    "Architecture"   to Icons.Default.Architecture,
    "Handyman"       to Icons.Default.Handyman,
    "Carpenter"      to Icons.Default.Carpenter,
    "Precision"      to Icons.Default.PrecisionManufacturing,
    "Painting"       to Icons.Default.Brush,
    "Fashion"        to Icons.Default.Checkroom,
    // ── Sleep & Recovery ────────────────────────────────────────────────
    "Alarm"          to Icons.Default.Alarm,
    "Bed"            to Icons.Default.KingBed,
    "Hotel"          to Icons.Default.Hotel,
    "Rest"           to Icons.Default.NightShelter,
    // ── Food & Drink ────────────────────────────────────────────────────
    "Breakfast"      to Icons.Default.BreakfastDining,
    "Lunch"          to Icons.Default.LunchDining,
    "Dinner"         to Icons.Default.DinnerDining,
    "Grill"          to Icons.Default.OutdoorGrill,
    "Cake"           to Icons.Default.Cake,
    "Ramen"          to Icons.Default.RamenDining,
    "Bar"            to Icons.Default.LocalBar,
    "Wine"           to Icons.Default.WineBar,
    "Blender"        to Icons.Default.Blender,
    // ── Health & Tracking ───────────────────────────────────────────────
    "Heart Rate"     to Icons.Default.MonitorHeart,
    "Health"         to Icons.Default.HealthAndSafety,
    "Vaccine"        to Icons.Default.Vaccines,
    "Medication"     to Icons.Default.Medication,
    "Blood Type"     to Icons.Default.Bloodtype,
    "Hot Tub"        to Icons.Default.HotTub,
    "Lab"            to Icons.Default.Biotech,
    "Skin Care"      to Icons.Default.FaceRetouchingNatural,
    // ── Finance ─────────────────────────────────────────────────────────
    "Trending"       to Icons.AutoMirrored.Filled.TrendingUp,
    "Bank"           to Icons.Default.AccountBalance,
    "Calculator"     to Icons.Default.Calculate,
    "Receipt"        to Icons.AutoMirrored.Filled.ReceiptLong,
    "Chart"          to Icons.AutoMirrored.Filled.ShowChart,
    // ── Tech & Digital ──────────────────────────────────────────────────
    "Podcast"        to Icons.Default.Podcasts,
    "Keyboard"       to Icons.Default.Keyboard,
    "Desktop"        to Icons.Default.DesktopWindows,
    "Headphones"     to Icons.Default.Headphones,
    "Phone"          to Icons.Default.PhoneAndroid,
    "Videogame"      to Icons.Default.VideogameAsset,
    // ── Learning & Culture ──────────────────────────────────────────────
    "History"        to Icons.Default.HistoryEdu,
    "Stories"        to Icons.Default.AutoStories,
    "Library"        to Icons.AutoMirrored.Filled.LibraryBooks,
    "News"           to Icons.Default.Newspaper,
    // ── Home & Social ───────────────────────────────────────────────────
    "Fireplace"      to Icons.Default.Fireplace,
    "Leaderboard"    to Icons.Default.Leaderboard,
    "Groups"         to Icons.Default.Groups,
    // ── Programming & Tech ──────────────────────────────────────────────
    "Code"           to Icons.Default.Code,
    "Terminal"       to Icons.Default.Terminal,
    "Bug Fix"        to Icons.Default.BugReport,
    "Database"       to Icons.Default.Storage,
    "API"            to Icons.Default.Api,
    "Git Tree"       to Icons.Default.AccountTree,
    "Dev Mode"       to Icons.Default.DeveloperMode,
    "Memory"         to Icons.Default.Memory,
    // ── Fiber Arts ──────────────────────────────────────────────────────
    "Knitting"       to Icons.Default.AllInclusive,
    "Crochet"        to Icons.Default.Loop,
    "Weaving"        to Icons.Default.GridOn,
    "Textile"        to Icons.Default.Texture,
    "Pattern Work"   to Icons.Default.Dashboard,
    // ── Writing & Journaling ────────────────────────────────────────────
    "Journal"        to Icons.Default.EditNote,
    "Diary"          to Icons.AutoMirrored.Filled.Note,
    "Calligraphy"    to Icons.Default.Gesture,
    "Blog"           to Icons.AutoMirrored.Filled.Article,
    "Annotation"     to Icons.Default.BorderColor,
    // ── Music Making ────────────────────────────────────────────────────
    "Album"          to Icons.Default.Album,
    "Lyrics"         to Icons.Default.Lyrics,
    "Music Lib"      to Icons.Default.LibraryMusic,
    "Queue Music"    to Icons.AutoMirrored.Filled.QueueMusic,
    "Equalizer"      to Icons.Default.GraphicEq,
    // ── Audio & Listening ───────────────────────────────────────────────
    "Audiobook"      to Icons.Default.RecordVoiceOver,
    "Radio"          to Icons.Default.Radio,
    "Voice Rec"      to Icons.Default.KeyboardVoice,
    "On Demand"      to Icons.Default.OndemandVideo,
    // ── Studying ────────────────────────────────────────────────────────
    "Math"           to Icons.Default.Functions,
    "Quiz"           to Icons.Default.Quiz,
    "Reading Room"   to Icons.Default.LocalLibrary,
    "Grading"        to Icons.AutoMirrored.Filled.Grading,
    "Spellcheck"     to Icons.Default.Spellcheck,
    // ── Sculpting & Fine Arts ───────────────────────────────────────────
    "Sculpting"      to Icons.Default.Category,
    "Pottery"        to Icons.Default.Interests,
    "Art Studio"     to Icons.Default.FormatPaint,
    "Color Mix"      to Icons.Default.ColorLens,
    // ── Racing & Events ─────────────────────────────────────────────────
    "Finish Line"    to Icons.Default.Flag,
    "Stopwatch"      to Icons.Default.AvTimer,
    "Speed Run"      to Icons.Default.Speed,
    "Score"          to Icons.Default.SportsScore,
    "Gymnastics"     to Icons.Default.SportsGymnastics,
    "Roller Skate"   to Icons.Default.RollerSkating,
    "Kabaddi"        to Icons.Default.SportsKabaddi,
    "Adaptive"       to Icons.Default.AccessibilityNew,
    // ── Nature & Outdoors ───────────────────────────────────────────────
    "Landscape"      to Icons.Default.Landscape,
    "Beach"          to Icons.Default.BeachAccess,
    "Nature Walk"    to Icons.Default.NaturePeople,
    "Beekeeper"      to Icons.Default.Hive,
    "Stars"          to Icons.Default.Stars,
    "Night Sky"      to Icons.Default.Nightlight,
    "Night Stroll"   to Icons.Default.NightsStay,
    // ── Social & Community ──────────────────────────────────────────────
    "People"         to Icons.Default.EmojiPeople,
    "Handshake"      to Icons.Default.Handshake,
    "Dog Walk"       to Icons.Default.EmojiNature,
    "Travel Plan"    to Icons.Default.FlightTakeoff,
    "Map"            to Icons.Default.Map,
    // ── Wellness & Mind ─────────────────────────────────────────────────
    "Weight Track"   to Icons.Default.Scale,
    "Fine Tune"      to Icons.Default.Tune,
    "Happiness"      to Icons.Default.SentimentVerySatisfied,
    "Balance"        to Icons.Default.Balance,
    "Faces"          to Icons.Default.TagFaces,
    // ── Productivity & Goals ────────────────────────────────────────────
    "Task Done"      to Icons.Default.TaskAlt,
    "Assignment"     to Icons.Default.AssignmentTurnedIn,
    "Event"          to Icons.Default.Event,
    "Today"          to Icons.Default.Today,
    "Launch"         to Icons.Default.RocketLaunch,
    // ── Cards, Games & DIY ──────────────────────────────────────────────
    "Trading Cards"  to Icons.Default.Style,
    "Board Game"     to Icons.Default.Widgets,
    "Dice"           to Icons.Default.Casino,
    "PIN"            to Icons.Default.Pin,
    "Deck"           to Icons.Default.Deck,
    "Plumbing"       to Icons.Default.Plumbing,
    "Smartwatch"     to Icons.Default.Watch,
)

private fun VectorGroup.addToBuilder(builder: ImageVector.Builder) {
    for (node in this) {
        when (node) {
            is VectorPath -> builder.addPath(
                pathData        = node.pathData,
                pathFillType    = node.pathFillType,
                fill            = node.fill,
                fillAlpha       = node.fillAlpha,
                stroke          = node.stroke,
                strokeAlpha     = node.strokeAlpha,
                strokeLineWidth = node.strokeLineWidth,
                strokeLineCap   = node.strokeLineCap,
                strokeLineJoin  = node.strokeLineJoin,
                strokeLineMiter = node.strokeLineMiter,
                trimPathStart   = node.trimPathStart,
                trimPathEnd     = node.trimPathEnd,
                trimPathOffset  = node.trimPathOffset
            )
            is VectorGroup -> {
                builder.addGroup(
                    rotate       = node.rotation,
                    pivotX       = node.pivotX,
                    pivotY       = node.pivotY,
                    scaleX       = node.scaleX,
                    scaleY       = node.scaleY,
                    translationX = node.translationX,
                    translationY = node.translationY,
                    clipPathData = node.clipPathData
                )
                node.addToBuilder(builder)
                builder.clearGroup()
            }
            else -> {}
        }
    }
}

/** Port of androidMain's `todoIconList` (`ui/util/Extensions.kt`). */
internal val todoIconList: List<ImageVector> = listOf(
    Icons.Filled.Star,
    Icons.Filled.Favorite,
    Icons.Filled.Work,
    Icons.Filled.Home,
    Icons.Filled.CheckCircle,
    Icons.Filled.WaterDrop
)
