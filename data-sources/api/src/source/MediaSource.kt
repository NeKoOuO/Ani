/*
 * Ani
 * Copyright (C) 2022-2024 Him188
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.him188.ani.datasources.api.source

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import me.him188.ani.datasources.api.EpisodeSort
import me.him188.ani.datasources.api.Media
import me.him188.ani.datasources.api.paging.SizedSource
import me.him188.ani.datasources.api.topic.ResourceLocation
import java.io.File
import kotlin.contracts.contract

/**
 * 一个查询单个剧集的可下载的资源的服务, 称为数据源.
 * 数据源不提供条目数据, 而是依赖条目服务 (即 Bangumi) 提供的条目数据.
 * 因此数据源只需要支持查询该剧集的所有可下载资源.
 *
 * 未来可能会支持更多的条目服务, 但目前只支持 Bangumi.
 *
 * ### 查询
 *
 * 数据源从一个地方查询资源, 例如在线视频网站, BitTorrent 网络, 本地文件系统等.
 *
 * 每一次查询 ([MediaSource.fetch]) 都需要一个查询请求 [MediaFetchRequest],
 * 数据源尽可能多地使用请求中的信息去匹配资源, 并返回匹配到的资源列表.
 *
 * ### 数据源全局唯一
 *
 * 每个数据源都拥有全局唯一的 ID [mediaSourceId], 可用于保存用户偏好, 识别缓存资源的来源等.
 */
interface MediaSource {
    /**
     * 全局唯一的 ID. 可用于保存用户偏好, 识别缓存资源的来源等.
     */
    val mediaSourceId: String

    /**
     * 数据源 [MediaSource] 以及资源 [Media] 的存放位置,
     * 因为一个资源既可以来源于网络, 也可以来自本地文件系统等.
     */
    val location: MediaSourceLocation
        get() = MediaSourceLocation.Online

    /**
     * 数据源类型. 不同类型的资源在缓冲速度上可能有本质上的区别.
     */
    val kind: MediaSourceKind

    /**
     * 检查该数据源是否可用.
     *
     * @see Boolean.toConnectionStatus
     */ // 这会在设置的数据源测试中使用.
    suspend fun checkConnection(): ConnectionStatus

    /**
     * 使用 [MediaFetchRequest] 中的信息, 尽可能多地查询一个剧集的所有可下载的资源, 返回一个分页的资源列表.
     */
    suspend fun fetch(query: MediaFetchRequest): SizedSource<MediaMatch>
}

/**
 * 数据源类型.
 *
 * 不同类型的资源在缓冲速度上可能有本质上的区别.
 */ // 在数据源选择器中, 每个数据源会以 [MediaSourceKind] 分类展示.
@Serializable
enum class MediaSourceKind {
    /**
     * 在线视频网站. 资源为 [ResourceLocation.WebVideo] 或 [ResourceLocation.HttpStreamingFile].
     */
    WEB,

    /**
     * P2P BitTorrent 网络. 资源为 [ResourceLocation.HttpTorrentFile] 或 [ResourceLocation.MagnetLink]
     */
    BitTorrent,

    /**
     * 本地视频缓存. 只表示那些通过 `MediaCacheManager` 缓存的视频.
     */
    LocalCache
}

/**
 * A media matched from the source.
 */
data class MediaMatch(
    val media: Media,
    val kind: MatchKind,
)

enum class MatchKind {
    /**
     * The request has an exact match with the cache.
     * Usually because episode id is the same.
     */
    EXACT,

    /**
     * The request does not have a [EXACT] match but a [FUZZY] one.
     *
     * This is done on a best-effort basis where they can be false positives.
     */
    FUZZY,

    /**
     * The request does not match the cache.
     *
     * This is done on a best-effort basis where they can be false negatives.
     */
    NONE
}

/**
 * 一个数据源查询请求. 该请求包含尽可能多的信息以便 [MediaSource] 可以查到尽可能多的结果.
 */
