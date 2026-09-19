package com.amon.timer

/**
 * 🟢 MODULE: TREE & SUBJECT REGISTRY
 * यह फ़ाइल सभी विषयों और उनके अनोखे पेड़ों का हिसाब रखती है।
 */

// 1. पेड़ के बढ़ने के 4 पड़ाव (Stage 1 to 4)
enum class TreeGrowthStage {
    SPROUT,   // स्टेज 1: नन्हीं कोंपल (0% - 25%)
    SAPLING,  // स्टेज 2: बाल-वृक्ष / टहनी (25% - 60%)
    DENSE,    // स्टेज 3: घना पेड़ (60% - 95%)
    MATURE    // स्टेज 4: फलों से लदा पूरा पेड़ (100%)
}

// 2. पेड़ की बुनियादी जानकारी
data class TreeType(
    val id: String,
    val nameEn: String,
    val nameHi: String,
    val fruitName: String
)

// 3. विषय की जानकारी और उसका अपना अनोखा पेड़
data class Subject(
    val id: String,
    val name: String,
    val tree: TreeType
)

object PlantRegistry {

    // 14 मुख्य विषयों के 14 अनोखे पेड़
    val defaultSubjects = listOf(
        Subject("hindi", "Hindi", TreeType("bael", "Bael Tree", "बेल", "Bael")),
        Subject("english", "English", TreeType("cherry", "Cherry Tree", "चेरी", "Cherry Blossom")),
        Subject("science", "Science", TreeType("kiwi", "Kiwi Tree", "कीवी", "Kiwi")),
        Subject("math", "Math", TreeType("lemon", "Lemon Tree", "नींबू", "Lemon")),
        Subject("geography", "Geography", TreeType("coconut", "Coconut Tree", "नारियल", "Coconut")),
        Subject("art_culture", "Art and Culture", TreeType("mango", "Mango Tree", "आम", "Mango")),
        Subject("polity", "Polity", TreeType("walnut", "Walnut Tree", "अखरोट", "Walnut")),
        Subject("economics", "Economics", TreeType("orange", "Orange Tree", "संतरा", "Orange")),
        Subject("current_affairs", "Current Affairs", TreeType("starfruit", "Starfruit Tree", "कमरख", "Starfruit")),
        Subject("psychology", "Psychology", TreeType("peach", "Peach Tree", "आड़ू", "Peach")),
        Subject("history", "History", TreeType("olive", "Olive Tree", "जैतून", "Olive")),
        Subject("biology", "Biology", TreeType("fig", "Fig Tree", "अंजीर", "Fig")),
        Subject("chemistry", "Chemistry", TreeType("pomegranate", "Pomegranate Tree", "अनार", "Pomegranate")),
        Subject("physics", "Physics", TreeType("apple", "Apple Tree", "सेब", "Apple"))
    )

    // भविष्य के नए विषयों के लिए रिज़र्व पेड़ों का खज़ाना (Vault)
    // जब भी कोई नया विषय बनेगा, इनमें से अगला अनोखा पेड़ आवंटित होगा
    val reservedTreeVault = listOf(
        TreeType("banana", "Banana Tree", "केला", "Banana"),
        TreeType("guava", "Guava Tree", "अमरूद", "Guava"),
        TreeType("papaya", "Papaya Tree", "पपीता", "Papaya"),
        TreeType("plum", "Plum Tree", "आलूबुखारा", "Plum"),
        TreeType("pear", "Pear Tree", "नाशपाती", "Pear"),
        TreeType("jackfruit", "Jackfruit Tree", "कटहल", "Jackfruit"),
        TreeType("cashew", "Cashew Tree", "काजू", "Cashew"),
        TreeType("custard_apple", "Custard Apple Tree", "शरीफ़ा", "Custard Apple"),
        TreeType("apricot", "Apricot Tree", "खुबानी", "Apricot"),
        TreeType("lychee", "Lychee Tree", "लीची", "Lychee")
    )

    // प्रोग्रेस के हिसाब से वर्तमान स्टेज तय करने का फ़ंक्शन
    fun getGrowthStage(fractionComplete: Float): TreeGrowthStage {
        return when {
            fractionComplete < 0.25f -> TreeGrowthStage.SPROUT
            fractionComplete < 0.60f -> TreeGrowthStage.SAPLING
            fractionComplete < 0.95f -> TreeGrowthStage.DENSE
            else -> TreeGrowthStage.MATURE
        }
    }
}
