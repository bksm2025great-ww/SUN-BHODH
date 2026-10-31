package com.amon.timer.groups

data class GroupMember(
    val id: String,
    val nickname: String,
    val role: MemberRole = MemberRole.MEMBER,
    val studyTimeMinutes: Int = 0,
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
    val isPublic: Boolean = true,
    val memberCount: Int = 1,
    val focusingCount: Int = 0,
    val isPinned: Boolean = false,        // ❌ कोई जबरदस्ती स्टार नहीं, डिफ़ॉल्ट बंद
    val isJoined: Boolean = false,
    val isDefault: Boolean = false,
    val inviteCode: String? = null,
    val notificationsEnabled: Boolean = true,
    val members: List<GroupMember> = emptyList() // ❌ कोई फर्जी मेंबर्स की लिस्ट नहीं
)

data class WeekPeriod(
    val id: String,
    val label: String,
    val dateRange: String,
    val isCurrent: Boolean
)

object MockRepository {
    // 🏷️ सिर्फ एडमिन/डिफ़ॉल्ट कैटेगरीज़
    val categories = listOf(
        "All",
        "General",
        "Exams",
        "Self Study"
    )

    val weeks = listOf(
        WeekPeriod("w_curr", "Current Week", "This Week", true)
    )

    const val CURRENT_USER_NICKNAME = "Amon_User"

    // 🏛️ केवल 1 असली ग्लोबल ग्रुप - 0 फर्जी मेंबर्स के साथ
    val sampleGroups = listOf(
        StudyGroup(
            id = "universal_lib",
            name = "Universal Library",
            category = "All",
            isPublic = true,
            memberCount = 1,              // असली शुरुआत 1 मेंबर से
            focusingCount = 0,            // कोई फर्जी काउंट नहीं
            isPinned = false,             // अनपिन डिफ़ॉल्ट
            isJoined = true,              // नया यूज़र इसमें ऑटो जॉइन रहेगा
            isDefault = true,
            inviteCode = "AMON-GLOBAL",
            members = emptyList()         // खाली लिस्ट, जब लोग जुड़ेंगे तब भरेगी
        )
    )

    // कोई फर्जी मेंबर्स नहीं, बिल्कुल खाली लिस्ट
    fun getSampleMembers(): List<GroupMember> {
        return emptyList()
    }
}
