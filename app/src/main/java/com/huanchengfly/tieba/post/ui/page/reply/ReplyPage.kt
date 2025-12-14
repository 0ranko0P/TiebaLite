package com.huanchengfly.tieba.post.ui.page.reply

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.InsertPhoto
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.huanchengfly.tieba.post.LocalHabitSettings
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.block
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.page.Destination.Reply
import com.huanchengfly.tieba.post.ui.page.reply.ReplyPanelType.EMOJI
import com.huanchengfly.tieba.post.ui.page.reply.ReplyPanelType.IMAGE
import com.huanchengfly.tieba.post.ui.page.reply.ReplyPanelType.NONE
import com.huanchengfly.tieba.post.ui.page.reply.ReplyViewModel.Companion.MAX_SELECTABLE_IMAGE
import com.huanchengfly.tieba.post.ui.utils.imeNestedScroll
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultDialogContentPadding
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.StrongBox
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.edittext.widget.UndoableEditText
import com.huanchengfly.tieba.post.utils.DisplayUtil.toDpSize
import com.huanchengfly.tieba.post.utils.Emoticon
import com.huanchengfly.tieba.post.utils.EmoticonManager.EmoticonInlineImage
import com.huanchengfly.tieba.post.utils.EmoticonUtil
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.hideKeyboard
import com.huanchengfly.tieba.post.utils.keyboardAnimationHeight
import com.huanchengfly.tieba.post.utils.keyboardMaxHeight
import com.huanchengfly.tieba.post.utils.showKeyboard
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class ReplyPanelType {
    NONE,
    EMOJI,
    IMAGE,
    VOICE
}

@Composable
fun ReplyPageBottomSheet(
    params: Reply,
    onBack: () -> Unit,
    viewModel: ReplyViewModel = pageViewModel(),
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomStart
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            ReplyPageContent(
                viewModel = viewModel,
                onBack = onBack,
                forumName = params.forumName,
                postId = params.postId,
                subPostId = params.subPostId,
                replyUserName = params.replyUserName,
                tbs = params.tbs
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReplyPageContent(
    viewModel: ReplyViewModel,
    onBack: () -> Unit,
    forumName: String,
    postId: Long? = null,
    subPostId: Long? = null,
    replyUserName: String? = null,
    tbs: String? = null,
) {
    val pickMediasLauncher = rememberLauncherForActivityResult(PickMultipleVisualMedia(MAX_SELECTABLE_IMAGE)) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        viewModel.send(ReplyUiIntent.AddImage(uris.map { it.toString() }))
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val account = LocalAccount.current
    val curTbs = tbs ?: account?.tbs.orEmpty()
    val colors = MaterialTheme.colorScheme

    val isUploading by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::isUploading,
        initial = false
    )
    val isSending by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::isSending,
        initial = false
    )
    val isReplying by remember { derivedStateOf { isUploading || isSending } }
    val selectedImageList by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::selectedImageList,
        initial = persistentListOf()
    )
    val isOriginImage by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::isOriginImage,
        initial = false
    )

    var inputLength by remember { mutableIntStateOf(0) }
    var editTextView by remember { mutableStateOf<UndoableEditText?>(null) }

    LaunchedEffect(Unit) {
        viewModel.text.collect { t ->
            editTextView?.apply {
                val start = selectionStart
                removeTextChangedListener(viewModel) // Avoid onTextChanged loop
                setSelection(0)
                setText(t)
                setSelection(start.coerceIn(0, t.length))
                addTextChangedListener(viewModel)
                inputLength = t.length
            }
        }
    }

    viewModel.onEvent<CommonUiEvent.Toast> {
        Toast.makeText(context, it.message, it.length).show()
    }

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
            viewModel.onSendReplyWithImage(it.resultList, curTbs)
        }
    }

    fun showKeyboard() {
        editTextView?.apply {
            showKeyboard(this)
            requestFocus()
        }
    }

    fun hideKeyboard() {
        editTextView?.apply {
            hideKeyboard(this)
            clearFocus()
        }
    }

    var curKeyboardType by remember { mutableStateOf(NONE) }

    fun switchToPanel(type: ReplyPanelType) {
        if (curKeyboardType == type || type == NONE) { // Closing current panel
            if (curKeyboardType != NONE) {
                coroutineScope.launch {
                    showKeyboard()
                    delay(500) // Wait ime animation
                    curKeyboardType = NONE
                }
            } else {
                curKeyboardType = NONE
            }
        } else {
            hideKeyboard()
            curKeyboardType = type
        }
    }

    StrongBox {
        BackHandler(enabled = curKeyboardType != NONE) { // Close current panel when back
            switchToPanel(NONE)
        }
    }

    val density = LocalDensity.current
    val textStyle = LocalTextStyle.current
    val textMeasurer = rememberTextMeasurer()

    val minHeight: Dp = remember(textStyle, density) {
        textMeasurer.measure(
            text = AnnotatedString("\n\n"),
            style = textStyle.copy(fontSize = 14.sp * density.fontScale)
        )
        .size.toDpSize(density).height
    }
    val maxHeight: Dp = minHeight * 3

    val textFieldScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .background(TiebaLiteTheme.extendedColorScheme.navigationContainer)
            .navigationBarsPadding(),
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
                text = inputLength.toString(),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Box(
            modifier = Modifier
                .wrapContentHeight()
                .imeNestedScroll(textFieldScrollState),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .requiredHeightIn(min = minHeight, max = maxHeight)
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
                        .wrapContentHeight(align = Alignment.Top),
                    update = {
                        it.setTextColor(colors.onSurface.toArgb())
                    }
                )
            }

