package com.example.gliphlights.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gliphlights.editor.model.GlyphNodeLayout
import com.example.gliphlights.editor.render.PreviewRenderer
import com.example.gliphlights.models.DeviceInfo
import com.example.gliphlights.models.GlyphState
import com.example.gliphlights.ui.theme.GlyphGlow
import com.example.gliphlights.ui.theme.NothingBlack
import com.example.gliphlights.viewmodel.DashboardActivityMode
import com.example.gliphlights.viewmodel.StudioDestination

/**
 * Compact live Glyph doughnut bound to [activeChannels].
 * Sized to fill its parent; uses the same node map as Editor / Physics.
 */
@Composable
fun LiveGlyphPreview(
    activeChannels: Set<Int>,
    modifier: Modifier = Modifier,
    glowActive: Boolean = activeChannels.isNotEmpty(),
    channelFills: FloatArray? = null
) {
    val pulse by rememberInfiniteTransition(label = "glyphPulse").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(NothingBlack)
    ) {
        val density = LocalDensity.current
        val layout = remember(constraints.maxWidth, constraints.maxHeight) {
            GlyphNodeLayout(
                viewportWidth = with(density) { maxWidth.toPx() },
                viewportHeight = with(density) { maxHeight.toPx() },
                centerYFraction = 0.50f
            )
        }

        if (glowActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.12f * pulse)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(GlyphGlow.copy(alpha = 0.4f), Color.Transparent)
                        )
                    )
            )
        }

        PreviewRenderer(
            layout = layout,
            activeChannels = activeChannels,
            hitNodeId = null,
            channelFills = channelFills,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun LiveHeroCard(
    glyphState: GlyphState,
    deviceInfo: DeviceInfo,
    activityMode: DashboardActivityMode,
    fps: Int,
    onPower: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (glyphState.isActive) {
            GlyphGlow.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        },
        animationSpec = tween(400),
        label = "heroBorder"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deviceInfo.model.ifBlank { "Nothing Phone" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(active = deviceInfo.isSupported)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (deviceInfo.isSupported) "Connected" else "Unsupported device",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                PowerOrb(
                    active = glyphState.isActive,
                    onClick = onPower
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LiveGlyphPreview(
                activeChannels = glyphState.activeChannels,
                glowActive = glyphState.isActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetaChip(
                    label = activityMode.label,
                    emphasized = activityMode != DashboardActivityMode.IDLE
                )
                MetaChip(
                    label = if (glyphState.isActive) {
                        "${glyphState.activeChannels.size} lit"
                    } else {
                        "Lights off"
                    }
                )
                if (fps > 0) {
                    MetaChip(label = "$fps FPS")
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = when (activityMode) {
                        DashboardActivityMode.ANIMATING -> "Pulse sequence"
                        DashboardActivityMode.LIVE -> "Manual output"
                        DashboardActivityMode.IDLE -> "Ready to create"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ContinueEditingCard(
    destination: StudioDestination,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by rememberInfiniteTransition(label = "ctaBreath").animateFloat(
        initialValue = 1f,
        targetValue = 1.01f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ctaScale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                role = Role.Button,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Continue Editing",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = destination.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = destination.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun QuickToolTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                role = Role.Button,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MetaChip(
    label: String,
    emphasized: Boolean = false
) {
    val bg = if (emphasized) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    }
    val fg = if (emphasized) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun PowerOrb(
    active: Boolean,
    onClick: () -> Unit
) {
    val ring by animateColorAsState(
        targetValue = if (active) GlyphGlow.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        label = "powerRing"
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(ring)
            .padding(4.dp)
            .clip(CircleShape)
            .background(
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 24.dp),
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PowerSettingsNew,
            contentDescription = if (active) "Turn Glyph off" else "Turn Glyph on",
            tint = if (active) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
        )
    }
}
