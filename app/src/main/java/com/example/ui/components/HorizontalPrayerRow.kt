package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.PrayerType
import com.example.ui.theme.CreamBorder
import com.example.ui.theme.CreamSurface
import com.example.ui.theme.CreamSurfaceVariant
import com.example.ui.theme.DarkBrownText
import com.example.ui.theme.MediumBrownText
import com.example.ui.theme.SageGreenBorder
import com.example.ui.theme.SageGreenLight
import com.example.ui.theme.SageGreenPrimary

@Composable
fun HorizontalPrayerRow(
    prayerList: List<PrayerItemState>,
    onToggleCompletion: (PrayerType) -> Unit,
    onToggleReminder: (PrayerType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Today's Prayers",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = DarkBrownText,
                    fontSize = 17.sp
                )
            )

            Text(
                text = "Swipe to view all five ➔",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MediumBrownText,
                    fontSize = 11.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("horizontal_prayers_lazy_row"),
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = prayerList,
                key = { item -> "h_${item.type.name}" }
            ) { prayerState ->
                HorizontalPrayerCard(
                    prayerState = prayerState,
                    onToggleCompletion = { onToggleCompletion(prayerState.type) },
                    onToggleReminder = { onToggleReminder(prayerState.type) }
                )
            }
        }
    }
}

@Composable
fun HorizontalPrayerCard(
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
            isCompleted -> SageGreenLight.copy(alpha = 0.5f)
            isNext -> CreamSurface
            else -> CreamSurface
        },
        label = "h_card_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isCompleted -> SageGreenBorder
            isNext -> SageGreenPrimary
            else -> CreamBorder
        },
        label = "h_card_border"
    )

    Card(
        modifier = modifier
            .width(210.dp)
            .clip(RoundedCornerShape(18.dp))
            .testTag("h_prayer_card_${prayer.name.lowercase()}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(borderColor)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Icon, Prayer Name, Reminder Bell
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isCompleted) SageGreenPrimary else SageGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = prayer.icon,
                            contentDescription = null,
                            tint = if (isCompleted) Color.White else SageGreenPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = prayer.displayName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = DarkBrownText,
                                fontSize = 14.sp
                            )
                        )
                        Text(
                            text = prayer.arabicName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Serif,
                                color = MediumBrownText,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Reminder Bell
                IconButton(
                    onClick = onToggleReminder,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (prayerState.isReminderEnabled) SageGreenLight.copy(alpha = 0.7f)
                            else CreamSurfaceVariant
                        )
                        .testTag("h_prayer_reminder_${prayer.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = if (prayerState.isReminderEnabled) Icons.Default.Notifications else Icons.Outlined.NotificationsOff,
                        contentDescription = "Toggle reminder for ${prayer.displayName}",
                        tint = if (prayerState.isReminderEnabled) SageGreenPrimary else MediumBrownText.copy(alpha = 0.6f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Time & Next Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = if (isNext) SageGreenPrimary else MediumBrownText,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = prayerState.calculatedTime,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isNext) SageGreenPrimary else DarkBrownText,
                            fontSize = 15.sp
                        ),
                        modifier = Modifier.testTag("h_prayer_time_${prayer.name.lowercase()}")
                    )
                }

                if (isNext) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SageGreenLight
                    ) {
                        Text(
                            text = "Next",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SageGreenPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                } else if (prayerState.hasJournalEntry) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CreamSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = SageGreenPrimary,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "Photo",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SageGreenPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Large, Working Completion Control Button
            Button(
                onClick = onToggleCompletion,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) SageGreenPrimary else SageGreenLight,
                    contentColor = if (isCompleted) Color.White else SageGreenPrimary
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("h_prayer_action_btn_${prayer.name.lowercase()}")
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Prayed ✓",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                } else {
                    Text(
                        text = "Mark as prayed",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}
