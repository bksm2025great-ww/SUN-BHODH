package com.amon.timer

// Yeh hamari diary ka ek record (panna) hai
data class FocusSession(
    val id: Long = System.currentTimeMillis(), // Har session ki ek alag pehchan (Timestamp)
    val date: String,                          // Tarikh
    val subject: String,                       // Subject ka naam
    val durationMinutes: Int,                  // Kitne minute focus kiya
    val earnedTrees: Int = 0                   // Default 0 taaki koi error na aaye
)
