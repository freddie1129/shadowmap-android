package com.gooludou.shadowplanner.feature.shadowmap.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gooludou.shadowplanner.core.ui.theme.ShadowMapTheme

@Composable
internal fun MapCenterPlus(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier = modifier.size(40.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val arm = 12.dp.toPx()
        val lineWidth = 1.dp.toPx()
        drawLine(color, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), lineWidth)
        drawLine(color, Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), lineWidth)
    }
}

@Preview(name = "Map center plus", showBackground = true)
@Composable
private fun MapCenterPlusPreview() {
    ShadowMapTheme(darkTheme = false, dynamicColor = false) {
        Surface(color = Color(0xFF6B8064)) {
            Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                MapCenterPlus()
            }
        }
    }
}
