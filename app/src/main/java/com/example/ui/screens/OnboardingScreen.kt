package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent

@Composable
fun OnboardingScreen(
    onFinish: (country: String, language: String, currency: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(1) }
    var selectedCountry by remember { mutableStateOf("IR") } // "IR" or "US"
    var selectedLanguage by remember { mutableStateOf("fa") } // "fa" or "en"

    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF061A14),
            Color(0xFF0B0F15),
            Color(0xFF0B0F15)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header / Brand
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.2f))
                        .border(1.5.dp, EmeraldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "💰", fontSize = 36.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "SINA FINANCE",
                    style = MaterialTheme.typography.headlineMedium,
                    color = EmeraldPrimary,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "Personal Finance Manager",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8)
                )

                Text(
                    text = "Developer: Sina Naderi",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldAccent.copy(alpha = 0.8f)
                )
            }

            // Step Content
            when (step) {
                1 -> {
                    // Country & Language selection
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Select Country & Currency",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "Choose your primary regional standard. You can switch at any time without data loss.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Iran Option
                        CountrySelectionCard(
                            flag = "🇮🇷",
                            countryName = "Iran (ایران)",
                            currencyName = "Toman (تومان)",
                            languageName = "Persian (فارسی)",
                            isSelected = selectedCountry == "IR",
                            onClick = {
                                selectedCountry = "IR"
                                selectedLanguage = "fa"
                            }
                        )

                        // US Option
                        CountrySelectionCard(
                            flag = "🇺🇸",
                            countryName = "United States",
                            currencyName = "US Dollar ($)",
                            languageName = "English",
                            isSelected = selectedCountry == "US",
                            onClick = {
                                selectedCountry = "US"
                                selectedLanguage = "en"
                            }
                        )
                    }
                }

                2 -> {
                    // Privacy & Security Guarantee
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(32.dp))
                        }

                        Text(
                            text = "100% Offline & Private",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131922))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PrivacyBullet("Local Database: All transactions and records are stored exclusively on your device.")
                                PrivacyBullet("No Unsolicited Tracking: No financial data is sent to external servers.")
                                PrivacyBullet("Safe SMS Processing: Sensitive OTPs and passwords are never extracted or read.")
                                PrivacyBullet("Optional Cloud Sync: Cloud backup only runs with your explicit Google authentication.")
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Buttons
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (step == 1) {
                            step = 2
                        } else {
                            val curr = if (selectedCountry == "IR") "TOMAN" else "USD"
                            onFinish(selectedCountry, selectedLanguage, curr)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(
                        text = if (step == 1) "Continue" else "Get Started",
                        color = Color.Black,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (step == 2) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { step = 1 },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back", color = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }
}

@Composable
private fun CountrySelectionCard(
    flag: String,
    countryName: String,
    currencyName: String,
    languageName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) EmeraldPrimary else Color(0xFF243042),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF13221C) else Color(0xFF131922)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = flag, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = countryName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "$currencyName • $languageName",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) EmeraldPrimary else Color(0xFF94A3B8)
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun PrivacyBullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(EmeraldPrimary)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFCBD5E1),
            lineHeight = 18.sp
        )
    }
}
