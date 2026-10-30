package com.amon.timer.groups

data class GroupMember(
    val id: String,
    val nickname: String,
    val role: MemberRole = MemberRole.MEMBER,
    val studyTimeMinutes: Int,
    val isCurrentlyFocusing: Boolean,
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
    val category: String,
    val isPublic: Boolean,
    val memberCount: Int,
    val focusingCount: Int,
    val isPinned: Boolean = false,
    val isJoined: Boolean = false,
    val isDefault: Boolean = false,
    val description: String,
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
    val categories = listOf(
        "All",
        "NEET",
        "JEE",
        "UPSC",
        "Class 10 Board",
        "Class 12",
        "Rajasthan and other states"
    )

    val weeks = listOf(
        WeekPeriod("w_curr", "Current Week", "Oct 26 - Nov 01", true),
        WeekPeriod("w_1", "Last Week", "Oct 19 - Oct 25", false),
        WeekPeriod("w_2", "2 Weeks Ago", "Oct 12 - Oct 18", false),
        WeekPeriod("w_3", "3 Weeks Ago", "Oct 05 - Oct 11", false)
    )

    const val CURRENT_USER_NICKNAME = "Kavya_AimAIR1"
    const val CURRENT_USER_RANK = 14
    const val CURRENT_USER_STUDY_TIME = "18h 45m"

    val sampleGroups = listOf(
        StudyGroup(
            id = "universal_lib",
            name = "Universal Library",
            category = "All",
            isPublic = true,
            memberCount = 14820,
            focusingCount = 342,
            isPinned = true,
            isJoined = true,
            isDefault = true,
            description = "The global 24x7 silent study hall for all serious aspirants worldwide. Open camera/mic-free virtual library room. Study with quiet discipline, join Pomodoro blocks, and build your daily focus streak.",
            inviteCode = "SUN-UNIV-FREE",
            members = getSampleMembers()
        ),
        StudyGroup(
            id = "neet_warriors",
            name = "NEET 2026 Warriors",
            category = "NEET",
            isPublic = true,
            memberCount = 7420,
            focusingCount = 128,
            isPinned = true,
            isJoined = true,
            isDefault = false,
            description = "Targeting 700+ in NEET UG 2026. Daily NCERT revision drills, Zoology/Botany flashcards, Physics numerical sprints, and mock exam analysis.",
            inviteCode = "SUN-NEET-720",
            members = getSampleMembers()
        ),
        StudyGroup(
            id = "upsc_drishti",
            name = "UPSC Drishti 24/7",
            category = "UPSC",
            isPublic = true,
            memberCount = 12940,
            focusingCount = 284,
            isPinned = false,
            isJoined = true,
            isDefault = false,
            description = "Civil Services Examination discipline room. Ethics case studies, Hindu editorial breakdown, GS answer writing accountability, and optional subject deep study.",
            inviteCode = "SUN-UPSC-IAS",
            members = getSampleMembers()
        ),
        StudyGroup(
            id = "jee_apex",
            name = "JEE Adv Apex Batch",
            category = "JEE",
            isPublic = false,
            memberCount = 2840,
            focusingCount = 96,
            isPinned = false,
            isJoined = true,
            isDefault = false,
            description = "Private rigorous problem-solving circle for IIT-JEE Advanced. Minimum 6 hours daily self-study threshold. Weekly Pathfinder & Irodov discussions.",
            inviteCode = "APEX-JEE-2026",
            members = getSampleMembers()
        ),
        StudyGroup(
            id = "class_10_cbse",
            name = "Class 10 Board 95%+",
            category = "Class 10 Board",
            isPublic = true,
            memberCount = 3200,
            focusingCount = 45,
            isPinned = false,
            isJoined = false,
            isDefault = false,
            description = "Targeting 95%+ in Class 10 CBSE/ICSE board exams. Daily math theorem practice, science diagrams revision, and sample paper solving.",
            inviteCode = "SUN-C10-CBSE",
            members = getSampleMembers()
        ),
        StudyGroup(
            id = "rajasthan_reet",
            name = "Rajasthan REET & RAS Circle",
            category = "Rajasthan and other states",
            isPublic = true,
            memberCount = 6150,
            focusingCount = 112,
            isPinned = false,
            isJoined = false,
            isDefault = false,
            description = "Dedicated focus circle for Rajasthan Administrative Services (RAS), REET level 1 & 2, and 1st/2nd Grade Teacher examinations.",
            inviteCode = "SUN-RAJ-RAS",
            members = getSampleMembers()
        ),
        StudyGroup(
            id = "class_12_science",
            name = "Class 12 Science Board",
            category = "Class 12",
            isPublic = true,
            memberCount = 4750,
            focusingCount = 78,
            isPinned = false,
            isJoined = false,
            isDefault = false,
            description = "PCM & PCB Board champions. Balancing pre-board revisions, lab practicals, derivations, and board question paper patterns.",
            inviteCode = "SUN-C12-SCI",
            members = getSampleMembers()
        )
    )

    fun getSampleMembers(): List<GroupMember> {
        return listOf(
            GroupMember("1", "Aarav_IITD", MemberRole.ADMIN, 3260, true, "Electrodynamics"),
            GroupMember("2", "Dr_Riya_AIIMS", MemberRole.MEMBER, 3105, true, "Human Physiology"),
            GroupMember("3", "Vikram_IAS_Dream", MemberRole.MEMBER, 2890, true, "Modern History"),
            GroupMember("4", "Ananya_JEE25", MemberRole.MEMBER, 2550, false, "Calculus"),
            GroupMember("5", "Rohan_MathsGod", MemberRole.MEMBER, 2355, true, "Complex Numbers"),
            GroupMember("6", "Sneha_MedLover", MemberRole.MEMBER, 2270, true, "Plant Genetics"),
            GroupMember("7", "Aditya_UPSC26", MemberRole.MEMBER, 2040, false, "Polity Laxmikanth"),
            GroupMember("8", "Pooja_BioChem", MemberRole.MEMBER, 1900, true, "Biomolecules"),
            GroupMember("9", "Kartik_Quant", MemberRole.MEMBER, 1705, false, "Rotational Motion"),
            GroupMember("10", "Ishita_Class12", MemberRole.MEMBER, 1570, true, "Organic Chemistry"),
            GroupMember("11", "Dev_Coder_JEE", MemberRole.MEMBER, 1495, false, "Coordinate Geometry"),
            GroupMember("12", "Meera_BoardPrep", MemberRole.MEMBER, 1350, true, "English Literature"),
            GroupMember("13", "Tanmay_Physics", MemberRole.MEMBER, 1215, false, "Optics"),
            GroupMember("14", CURRENT_USER_NICKNAME, MemberRole.MEMBER, 1125, false, "Mechanics"),
            GroupMember("15", "Priyanka_AIIMS", MemberRole.MEMBER, 940, true, "Cytology"),
            GroupMember("16", "Inactive_Scholar", MemberRole.MEMBER, 0, false, null, "3d ago")
        )
    }
}
