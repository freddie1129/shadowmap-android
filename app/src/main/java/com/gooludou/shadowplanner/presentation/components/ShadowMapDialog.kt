package com.gooludou.shadowplanner.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.gooludou.shadowplanner.ui.theme.ShadowMapDesign
import com.gooludou.shadowplanner.ui.theme.ShadowMapTheme

@Composable
fun ShadowMapDialog(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    dismissAction: @Composable (() -> Unit)? = null,
    neutralAction: @Composable (() -> Unit)? = null,
    positiveAction: @Composable (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismissRequest) {
        ShadowMapDialogSurface(
            content = content,
            dismissAction = dismissAction,
            neutralAction = neutralAction,
            positiveAction = positiveAction
        )
    }
}

@Composable
private fun ShadowMapDialogSurface(
    content: @Composable ColumnScope.() -> Unit,
    dismissAction: @Composable (() -> Unit)? = null,
    neutralAction: @Composable (() -> Unit)? = null,
    positiveAction: @Composable (() -> Unit)? = null
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = ShadowMapDesign.dimensions.spacingXs
    ) {
        Column(
            modifier = Modifier.padding(ShadowMapDesign.dimensions.spacingExtraLarge),
            verticalArrangement = Arrangement.spacedBy(ShadowMapDesign.dimensions.spacingMedium)
        ) {
            content()
            positiveAction?.invoke()
            neutralAction?.invoke()
            dismissAction?.invoke()
        }
    }
}

@Preview(name = "ShadowMap dialog · light", showBackground = true)
@Composable
private fun ShadowMapDialogLightPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        ShadowMapDialogSurface(content = { Text("Dialog content") })
    }
}

@Preview(name = "ShadowMap dialog · dark", showBackground = true)
@Composable
private fun ShadowMapDialogDarkPreview() {
    ShadowMapTheme(darkTheme = true, dynamicColor = false) {
        ShadowMapDialogSurface(content = { Text("Dialog content") })
    }
}
