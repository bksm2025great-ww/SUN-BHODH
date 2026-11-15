package com.amon.timer.groups

data class GroupMember(
    val id: String,
    val nickname: String,
    val role: MemberRole = MemberRole.MEMBER,
    val studyTimeMinutes: Int = 0,
    val streak: Int = 0, // 🔥 स्ट्रीक काउंट
    val isCurrentlyFocusing: Boolean = false,
    val currentSubject: String? = null,
    val lastActive: String = "Just now"
)

enum class MemberRole {
    ADMIN,
    MEMBER
}

data class StudyGroup(
    val id: String,
    val name: String,
    val category: String = "All",
    val isPublic: Boolean = false, // हमेशा प्राइवेट रहेगा
    val memberCount: Int = 1,
    val focusingCount: Int = 0,
    val isPinned: Boolean = false,
    val isJoined: Boolean = false,
    val isDefault: Boolean = false,
    val inviteCode: String = "",
    val notificationsEnabled: Boolean = true,
    val members: List<GroupMember> = emptyList()
)

data class WeekPeriod(
    val id: String,
    val label: String,
    val dateRange: String,
    val isCurrent: Boolean
)

object MockRepository {
    // 🏷️ बिल्कुल साफ़ और मिनिमल कैटेगरीज़ (सारे फालतू एग्जाम टैग्स हटा दिए गए हैं)
    val categories = listOf(
        "All",
        "Self Study",
        "General",
        "Focus"
    )

    val weeks = listOf(
        WeekPeriod("w_curr", "This Week", "Current", true),
        WeekPeriod("w_prev", "Last Week", "Previous", false)
    )

    // कोई पब्लिक या डिफ़ॉल्ट ग्रुप नहीं, पूरी तरह से प्राइवेट सर्कल
    val sampleGroups = emptyList<StudyGroup>()

    fun getSampleMembers(): List<GroupMember> {
        return emptyList()
    }
}
