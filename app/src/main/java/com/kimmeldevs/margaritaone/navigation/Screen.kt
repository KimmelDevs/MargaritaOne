package com.kimmeldevs.margaritaone.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.kimmeldevs.margaritaone.data.model.UserRole

sealed class Screen(val route: String) {
    // Signed out
    data object Login : Screen("login")
    data object SignUp : Screen("sign_up")
    data object ForgotPassword : Screen("forgot_password")

    // Signed in
    data object Bulletin : Screen("bulletin")
    data object MyReports : Screen("my_reports")
    data object Dashboard : Screen("dashboard")
    data object Users : Screen("users")
    data object Profile : Screen("profile")
    data object CreateReport : Screen("create_report")
    /** New post, or edit one with ?postId=... (#3). */
    data object CreatePost : Screen("create_post?postId={postId}") {
        const val ARG_POST_ID = "postId"
        fun createRoute(postId: String? = null) = if (postId == null) "create_post" else "create_post?postId=$postId"
    }
    data object ReactedPosts : Screen("reacted_posts")
    data object Barangays : Screen("barangays")
    data object ActivityLog : Screen("activity_log")

    // Both
    data object PrivacyNotice : Screen("privacy_notice")
    data object Hotlines : Screen("hotlines")

    data object ReportDetail : Screen("report_detail/{reportId}") {
        const val ARG_REPORT_ID = "reportId"
        fun createRoute(reportId: String) = "report_detail/$reportId"
    }
}

/** Bottom navigation tabs. Which ones a user sees depends on their role. */
enum class Tab(val screen: Screen, val label: String, val icon: ImageVector) {
    BULLETIN(Screen.Bulletin, "Bulletin", Icons.Filled.Campaign),
    MY_REPORTS(Screen.MyReports, "My reports", Icons.Filled.Assignment),
    DASHBOARD(Screen.Dashboard, "Reports", Icons.Filled.Dashboard),
    HOTLINES(Screen.Hotlines, "Hotlines", Icons.Filled.Call),
    USERS(Screen.Users, "Users", Icons.Filled.Group),
    PROFILE(Screen.Profile, "Profile", Icons.Filled.Person);

    companion object {
        fun forRole(role: UserRole): List<Tab> = when (role) {
            UserRole.CITIZEN -> listOf(BULLETIN, MY_REPORTS, HOTLINES, PROFILE)
            UserRole.STAFF -> listOf(DASHBOARD, BULLETIN, HOTLINES, PROFILE)
            UserRole.ADMIN -> listOf(DASHBOARD, BULLETIN, HOTLINES, USERS, PROFILE)
        }
    }
}
