package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.localization.LocalAppStrings

@Composable
fun ThemeModeSwitcher(
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("theme_switcher_row")
    ) {
        SegmentedButton(
            selected = !darkTheme,
            onClick = { onDarkThemeChange(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            icon = {
                Icon(
                    imageVector = if (!darkTheme) Icons.Filled.LightMode else Icons.Outlined.LightMode,
                    contentDescription = strings.lightModeLabel,
                    modifier = Modifier.size(18.dp)
                )
            },
            label = {
                Text(
                    text = strings.lightModeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("theme_light_button")
        )

        SegmentedButton(
            selected = darkTheme,
            onClick = { onDarkThemeChange(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            icon = {
                Icon(
                    imageVector = if (darkTheme) Icons.Filled.DarkMode else Icons.Outlined.DarkMode,
                    contentDescription = strings.darkModeLabel,
                    modifier = Modifier.size(18.dp)
                )
            },
            label = {
                Text(
                    text = strings.darkModeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("theme_dark_button")
        )
    }
}

@Composable
fun LanguageModeSwitcher(
    languageCode: String,
    onLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("language_switcher_row")
    ) {
        SegmentedButton(
            selected = languageCode == "ar",
            onClick = { onLanguageChange("ar") },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            icon = {
                if (languageCode == "ar") {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = "العربية",
                        modifier = Modifier.size(16.dp)
                    )
                }
            },
            label = {
                Text("العربية", style = MaterialTheme.typography.labelLarge)
            },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("lang_arabic_button")
        )

        SegmentedButton(
            selected = languageCode == "en",
            onClick = { onLanguageChange("en") },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            icon = {
                if (languageCode == "en") {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = "English",
                        modifier = Modifier.size(16.dp)
                    )
                }
            },
            label = {
                Text("English", style = MaterialTheme.typography.labelLarge)
            },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("lang_english_button")
        )
    }
}
