package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CreamBorder
import com.example.ui.theme.CreamSurface
import com.example.ui.theme.DarkBrownText
import com.example.ui.theme.MediumBrownText
import com.example.ui.theme.SageGreenLight
import com.example.ui.theme.SageGreenPrimary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun PrayerProgressCard(
    completedCount: Int,
    totalCount: Int,
    selectedDate: LocalDate,
    isToday: Boolean,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        label = "prayer_progress"
    )

    val titleText = if (isToday) {
        "Today's Prayer Progress"
    } else {
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
        "Prayer Progress • ${selectedDate.format(formatter)}"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("prayer_progress_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = CreamSurface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CreamBorder)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = DarkBrownText,
                            fontSize = 18.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$completedCount of $totalCount prayers completed",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (completedCount == totalCount && totalCount > 0) SageGreenPrimary else MediumBrownText,
                            fontWeight = if (completedCount == totalCount) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        modifier = Modifier.testTag("progress_summary_text")
                    )
                }

                if (completedCount > 0) {
                    IconButton(
                        onClick = onResetClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SageGreenLight.copy(alpha = 0.5f))
                            .testTag("reset_progress_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Reset prayers for this date",
                            tint = SageGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Smooth Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = SageGreenPrimary,
                trackColor = SageGreenLight,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Calm feedback text
            val feedbackMessage = when {
                completedCount == totalCount -> "All 5 prayers marked complete for this day. Al-Hamdulillah!"
                completedCount > 0 -> "Keep up your spiritual discipline and mindfulness."
                else -> "Mark your completed prayers throughout the day."
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (completedCount == totalCount) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                    contentDescription = null,
                    tint = if (completedCount == totalCount) SageGreenPrimary else MediumBrownText.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = feedbackMessage,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MediumBrownText,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}
