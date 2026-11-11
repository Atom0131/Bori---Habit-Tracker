package com.apagon.rhythm.data.agent

import com.apagon.rhythm.ui.util.habitIconLibrary

/**
 * Icons for habits an AI agent creates: the agent can name one ("Running"), and when it doesn't, Bori
 * guesses from the habit's name. Indexes are into [habitIconLibrary], the same list (and order) the
 * phone uses, so the icon syncs as-is.
 */
internal object HabitIconGuess {

    val labels: List<String> by lazy { habitIconLibrary.map { it.first } }

    /**
     * Words people use for a habit → an icon label. Checked after the labels themselves, so "yoga" or
     * "reading" match directly. Keys are matched against whole words, or as a word's start for keys of
     * 4+ letters ("meditating" → "medit").
     */
    val SYNONYMS: Map<String, String> = linkedMapOf(
        "run" to "Running", "jog" to "Running", "jogging" to "Running", "5k" to "Running", "marathon" to "Running",
        "read" to "Reading", "book" to "Reading", "books" to "Reading",
        "water" to "Hydration", "drink" to "Hydration", "hydrate" to "Hydration",
        "meds" to "Medication", "medicine" to "Medication", "pill" to "Medication", "pills" to "Medication",
        "vitamin" to "Medication", "supplement" to "Medication",
        "walk" to "Walking", "steps" to "Walking",
        "workout" to "Gym", "lift" to "Gym", "lifting" to "Gym", "weights" to "Gym", "exercise" to "Gym",
        "pushups" to "Gym", "push-ups" to "Gym", "squats" to "Gym", "strength" to "Gym",
        "medit" to "Meditation", "mindful" to "Meditation", "breathe" to "Meditation", "breathing" to "Meditation",
        "stretch" to "Yoga", "pilates" to "Yoga",
        "journal" to "Journal", "write" to "Journal", "writing" to "Journal", "gratitude" to "Journal",
        "diary" to "Diary",
        "study" to "Study", "learn" to "Study", "homework" to "Study", "class" to "Study", "flashcards" to "Study",
        "code" to "Code", "coding" to "Code", "program" to "Code", "programming" to "Code", "leetcode" to "Code",
        "guitar" to "Music", "sing" to "Music", "singing" to "Music", "practice" to "Music", "violin" to "Music",
        "drums" to "Music",
        "clean" to "Cleaning", "tidy" to "Cleaning", "laundry" to "Cleaning", "dishes" to "Cleaning", "vacuum" to "Cleaning",
        "cook" to "Cooking", "meal" to "Cooking", "recipe" to "Cooking",
        "budget" to "Savings", "save" to "Savings", "saving" to "Savings", "money" to "Savings", "spend" to "Savings",
        "spanish" to "Language", "french" to "Language", "english" to "Language", "japanese" to "Language",
        "german" to "Language", "duolingo" to "Language", "language" to "Language",
        "dog" to "Pets", "cat" to "Pets", "pet" to "Pets",
        "teeth" to "Skin Care", "floss" to "Skin Care", "skincare" to "Skin Care", "skin" to "Skin Care",
        "sleep" to "Sleep", "bed" to "Sleep", "bedtime" to "Sleep", "nap" to "Sleep",
        "wake" to "Alarm", "early" to "Alarm",
        "pray" to "Heart", "prayer" to "Heart", "church" to "Heart", "love" to "Heart",
        "bike" to "Cycling", "cycle" to "Cycling", "swim" to "Swimming", "hike" to "Hiking",
        "plants" to "Garden", "plant" to "Garden", "garden" to "Garden",
        "tea" to "Coffee", "coffee" to "Coffee",
        "draw" to "Art", "drawing" to "Art", "sketch" to "Art", "paint" to "Painting",
        "eat" to "Nutrition", "fruit" to "Nutrition", "veggies" to "Nutrition", "vegetables" to "Nutrition",
        "diet" to "Nutrition", "protein" to "Nutrition",
        "breakfast" to "Breakfast", "lunch" to "Lunch", "dinner" to "Dinner",
        "screen" to "Phone", "phone" to "Phone", "social" to "Phone",
        "call" to "Chat", "family" to "Groups", "friends" to "Groups",
        "podcast" to "Podcast", "news" to "News", "work" to "Work", "plan" to "Calendar", "planning" to "Calendar",
        "shower" to "Shower", "game" to "Gaming", "gaming" to "Gaming", "photo" to "Photography",
        "blood" to "Heart Rate", "pressure" to "Heart Rate", "weigh" to "Heart Rate",
    )

    /** The index of the icon named [name] (any case), or null. */
    fun byName(name: String): Int? {
        val n = name.trim().lowercase()
        if (n.isEmpty()) return null
        labels.indexOfFirst { it.lowercase() == n }.takeIf { it >= 0 }?.let { return it }
        return SYNONYMS[n]?.let { target -> labels.indexOfFirst { it == target }.takeIf { it >= 0 } }
    }

    /** Best icon for a habit from its name (then its description), or -1 for the colour-based default. */
    fun guess(name: String, description: String = ""): Int {
        for (text in listOf(name, description)) {
            val words = text.lowercase().split(Regex("[^a-z0-9-]+")).filter { it.isNotEmpty() }
            // 1. A word that is an icon label, or its -ing form: "Yoga", "Reading", "run" → Running.
            for (w in words) {
                labels.indexOfFirst { l ->
                    val ll = l.lowercase()
                    ll == w || ll == w + "ing" || ll == w + w.last() + "ing" || (w.length >= 4 && ll == w.removeSuffix("s"))
                }.takeIf { it >= 0 }?.let { return it }
            }
            // 2. A synonym.
            for (w in words) {
                val target = SYNONYMS[w] ?: SYNONYMS.entries.firstOrNull { (k, _) -> k.length >= 4 && w.startsWith(k) }?.value
                target?.let { t -> labels.indexOfFirst { it == t }.takeIf { it >= 0 }?.let { return it } }
            }
        }
        return -1
    }
}
