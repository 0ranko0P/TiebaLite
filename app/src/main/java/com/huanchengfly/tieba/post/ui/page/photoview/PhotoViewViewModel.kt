package com.huanchengfly.tieba.post.ui.page.photoview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.iielse.imageviewer.adapter.ItemType
import com.github.iielse.imageviewer.core.DataProvider
import com.github.iielse.imageviewer.core.Photo
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.PicPageBean
import com.huanchengfly.tieba.post.api.models.bestQualitySrc
import com.huanchengfly.tieba.post.api.models.isGif
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.models.LoadPicPageData
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.models.PicItem
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.utils.JobQueue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

class PhotoViewViewModel : ViewModel(), DataProvider {
    private val _state: MutableStateFlow<PhotoViewUiState> = MutableStateFlow(PhotoViewUiState())
    val state: StateFlow<PhotoViewUiState> get() = _state

    private val handler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "onError: ", e)
        App.INSTANCE.toastShort(e.getErrorMessage())
    }

    private val queue = JobQueue()

    private var data: LoadPicPageData? = null

    private fun List<PicPageBean.PicBean>.toUniquePhotoViewItems(): List<PhotoViewItem> {
        val oldDataIds = _state.value.data.mapTo(HashSet()) { it.picId }
        return this
            .filterNot { oldDataIds.contains(it.img.original.id) }
            .map { it.toPhotoItem() }
    }

    fun initData(viewData: PhotoViewData) {
        if (this.data != null) return
        this.data = viewData.data

        if (viewData.data == null) {
            val newState = state.value.copy(
                data = viewData.picItems
                    .mapIndexed { i, item -> PhotoViewItem(item = item, overallIndex = i + 1) }
                    .toImmutableList(),
                totalAmount = viewData.picItems.size,
                initialIndex = viewData.index
            )
            viewModelScope.launch(Dispatchers.Main.immediate) { _state.emit(newState) }
        } else {
            queue.submit(Dispatchers.IO + handler) {
                viewData.data.toPageFlow(viewData.data.picId, viewData.data.picIndex, prev = false)
                    .retryWhen { cause, attempt ->  cause !is TiebaApiException && attempt < 3 }
                    .collect { picPageBean ->
                        val picAmount = picPageBean.picAmount.toInt()
                        val fetchedItems = picPageBean.picList.toUniquePhotoViewItems()
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
                        val newState = state.value.copy(
                            data = items.toImmutableList(),
                            hasNext = hasNext,
                            hasPrev = hasPrev,
                            totalAmount = picAmount,
                            initialIndex = initialIndex ?: (viewData.data.picIndex - 1),
                        )

                        withContext(Dispatchers.Main.immediate) {
                            this@PhotoViewViewModel.data = viewData.data
                            _state.emit(newState)
                        }
                    }
            }
        }
    }

    override fun loadInitial(): List<Photo> {
        if (data == null) throw RuntimeException("ViewModel is uninitialized!, call initData before load")

        val items = state.value.data
        val initIndex = state.value.initialIndex
        return if (initIndex != 0) {
            // Trim out items before initial index
            // Basically the same with [ViewPager.setCurrentItem()]
            items.subList(initIndex, items.size)
        } else {
            state.value.data
        }
    }

    override fun loadBefore(key: Long, callback: (List<Photo>) -> Unit) {
        queue.submit(Dispatchers.IO + handler) {
            val uiState = _state.value
            val items = uiState.data
            val index = items.indexOfFirst { it.id() == key }

            if (index > 0) {
                callback(items.subList(0, index)) // Trimmed list from loadInitial()
            } else if (index == -1 || !uiState.hasPrev) {
                callback(emptyList())
            } else {
                val item: PhotoViewItem = items[index]

                data!!.toPageFlow(item.picId, item.overallIndex, prev = true)
                    .retryWhen { cause, attempt ->  cause !is TiebaApiException && attempt < 3 }
                    .collect { picPageBean ->
                        val hasPrev = picPageBean.picList.first().overAllIndex.toInt() > 1
                        val uniqueItems = picPageBean.picList.toUniquePhotoViewItems()
                        val newItems = (uniqueItems + uiState.data).toImmutableList()
                        withContext(Dispatchers.Main.immediate) {
                            _state.emit(
                                uiState.copy(data = newItems, hasPrev = hasPrev)
                            )
                            callback(uniqueItems)
                        }
                    }
            }
        }
    }

    override fun loadAfter(key: Long, callback: (List<Photo>) -> Unit) {
        queue.submit(Dispatchers.IO + handler) {
            val uiState = _state.value
            val items = uiState.data
            val index = items.indexOfFirst { it.id() == key }

            if (index == -1 || !uiState.hasNext) {
                callback(emptyList())
                return@submit
            }

            val item: PhotoViewItem = items[index]
            data!!.toPageFlow(item.picId, item.overallIndex, prev = false)
                .retryWhen { cause, attempt ->  cause !is TiebaApiException && attempt < 3 }
                .collect { picPageBean ->
                    val newData = picPageBean.picList
                    val hasNext = newData.last().overAllIndex.toInt() < picPageBean.picAmount.toInt()
                    val uniqueItems = newData.toUniquePhotoViewItems()
                    val newItems = (uiState.data + uniqueItems).toImmutableList()

                    withContext(Dispatchers.Main.immediate) {
                        _state.emit(uiState.copy(data = newItems, hasNext = hasNext))
                        callback(uniqueItems)
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        queue.cancel()
    }

    companion object {
        private const val TAG = "PhotoViewViewModel"

        @OptIn(FlowPreview::class)
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
            ).timeout(4.seconds)
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

                    isLongPic || originSize >= 1024 * 1024 * 2 -> ItemType.SUBSAMPLING

                    else -> ItemType.PHOTO
                }
            )
        }
    }
}

data class PhotoViewUiState(
    val data: ImmutableList<PhotoViewItem> = persistentListOf(),
    val totalAmount: Int = 0,
    val hasNext: Boolean = false,
    val hasPrev: Boolean = false,
    val initialIndex: Int = 0
)

data class PhotoViewItem(
    val picId: String,
    val originUrl: String,
    val overallIndex: Int,
    val postId: Long? = null,
    val type: Int
): Photo {

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