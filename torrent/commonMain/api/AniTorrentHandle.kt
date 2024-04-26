package me.him188.ani.app.torrent.api

import me.him188.ani.app.torrent.api.pieces.Piece
import org.libtorrent4j.AnnounceEntry
import org.libtorrent4j.FileStorage
import org.libtorrent4j.Priority
import org.libtorrent4j.TorrentHandle

interface AniTorrentHandle {
    val name: String

    fun addTracker(url: String)

    val contents: TorrentContents

    fun resume()
    fun pause()

    fun setPieceDeadline(pieceIndex: Int, deadline: Int)
}

interface TorrentContents {
    fun createPieces(): List<Piece>

    val files: List<TorrentFile>
}

interface TorrentFile {
    val path: String
    val size: Long
    var priority: FilePriority
}


internal fun TorrentHandle.asAniTorrentHandle(): AniTorrentHandle = Torrent4jHandle(this)

internal class Torrent4jHandle(
    private val handle: TorrentHandle,
) : AniTorrentHandle {
    override val name: String get() = handle.name

    override fun addTracker(url: String) {
        handle.addTracker(AnnounceEntry(url))
    }

    override val contents: TorrentContents by lazy { Torrent4JContents(handle) }
    override fun resume() {
        handle.resume()
    }

    override fun pause() {
        handle.pause()
    }

    override fun setPieceDeadline(pieceIndex: Int, deadline: Int) {
        handle.setPieceDeadline(pieceIndex, deadline)
    }
}

class Torrent4JContents(
    private val handle: TorrentHandle,
) : TorrentContents {
    override fun createPieces(): List<Piece> {
        val info = torrentInfo
        val numPieces = info.numPieces()
        val pieces =
            Piece.buildPieces(numPieces) { info.pieceSize(it).toUInt().toLong() }
        return pieces
    }

    override val files: List<TorrentFile> by lazy {
        val files: FileStorage = torrentInfo.files()
        List(files.numPieces()) { Torrent4jFile(handle, files, it) }
    }

    private val torrentInfo: org.libtorrent4j.TorrentInfo
        get() {
            val torrentInfo: org.libtorrent4j.TorrentInfo? = handle.torrentFile()
            check(torrentInfo != null) {
                "${handle.name}: Actual torrent info is null"
            }
            return torrentInfo
        }
}

private class Torrent4jFile(
    private val handle: TorrentHandle,
    private val files: FileStorage,
    private val index: Int,
) : TorrentFile {
    override val size: Long
        get() = files.fileSize(index)
    override val path: String
        get() = files.filePath(index)
    override var priority: FilePriority
        get() = handle.filePriority(index).toFilePriority()
        set(value) {
            handle.filePriority(index, value.toLibtorrentPriority())
        }
}

internal fun FilePriority.toLibtorrentPriority(): Priority = when (this) {
    FilePriority.HIGH -> Priority.TOP_PRIORITY
    FilePriority.NORMAL -> Priority.DEFAULT
    FilePriority.LOW -> Priority.TWO
    FilePriority.IGNORE -> Priority.IGNORE
}

internal fun Priority.toFilePriority(): FilePriority {
    return when (this) {
        Priority.IGNORE -> FilePriority.IGNORE
        Priority.LOW -> FilePriority.LOW
        Priority.TWO -> FilePriority.NORMAL
        Priority.THREE -> FilePriority.NORMAL
        Priority.DEFAULT -> FilePriority.NORMAL
        Priority.FIVE -> FilePriority.NORMAL
        Priority.SIX -> FilePriority.NORMAL
        Priority.TOP_PRIORITY -> FilePriority.HIGH
    }
}