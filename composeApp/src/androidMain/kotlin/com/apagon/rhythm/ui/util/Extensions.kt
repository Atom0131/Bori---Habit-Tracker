package com.apagon.rhythm.ui.util
import com.apagon.rhythm.core.time.*

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.text.format.DateFormat
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Cabin
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.IceSkating
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Motorcycle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Rowing
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Shower
import androidx.compose.material.icons.filled.Skateboarding
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Blender
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.BreakfastDining
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Carpenter
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.HotTub
import androidx.compose.material.icons.filled.Kayaking
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KingBed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.NightShelter
import androidx.compose.material.icons.filled.NordicWalking
import androidx.compose.material.icons.filled.OutdoorGrill
import androidx.compose.material.icons.filled.Paragliding
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Sledding
import androidx.compose.material.icons.filled.Snowboarding
import androidx.compose.material.icons.filled.Snowshoeing
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material.icons.filled.SportsRugby
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Deck
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiNature
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Grading
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Hive
import androidx.compose.material.icons.filled.Interests
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NaturePeople
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.RollerSkating
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TagFaces
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.DownhillSkiing
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.EmojiFoodBeverage
import androidx.compose.material.icons.rounded.EnergySavingsLeaf
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Hiking
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Recycling
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsBasketball
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.SportsVolleyball
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.apagon.rhythm.data.model.ChecklistItem
import com.apagon.rhythm.data.model.ChecklistProgress
import com.apagon.rhythm.data.model.Habit
import com.apagon.rhythm.data.model.HabitFrequency
import com.apagon.rhythm.data.model.Todo
import com.apagon.rhythm.data.model.TodoPriority
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.time.Instant
import com.apagon.rhythm.core.time.DateTimeFormatter

internal fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Consumes vertical drag gestures so they never bubble up to an enclosing `ModalBottomSheet`'s
 * own swipe-to-dismiss gesture. Apply to a sheet's content root (not its drag handle) so only the
 * handle can drag the sheet away — dragging the body just does nothing instead of dragging the
 * sheet down and snapping back.
 */
internal fun Modifier.blockSheetBodyDrag(): Modifier = this.pointerInput(Unit) {
    detectVerticalDragGestures { change, _ -> change.consume() }
}

private val sheetOverscrollBlockingConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available
    override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
}

/**
 * For a sheet's *scrollable* content (LazyColumn, LazyVerticalGrid, Column.verticalScroll) —
 * swallows the leftover scroll/fling once the list can't consume any more, so overscroll at the
 * top boundary never bubbles up into the enclosing ModalBottomSheet's own nested-scroll-driven
 * swipe-to-dismiss. In-bounds scrolling of the list itself is untouched. Use this instead of
 * [blockSheetBodyDrag] on anything actually scrollable — raw pointer consumption there would
 * break the scroll gesture itself.
 */
internal fun Modifier.blockSheetBoundaryOverscroll(): Modifier = this.nestedScroll(sheetOverscrollBlockingConnection)

