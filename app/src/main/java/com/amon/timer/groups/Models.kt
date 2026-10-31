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
    val isPinned: Boolean = false,
    val isJoined: Boolean = false,
    val isDefault: Boolean = false,
    val inviteCode: String? = null,
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
    // 🏷️ आपकी मनपसंद असली कैटेगरीज़
    val categories = listOf(
        "All",
        "Self Study",
        "UPSC",
        "NEET",
        "RPSC",
        "RSMSSB",
        "RBSE 10th",
        "RBSE 12th"
    )

    val weeks = listOf(
        WeekPeriod("w_curr", "This Week", "Current", true),
        WeekPeriod("w_prev", "Last Week", "Previous", false)
    )

    const val CURRENT_USER_NICKNAME = "Amon_Admin"

    // 🏛️ शुरूआती ग्रुप्स (सार्वजनिक और कुछ सुझाव ताकि एक्सप्लोर खाली न दिखे)
    val sampleGroups = listOf(
        StudyGroup(
            id = "universal_lib",
            name = "Universal Library",
            category = "Self Study",
            isPublic = true,
            memberCount = 1,
            focusingCount = 0,
            isPinned = false,
            isJoined = true,
            isDefault = true,
            inviteCode = "AMON-GLOBAL",
            members = listOf(
                GroupMember(id = "user_me", nickname = CURRENT_USER_NICKNAME, role = MemberRole.ADMIN, lastActive = "Active now")
            )
        ),
        StudyGroup(
            id = "rpsc_sprint",
            name = "Rajasthan RAS / RPSC Warriors",
            category = "RPSC",
            isPublic = true,
            memberCount = 12,
            focusingCount = 3,
            isPinned = false,
            isJoined = false,
            isDefault = false,
            inviteCode = "RAS-2026"
        ),
        StudyGroup(
            id = "upsc_prelims",
            name = "UPSC Target 2026",
            category = "UPSC",
            isPublic = true,
            memberCount = 28,
            focusingCount = 5,
            isPinned = false,
            isJoined = false,
            isDefault = false,
            inviteCode = "UPSC-SPRINT"
        ),
        StudyGroup(
            id = "neet_crack",
            name = "NEET Biology & Chem Sprint",
            category = "NEET",
            isPublic = true,
            memberCount = 19,
            focusingCount = 2,
            isPinned = false,
            isJoined = false,
            isDefault = false,
            inviteCode = "NEET-MED"
        )
    )

    fun getSampleMembers(): List<GroupMember> {
        return listOf(
            GroupMember(id = "user_me", nickname = CURRENT_USER_NICKNAME, role = MemberRole.ADMIN, studyTimeMinutes = 120, lastActive = "Active now")
        )
    }
}
