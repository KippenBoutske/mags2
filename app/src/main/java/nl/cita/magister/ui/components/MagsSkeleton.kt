package nl.cita.magister.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nl.cita.magister.ui.theme.LocalSafeMode

/** Concept-1 skeleton: warme, ronde balken met een zachte pulse. */
@Composable
fun MagsSkeletonBar(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
) {
    val pulse by rememberInfiniteTransition(label = "skel").animateFloat(
        initialValue = 0.42f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            tween(1100, easing = LinearEasing),
            RepeatMode.Reverse,
        ),
        label = "skel-pulse",
    )
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = pulse)),
    )
}

@Composable
fun MagsSkeletonCard(
    modifier: Modifier = Modifier,
    lines: Int = 3,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(20.dp)
            .testTag("skeleton-card")
            .semantics { contentDescription = "Laden" },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MagsSkeletonBar(Modifier.fillMaxWidth(0.42f), height = 12.dp)
        repeat(lines) { index ->
            MagsSkeletonBar(
                Modifier.fillMaxWidth(if (index == lines - 1) 0.62f else 1f),
                height = 16.dp,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TodaySkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .testTag("today-skeleton")
            .semantics { contentDescription = "Vandaag laden" },
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MorphLoader()
        MagsSkeletonCard(lines = 4)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(2) {
                MagsSkeletonBar(
                    Modifier
                        .width(128.dp)
                        .height(36.dp)
                        .clip(CircleShape),
                    height = 36.dp,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(96.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }
        }
        MagsSkeletonCard(lines = 3)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ListSkeleton(
    rows: Int = 3,
    modifier: Modifier = Modifier,
    contentDescription: String = "Laden",
) {
    Column(
        modifier
            .fillMaxWidth()
            .testTag("list-skeleton")
            .semantics { this.contentDescription = contentDescription },
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MorphLoader()
        repeat(rows) { MagsSkeletonCard(lines = 2) }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorphLoader(modifier: Modifier = Modifier) {
    if (LocalSafeMode.current) {
        androidx.compose.material3.CircularProgressIndicator(modifier.size(48.dp))
    } else {
        LoadingIndicator(modifier.size(56.dp).testTag("morph-loader"))
    }
}

@Composable
fun SchoolSearchSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .testTag("school-skeleton")
            .semantics { contentDescription = "Scholen zoeken" },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(3) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                )
                MagsSkeletonBar(
                    Modifier
                        .padding(start = 12.dp)
                        .fillMaxWidth(0.7f),
                    height = 12.dp,
                )
            }
        }
    }
}
