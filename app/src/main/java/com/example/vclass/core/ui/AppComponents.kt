package com.example.vclass.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.text.SimpleDateFormat
import java.util.TimeZone
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.vclass.core.ui.theme.BrandBackground
import com.example.vclass.core.ui.theme.BrandForeground

internal object AppSpacing {
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppTopBar(
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title, modifier = Modifier.semantics { heading() }) },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BrandBackground,
            scrolledContainerColor = BrandBackground,
            navigationIconContentColor = BrandForeground,
            titleContentColor = BrandForeground,
            actionIconContentColor = BrandForeground
        )
    )
}

@Composable
internal fun EmptyContent(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(AppSpacing.medium), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun rememberDateFormatter(pattern: String, zone: TimeZone = TimeZone.getDefault()): SimpleDateFormat {
    val locale = LocalConfiguration.current.locales[0]
    return remember(pattern, locale, zone) { SimpleDateFormat(pattern, locale).apply { timeZone = zone } }
}