@Serializable
class MediaFetchRequest(
    /**
     * 条目服务 (Bangumi) 提供的条目 ID. 若数据源支持, 可以用此信息做精确匹配.
     * 可能为 `null`, 表示未知.
     */
    val subjectId: String? = null,
    /**
     * 条目服务 (Bangumi) 提供的剧集 ID. 若数据源支持, 可以用此信息做精确匹配.
     * 可能为 `null`, 表示未知.
     */
    val episodeId: String? = null,
    /**
     * 条目的主简体中文名称.
     * 建议使用 [subjectNames] 用所有已知名称去匹配.
     */
    val subjectNameCN: String? = null,
    /**
     * 已知的该条目的所有名称. 包含季度信息.
     *
     * 所有名称包括简体中文译名, 各种别名, 简称, 以及日文原名.
     *
     * E.g. "关于我转生变成史莱姆这档事 第三季"
     */
    val subjectNames: Set<String>,
    /**
     * 在系列中的集数, 例如第二季的第一集为 26.
     *
     * E.g. "49", "01".
     *
     * @see EpisodeSort
     */
    val episodeSort: EpisodeSort,
    /**
     * 条目服务 (Bangumi) 提供的剧集名称, 例如 "恶魔与阴谋", 不会包含 "第 x 集".
     * 不一定为简体中文, 可能为日文. 也可能为空字符串.
     */
    val episodeName: String,
    /**
     * 在当前季度中的集数, 例如第二季的第一集为 01
     *
     * E.g. "49", "01".
     *
     * @see EpisodeSort
     */
    val episodeEp: EpisodeSort? = episodeSort,
)


/**
 * 数据源 [MediaSource] 以及资源 [Media] 的存放位置.
 */
@Serializable(MediaSourceLocation.AsStringSerializer::class)
sealed class MediaSourceLocation {
    /**
     * 资源位于公共网络. 例如一个在线视频网站, 或者 BitTorrent 网络.
     */
    data object Online : MediaSourceLocation()

    /**
     * 资源位于当前局域网内 (下载很快延迟很低). 例如 NAS 或自建的视频服务器.
     * 如果需要通过公网访问自建视频服务器, 那该服务器属于 [Online] 而不是 [Lan].
     */
    data object Lan : MediaSourceLocation()

    /**
     * 资源位于本地文件系统. 必须是能通过 [File] 直接访问的.
     */
    data object Local : MediaSourceLocation()

    companion object {
        val entries = listOf(Online, Lan, Local)
    }

    internal object AsStringSerializer : KSerializer<MediaSourceLocation> {
        override val descriptor = String.serializer().descriptor

        override fun serialize(encoder: Encoder, value: MediaSourceLocation) {
            encoder.encodeString(
                getText(value)
            )
        }

        private fun getText(value: MediaSourceLocation) = when (value) {
            Online -> "ONLINE"
            Lan -> "LAN"
            Local -> "LOCAL"
        }

        override fun deserialize(decoder: Decoder): MediaSourceLocation {
            val string = decoder.decodeString()
            for (entry in entries) { // type safe
                if (getText(entry) == string) {
                    return entry
                }
            }
            throw SerializationException("Unknown MediaSourceLocation: $string")
        }
    }
}

fun MediaSourceLocation.isLowEffort(): Boolean {
    contract {
        returns(false) implies (this@isLowEffort is MediaSourceLocation.Online)
        returns(true) implies (this@isLowEffort is MediaSourceLocation.Lan || this@isLowEffort is MediaSourceLocation.Local)
    }
    return this is MediaSourceLocation.Lan || this is MediaSourceLocation.Local
}

enum class ConnectionStatus {
    SUCCESS,
    FAILED,
}

fun Boolean.toConnectionStatus() = if (this) ConnectionStatus.SUCCESS else ConnectionStatus.FAILED

interface SearchOrdering {
    val id: String
    val name: String
}
