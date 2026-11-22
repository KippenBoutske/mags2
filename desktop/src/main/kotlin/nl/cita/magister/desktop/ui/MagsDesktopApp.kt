package nl.cita.magister.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nl.cita.magister.desktop.ui.screens.GradesView
import nl.cita.magister.desktop.ui.screens.HomeworkView
import nl.cita.magister.desktop.ui.screens.LoginView
import nl.cita.magister.desktop.ui.screens.MessagesView
import nl.cita.magister.desktop.ui.screens.MoreView
import nl.cita.magister.desktop.ui.screens.ScheduleView
import nl.cita.magister.desktop.ui.screens.TodayView
import nl.cita.magister.desktop.ui.theme.MagsDesktopTheme

@Composable
fun MagsDesktopApp(controller: MagsDesktopController) {
    val prefs by controller.prefs.collectAsState()
    val authenticated by controller.authenticated.collectAsState()
    val page by controller.page.collectAsState()
    val profile by controller.profile.collectAsState()
    val refreshing by controller.refreshing.collectAsState()
    val statusMessage by controller.statusMessage.collectAsState()
    val homework by controller.homework.collectAsState()
    val messages by controller.messages.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        controller.clearStatus()
    }

    MagsDesktopTheme(themeMode = prefs.themeMode) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (!authenticated) {
                    LoginView(controller)
                } else {
                    val openHwCount = homework.count { !it.isDone }
                    val unreadMsgCount = messages.count { !it.isRead }

                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left Navigation Sidebar
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.width(232.dp).fillMaxHeight(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp, vertical = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                // App brand + refresh button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Mags",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    IconButton(
                                        onClick = { controller.refreshAll() },
                                        enabled = !refreshing,
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        if (refreshing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                            )
                                        } else {
                                            Icon(
                                                Icons.Outlined.Refresh,
                                                contentDescription = "Verversen (F5)",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }

                                if (prefs.isDemo) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                    ) {
                                        Text(
                                            "Voorbeeldmodus",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        )
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                SidebarItem(
                                    label = DesktopPage.Vandaag.label,
                                    icon = Icons.Outlined.Today,
                                    selected = page == DesktopPage.Vandaag,
                                    onClick = { controller.navigate(DesktopPage.Vandaag) },
                                )
                                SidebarItem(
                                    label = DesktopPage.Rooster.label,
                                    icon = Icons.Outlined.CalendarViewWeek,
                                    selected = page == DesktopPage.Rooster,
                                    onClick = { controller.navigate(DesktopPage.Rooster) },
                                )
                                SidebarItem(
                                    label = DesktopPage.Cijfers.label,
                                    icon = Icons.Outlined.Leaderboard,
                                    selected = page == DesktopPage.Cijfers,
                                    onClick = { controller.navigate(DesktopPage.Cijfers) },
                                )
                                SidebarItem(
                                    label = DesktopPage.Huiswerk.label,
                                    icon = Icons.AutoMirrored.Outlined.Assignment,
                                    badge = openHwCount.takeIf { it > 0 }?.toString(),
                                    selected = page == DesktopPage.Huiswerk,
                                    onClick = { controller.navigate(DesktopPage.Huiswerk) },
                                )
                                SidebarItem(
                                    label = DesktopPage.Berichten.label,
                                    icon = Icons.Outlined.MailOutline,
                                    badge = unreadMsgCount.takeIf { it > 0 }?.toString(),
                                    selected = page == DesktopPage.Berichten,
                                    onClick = { controller.navigate(DesktopPage.Berichten) },
                                )

                                Spacer(Modifier.weight(1f))

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                SidebarItem(
                                    label = DesktopPage.Meer.label,
                                    icon = Icons.Outlined.Settings,
                                    selected = page == DesktopPage.Meer,
                                    onClick = { controller.navigate(DesktopPage.Meer) },
                                )

                                // Student pill at bottom
                                profile?.let { p ->
                                    val initials = listOf(p.firstName, p.lastName)
                                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                        .joinToString("")
                                        .ifBlank { "M" }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.small)
                                            .clickable { controller.navigate(DesktopPage.Meer) }
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                initials,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "${p.firstName} ${p.lastName}".trim(),
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                p.schoolName.ifBlank { p.tenantHost },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Active page content
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.background),
                        ) {
                            when (page) {
                                DesktopPage.Vandaag -> TodayView(controller)
                                DesktopPage.Rooster -> ScheduleView(controller)
                                DesktopPage.Cijfers -> GradesView(controller)
                                DesktopPage.Huiswerk -> HomeworkView(controller)
                                DesktopPage.Berichten -> MessagesView(controller)
                                DesktopPage.Meer -> MoreView(controller)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    badge: String? = null,
    onClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else fg,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (!badge.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}
