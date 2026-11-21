package nl.cita.magister.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.cita.magister.haptics.HapticEvent
import nl.cita.magister.haptics.LocalMagsHaptics
import nl.cita.magister.ui.components.CookieFlower
import nl.cita.magister.ui.components.ExpressiveScallopShape
import nl.cita.magister.ui.motion.ExpressiveAuraBackground
import nl.cita.magister.ui.motion.GradeConfettiBurst
import nl.cita.magister.ui.motion.LivePulseDot
import nl.cita.magister.ui.motion.expressiveSheen

private const val TOUR_PAGES = 4

@Composable
fun MagsWelcomeTour(
    firstName: String?,
    onFinished: () -> Unit,
) {
    val haptics = LocalMagsHaptics.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { TOUR_PAGES })
    var confettiTick by remember { mutableIntStateOf(1) }
    var exiting by remember { mutableStateOf(false) }
    val exitAlpha = remember { Animatable(1f) }
    val exitScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        haptics.play(HapticEvent.LoginSuccess)
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage > 0) {
            haptics.play(HapticEvent.TabSwitch)
        }
    }

    fun finishTour() {
        if (exiting) return
        exiting = true
        confettiTick += 1
        haptics.play(HapticEvent.GradeHigh)
        scope.launch {
            delay(220L)
            launch { exitScale.animateTo(1.06f, tween(260, easing = FastOutSlowInEasing)) }
            exitAlpha.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = exitAlpha.value
                scaleX = exitScale.value
                scaleY = exitScale.value
            }
            .background(MaterialTheme.colorScheme.background)
            .testTag("welcome-tour"),
    ) {
        ExpressiveAuraBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top bar: progress pills + skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(TOUR_PAGES) { idx ->
                        val active = idx == pagerState.currentPage
                        val pillWidth by animateDpAsState(
                            targetValue = if (active) 28.dp else 8.dp,
                            animationSpec = spring(
                                dampingRatio = 0.65f,
                                stiffness = Spring.StiffnessMedium,
                            ),
                            label = "tour-dot-$idx",
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(pillWidth)
                                .clip(CircleShape)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                                ),
                        )
                    }
                }
                TextButton(
                    onClick = {
                        haptics.play(HapticEvent.Tap)
                        finishTour()
                    },
                    modifier = Modifier.testTag("tour-skip"),
                ) {
                    Text(
                        "Overslaan",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Center pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                when (page) {
                    0 -> WelcomeSlide(firstName = firstName)
                    1 -> LiveScheduleSlide()
                    2 -> GradesCalculatorSlide()
                    else -> HapticsFeelSlide(
                        onTryHaptic = {
                            confettiTick += 1
                            haptics.play(HapticEvent.GradeHigh)
                        },
                    )
                }
            }

            // Bottom expressive CTA button
            val isLast = pagerState.currentPage == TOUR_PAGES - 1
            Button(
                onClick = {
                    if (isLast) {
                        finishTour()
                    } else {
                        haptics.play(HapticEvent.Tap)
                        scope.launch {
                            pagerState.animateScrollToPage(
                                page = pagerState.currentPage + 1,
                                animationSpec = spring(
                                    dampingRatio = 0.78f,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("tour-next"),
            ) {
                AnimatedContent(
                    targetState = isLast,
                    transitionSpec = {
                        (fadeIn(tween(200)) + scaleIn(initialScale = 0.92f)) togetherWith fadeOut(tween(150))
                    },
                    label = "tour-cta",
                ) { last ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = if (last) "Aan de slag" else "Volgende",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Icon(
                            imageVector = if (last) Icons.Outlined.Check else Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                        )
                    }
                }
            }
        }

        GradeConfettiBurst(
            triggerKey = confettiTick,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun WelcomeSlide(firstName: String?) {
    val badgeScale = remember { Animatable(0.45f) }
    val greetingAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }

    val spin = rememberInfiniteTransition(label = "welcome-spin")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(22_000, easing = LinearEasing), RepeatMode.Restart),
        label = "welcome-spin-angle",
    )

    LaunchedEffect(Unit) {
        launch {
            badgeScale.animateTo(
                1f,
                spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow),
            )
        }
        delay(120L)
        launch { greetingAlpha.animateTo(1f, tween(340, easing = FastOutSlowInEasing)) }
        delay(160L)
        launch { titleAlpha.animateTo(1f, tween(360, easing = FastOutSlowInEasing)) }
        delay(150L)
        launch { subtitleAlpha.animateTo(1f, tween(360, easing = FastOutSlowInEasing)) }
    }

    val cleanName = firstName?.trim()?.takeIf { it.isNotBlank() }
    val greeting = if (cleanName != null) "Hallo $cleanName" else "Hallo!"

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(144.dp)
                .graphicsLayer {
                    scaleX = badgeScale.value
                    scaleY = badgeScale.value
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .graphicsLayer { rotationZ = angle }
                    .clip(ExpressiveScallopShape(lobes = 12, depthFraction = 0.08f))
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
            CookieFlower(
                size = 86.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = greeting,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.2).sp,
            ),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                alpha = greetingAlpha.value
                translationY = (1f - greetingAlpha.value) * 24f
            },
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Welkom in Mags",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                alpha = titleAlpha.value
                translationY = (1f - titleAlpha.value) * 20f
            },
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Jouw rooster, cijfers en huiswerk in één supervloeiende Material You-app.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .graphicsLayer {
                    alpha = subtitleAlpha.value
                    translationY = (1f - subtitleAlpha.value) * 16f
                },
        )
    }
}

