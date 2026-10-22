package nl.cita.magister.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.zIndex
import kotlin.math.abs
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import nl.cita.magister.api.School
import nl.cita.magister.model.Absence
import nl.cita.magister.model.ApiFailure
import nl.cita.magister.model.Appointment
import nl.cita.magister.model.Grade
import nl.cita.magister.model.HomeworkItem
import nl.cita.magister.model.MessageDetail
import nl.cita.magister.model.MessagePreview
import nl.cita.magister.model.SectionErrors
import nl.cita.magister.model.StudentProfile
import nl.cita.magister.model.StudyGuide
import nl.cita.magister.model.SubjectAverage
import nl.cita.magister.ui.grades.GradesScreen
import nl.cita.magister.ui.homework.HomeworkScreen
import nl.cita.magister.ui.login.BootSplash
import nl.cita.magister.ui.login.LoginScreen
import nl.cita.magister.ui.login.LoginWebView
import nl.cita.magister.ui.messages.MessagesScreen
import nl.cita.magister.ui.motion.magsSpring
import nl.cita.magister.ui.nav.MagsChrome
import nl.cita.magister.ui.nav.MagsPage
import nl.cita.magister.ui.nav.MagsTopBar
import nl.cita.magister.ui.nav.PhotosDock
import nl.cita.magister.ui.schedule.ScheduleScreen
import nl.cita.magister.ui.search.SearchHit
import nl.cita.magister.ui.search.SearchSheet
import nl.cita.magister.ui.settings.SettingsScreen
import nl.cita.magister.ui.theme.LocalSafeMode
import nl.cita.magister.ui.today.TodayScreen
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CitaRoot(viewModel: CitaViewModel) {
    val booting by viewModel.booting.collectAsStateWithLifecycle()
    val signedIn by viewModel.signedIn.collectAsStateWithLifecycle()
    val webViewUrl by viewModel.webViewUrl.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbar.collect { snackbarHost.showSnackbar(it) }
    }

    if (booting) {
        BootSplash()
        return
    }

    if (webViewUrl != null) {
        LoginWebView(
            url = webViewUrl!!,
            onRedirect = viewModel::handleRedirect,
            onClose = viewModel::closeWebView,
        )
        return
    }

    if (!signedIn) {
        val school by viewModel.schoolName.collectAsStateWithLifecycle()
        val tenant by viewModel.tenantHost.collectAsStateWithLifecycle()
        val hits by viewModel.schoolHits.collectAsStateWithLifecycle()
        val searching by viewModel.schoolSearching.collectAsStateWithLifecycle()
        val error by viewModel.lastError.collectAsStateWithLifecycle()
        Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }) {
            LoginScreen(
                schoolName = school,
                tenantHost = tenant,
                schools = hits,
                searching = searching,
                lastError = error,
                onQuery = viewModel::searchSchools,
                onSchoolChange = viewModel::setSchool,
                onStartLogin = { viewModel.startLogin().authorizeUrl },
                onWebViewLogin = viewModel::openWebViewLogin,
                onDemo = viewModel::enterDemo,
            )
        }
        return
    }

    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val grades by viewModel.grades.collectAsStateWithLifecycle()
    val averages by viewModel.averages.collectAsStateWithLifecycle()
    val appointments by viewModel.appointments.collectAsStateWithLifecycle()
    val homework by viewModel.homework.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val absences by viewModel.absences.collectAsStateWithLifecycle()
    val studyGuides by viewModel.studyGuides.collectAsStateWithLifecycle()
    val assignments by viewModel.assignments.collectAsStateWithLifecycle()
    val detail by viewModel.messageDetail.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val exporting by viewModel.exporting.collectAsStateWithLifecycle()
    val tenant by viewModel.tenantHost.collectAsStateWithLifecycle()
    val school by viewModel.schoolName.collectAsStateWithLifecycle()
    val webhook by viewModel.webhookUrl.collectAsStateWithLifecycle()
    val demo by viewModel.demoMode.collectAsStateWithLifecycle()
    val hits by viewModel.schoolHits.collectAsStateWithLifecycle()
    val searching by viewModel.schoolSearching.collectAsStateWithLifecycle()
    val error by viewModel.lastError.collectAsStateWithLifecycle()
    val sectionErrors by viewModel.sectionErrors.collectAsStateWithLifecycle()
    val dynamic by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val clock by viewModel.clock.collectAsStateWithLifecycle()
    val pendingPage by viewModel.pendingPage.collectAsStateWithLifecycle()
    val offline by viewModel.offline.collectAsStateWithLifecycle()

    MagsSignedInScaffold(
        profile = profile,
        grades = grades,
        averages = averages,
        appointments = appointments,
        homework = homework,
        messages = messages,
        absences = absences,
        studyGuides = studyGuides,
        assignments = assignments,
        detail = detail,
        refreshing = refreshing,
        exporting = exporting,
        tenantHost = tenant,
        schoolName = school,
        webhookUrl = webhook,
        hasWebhookKey = viewModel.hasWebhookKey,
        demoMode = demo,
        schools = hits,
        searching = searching,
        lastError = error,
        snackbarHost = snackbarHost,
        onRefresh = viewModel::refresh,
        onExport = viewModel::exportToPlanGoon,
        onShareJson = viewModel::shareExportJson,
        onToggleHomework = viewModel::toggleHomework,
        onOpenMessage = viewModel::openMessage,
        onCloseMessage = viewModel::closeMessage,
        onQuery = viewModel::searchSchools,
        onSwitchSchool = viewModel::switchSchool,
        onSaveWebhook = viewModel::saveWebhook,
        onLogout = viewModel::logout,
        sectionErrors = sectionErrors,
        dynamicColor = dynamic,
        onDynamicColor = viewModel::setDynamicColor,
        notifications = notifications,
        onNotifications = viewModel::setNotifications,
        onVisibleWeek = viewModel::prefetchAgenda,
        clockDate = clock.date,
        clockTime = clock.time,
        pendingPage = pendingPage,
        onPendingPageConsumed = viewModel::consumePendingPage,
        offline = offline,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MagsSignedInScaffold(
    profile: StudentProfile?,
    grades: List<Grade>,
    averages: List<SubjectAverage>,
    appointments: List<Appointment>,
    homework: List<HomeworkItem>,
    messages: List<MessagePreview>,
    absences: List<Absence> = emptyList(),
    studyGuides: List<StudyGuide> = emptyList(),
    assignments: List<HomeworkItem> = emptyList(),
    detail: MessageDetail?,
    refreshing: Boolean,
    exporting: Boolean,
    tenantHost: String,
    schoolName: String,
    webhookUrl: String,
    hasWebhookKey: Boolean,
    demoMode: Boolean,
    schools: List<School>,
    searching: Boolean,
    lastError: ApiFailure?,
    snackbarHost: SnackbarHostState,
    onRefresh: () -> Unit,
    onExport: () -> Unit,
    onShareJson: () -> Unit,
    onToggleHomework: (HomeworkItem, Boolean) -> Unit,
    onOpenMessage: (Int) -> Unit,
    onCloseMessage: () -> Unit,
    onQuery: (String) -> Unit,
    onSwitchSchool: (host: String, name: String, tenantId: String?) -> Unit,
    onSaveWebhook: (url: String, key: String) -> Unit,
    onLogout: () -> Unit,
    initialPage: MagsPage = MagsPage.Vandaag,
    sectionErrors: SectionErrors = SectionErrors(),
    dynamicColor: Boolean = false,
    onDynamicColor: (Boolean) -> Unit = {},
    notifications: Boolean = true,
    onNotifications: (Boolean) -> Unit = {},
    onVisibleWeek: (LocalDate) -> Unit = {},
    clockDate: LocalDate = LocalDate.now(),
    clockTime: LocalTime = LocalTime.now(),
    pendingPage: MagsPage? = null,
    onPendingPageConsumed: () -> Unit = {},
    offline: Boolean = false,
) {
    var page by remember { mutableStateOf(initialPage) }
    var lastDock by remember { mutableStateOf(if (initialPage.isDockTab) initialPage else MagsPage.Vandaag) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var expansion by remember { mutableFloatStateOf(1f) }
    var backProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(pendingPage) {
        val next = pendingPage ?: return@LaunchedEffect
        if (next.isDockTab) lastDock = next
        page = next
        expansion = 1f
        onPendingPageConsumed()
    }
    val safe = LocalSafeMode.current
    val density = LocalDensity.current

    val scroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // HorizontalPager days must win; the dock only reacts to vertical collapse.
                if (abs(available.x) > abs(available.y)) return Offset.Zero
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (abs(consumed.x) >= abs(consumed.y)) return Offset.Zero
                if (consumed.y != 0f) {
                    expansion = (expansion + consumed.y / 180f).coerceIn(0f, 1f)
                }
                return Offset.Zero
            }
        }
    }

    val visualExpansion by animateFloatAsState(
        targetValue = expansion,
        animationSpec = magsSpring(),
        label = "dock-expand",
    )

    val canPopOverlay = page == MagsPage.Berichten || page == MagsPage.Meer || searchOpen || detail != null
    val dismissOverlay = {
        when {
            detail != null -> onCloseMessage()
            searchOpen -> {
                searchOpen = false
                searchQuery = ""
            }
            else -> page = lastDock
        }
    }

    if (safe) {
        BackHandler(canPopOverlay) { dismissOverlay() }
    } else {
        PredictiveBackHandler(enabled = canPopOverlay) { progress ->
            try {
                progress.collect { event -> backProgress = event.progress }
                dismissOverlay()
            } catch (_: CancellationException) {
                // gebaar geannuleerd
            } finally {
                backProgress = 0f
            }
        }
    }

    val topInset = WindowInsets.statusBars.getTop(density)
    val bottomInset = WindowInsets.navigationBars.getBottom(density)
    val contentPadding = PaddingValues(
        top = with(density) { topInset.toDp() } + 8.dp,
        bottom = with(density) { bottomInset.toDp() } + MagsChrome.DockClearance,
    )

    val ptr = rememberPullToRefreshState()
    val overlayScale = 1f - backProgress * 0.06f
    val overlayAlpha = 1f - backProgress * 0.25f

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(snackbarHost) { data ->
                Snackbar(modifier = Modifier.padding(bottom = MagsChrome.DockClearance), snackbarData = data)
            }
        },
    ) { _ ->
        Box(
            Modifier
                .fillMaxSize()
                .nestedScroll(scroll),
        ) {
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = onRefresh,
                state = ptr,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    if (safe) {
                        PullToRefreshDefaults.Indicator(
                            modifier = Modifier.align(Alignment.TopCenter),
                            isRefreshing = refreshing,
                            state = ptr,
                        )
                    } else {
                        PullToRefreshDefaults.LoadingIndicator(
                            modifier = Modifier.align(Alignment.TopCenter),
                            isRefreshing = refreshing,
                            state = ptr,
                        )
                    }
                },
            ) {
                SharedTransitionLayout {
                    AnimatedContent(
                        targetState = page,
                        transitionSpec = {
                            val forward = targetState.ordinal >= initialState.ordinal
                            val slide = if (forward) 28 else -28
                            (
                                fadeIn(magsSpring()) +
                                    slideInHorizontally { slide } +
                                    scaleIn(initialScale = 0.97f, animationSpec = magsSpring())
                                ) togetherWith (
                                fadeOut() + slideOutHorizontally { -slide / 2 }
                                )
                        },
                        label = "page",
                    ) { dest ->
                        val vis = this@AnimatedContent
                        val shared = this@SharedTransitionLayout
                        Box(
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = overlayScale
                                    scaleY = overlayScale
                                    alpha = overlayAlpha
                                },
                        ) {
                            when (dest) {
                                MagsPage.Vandaag -> TodayScreen(
                                    profile = profile,
                                    appointments = appointments,
                                    homework = homework,
                                    averages = averages,
                                    messages = messages,
                                    contentPadding = contentPadding,
                                    onOpenPage = { next ->
                                        if (next.isDockTab) lastDock = next
                                        page = next
                                        expansion = 1f
                                    },
                                    onToggleHomework = onToggleHomework,
                                    agendaError = sectionErrors.agenda,
                                    gradesError = sectionErrors.grades,
                                    messagesError = sectionErrors.messages,
                                    loading = refreshing,
                                    clockDate = clockDate,
                                    clockTime = clockTime,
                                    offline = offline,
                                )
                                MagsPage.Rooster -> ScheduleScreen(
                                    appointments,
                                    contentPadding,
                                    agendaError = sectionErrors.agenda,
                                    onVisibleWeek = onVisibleWeek,
                                    loading = refreshing,
                                    homework = homework,
                                    clockDate = clockDate,
                                    clockTime = clockTime,
                                    sharedTransitionScope = shared,
                                    animatedVisibilityScope = vis,
                                )
                                MagsPage.Cijfers -> GradesScreen(
                                    profile = profile,
                                    averages = averages,
                                    grades = grades,
                                    refreshing = refreshing,
                                    exporting = exporting,
                                    contentPadding = contentPadding,
                                    onExport = onExport,
                                    onShareJson = onShareJson,
                                    gradesError = sectionErrors.grades,
                                )
                                MagsPage.Huiswerk -> HomeworkScreen(
                                    homework,
                                    contentPadding,
                                    onToggleHomework,
                                    agendaError = sectionErrors.agenda,
                                    loading = refreshing,
                                    clockDate = clockDate,
                                    snackbarHost = snackbarHost,
                                )
                                MagsPage.Berichten -> MessagesScreen(
                                    messages,
                                    detail,
                                    contentPadding,
                                    onOpenMessage,
                                    onCloseMessage,
                                    messagesError = sectionErrors.messages,
                                    loading = refreshing,
                                )
                                MagsPage.Meer -> SettingsScreen(
                                    profile = profile,
                                    tenantHost = tenantHost,
                                    schoolName = schoolName,
                                    webhookUrl = webhookUrl,
                                    hasWebhookKey = hasWebhookKey,
                                    demoMode = demoMode,
                                    schools = schools,
                                    searching = searching,
                                    lastError = lastError,
                                    contentPadding = contentPadding,
                                    onQuery = onQuery,
                                    onSwitchSchool = onSwitchSchool,
                                    onSaveWebhook = onSaveWebhook,
                                    onExport = onExport,
                                    onShareJson = onShareJson,
                                    onLogout = onLogout,
                                    dynamicColor = dynamicColor,
                                    onDynamicColor = onDynamicColor,
                                    notifications = notifications,
                                    onNotifications = onNotifications,
                                    absences = absences,
                                    studyGuides = studyGuides,
                                    assignments = assignments,
                                    absencesError = sectionErrors.absences,
                                    studyGuidesError = sectionErrors.studyGuides,
                                    assignmentsError = sectionErrors.assignments,
                                    unreadMessages = messages.count { !it.isRead },
                                    onOpenMessages = { page = MagsPage.Berichten },
                                )
                            }
                        }
                    }
                }
            }

            if (page != MagsPage.Meer) {
                MagsTopBar(
                    profile = profile,
                    unreadMessages = messages.count { !it.isRead },
                    onMessages = { page = MagsPage.Berichten },
                    onMore = { page = MagsPage.Meer },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .zIndex(1f),
                    actions = {
                        if (page == MagsPage.Cijfers) {
                            IconButton(
                                onClick = onExport,
                                enabled = !exporting,
                                modifier = Modifier.testTag("cijfers-deel"),
                            ) {
                                Icon(Icons.Outlined.IosShare, contentDescription = "Deel")
                            }
                        }
                    },
                )
            }

            val scrimBg = MaterialTheme.colorScheme.background
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(MagsChrome.ScrimHeight + with(density) { bottomInset.toDp() })
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.22f to scrimBg.copy(alpha = 0.72f),
                            0.48f to scrimBg.copy(alpha = 0.96f),
                            0.62f to scrimBg,
                            1f to scrimBg,
                        ),
                    )
                    .zIndex(1.5f)
                    .testTag("dock-scrim"),
            )

            PhotosDock(
                selected = page.takeIf { it.isDockTab },
                expansion = visualExpansion,
                onSelect = { next ->
                    lastDock = next
                    page = next
                    expansion = 1f
                },
                onSearch = { searchOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
                    .zIndex(2f)
                    .graphicsLayer {
                        translationY = (1f - visualExpansion) * 18f
                    },
            )
        }
    }

    if (searchOpen) {
        SearchSheet(
            query = searchQuery,
            onQuery = { searchQuery = it },
            grades = grades,
            appointments = appointments,
            homework = homework,
            messages = messages,
            absences = absences,
            studyGuides = studyGuides,
            onDismiss = {
                searchOpen = false
                searchQuery = ""
            },
            onOpen = { hit: SearchHit ->
                searchOpen = false
                searchQuery = ""
                if (hit.page.isDockTab) lastDock = hit.page
                page = hit.page
                hit.messageId?.let(onOpenMessage)
                expansion = 1f
            },
        )
    }
}
