package com.huanchengfly.tieba.post.ui.page.photoview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.iielse.imageviewer.adapter.ItemType
import com.github.iielse.imageviewer.core.DataProvider
import com.github.iielse.imageviewer.core.Photo
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.PicPageBean
import com.huanchengfly.tieba.post.api.models.bestQualitySrc
import com.huanchengfly.tieba.post.api.models.isGif
import com.huanchengfly.tieba.post.api.models.isLongPic
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.firstOrThrow
import com.huanchengfly.tieba.post.models.LoadPicPageData
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.models.PicItem
import com.huanchengfly.tieba.post.ui.widgets.compose.video.util.set
import com.huanchengfly.tieba.post.utils.JobQueue
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotoViewViewModel : ViewModel(), DataProvider {

    private val _state: MutableStateFlow<PhotoViewUiState> = MutableStateFlow(PhotoViewUiState())
    val state: StateFlow<PhotoViewUiState> get() = _state

    private val handler = TbLiteExceptionHandler(TAG) { _, e, _ ->
        _state.update { it.copy(error = e) }
    }

    private val queue = JobQueue()

    private var data: LoadPicPageData? = null

    private fun List<PicPageBean.PicBean>.toUniquePhotoViewItems(old: List<PhotoViewItem>): List<PhotoViewItem> {
        val oldDataIds = old.mapTo(HashSet()) { it.picId }
        return this
            .filterNot { oldDataIds.contains(it.img.original.id) }
            .map { it.toPhotoItem() }
    }

    fun initData(viewData: PhotoViewData) {
        if (this.data != null) return
        this.data = viewData.data

        if (viewData.data == null) {
             _state.set {
                 copy(
                     data = viewData.picItems
                         .mapIndexed { i, item -> PhotoViewItem(item = item, overallIndex = i + 1) },
                     totalAmount = viewData.picItems.size,
                     initialIndex = viewData.index
                 )
             }
        } else {
            viewModelScope.launch(Dispatchers.Default + handler) {
                val picPageBean = viewData.data
                    .toPageFlow(viewData.data.picId, viewData.data.picIndex, prev = false)
                    .firstOrThrow()

                val stateSnapshot = _state.first()
                val picAmount = picPageBean.picAmount ?: throw TiebaException("加载列表失败, 远古坟贴?")
                val fetchedItems = picPageBean.picList.toUniquePhotoViewItems(old = stateSnapshot.data)
                val firstItemIndex = fetchedItems.first().overallIndex
                val localItems =
                    if (viewData.data.picIndex == 1) emptyList() else viewData.picItems.subList(
                        0,
                        viewData.data.picIndex - 1
                    ).mapIndexed { index, item ->
                        PhotoViewItem(
                            item = item,
                            overallIndex = firstItemIndex - (viewData.data.picIndex - 1 - index),
                        )
                    }
                val items = localItems + fetchedItems
                val hasNext = items.last().overallIndex < picAmount
                val hasPrev = items.first().overallIndex > 1
                val initialIndex: Int? = items
                    .indexOfFirst { it.picId == viewData.data.picId }
                    .takeIf { it != -1 }
                val newState = PhotoViewUiState(
                    data = items.toImmutableList(),
                    hasNext = hasNext,
                    hasPrev = hasPrev,
                    totalAmount = picAmount,
                    initialIndex = initialIndex ?: (viewData.data.picIndex - 1),
                )

                withContext(Dispatchers.Main.immediate) {
                    data = viewData.data
                    _state.set { newState }
                }
            }
        }
    }

    override fun loadInitial(): List<Photo> {
        if (data == null) throw RuntimeException("ViewModel is uninitialized!, call initData before load")

        val stateSnapshot = _state.value
        val items = stateSnapshot.data
        val initIndex = stateSnapshot.initialIndex
        return if (initIndex != 0) {
            // Trim out items before initial index
            // Basically the same with [ViewPager.setCurrentItem()]
            items.subList(initIndex, items.size)
        } else {
            stateSnapshot.data
        }
    }

    override fun loadBefore(key: Long, callback: (List<Photo>) -> Unit) {
        queue.submit(Dispatchers.Default + handler) {
            val uiState = _state.first()
            val items = uiState.data
            val index = items.indexOfFirst { it.id() == key }

            if (index > 0) {
                callback(items.subList(0, index)) // Trimmed list from loadInitial()
            } else if (index == -1 || !uiState.hasPrev) {
                callback(emptyList())
            } else {
                val item: PhotoViewItem = items[index]

                val picPageBean = data!!.toPageFlow(item.picId, item.overallIndex, prev = true)
                    .retryWhen { cause, attempt ->  cause !is TiebaApiException && attempt < 3 }
                    .firstOrThrow()

                val hasPrev = picPageBean.picList.first().overAllIndex.toInt() > 1
                val uniqueItems = picPageBean.picList.toUniquePhotoViewItems(uiState.data)
                val newItems = (uniqueItems + uiState.data).toImmutableList()

                withContext(Dispatchers.Main.immediate) {
                    _state.set { copy(data = newItems, hasPrev = hasPrev) }
                    callback(uniqueItems)
                }
            }
        }
    }

    override fun loadAfter(key: Long, callback: (List<Photo>) -> Unit) {
        queue.submit(Dispatchers.Default + handler) {
            val uiState = _state.value
            val items = uiState.data
            val index = items.indexOfFirst { it.id() == key }

            if (index == -1 || !uiState.hasNext) {
                callback(emptyList())
                return@submit
            }

            val item: PhotoViewItem = items[index]
            val picPageBean = data!!.toPageFlow(item.picId, item.overallIndex, prev = false)
                .retryWhen { cause, attempt ->  cause !is TiebaApiException && attempt < 3 }
                .firstOrThrow()

            val newData = picPageBean.picList
            val picAmount = picPageBean.picAmount ?: throw TiebaException("加载列表失败, 远古坟贴?")
            val hasNext = newData.last().overAllIndex.toInt() < picAmount
            val uniqueItems = newData.toUniquePhotoViewItems(old = uiState.data)
            val newItems = (uiState.data + uniqueItems).toImmutableList()

            withContext(Dispatchers.Main.immediate) {
                _state.set { copy(data = newItems, hasNext = hasNext) }
                callback(uniqueItems)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        queue.cancel()
    }

    companion object {
        private const val TAG = "PhotoViewViewModel"

        private fun LoadPicPageData.toPageFlow(picId: String, picIndex: Int, prev: Boolean): Flow<PicPageBean> {
            return TiebaApi.getInstance().picPageFlow(
                forumId = forumId.toString(),
                forumName = forumName,
                threadId = threadId.toString(),
                seeLz = seeLz,
                picId = picId,
                picIndex = picIndex.toString(),
                objType = objType,
                prev = prev
            )
        }

        private fun PicPageBean.PicBean.toPhotoItem(): PhotoViewItem {
            val originSize = img.original.size.toIntOrNull() ?: 0 // Bytes

            return PhotoViewItem(
                picId = img.original.id,
                originUrl = img.bestQualitySrc,
                overallIndex = overAllIndex.toInt(),
                postId = postId?.toLongOrNull(),
                type = when {
                    img.isGif -> ItemType.PHOTO

                    img.original.isLongPic() || originSize >= 1024 * 1024 * 2 -> ItemType.SUBSAMPLING

                    else -> ItemType.PHOTO
                }
            )
        }
    }
}

data class PhotoViewUiState(
    val data: List<PhotoViewItem> = persistentListOf(),
    val totalAmount: Int = 0,
    val hasNext: Boolean = false,
    val hasPrev: Boolean = false,
    val initialIndex: Int = 0,
    val error: Throwable? = null,
)

data class PhotoViewItem(
    val picId: String,
    val originUrl: String,
    val overallIndex: Int,
    val postId: Long? = null,
    val type: Int
): Photo {

    // Updated by SimpleImageLoader
    var progress: Int = 0

    constructor(item: PicItem, overallIndex: Int): this(
        picId = item.picId,
        originUrl = item.originUrl,
        overallIndex = overallIndex,
        postId = item.postId,
        type = ItemType.PHOTO
    )

    override fun id(): Long = picId.hashCode().toLong()

    override fun itemType(): Int = type
}