//            b/135556699 Support for text editing with AnnotatedString (multi style text editing / sometimes rich text editing)
//
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

        ImeActionRow(
            onPanelClicked = ::switchToPanel,
            selectedImageProvider = { selectedImageList }.takeIf { postId == null || postId == 0L }, // Not reply post
            isReplying = { isReplying },
            canReply = {
                inputLength > 0 || selectedImageList.isNotEmpty()
            },
            onReplyClicked = {
                if (selectedImageList.isEmpty()) {
                    viewModel.onSendReply(curTbs = curTbs)
                } else {
                    waitUploadSuccessToSend = true
                    viewModel.send(ReplyUiIntent.UploadImages(forumName, selectedImageList, isOriginImage))
                }
            }
        )

        StrongBox {
            val imeMaxHeight by keyboardMaxHeight()
            val imeCurrentHeight by keyboardAnimationHeight()
            val isFloatingIme by remember { derivedStateOf { imeMaxHeight == Dp.Hairline } }
            val panelMaxHeight = if (isFloatingIme) 250.dp else imeMaxHeight
            val panelHeightAni by animateDpAsState(if (curKeyboardType == NONE) imeCurrentHeight else panelMaxHeight)

            when (curKeyboardType) {
                NONE -> {
                    Spacer(
                        modifier = Modifier.block { // use animated height on floating keyboard
                            if (isFloatingIme) height(panelHeightAni) else imePadding()
                        }
                    )
                }

                EMOJI -> {
                    EmoticonPanel(
                        modifier = Modifier.height(panelHeightAni),
                        emoticons = viewModel.emoticons,
                        onEmoticonClick = { emoticon ->
                            editTextView?.let {
                                val start = it.selectionStart
                                val emoText = EmoticonUtil.inlineTextFormat(name = emoticon.name)
                                it.text?.insert(start, emoText)
                                it.setSelection(start + emoText.length)
                            }
                        }
                    )
                }

                IMAGE -> {
                    ImagePanel(
                        selectedImages = selectedImageList,
                        onAddImageClicked = {
                            pickMediasLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                        },
                        onRemoveImage = {
                            viewModel.send(ReplyUiIntent.RemoveImage(it))
                        },
                        isOriginImage = isOriginImage,
                        onIsOriginImageChange = {
                            viewModel.send(ReplyUiIntent.ToggleIsOriginImage(it))
                        },
                        modifier = Modifier.height(panelHeightAni)
                    )
                }

                else -> {}
            }
        }
    }

    ReplyWarningDialog(vm = viewModel, onBack = onBack)
}

