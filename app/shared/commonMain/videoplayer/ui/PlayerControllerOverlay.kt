package me.him188.ani.app.videoplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.him188.ani.app.ui.foundation.AniTopAppBar
import me.him188.ani.app.ui.theme.aniDarkColorTheme
import me.him188.ani.app.ui.theme.slightlyWeaken
import me.him188.ani.app.ui.theme.weaken
import me.him188.ani.app.videoplayer.PlayerController
import me.him188.ani.app.videoplayer.Video
import moe.tlaster.precompose.flow.collectAsStateWithLifecycle


/**
 * 覆盖在视频播放器上层的控制器
 */
@Composable
fun PlayerControllerOverlay(
    topBar: @Composable RowScope.() -> Unit = {},
    floatingBox: @Composable BoxScope.() -> Unit = {},
    bottomBar: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize(), color = Color.Transparent) {
        Box {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth()) {
                    topBar()
                }

                Spacer(Modifier.weight(1f, fill = true))

                Row(Modifier.fillMaxWidth()) {
                    CompositionLocalProvider(LocalContentColor provides Color.White) {
                        bottomBar()
                    }
                }
            }

            Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                    CompositionLocalProvider(LocalContentColor provides aniDarkColorTheme().onBackground.slightlyWeaken()) {
                        floatingBox()
                    }
                }
            }
        }
    }
}

@Composable
internal expect fun PreviewVideoControllerOverlay()


@Composable
fun PlayerControllerOverlayBottomBar(
    video: Video,
    controller: PlayerController,
    modifier: Modifier = Modifier,
) {
    val darkBackground = aniDarkColorTheme().background
    Row(
        Modifier.background(
            // 渐变, 靠近视频的区域透明
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                (1 - 0.612f) to darkBackground.copy(alpha = 0.08f),
                1f to darkBackground.copy(alpha = 0.9f),
            )
        ).then(modifier), verticalAlignment = Alignment.CenterVertically
    ) {
        // 播放 / 暂停按钮
        val state by controller.state.collectAsStateWithLifecycle(null)
        Box(Modifier.padding(horizontal = 8.dp).size(32.dp)) {
            IconButton(
                onClick = {
                    if (state?.isPlaying == true) {
                        controller.pause()
                    } else {
                        controller.resume()
                    }
                },
            ) {
                if (state?.isPlaying == true) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                } else {
                    Icon(Icons.Default.Pause, contentDescription = null)
                }
            }
        }

        val downloadProgress by video.downloadProgress.collectAsStateWithLifecycle(0f)
        val lengthDuration by video.length.collectAsStateWithLifecycle(0)
        val playedDuration by controller.playedDuration.collectAsStateWithLifecycle(0)
        val playProgress by controller.playProgress.collectAsStateWithLifecycle(0f)

        Text(
            text = remember(playedDuration, lengthDuration) { renderSeconds(playedDuration, lengthDuration) },
            Modifier.padding(end = 8.dp),
            style = MaterialTheme.typography.labelSmall,
        )

        Box(Modifier.weight(1f).height(4.dp).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            LinearProgressIndicator(
                modifier = Modifier.matchParentSize(),
                progress = downloadProgress,
                trackColor = MaterialTheme.colorScheme.tertiary.weaken(),
            )
            LinearProgressIndicator(
                modifier = Modifier.matchParentSize().alpha(0.9f),
                progress = playProgress,
                trackColor = MaterialTheme.colorScheme.primary,
            )
        }

        Box(Modifier.padding(horizontal = 8.dp).size(32.dp)) {
            IconButton(
                onClick = {
                    // TODO: full screen 
                },
            ) {
                Icon(Icons.Default.Fullscreen, contentDescription = null)
            }
        }
    }
}

private fun renderSeconds(played: Int, length: Int): String {
    return if (played < 60 && length < 60) {
        "0:${played} / 0:${length}"
    } else if (played < 3600 && length < 3600) {
        "${played / 60}:${played % 60} / ${length / 60}:${length % 60}"
    } else {
        "${played / 3600}:${played % 3600 / 60}:${played % 60} / ${length / 3600}:${length % 3600 / 60}:${length % 60}"
    }
}


@Composable
fun PlayerControllerOverlayTopBar(
    alpha: Float,
    startActions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val darkBackground = aniDarkColorTheme().background
    CompositionLocalProvider(LocalContentColor provides aniDarkColorTheme().onBackground) {
        AniTopAppBar(
            Modifier
                .alpha(alpha)
                .background(
                    // 渐变, 靠近视频的区域透明
                    brush = Brush.verticalGradient(
                        0f to darkBackground.copy(alpha = 0.9f),
                        0.612f to darkBackground.copy(alpha = 0.8f),
                        1.00f to Color.Transparent,
                    )
                )
                .then(modifier)
                .fillMaxWidth(),
            actions = startActions,
            containerColor = Color.Transparent
        )
    }
}