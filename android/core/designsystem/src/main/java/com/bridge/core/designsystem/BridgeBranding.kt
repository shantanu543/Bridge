package com.bridge.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** The supplied Bridge gradient mark, cropped to its visible artwork for a true-to-brand lockup. */
@Composable
fun BridgeLogoMark(
    modifier: Modifier = Modifier,
    contentDescription: String = "Bridge logo",
) {
    Image(
        painter = painterResource(R.drawable.bridge_logo_mark),
        contentDescription = contentDescription,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        contentScale = ContentScale.Fit,
    )
}