@Composable
private fun ImeActionRow(
    modifier: Modifier = Modifier,
    onPanelClicked: (ReplyPanelType) -> Unit = {},
    selectedImageProvider: (() -> List<String>)?, // Null to disable ImagePanel
    isReplying: () -> Boolean = { false },
    canReply: () -> Boolean = { true },
    onReplyClicked: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        IconButton(
            onClick = { onPanelClicked(EMOJI) },
            modifier = Modifier.size(size = Sizes.Tiny)
        ) {
            Icon(
                imageVector = Icons.Outlined.EmojiEmotions,
                contentDescription = stringResource(id = R.string.insert_emotions),
            )
        }

        if (selectedImageProvider != null) {
            Box (
                modifier = Modifier
                    .size(size = Sizes.Tiny)
                    .clickableNoIndication(onClick = { onPanelClicked(IMAGE) })
            ) {
                BadgedBox(
                    badge = {
                        val selectedImageList = selectedImageProvider()
                        if (selectedImageList.isNotEmpty()) {
                            Badge(
                                containerColor = colorScheme.primary,
                                contentColor = colorScheme.onPrimary,
                                content = { Text(text = selectedImageList.size.toString()) }
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.InsertPhoto,
                        contentDescription = stringResource(id = R.string.insert_photo),
                        modifier = Modifier.fillMaxSize()
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
        if (isReplying()) {
            CircularProgressIndicator(
                modifier = Modifier.size(Sizes.Tiny),
                strokeWidth = 2.dp,
                color = colorScheme.primary
            )
        } else {
            IconButton(
                onClick = onReplyClicked,
                enabled = canReply(),
                modifier = Modifier.size(Sizes.Tiny)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = stringResource(id = R.string.send_reply)
                )
            }
        }
    }
}

@Composable
private fun EmoticonPanel(
    modifier: Modifier = Modifier,
    emoticons: List<Emoticon>,
    onEmoticonClick: (Emoticon) -> Unit,
) {
    val emoSize = Sizes.Medium

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = emoSize),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(emoticons) { emoticon ->
                EmoticonInlineImage(
                    id = emoticon.id,
                    description = emoticon.name,
                    modifier = Modifier
                        .size(size = emoSize)
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
    selectedImages: List<String>,
    onAddImageClicked: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    isOriginImage: Boolean,
    onIsOriginImageChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
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
                        modifier = Modifier.align(Alignment.TopEnd),
                        colors = IconButtonDefaults.iconButtonColors(MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(id = R.string.desc_remove_image)
                        )
                    }
                }
            }
            if (selectedImages.size < MAX_SELECTABLE_IMAGE) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.secondary)
                            .clickable(onClick = onAddImageClicked),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(id = R.string.desc_add_image),
                            modifier = Modifier.minimumInteractiveComponentSize(),
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .clickableNoIndication {
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

private fun getDispatchUri(threadId: Long, postId: Long?): Uri {
    return if (postId != null) {
        Uri.parse("com.baidu.tieba://unidispatch/pb?obj_locate=comment_lzl_cut_guide&obj_source=wise&obj_name=index&obj_param2=chrome&has_token=0&qd=scheme&refer=tieba.baidu.com&wise_sample_id=3000232_2&hightlight_anchor_pid=${postId}&is_anchor_to_comment=1&comment_sort_type=0&fr=bpush&tid=${threadId}")
    } else {
        Uri.parse("com.baidu.tieba://unidispatch/pb?obj_locate=pb_reply&obj_source=wise&obj_name=index&obj_param2=chrome&has_token=0&qd=scheme&refer=tieba.baidu.com&wise_sample_id=3000232_2-99999_9&fr=bpush&tid=${threadId}")
    }
}

private fun Context.launchOfficialApp(threadId: Long, postId: Long?) {
    val intent = Intent(Intent.ACTION_VIEW).setData(getDispatchUri(threadId, postId))
    val resolveInfos = packageManager
        .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        .filter { it.activityInfo.packageName != packageName }

    try {
        if (resolveInfos.isNotEmpty()) {
            startActivity(intent)
        } else {
            toastShort(R.string.toast_official_client_not_install)
        }
    } catch (_: ActivityNotFoundException) {
        toastShort(R.string.toast_official_client_not_install)
    }
}

@Composable
private fun ReplyWarningDialog(vm: ReplyViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val hideWarning = LocalHabitSettings.current.hideReplyWarning
    // Show warning dialog only once
    var dismissed by rememberSaveable { mutableStateOf(hideWarning) }
    if (dismissed) return

    val warningDialogState = rememberDialogState()
    if (warningDialogState.show) {
        ReplyWarningDialog(
            dialogState = warningDialogState,
            onDismiss = { dismissed = true },
            onLaunchOfficialApp = { context.launchOfficialApp(vm.threadId, vm.postId) },
            onBack = onBack
        )
    }

    LaunchedEffect(Unit) {
        delay(AnimationConstants.DefaultDurationMillis.toLong()) // Wait BottomSheet animation
        warningDialogState.show()
    }
}

@Composable
private fun ReplyWarningDialog(
    modifier: Modifier = Modifier,
    dialogState: DialogState,
    onDismiss: () -> Unit,
    onLaunchOfficialApp: () -> Unit,
    onBack: () -> Unit,
) {
    Dialog(
        modifier = modifier,
        dialogState = dialogState,
        onDismiss = onDismiss,
        title = { Text(text = stringResource(id = R.string.title_dialog_reply_warning)) },
        buttons = {
            Column(
                modifier = Modifier.padding(horizontal = DefaultDialogContentPadding),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                DialogPositiveButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.button_official_client_reply),
                    onClick = onLaunchOfficialApp
                )
                DialogNegativeButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.btn_continue_reply),
                )
                DialogNegativeButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.btn_cancel_reply),
                    onClick = onBack
                )
            }
        },
    ) {
        Text(
            text = stringResource(id = R.string.message_dialog_reply_warning),
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

@Preview("ImeActionRow")
@Composable
private fun ImeActionRowPreview() = TiebaLiteTheme {
    Surface(modifier = Modifier.padding(12.dp)) {
        ImeActionRow(
            onPanelClicked = {},
            selectedImageProvider = null,
            isReplying = { true },
        )
    }
}

@Preview("ImagePanel")
@Composable
private fun ImagePanelPreview() = TiebaLiteTheme {
    Surface(modifier = Modifier.height(260.dp)) { // Normal keyboard height
        ImagePanel(
            selectedImages = listOf("null.jpg"),
            onAddImageClicked = {},
            onRemoveImage = {},
            isOriginImage = false,
            onIsOriginImageChange = {}
        )
    }
}