/** Category icons mapped by colorIndex (same order as habitColorPalette, in ui/theme/HabitColors.kt). */
internal val habitCategoryIcons: List<ImageVector> = listOf(
    Icons.Default.SelfImprovement,      // 0 Purple
    Icons.Default.Park,                 // 1 Emerald
    Icons.AutoMirrored.Filled.DirectionsRun, // 2 Azure
    Icons.Default.Favorite,             // 3 Red
    Icons.AutoMirrored.Filled.MenuBook, // 4 Orange
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



/** Expanded icon library for user selection. Each entry is (label, icon). */
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
    "Walking"        to Icons.Rounded.DirectionsWalk,
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
    "Trending"       to Icons.Default.TrendingUp,
    "Bank"           to Icons.Default.AccountBalance,
    "Calculator"     to Icons.Default.Calculate,
    "Receipt"        to Icons.Default.ReceiptLong,
    "Chart"          to Icons.Default.ShowChart,
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
    "Library"        to Icons.Default.LibraryBooks,
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
    "Diary"          to Icons.Default.Note,
    "Calligraphy"    to Icons.Default.Gesture,
    "Blog"           to Icons.Default.Article,
    "Annotation"     to Icons.Default.BorderColor,
    // ── Music Making ────────────────────────────────────────────────────
    "Album"          to Icons.Default.Album,
    "Lyrics"         to Icons.Default.Lyrics,
    "Music Lib"      to Icons.Default.LibraryMusic,
    "Queue Music"    to Icons.Default.QueueMusic,
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
    "Grading"        to Icons.Default.Grading,
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

/** Returns the icon for this habit: the user-selected icon if set, otherwise the color-derived default. */
internal fun Habit.resolvedIcon(): ImageVector =
    if (iconIndex >= 0 && iconIndex < habitIconLibrary.size)
        habitIconLibrary[iconIndex].second
    else
        habitCategoryIcons.getOrElse(colorIndex) { habitCategoryIcons[0] }

internal val DAY_NAMES_SINGLE = listOf("S", "M", "T", "W", "T", "F", "S")
private val DAY_NAMES_SHORT = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private val DAY_NAMES_LONG  = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

internal val todoPriorityColors = mapOf(
    TodoPriority.HIGH   to Color(0xFFE53935),
    TodoPriority.MEDIUM to Color(0xFFF57C00),
    TodoPriority.LOW    to Color(0xFF43A047),
    TodoPriority.NONE   to Color.Unspecified
)

internal val todoIconList: List<ImageVector> = listOf(
    Icons.Filled.Star,
    Icons.Filled.Favorite,
    Icons.Filled.Work,
    Icons.Filled.Home,
    Icons.Filled.CheckCircle,
    Icons.Filled.WaterDrop
)

private val REMINDER_TIME_FMT_12 = DateTimeFormatter.ofPattern("h:mm a")
private val REMINDER_TIME_FMT_24 = DateTimeFormatter.ofPattern("HH:mm")
private val REMINDER_FULL_FMT_12 = DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a")
private val REMINDER_FULL_FMT_24 = DateTimeFormatter.ofPattern("EEE, MMM d 'at' HH:mm")

/** Short form (used in list trailing label): "Daily", "Mon, Wed", "2×/week", "1st, 3rd", "3×/month" */
/** Long form (used in detail sheet): "Every day", "Weekly on Monday…", "Monthly on the 1st…" */
internal fun Habit.scheduleLabel(short: Boolean = false): String = when (frequency) {
    HabitFrequency.DAILY -> if (short) "Daily" else "Every day"
    HabitFrequency.WEEKLY -> {
        val days = (0..6).filter { (weekDaysMask and (1 shl it)) != 0 }
        if (days.isNotEmpty()) {
            val names = days.joinToString(", ") { if (short) DAY_NAMES_SHORT[it] else DAY_NAMES_LONG[it] }
            if (short) names else "Weekly on $names"
        } else {
            if (short) "${targetDaysPerWeek}×/week" else "Weekly ($targetDaysPerWeek× per week)"
        }
    }
    HabitFrequency.MONTHLY -> {
        val days = (1..31).filter { (monthDaysMask and (1 shl (it - 1))) != 0 }
        if (days.isNotEmpty()) {
            val names = days.joinToString(", ") { it.ordinal() }
            if (short) names else "Monthly on the $names"
        } else {
            if (short) "${targetDaysPerMonth}×/month" else "Monthly ($targetDaysPerMonth× per month)"
        }
    }
}

/** Returns time-only string from "yyyy-MM-dd HH:mm", or empty string on failure. */
internal fun String.formatAsReminderTime(is24Hour: Boolean = false): String = try {
    val fmt = if (is24Hour) REMINDER_TIME_FMT_24 else REMINDER_TIME_FMT_12
    LocalDateTime.parse(this, REMINDER_INPUT_FMT).format(fmt)
} catch (e: Exception) { "" }

/** Returns full datetime string from "yyyy-MM-dd HH:mm", or empty string on failure. */
internal fun String.formatAsReminderDateTime(is24Hour: Boolean): String = try {
    val fmt = if (is24Hour) REMINDER_FULL_FMT_24 else REMINDER_FULL_FMT_12
    LocalDateTime.parse(this, REMINDER_INPUT_FMT).format(fmt)
} catch (e: Exception) { "" }

internal fun Context.isSystem24Hour(): Boolean = DateFormat.is24HourFormat(this)

internal fun Int.ordinal(): String {
    val suffix = when {
        this in 11..13 -> "th"
        this % 10 == 1 -> "st"
        this % 10 == 2 -> "nd"
        this % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$this$suffix"
}



internal fun String.toFormattedTime(is24Hour: Boolean): String {
    val parts = this.split(":")
    if (parts.size < 2) return this
    val h = parts[0].toIntOrNull() ?: return this
    val m = parts[1].toIntOrNull() ?: return this
    return formatTime(h, m, is24Hour)
}

internal fun formatTime(hour: Int, minute: Int, is24Hour: Boolean): String =
    if (is24Hour) "%02d:%02d".format(hour, minute)
    else {
        val amPm = if (hour < 12) "AM" else "PM"
        val h = when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }
        "%d:%02d %s".format(h, minute, amPm)
    }


internal object VibrationPatterns {
    val ALL = listOf("default", "subtle", "double", "triple", "heartbeat", "long")
    fun nameOf(id: String) = when (id) {
        "subtle"    -> "Subtle"
        "double"    -> "Double Tap"
        "triple"    -> "Triple Tap"
        "heartbeat" -> "Heartbeat"
        "long"      -> "Long Buzz"
        else        -> "Standard"
    }
    fun patternOf(id: String): LongArray = when (id) {
        "subtle"    -> longArrayOf(0, 200, 300)
        "double"    -> longArrayOf(0, 150, 100, 150, 500)
        "triple"    -> longArrayOf(0, 100, 80, 100, 80, 100, 600)
        "heartbeat" -> longArrayOf(0, 200, 150, 500, 350)
        "long"      -> longArrayOf(0, 1000, 400)
        else        -> longArrayOf(0, 800, 400, 800, 400)
    }
}

/**
 * Utility to handle repeating vibration for high-priority alerts.
 */
internal object AlertVibrator {
    fun start(context: Context, patternId: String = "default") {
        val pattern = VibrationPatterns.patternOf(patternId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.vibrate(CombinedVibration.createParallel(VibrationEffect.createWaveform(pattern, 0)))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
        }
    }

    fun stop(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).cancel()
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
        }
    }
}

// REMINDER_INPUT_FMT, toDayStartEndMillis, getDueDateAsLocalDate moved to
// commonMain ui/util/DateExtensions.kt (shared with iOS).
