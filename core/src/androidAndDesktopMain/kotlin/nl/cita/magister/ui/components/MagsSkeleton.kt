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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nl.cita.magister.ui.theme.LocalSafeMode

@Composable
fun MagsSkeletonBar(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
) {
    val shift by rememberInfiniteTransition(label = "skel").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "skel-shimmer",
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val hi = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    0f to base,
                    0.45f to hi,
                    1f to base,
                    start = Offset(1000f * (shift - 1f), 0f),
                    end = Offset(1000f * shift, 0f),
                ),
            ),
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
            .background(MaterialTheme.colorScheme.surfaceContainer)
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

@Composable
fun TodaySkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .testTag("today-skeleton")
            .semantics { contentDescription = "Vandaag laden" },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MagsSkeletonCard(lines = 4)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(2) {
                MagsSkeletonBar(
                    Modifier
                        .width(128.dp)
                        .clip(CircleShape),
                    height = 36.dp,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) {
                MagsSkeletonCard(
                    modifier = Modifier.weight(1f),
                    lines = 1,
                )
            }
        }
        MagsSkeletonCard(lines = 3)
    }
}

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
    ) {
        repeat(rows) { MagsSkeletonCard(lines = 2) }
    }
}

@Composable
fun MorphLoader(modifier: Modifier = Modifier) {
    if (LocalSafeMode.current) {
        androidx.compose.material3.CircularProgressIndicator(modifier.size(48.dp))
    } else {
        nl.cita.magister.ui.theme.MagsLoadingIndicator(modifier.size(56.dp).testTag("morph-loader"))
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
