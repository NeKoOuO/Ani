package me.him188.ani.app.videoplayer.torrent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import me.him188.ani.app.torrent.TorrentDownloadSession
import me.him188.ani.app.torrent.TorrentDownloader
import me.him188.ani.app.videoplayer.Video
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.Closeable
import java.io.File

class TorrentVideo(
    private val torrentSession: TorrentDownloadSession,
) : Video, Closeable {
    override val file: File get() = torrentSession.savedFile
    override val totalBytes: Flow<Long> get() = torrentSession.totalBytes
    override val downloadedBytes: Flow<Long> get() = torrentSession.downloadedBytes
    override val downloadRate: Flow<Long> get() = torrentSession.downloadRate
    override val downloadProgress: Flow<Float> get() = torrentSession.progress

    override val torrentSource: TorrentDownloadSession
        get() = torrentSession

    override val length: Flow<Int> = MutableSharedFlow() // TODO: read video metadata and get length

    override fun close() {
        torrentSession.close()
    }
}

interface TorrentVideoFactory {
    suspend fun createFromMagnet(magnet: String): TorrentVideo
}

internal class TorrentVideoFactoryImpl : TorrentVideoFactory, KoinComponent {
    private val torrentDownloader: TorrentDownloader by inject()

    override suspend fun createFromMagnet(magnet: String): TorrentVideo {
        val torrentData = withContext(Dispatchers.IO) {
            torrentDownloader.fetchMagnet(magnet)
        }

        val torrentSession = torrentDownloader.startDownload(torrentData)
        return TorrentVideo(torrentSession)
    }
}