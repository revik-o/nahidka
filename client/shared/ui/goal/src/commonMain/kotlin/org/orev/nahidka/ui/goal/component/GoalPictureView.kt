package org.orev.nahidka.ui.goal.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import org.orev.nahidka.feature.goals.dto.GoalPicture
import org.orev.nahidka.ui.common.photo.rememberPhotoBitmap

private const val GOAL_PICTURE_SYMBOL_SCALE = 0.5f

@Composable
internal fun GoalPictureView(picture: GoalPicture?, goalTitle: String, pictureSize: Dp) {
    Box(
        modifier = Modifier
            .size(pictureSize)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        when (picture) {
            is GoalPicture.Emoji -> GoalPictureSymbol(picture.symbol, pictureSize)
            is GoalPicture.Photo -> GoalPicturePhoto(picture, goalTitle)
            null -> GoalPictureSymbol(
                goalTitle
                    .take(1)
                    .uppercase(),
                pictureSize,
            )
        }
    }
}

@Composable
private fun GoalPictureSymbol(symbol: String, pictureSize: Dp) {
    Text(
        text = symbol,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        fontSize = with(LocalDensity.current) { (pictureSize * GOAL_PICTURE_SYMBOL_SCALE).toSp() },
        maxLines = 1,
    )
}

@Composable
private fun GoalPicturePhoto(photo: GoalPicture.Photo, goalTitle: String) {
    rememberPhotoBitmap(photo.content)?.let { photoBitmap ->
        Image(
            bitmap = photoBitmap,
            contentDescription = goalTitle,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}
