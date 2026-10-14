package com.kimmeldevs.margaritaone.navigation

import com.kimmeldevs.margaritaone.i18n.t
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.kimmeldevs.margaritaone.push.NotificationPermissionPrompt
import com.kimmeldevs.margaritaone.push.PushTarget
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.kimmeldevs.margaritaone.data.model.Profile
import com.kimmeldevs.margaritaone.data.model.UserRole
import com.kimmeldevs.margaritaone.ui.components.EmergencyBanner
import com.kimmeldevs.margaritaone.ui.components.LoadingBox
import com.kimmeldevs.margaritaone.ui.components.OfflineBanner
import com.kimmeldevs.margaritaone.ui.viewmodel.EmergencyViewModel
import com.kimmeldevs.margaritaone.util.isOnlineFlow
import com.kimmeldevs.margaritaone.ui.components.MessageBox
import com.kimmeldevs.margaritaone.ui.screens.admin.ActivityLogScreen
import com.kimmeldevs.margaritaone.ui.screens.admin.BarangaysScreen
import com.kimmeldevs.margaritaone.ui.screens.auth.ForgotPasswordScreen
import com.kimmeldevs.margaritaone.ui.screens.auth.LoginScreen
import com.kimmeldevs.margaritaone.ui.screens.auth.SignUpScreen
import com.kimmeldevs.margaritaone.ui.screens.bulletin.BulletinScreen
import com.kimmeldevs.margaritaone.ui.screens.bulletin.CreatePostScreen
import com.kimmeldevs.margaritaone.ui.screens.hotlines.HotlinesScreen
import com.kimmeldevs.margaritaone.ui.screens.privacy.PrivacyNoticeScreen
import com.kimmeldevs.margaritaone.ui.screens.profile.ProfileScreen
import com.kimmeldevs.margaritaone.ui.screens.report.CreateReportScreen
import com.kimmeldevs.margaritaone.ui.screens.report.MyReportsScreen
import com.kimmeldevs.margaritaone.ui.screens.report.ReportDetailScreen
import com.kimmeldevs.margaritaone.ui.screens.staff.DashboardScreen
import com.kimmeldevs.margaritaone.ui.screens.staff.UsersScreen
import com.kimmeldevs.margaritaone.ui.viewmodel.AuthViewModel
import com.kimmeldevs.margaritaone.ui.viewmodel.SessionState
import com.kimmeldevs.margaritaone.ui.viewmodel.SessionViewModel

/** Root of the UI: picks the signed-out or signed-in graph from the session state. */
@Composable
fun MargaritaOneRoot(
    sessionViewModel: SessionViewModel = viewModel(factory = SessionViewModel.Factory)
) {
    val session by sessionViewModel.state.collectAsStateWithLifecycle()

    when (val state = session) {
        SessionState.Loading -> LoadingBox(Modifier.safeDrawingPadding())
        SessionState.NotConfigured -> MessageBox(
            modifier = Modifier.safeDrawingPadding(),
            message = "Supabase is not configured.\n\nAdd SUPABASE_URL and SUPABASE_ANON_KEY to local.properties, " +
                "then sync Gradle and rebuild."
        )
        is SessionState.Error -> MessageBox(
            modifier = Modifier.safeDrawingPadding(),
            message = state.message,
            actionLabel = "Try again",
            onAction = sessionViewModel::loadProfile
        )
        SessionState.SignedOut -> SignedOutNavGraph()
        is SessionState.SignedIn -> {
            // A different user or role gets a fresh back stack with the right tabs.
            key(state.profile.id, state.profile.role) {
                SignedInNavGraph(
                    profile = state.profile,
                    onProfileChanged = sessionViewModel::loadProfile,
                    onSignOut = sessionViewModel::signOut
                )
            }
        }
    }
}

@Composable
private fun SignedOutNavGraph() {
    val navController = rememberNavController()
    // Shared between login and sign-up so the form state survives switching screens.
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val authState by authViewModel.state.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(
                state = authState,
                onSignIn = authViewModel::signIn,
                onGoToSignUp = {
                    authViewModel.clearMessages()
                    authViewModel.consumeSignUpResult()
                    navController.navigate(Screen.SignUp.route)
                },
                onForgotPassword = {
                    authViewModel.clearMessages()
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onOpenHotlines = { navController.navigate(Screen.Hotlines.route) }
            )
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.Hotlines.route) {
            HotlinesScreen(canEdit = false, onBackClick = { navController.popBackStack() })
        }
        composable(Screen.SignUp.route) {
            // Signed up, but the email needs confirming: back to login with the email filled in and the message shown.
            LaunchedEffect(authState.signUpComplete) {
                if (authState.signUpComplete) navController.popBackStack(Screen.Login.route, inclusive = false)
            }
            SignUpScreen(
                state = authState,
                onSignUp = authViewModel::signUp,
                onOpenPrivacyNotice = { navController.navigate(Screen.PrivacyNotice.route) },
                onBackClick = {
                    authViewModel.clearMessages()
                    authViewModel.consumeSignUpResult()
                    navController.popBackStack()
                },
                onGoToSignIn = {
                    // Keep the email to fill in; drop the "already exists" error.
                    authViewModel.clearMessages()
                    navController.popBackStack(Screen.Login.route, inclusive = false)
                }
            )
        }
        composable(Screen.PrivacyNotice.route) {
            PrivacyNoticeScreen(onBackClick = { navController.popBackStack() })
        }
    }
}

