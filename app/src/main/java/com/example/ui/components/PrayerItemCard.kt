package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PrayerItemState
import com.example.ui.theme.CreamBorder
import com.example.ui.theme.CreamSurface
import com.example.ui.theme.CreamSurfaceVariant
import com.example.ui.theme.DarkBrownText
import com.example.ui.theme.MediumBrownText
import com.example.ui.theme.SageGreenBorder
import com.example.ui.theme.SageGreenLight
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.theme.WarmGold

@Composable
fun PrayerItemCard(
    prayerState: PrayerItemState,
    onToggleCompletion: () -> Unit,
    onToggleReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prayer = prayerState.type
    val isCompleted = prayerState.isCompleted
    val isNext = prayerState.isNextPrayer && !isCompleted

    val cardBg by animateColorAsState(
        targetValue = when {
            isCompleted -> SageGreenLight.copy(alpha = 0.45f)
            isNext -> CreamSurface
            else -> CreamSurface
        },
        label = "card_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isCompleted -> SageGreenBorder
            isNext -> SageGreenPrimary
            else -> CreamBorder
        },
        label = "card_border"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onToggleCompletion)
            .testTag("prayer_card_${prayer.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(borderColor)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Main Prayer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Checkbox / Green Tick with minimum 48dp touch target
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleCompletion
                        )
                        .testTag("prayer_checkbox_${prayer.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        // Clear Green Tick
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SageGreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed (tap to undo)",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        // Uncompleted outline ring
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .border(2.dp, MediumBrownText.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MediumBrownText.copy(alpha = 0.2f))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Prayer Name, Arabic Name, Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = prayer.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkBrownText,
                                fontSize = 16.sp
                            )
                        )

                        Text(
                            text = prayer.arabicName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Normal,
                                color = MediumBrownText,
                                fontSize = 14.sp
                            )
                        )

                        if (isNext) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SageGreenLight
                            ) {
                                Text(
                                    text = "Next",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = SageGreenPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Prayer Time Display
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = if (isNext) SageGreenPrimary else MediumBrownText,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = prayerState.calculatedTime,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isNext) SageGreenPrimary else DarkBrownText,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.testTag("prayer_time_${prayer.name.lowercase()}")
                        )
                        Text(
                            text = "• ${prayer.subtitle}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MediumBrownText,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Individual Reminder Toggle Button (Bell icon)
                IconButton(
                    onClick = onToggleReminder,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (prayerState.isReminderEnabled) SageGreenLight.copy(alpha = 0.6f)
                            else CreamSurfaceVariant
                        )
                        .testTag("prayer_reminder_${prayer.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = if (prayerState.isReminderEnabled) Icons.Default.Notifications else Icons.Outlined.NotificationsOff,
                        contentDescription = if (prayerState.isReminderEnabled) "Reminder active (tap to turn off)" else "Reminder muted (tap to turn on)",
                        tint = if (prayerState.isReminderEnabled) SageGreenPrimary else MediumBrownText.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Prominent Visible Status and "Mark as prayed" Button Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp), // align with text column
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Visible Status text: "Completed" or "Not completed"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCompleted) SageGreenLight else CreamSurfaceVariant,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isCompleted) SageGreenBorder else CreamBorder
                        )
                    ),
                    modifier = Modifier.testTag("prayer_status_badge_${prayer.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SageGreenPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = MediumBrownText,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = prayerState.statusText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isCompleted) SageGreenPrimary else MediumBrownText,
                                fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                if (prayerState.hasJournalEntry) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CreamSurfaceVariant,
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(SageGreenBorder)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Journal photo attached",
                                tint = SageGreenPrimary,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Photo",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SageGreenPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // "Mark as prayed" / "Tap to undo" toggle button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCompleted) SageGreenPrimary else SageGreenLight,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onToggleCompletion)
                        .testTag("prayer_action_btn_${prayer.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Prayed ✓",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        } else {
                            Text(
                                text = "Mark as prayed",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SageGreenPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
