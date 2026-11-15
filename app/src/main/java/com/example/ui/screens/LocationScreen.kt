package com.example.ui.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.toArgb
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.calculation.AsrMethod
import com.example.data.calculation.CalculationMethod
import com.example.data.location.CityLocation
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.CreamBorder
import com.example.ui.theme.CreamSurface
import com.example.ui.theme.CreamSurfaceVariant
import com.example.ui.theme.DarkBrownText
import com.example.ui.theme.MediumBrownText
import com.example.ui.theme.SageGreenBorder
import com.example.ui.theme.SageGreenLight
import com.example.ui.theme.SageGreenPrimary
import com.example.ui.viewmodel.PrayerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    uiState: PrayerUiState,
    onNavigateHome: () -> Unit,
    onSelectCity: (CityLocation) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCalculationMethodChange: (CalculationMethod) -> Unit,
    onAsrMethodChange: (AsrMethod) -> Unit,
    onDetectGpsLocation: (Context) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var permissionDeniedNotice by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            permissionDeniedNotice = false
            onDetectGpsLocation(context)
        } else {
            permissionDeniedNotice = true
        }
    }

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                window.statusBarColor = CreamBackground.toArgb()
                androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = CreamBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateHome,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CreamSurface)
                        .border(1.dp, CreamBorder, CircleShape)
                        .testTag("location_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = DarkBrownText
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Location & Prayer Times",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = DarkBrownText,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = "Configure your city & calculation settings",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MediumBrownText,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                        .testTag("location_scroll_container"),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Current Selected Location Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("active_location_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CreamSurface),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(SageGreenBorder)
                            )
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(SageGreenLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = SageGreenPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "Current City",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MediumBrownText,
                                                    fontSize = 11.sp
                                                )
                                            )
                                            Text(
                                                text = "${uiState.selectedLocation.name}, ${uiState.selectedLocation.country}",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = DarkBrownText,
                                                    fontSize = 17.sp
                                                ),
                                                modifier = Modifier.testTag("selected_city_name_text")
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = SageGreenPrimary
                                    ) {
                                        Text(
                                            text = "Active",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Coordinates: ${String.format("%.4f", uiState.selectedLocation.latitude)}°, ${String.format("%.4f", uiState.selectedLocation.longitude)}° • Zone: ${uiState.selectedLocation.timezoneId}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MediumBrownText,
                                        fontSize = 12.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SageGreenLight.copy(alpha = 0.6f),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(SageGreenBorder)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "✓ Saved permanently in local storage. GPS does NOT need to remain on.",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = SageGreenPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // GPS Button
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    },
                                    enabled = !uiState.isDetectingLocation,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SageGreenPrimary,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("gps_location_button")
                                ) {
                                    if (uiState.isDetectingLocation) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Detecting current location...")
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Auto-Detect with GPS (Optional)",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Permission Denied Info Banner
                    if (permissionDeniedNotice) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = CreamSurfaceVariant,
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(CreamBorder)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("permission_denied_notice")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = SageGreenPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Location permission is optional. You can search and pick any city manually below.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = DarkBrownText,
                                            fontSize = 12.sp
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { permissionDeniedNotice = false },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = MediumBrownText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Location Error Banner
                    if (uiState.locationErrorMessage != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = CreamSurfaceVariant,
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(CreamBorder)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("location_error_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = uiState.locationErrorMessage,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = DarkBrownText,
                                            fontSize = 12.sp
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = onDismissError,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = MediumBrownText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Today's Calculated Prayer Times Preview for Selected City
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("prayer_times_preview_card"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = CreamSurface),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(CreamBorder)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Calculated Times for Today (${uiState.selectedLocation.name})",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = DarkBrownText,
                                        fontSize = 14.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                val times = uiState.calculatedPrayerTimes
                                val prayerRows = listOf(
                                    "Fajr" to times.formattedFajr,
                                    "Sunrise" to times.formattedSunrise,
                                    "Dhuhr" to times.formattedDhuhr,
                                    "Asr" to times.formattedAsr,
                                    "Maghrib" to times.formattedMaghrib,
                                    "Isha" to times.formattedIsha
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    prayerRows.take(3).forEach { (name, time) ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MediumBrownText,
                                                    fontSize = 11.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = time,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = SageGreenPrimary,
                                                    fontSize = 13.sp
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    prayerRows.drop(3).forEach { (name, time) ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MediumBrownText,
                                                    fontSize = 11.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = time,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = SageGreenPrimary,
                                                    fontSize = 13.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Calculation & Asr Method Settings
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("calculation_settings_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CreamSurface),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(CreamBorder)
                            )
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = null,
                                        tint = SageGreenPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Calculation Settings",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.Bold,
                                            color = DarkBrownText,
                                            fontSize = 16.sp
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Calculation Method Dropdown
                                Text(
                                    text = "Calculation Method",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = DarkBrownText,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                var expandedMethod by remember { mutableStateOf(false) }

                                ExposedDropdownMenuBox(
                                    expanded = expandedMethod,
                                    onExpandedChange = { expandedMethod = !expandedMethod },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = uiState.calculationMethod.title,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMethod) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                            .testTag("calculation_method_dropdown"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = SageGreenPrimary,
                                            unfocusedBorderColor = CreamBorder,
                                            focusedContainerColor = CreamSurface,
                                            unfocusedContainerColor = CreamSurface
                                        )
                                    )

                                    ExposedDropdownMenu(
                                        expanded = expandedMethod,
                                        onDismissRequest = { expandedMethod = false },
                                        modifier = Modifier.background(CreamSurface)
                                    ) {
                                        CalculationMethod.values().forEach { method ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = method.title,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            color = DarkBrownText,
                                                            fontWeight = if (uiState.calculationMethod == method) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    )
                                                },
                                                onClick = {
                                                    onCalculationMethodChange(method)
                                                    expandedMethod = false
                                                },
                                                trailingIcon = {
                                                    if (uiState.calculationMethod == method) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = SageGreenPrimary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Asr Juristic Method (Madhab)
                                Text(
                                    text = "Asr Juristic Method (Madhab)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = DarkBrownText,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AsrMethod.values().forEach { method ->
                                        val isSelected = uiState.asrMethod == method
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) SageGreenPrimary else CreamSurfaceVariant,
                                            border = CardDefaults.outlinedCardBorder().copy(
                                                brush = androidx.compose.ui.graphics.SolidColor(
                                                    if (isSelected) SageGreenPrimary else CreamBorder
                                                )
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onAsrMethodChange(method) }
                                                .testTag("asr_method_${method.name.lowercase()}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = if (method == AsrMethod.STANDARD) "Standard (Shafi'i)" else "Hanafi",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        color = if (isSelected) Color.White else DarkBrownText,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. City Search & Selection
                    item {
                        Text(
                            text = "Search & Select City",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = DarkBrownText,
                                fontSize = 16.sp
                            ),
                            modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = uiState.citySearchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("city_search_input"),
                            placeholder = {
                                Text(
                                    text = "Search city or country (e.g. London, Istanbul, New York)...",
                                    color = MediumBrownText,
                                    fontSize = 13.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = SageGreenPrimary
                                )
                            },
                            trailingIcon = {
                                if (uiState.citySearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = MediumBrownText
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SageGreenPrimary,
                                unfocusedBorderColor = CreamBorder,
                                focusedContainerColor = CreamSurface,
                                unfocusedContainerColor = CreamSurface
                            ),
                            singleLine = true
                        )
                    }

                    // Filtered Cities list
                    items(
                        items = uiState.filteredCities,
                        key = { "${it.name}_${it.country}" }
                    ) { city ->
                        val isSelected = city.name == uiState.selectedLocation.name &&
                                city.country == uiState.selectedLocation.country

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectCity(city) }
                                .testTag("city_item_${city.name.lowercase().replace(" ", "_")}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) SageGreenLight.copy(alpha = 0.5f) else CreamSurface
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isSelected) SageGreenBorder else CreamBorder
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) SageGreenPrimary else SageGreenLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else SageGreenPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = city.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = DarkBrownText
                                            )
                                        )
                                        Text(
                                            text = "${city.country} • ${city.timezoneId}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MediumBrownText,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = SageGreenPrimary,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
