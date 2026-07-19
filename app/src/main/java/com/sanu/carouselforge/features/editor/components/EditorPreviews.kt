package com.sanu.carouselforge.features.editor.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sanu.carouselforge.core.theme.CarouselForgeTheme
import com.sanu.carouselforge.features.editor.EditorState

@Preview(
    name = "Compact 320",
    device = "spec:width=320dp,height=568dp,dpi=420",
    showSystemUi = true,
)
@Preview(
    name = "Phone 360",
    device = "spec:width=360dp,height=800dp,dpi=420",
    showSystemUi = true,
)
@Preview(
    name = "Large phone 411",
    device = "spec:width=411dp,height=891dp,dpi=420",
    showSystemUi = true,
)
@Preview(
    name = "Landscape",
    device = "spec:width=800dp,height=360dp,dpi=420",
    showSystemUi = true,
)
@Preview(
    name = "Tablet",
    device = "spec:width=600dp,height=960dp,dpi=320",
    showSystemUi = true,
)
@Composable
private fun CanvasViewportPreview() {
    CarouselForgeTheme {
        CanvasViewport(
            state = EditorState.Editing(
                layers = emptyList(),
                selectedLayerId = null,
                gridSnapEnabled = true,
                canvasWidth = 1080,
                canvasHeight = 1080,
            ),
            onSelectLayer = {},
            onTransform = { _, _ -> },
            onGestureEnd = { _, _, _ -> },
            onAddImage = {},
            modifier = Modifier,
        )
    }
}
