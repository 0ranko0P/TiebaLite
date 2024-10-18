package com.huanchengfly.tieba.post.ui.page.reply

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.InsertPhoto
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.pxToDpFloat
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.Destination.Reply
import com.huanchengfly.tieba.post.ui.page.reply.ReplyPanelType.EMOJI
import com.huanchengfly.tieba.post.ui.page.reply.ReplyPanelType.IMAGE
import com.huanchengfly.tieba.post.ui.page.reply.ReplyPanelType.NONE
import com.huanchengfly.tieba.post.ui.utils.imeNestedScroll
import com.huanchengfly.tieba.post.ui.widgets.compose.BaseDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyBackHandler
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.edittext.widget.UndoableEditText
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.AppPreferencesUtils.Companion.KEY_REPLY_WARNING
import com.huanchengfly.tieba.post.utils.Emoticon
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.EmoticonManager.EmoticonInlineImage
import com.huanchengfly.tieba.post.utils.PickMediasRequest
import com.huanchengfly.tieba.post.utils.hideKeyboard
import com.huanchengfly.tieba.post.utils.showKeyboard
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ReplyDialog(
    args: Reply,
    state: DialogState = rememberDialogState(),
) {
    BaseDialog(
        dialogState = state,
        imePadding = false,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            color = ExtendedTheme.colors.windowBackground,
            elevation = 0.dp,
        ) {
            ReplyPageContent(
                viewModel = pageViewModel(),
                onBack = { dismiss() },
                forumId = args.forumId,
                forumName = args.forumName,
                threadId = args.threadId,
                postId = args.postId,
                subPostId = args.subPostId,
                replyUserId = args.replyUserId,
                replyUserName = args.replyUserName,
                replyUserPortrait = args.replyUserPortrait,
                tbs = args.tbs,
                isDialog = true
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReplyPageContent(
    viewModel: ReplyViewModel,
    onBack: () -> Unit,
    forumId: Long,
    forumName: String,
    threadId: Long,
    postId: Long? = null,
    subPostId: Long? = null,
    replyUserId: Long? = null,
    replyUserName: String? = null,
    replyUserPortrait: String? = null,
    tbs: String? = null,
    isDialog: Boolean = false,
) {
    viewModel.initialize(threadId, postId, subPostId)

    val context = LocalContext.current
    val curTbs = remember(tbs) { tbs ?: AccountUtil.getAccountInfo { this.tbs }.orEmpty() }

    val isUploading by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::isUploading,
        initial = false
    )
    val isSending by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::isSending,
        initial = false
    )
    val isReplying by remember { derivedStateOf { isUploading || isSending } }
    val curKeyboardType by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::replyPanelType,
        initial = NONE
    )
    val selectedImageList by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::selectedImageList,
        initial = persistentListOf()
    )
    val isOriginImage by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::isOriginImage,
        initial = false
    )

    val keyboardController = LocalSoftwareKeyboardController.current
    val text by viewModel.text
    var editTextView by remember { mutableStateOf<UndoableEditText?>(null) }
    fun getText(): String {
        return editTextView?.text?.toString().orEmpty()
    }

    LaunchedEffect(key1 = text, key2 = editTextView) {
        editTextView?.apply {
            val start = selectionStart
            removeTextChangedListener(viewModel) // Avoid onTextChanged loop
            setText(text)
            setSelection(start)
            addTextChangedListener(viewModel)
        }
    }

    val textLength by remember { derivedStateOf { text.length } }
    val isTextEmpty by remember { derivedStateOf { text.isEmpty() } }

    viewModel.onEvent<ReplyUiEvent.ReplySuccess> {
        if (it.expInc.isEmpty()) {
            context.toastShort(R.string.toast_reply_success_default)
        } else {
            context.toastShort(R.string.toast_reply_success, it.expInc)
        }
        viewModel.deleteDraft()
        onBack()
    }

    var waitUploadSuccessToSend by remember { mutableStateOf(false) }
    viewModel.onEvent<ReplyUiEvent.UploadSuccess> {
        if (waitUploadSuccessToSend) {
            waitUploadSuccessToSend = false
            val imageContent = it.resultList
                .joinToString("\n") { image ->
                    "#(pic,${image.picId},${image.picInfo.originPic.width},${image.picInfo.originPic.height})"
                }
            viewModel.send(
                ReplyUiIntent.Send(
                    "${getText()}\n$imageContent",
                    forumId,
                    forumName,
                    threadId,
                    curTbs,
                    postId,
                    subPostId,
                    replyUserId,
                )
            )
        }
    }

    var closingPanel by remember { mutableStateOf(false) }
    var startClosingAnimation by remember { mutableStateOf(false) }

    fun showKeyboard() {
        editTextView?.apply {
            showKeyboard(context, this)
            requestFocus()
        }
        keyboardController?.show()
    }

    fun hideKeyboard() {
        editTextView?.apply {
            hideKeyboard(context, this)
            clearFocus()
        }
        keyboardController?.hide()
    }

    fun switchToPanel(type: ReplyPanelType) {
        if (curKeyboardType == type || type == NONE) {
            if (curKeyboardType != NONE) {
                showKeyboard()
                closingPanel = true
                startClosingAnimation = false
            }
            viewModel.send(ReplyUiIntent.SwitchPanel(NONE))
        } else {
            hideKeyboard()
            viewModel.send(ReplyUiIntent.SwitchPanel(type))
        }
    }

    MyBackHandler(enabled = curKeyboardType != NONE) {
        switchToPanel(NONE)
    }

    val density = LocalDensity.current
    val imeInset = WindowInsets.ime
    val imeAnimationTargetInset = WindowInsets.imeAnimationTarget

    val imeCurrentHeight by produceState(initialValue = 0, imeInset, density) {
        snapshotFlow { imeInset.getBottom(density) }
            .distinctUntilChanged()
            .collect { value = it }
    }
    val imeAnimationTargetHeight by produceState(
        initialValue = 0,
        imeAnimationTargetInset,
        density
    ) {
        snapshotFlow { imeAnimationTargetInset.getBottom(density) }
            .distinctUntilChanged()
            .collect { value = it }
    }
    val imeAnimationEnd by remember { derivedStateOf { imeCurrentHeight == imeAnimationTargetHeight } }
    val imeVisibleHeightPx by produceState(
        initialValue = 800,
        imeAnimationTargetInset,
        density
    ) {
        snapshotFlow { imeAnimationTargetInset.getBottom(density) }
            .filter { it > 0 }
            .distinctUntilChanged()
            .collect {
                value = it
            }
    }

    val panelHeight by remember {
        derivedStateOf { max(imeVisibleHeightPx.pxToDpFloat(), 150f).dp }
    }

    val textMeasurer = rememberTextMeasurer()

    val minResult = textMeasurer.measure(
        AnnotatedString("\n\n"),
        style = LocalTextStyle.current
    ).size.height.pxToDpFloat().dp

    val maxResult = textMeasurer.measure(
        AnnotatedString("\n\n\n\n\n"),
        style = LocalTextStyle.current
    ).size.height.pxToDpFloat().dp

    LaunchedEffect(closingPanel, imeAnimationEnd) {
        if (closingPanel) {
            if (!startClosingAnimation && !imeAnimationEnd) {
                startClosingAnimation = true
            } else if (startClosingAnimation && imeAnimationEnd) {
                closingPanel = false
                startClosingAnimation = false
            }
        }
    }

    val canSend by remember { derivedStateOf { !isTextEmpty || selectedImageList.isNotEmpty() } }

    val textFieldScrollState = rememberScrollState()

    val parentModifier = if (curKeyboardType == NONE && !closingPanel) {
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    } else {
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .consumeWindowInsets(WindowInsets.ime)
    }

    Column(
        modifier = parentModifier,
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.title_reply),
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$textLength",
                style = MaterialTheme.typography.caption,
                color = ExtendedTheme.colors.textSecondary
            )
        }
        VerticalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Box(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .imeNestedScroll(textFieldScrollState),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .requiredHeightIn(min = minResult, max = maxResult)
                    .verticalScroll(textFieldScrollState)
            ) {
                AndroidView(
                    factory = { ctx ->
                        (View.inflate(
                            ctx,
                            R.layout.layout_reply_edit_text,
                            null
                        ) as UndoableEditText).apply {
                            editTextView = this
                            if (subPostId != null && subPostId != 0L && replyUserName != null) {
                                hint = ctx.getString(R.string.hint_reply, replyUserName)
                            }
                            setOnFocusChangeListener { _, hasFocus ->
                                if (hasFocus) {
                                    switchToPanel(NONE)
                                }
                            }

                            addTextChangedListener(viewModel)

                            val emoticonSize = (-paint.ascent() + paint.descent()).roundToInt()
                            viewModel.setEmoticonSize(emoticonSize)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.Top)
                )
            }
//            BaseTextField(
//                value = text,
//                onValueChange = { text = it },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp)
//                    .requiredHeightIn(min = minResult, max = maxResult)
//                    .focusRequester(focusRequester)
//                    .verticalScroll(textFieldScrollState)
//                    .onFocusChanged {
//                        Log.i("ReplyPage", "onFocusChanged: $it")
//                        if (it.hasFocus) {
//                            switchToKeyboard(NONE)
//                        }
//                    },
//                placeholder = { Text(text = stringResource(id = R.string.tip_reply)) },
//            )
        }
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            IconButton(
                onClick = { switchToPanel(EMOJI) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.EmojiEmotions,
                    contentDescription = stringResource(id = R.string.insert_emotions),
                    modifier = Modifier.size(24.dp)
                )
            }
            if (postId == null || postId == 0L) {
                IconButton(
                    onClick = { switchToPanel(IMAGE) },
                    modifier = Modifier.size(24.dp)
                ) {
                    BadgedBox(
                        badge = {
                            if (selectedImageList.isNotEmpty()) {
                                Badge(
                                    backgroundColor = ExtendedTheme.colors.primary,
                                    contentColor = ExtendedTheme.colors.background,
                                ) {
                                    Text(
                                        text = "${selectedImageList.size}",
                                        style = MaterialTheme.typography.caption
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.InsertPhoto,
                            contentDescription = stringResource(id = R.string.insert_photo),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
//            IconButton(
//                onClick = { switchToPanel(VOICE) },
//                modifier = Modifier.size(24.dp)
//            ) {
//                Icon(
//                    imageVector = Icons.Outlined.KeyboardVoice,
//                    contentDescription = stringResource(id = R.string.insert_voice),
//                    modifier = Modifier.size(24.dp)
//                )
//            }
            Spacer(modifier = Modifier.weight(1f))
            if (isReplying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = ExtendedTheme.colors.primary
                )
            } else {
                IconButton(
                    onClick = {
                        val replyContent = if (subPostId == null || subPostId == 0L) {
                            getText()
                        } else {
                            "回复 #(reply, ${replyUserPortrait}, ${replyUserName}) :${getText()}"
                        }
                        if (selectedImageList.isEmpty()) {
                            viewModel.send(
                                ReplyUiIntent.Send(
                                    content = replyContent,
                                    forumId = forumId,
                                    forumName = forumName,
                                    threadId = threadId,
                                    tbs = curTbs,
                                    postId = postId,
                                    subPostId = subPostId,
                                    replyUserId = replyUserId
                                )
                            )
                        } else {
                            waitUploadSuccessToSend = true
                            viewModel.send(
                                ReplyUiIntent.UploadImages(
                                    forumName,
                                    selectedImageList,
                                    isOriginImage
                                )
                            )
                        }
                    },
                    enabled = canSend,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(id = R.string.send_reply),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.ime)
        )
        if (curKeyboardType != NONE) {
            Column(modifier = Modifier.height(panelHeight)) {
                when (curKeyboardType) {
                    EMOJI -> {
                        EmoticonPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            onEmoticonClick = { emoticon ->
                                editTextView?.let {
                                    val start = it.selectionStart
                                    val emoText = "#(${emoticon.name})"
                                    it.text?.insert(start, emoText)
                                    it.setSelection(start + emoText.length)
                                }
                            }
                        )
                    }

                    IMAGE -> {
                        ImagePanel(
                            selectedImages = selectedImageList,
                            onNewImageSelected = { uris ->
                                viewModel.send(ReplyUiIntent.AddImage(uris.map { it.toString() }))
                            },
                            onRemoveImage = {
                                viewModel.send(ReplyUiIntent.RemoveImage(it))
                            },
                            isOriginImage = isOriginImage,
                            onIsOriginImageChange = {
                                viewModel.send(ReplyUiIntent.ToggleIsOriginImage(it))
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        )
                    }

                    else -> {}
                }
            }
        } else if (closingPanel) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(panelHeight)
            )
        }
    }

    DisposableEffect(editTextView) {
        if (editTextView != null) {
            showKeyboard()
        }

        onDispose {
            if (editTextView != null) {
                hideKeyboard()
            }
        }
    }

    fun getDispatchUri(): Uri {
        return if (postId != null) {
            Uri.parse("com.baidu.tieba://unidispatch/pb?obj_locate=comment_lzl_cut_guide&obj_source=wise&obj_name=index&obj_param2=chrome&has_token=0&qd=scheme&refer=tieba.baidu.com&wise_sample_id=3000232_2&hightlight_anchor_pid=${postId}&is_anchor_to_comment=1&comment_sort_type=0&fr=bpush&tid=${threadId}")
        } else {
            Uri.parse("com.baidu.tieba://unidispatch/pb?obj_locate=pb_reply&obj_source=wise&obj_name=index&obj_param2=chrome&has_token=0&qd=scheme&refer=tieba.baidu.com&wise_sample_id=3000232_2-99999_9&fr=bpush&tid=${threadId}")
        }
    }

    fun launchOfficialApp() {
        val intent = Intent(Intent.ACTION_VIEW).setData(getDispatchUri())
        val resolveInfos =
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            ).filter { it.activityInfo.packageName != context.packageName }
        try {
            if (resolveInfos.isNotEmpty()) {
                context.startActivity(intent)
            } else {
                context.toastShort(R.string.toast_official_client_not_install)
            }
        } catch (e: ActivityNotFoundException) {
            context.toastShort(R.string.toast_official_client_not_install)
        }
    }

    val warning by rememberPreferenceAsState(booleanPreferencesKey(KEY_REPLY_WARNING), true)
    val warningDialogState = rememberDialogState()
    Dialog(
        dialogState = warningDialogState,
        title = { Text(text = stringResource(id = R.string.title_dialog_reply_warning)) },
        buttons = {
            DialogPositiveButton(
                text = stringResource(id = R.string.button_official_client_reply),
                onClick = { launchOfficialApp() }
            )
            DialogNegativeButton(text = stringResource(id = R.string.btn_continue_reply))
            DialogNegativeButton(
                text = stringResource(id = R.string.btn_cancel_reply),
                onClick = { onBack() }
            )
        },
    ) {
        Text(
            text = stringResource(id = R.string.message_dialog_reply_warning),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }

    LaunchedEffect(Unit) {
        if (warning) {
            warningDialogState.show()
        }
    }
}

// TODO: 将软键盘状态相关逻辑抽离出来
@Composable
fun ReplyPage(
    params: Reply,
    onBack: () -> Unit,
    viewModel: ReplyViewModel = pageViewModel(),
) {
    ReplyPageContent(
        viewModel = viewModel,
        onBack = onBack,
        forumId = params.forumId,
        forumName = params.forumName,
        threadId = params.threadId,
        postId = params.postId,
        subPostId = params.subPostId,
        replyUserId = params.replyUserId,
        replyUserName = params.replyUserName,
        replyUserPortrait = params.replyUserPortrait,
        tbs = params.tbs
    )
}

@Composable
private fun EmoticonPanel(
    modifier: Modifier = Modifier,
    onEmoticonClick: (Emoticon) -> Unit,
) {
    val emoticons = remember {
        EmoticonManager.getAllEmoticon().filter { it.name.isNotEmpty() }
    }

    Column(
        modifier = modifier
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(48.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            items(emoticons) { emoticon ->
                EmoticonInlineImage(
                    id = emoticon.id,
                    description = emoticon.name,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(8.dp)
                        .clickable { onEmoticonClick(emoticon) }
                )
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ImagePanel(
    selectedImages: ImmutableList<String>,
    onNewImageSelected: (List<Uri>) -> Unit,
    onRemoveImage: (Int) -> Unit,
    isOriginImage: Boolean,
    onIsOriginImageChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val id = remember { UUID.randomUUID().toString() }
    val coroutineScope = rememberCoroutineScope()
    onGlobalEvent<GlobalEvent.SelectedImages>(
        coroutineScope,
        filter = { it.id == id }
    ) {
        onNewImageSelected(it.images)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxHeight(0.5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
//            item {
//                Spacer(modifier = Modifier.width(16.dp))
//            }
            itemsIndexed(selectedImages) { index, imageUri ->
                Box {
                    GlideImage(
                        model = imageUri,
                        contentDescription = stringResource(id = R.string.desc_image),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                    )
                    IconButton(
                        onClick = { onRemoveImage(index) },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(id = R.string.desc_remove_image)
                        )
                    }
                }
            }
            if (selectedImages.size < 9) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .background(ExtendedTheme.colors.chip)
                            .clickable {
                                coroutineScope.emitGlobalEvent(
                                    GlobalEvent.StartSelectImages(
                                        id,
                                        9 - selectedImages.size,
                                        PickMediasRequest.ImageOnly
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(id = R.string.desc_add_image),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onIsOriginImageChange(!isOriginImage)
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isOriginImage, onCheckedChange = onIsOriginImageChange)
            Text(text = stringResource(id = R.string.origin_image))
        }
    }
}