@Composable
private fun SignedInNavGraph(
    profile: Profile,
    onProfileChanged: () -> Unit,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val tabs = Tab.forRole(profile.role)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabs.any { it.screen.route == currentRoute }
    val isStaff = profile.role.isStaff
    val isAdmin = profile.role == UserRole.ADMIN

    val context = LocalContext.current
    val isOnline by remember { context.isOnlineFlow() }.collectAsStateWithLifecycle(initialValue = true)
    val emergencyViewModel: EmergencyViewModel = viewModel(factory = EmergencyViewModel.Factory)
    val emergency by emergencyViewModel.emergency.collectAsStateWithLifecycle()

    NotificationPermissionPrompt()
    // A tapped notification opens its report or the bulletin.
    val pushTarget by PushTarget.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pushTarget) {
        when (val target = pushTarget ?: return@LaunchedEffect) {
            is PushTarget.Report -> navController.navigate(Screen.ReportDetail.createRoute(target.reportId))
            PushTarget.Bulletin -> navController.navigateToTab(Screen.Bulletin.route)
        }
        PushTarget.consume()
    }

    Scaffold(
        bottomBar = {
            // Banners sit above the tab bar; on screens without tabs they clear the system nav bar.
            Column(modifier = if (showBottomBar) Modifier else Modifier.navigationBarsPadding()) {
                EmergencyBanner(
                    post = emergency,
                    onClick = { navController.navigateToTab(Screen.Bulletin.route) },
                    onDismiss = emergencyViewModel::dismiss
                )
                OfflineBanner(visible = !isOnline)
                if (showBottomBar) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute == tab.screen.route,
                                onClick = { navController.navigateToTab(tab.screen.route) },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(t(tab.label)) }
                            )
                        }
                    }
                }
            }
        }
    ) { outerPadding ->
        NavHost(
            navController = navController,
            startDestination = tabs.first().screen.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(outerPadding)
                .consumeWindowInsets(outerPadding)
        ) {
            val openReport: (String) -> Unit = { id -> navController.navigate(Screen.ReportDetail.createRoute(id)) }

            composable(Screen.Bulletin.route) {
                BulletinScreen(
                    canPost = isAdmin,
                    currentUserId = profile.id,
                    onNewPostClick = { navController.navigate(Screen.CreatePost.createRoute()) },
                    onEditPost = { id -> navController.navigate(Screen.CreatePost.createRoute(id)) }
                )
            }
            composable(Screen.MyReports.route) {
                MyReportsScreen(
                    currentUserId = profile.id,
                    onNewReportClick = { navController.navigate(Screen.CreateReport.route) },
                    onReportClick = openReport
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    profile = profile,
                    onProfileSaved = onProfileChanged,
                    onOpenPrivacyNotice = { navController.navigate(Screen.PrivacyNotice.route) },
                    onOpenReactedPosts = { navController.navigate(Screen.ReactedPosts.route) },
                    onOpenBarangays = { navController.navigate(Screen.Barangays.route) },
                    onOpenActivityLog = { navController.navigate(Screen.ActivityLog.route) },
                    onSignOut = onSignOut
                )
            }
            composable(Screen.PrivacyNotice.route) {
                PrivacyNoticeScreen(onBackClick = { navController.popBackStack() })
            }
            composable(Screen.Hotlines.route) {
                HotlinesScreen(canEdit = isAdmin)
            }
            composable(Screen.ReactedPosts.route) {
                BulletinScreen(
                    canPost = isAdmin,
                    currentUserId = profile.id,
                    onNewPostClick = {},
                    onEditPost = { id -> navController.navigate(Screen.CreatePost.createRoute(id)) },
                    reactedOnly = true,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.CreateReport.route) {
                CreateReportScreen(
                    onBackClick = { navController.popBackStack() },
                    onSubmitted = { id ->
                        navController.popBackStack()
                        openReport(id)
                    }
                )
            }
            composable(Screen.ReportDetail.route) {
                ReportDetailScreen(isStaff = isStaff, currentUserId = profile.id, onBackClick = { navController.popBackStack() })
            }

            if (isStaff) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(currentUserId = profile.id, onReportClick = openReport)
                }
            }
            if (isAdmin) {
                composable(
                    Screen.CreatePost.route,
                    arguments = listOf(
                        navArgument(Screen.CreatePost.ARG_POST_ID) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) {
                    CreatePostScreen(
                        onBackClick = { navController.popBackStack() },
                        onPosted = { navController.popBackStack() }
                    )
                }
                composable(Screen.Barangays.route) {
                    BarangaysScreen(onBackClick = { navController.popBackStack() })
                }
                composable(Screen.ActivityLog.route) {
                    ActivityLogScreen(onBackClick = { navController.popBackStack() })
                }
                composable(Screen.Users.route) {
                    UsersScreen(currentUserId = profile.id)
                }
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