@Composable
private fun LiveScheduleSlide() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .expressiveSheen(),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LivePulseDot(color = MaterialTheme.colorScheme.primary, size = 8.dp)
                    Text(
                        "NU · nog 18 min",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    "Wiskunde B",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Lokaal B204 · 10:15 – 11:05",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Watch,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "Live op telefoon & Wear OS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "Altijd live bij",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Zie direct in welk lokaal je moet zijn en hoeveel minuten je les nog duurt — ook als live melding en op je smartwatch.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun GradesCalculatorSlide() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                Triple("8,4", "WI", MaterialTheme.colorScheme.primaryContainer),
                Triple("9,1", "EN", MaterialTheme.colorScheme.tertiaryContainer),
                Triple("7,8", "NA", MaterialTheme.colorScheme.secondaryContainer),
            ).forEachIndexed { idx, (grade, subj, bg) ->
                val pop = remember { Animatable(0.6f) }
                LaunchedEffect(Unit) {
                    delay(idx * 90L)
                    pop.animateTo(1f, spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow))
                }
                Surface(
                    shape = ExpressiveScallopShape(lobes = 10, depthFraction = 0.08f),
                    color = bg,
                    modifier = Modifier
                        .size(if (idx == 1) 104.dp else 88.dp)
                        .graphicsLayer {
                            scaleX = pop.value
                            scaleY = pop.value
                        },
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            grade,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            subj,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(34.dp))

        Text(
            "Cijfers & calculator",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Bekijk al je cijfers per vak en reken met de schuifbalk meteen uit wat je op je volgende toets moet halen.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun HapticsFeelSlide(onTryHaptic: () -> Unit) {
    var tapped by remember { mutableStateOf(false) }
    val pulse = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = ExpressiveScallopShape(lobes = 12, depthFraction = 0.09f),
            color = if (tapped) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .size(138.dp)
                .graphicsLayer {
                    scaleX = pulse.value
                    scaleY = pulse.value
                }
                .clip(ExpressiveScallopShape(lobes = 12, depthFraction = 0.09f))
                .clickable {
                    tapped = !tapped
                    onTryHaptic()
                    scope.launch {
                        pulse.snapTo(0.86f)
                        pulse.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium))
                    }
                },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (tapped) "Lekker!" else "Tik eens!",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "Gemaakt om te voelen",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Swipe huiswerk af, voel het ratelende scrollwiel door je lijsten en laat Mags meekleuren met Material You.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}
