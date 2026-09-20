package com.amon.timer

// Yeh hamari diary ka ek record (panna) hai
data class FocusSession(
    val id: Long = System.currentTimeMillis(), // Har session ki ek alag pehchan (Time stamp)
    val date: String,                          // Tarikh (Jaise "20-09-2026")
    val subject: String,                       // Subject ka naam (Jaise "Math" ya "Coding")
    val durationMinutes: Int,                  // Kitne minute focus kiya
    val earnedTrees: Int                       // Is session mein kitne paudhe (trees) mile
)
