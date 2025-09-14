package com.huanchengfly.tieba.post.ui.page.user.edit

import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.UCropActivity
import com.huanchengfly.tieba.post.activities.UCropActivity.Companion.registerUCropResult
import com.huanchengfly.tieba.post.arch.BaseComposeActivity
import com.huanchengfly.tieba.post.arch.collectIn
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.theme.TiebaBlue
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.block
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BaseTextField
import com.huanchengfly.tieba.post.ui.widgets.compose.CounterTextField
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.ListSinglePicker
import com.huanchengfly.tieba.post.ui.widgets.compose.placeholder
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.StringUtil
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class EditProfileActivity : BaseComposeActivity() {
    private var colorScheme: ColorScheme? = null

    private val portraitFile: File by lazy { File(cacheDir, "cropped_portrait") }

    private val uCropLauncher = registerUCropResult {
        val result: Result<Uri> = it ?: return@registerUCropResult // Canceled
        if (result.isSuccess) {
            viewModel.send(EditProfileIntent.UploadPortrait(portraitFile))
        } else {
            val error = result.exceptionOrNull()?.message ?: getString(R.string.error_unknown)
            toastShort(error)
        }
    }

    private val pickMediasLauncher = registerForActivityResult(PickVisualMedia()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            delay(240L) // Wait exit animation of MediaPicker Activity

            // Launch UCropActivity now
            val primaryColor = (colorScheme?.primary?: TiebaBlue).toArgb()
            uCropLauncher.launch(buildUCropOptions(uri, primaryColor))
        }
    }

    private val viewModel: EditProfileViewModel by viewModels()

    private val intents by lazy {
        flowOf(EditProfileIntent.Init(AccountUtil.getUid() ?: "0"))
    }
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(0)
        window.setBackgroundDrawable(ColorDrawable(0))
        handler.post {
            intents.onEach(viewModel::send).launchIn(lifecycleScope)
        }
        viewModel.uiEventFlow
            .filterIsInstance<EditProfileEvent>()
            .collectIn(this) { handleEvent(it) }
    }

    @Composable
    override fun Content() {
        PageEditProfile(viewModel, onBackPressed = { onBackPressed() })

        val colors = MaterialTheme.colorScheme
        SideEffect {
            colorScheme = colors
        }
    }

    private fun handleEvent(event: EditProfileEvent) {
        when (event) {
            is EditProfileEvent.Init.Fail -> {
                toastShort(event.toast)
            }

            is EditProfileEvent.Submit.Result -> {
                if (event.success) {
                    if (event.changed) toastShort(R.string.toast_success)
                    finish()
                } else {
                    toastShort(event.message)
                }
            }

            EditProfileEvent.UploadPortrait.Pick -> {
                pickMediasLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
            }

            is EditProfileEvent.UploadPortrait.Fail -> toastShort(event.error)
            is EditProfileEvent.UploadPortrait.Success -> toastShort(event.message)
        }
    }

    /**
     * @return UCrop to launch [UCropActivity]
     * */
    private fun buildUCropOptions(sourceUri: Uri, @ColorInt primaryColor: Int): UCrop {
        val destUri = Uri.fromFile(portraitFile)
        return UCrop.of(sourceUri, destUri)
            .withAspectRatio(1f, 1f)
            .withOptions(UCrop.Options().apply {
                setShowCropFrame(true)
                setShowCropGrid(true)
                setStatusBarColor(primaryColor)
                setToolbarColor(primaryColor)
                setToolbarWidgetColor(Color.White.toArgb())
                setActiveControlsWidgetColor(primaryColor)
                setLogoColor(primaryColor)
                setCompressionFormat(Bitmap.CompressFormat.JPEG)
            }
            )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun EditProfileCard(
    portrait: String,
    name: String,
    nickName: String,
    sex: Int,
    intro: String,
    //birthdayShowStatus: Boolean,
    //birthdayTime: Long,
    loading: Boolean,
    onNickNameChange: ((String) -> Unit)? = null,
    onIntroChange: ((String) -> Unit)? = null,
    onUploadPortrait: () -> Unit = {},
    onModifySex: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val disabledText = MaterialTheme.colorScheme.outlineVariant
    Surface(
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.card_margin))
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .align(Alignment.CenterHorizontally)
                    .placeholder(visible = loading, shape = CircleShape)
            ) {
                GlideImage(
                    model = remember { StringUtil.getAvatarUrl(portrait) },
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    modifier = Modifier
                        .background(color = Color(0x77000000))
                        .fillMaxSize(),
                    onClick = onUploadPortrait
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoCamera,
                        contentDescription = stringResource(id = R.string.upload_portrait),
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            ConstraintLayout(
                modifier = Modifier.fillMaxWidth()
            ) {
                val (
                    nameTitle,
                    nameContent,
                    nickNameTitle,
                    nickNameContent,
                    sexTitle,
                    sexContent,
                    introTitle,
                    introContent,
                ) = createRefs()

                val titleEndBarrier = createEndBarrier(
                    nameTitle,
                    nickNameTitle,
                    sexTitle,
                    introTitle,
                    margin = 24.dp
                )

                Text(
                    text = stringResource(id = R.string.title_username),
                    color = disabledText,
                    modifier = Modifier.constrainAs(nameTitle) {
                        top.linkTo(parent.top)
                        bottom.linkTo(nameContent.bottom)
                        start.linkTo(parent.start)
                    }
                )
                Text(
                    text = name,
                    color = disabledText,
                    modifier = Modifier
                        .constrainAs(nameContent) {
                            top.linkTo(nameTitle.top)
                            bottom.linkTo(nameTitle.bottom)
                            start.linkTo(titleEndBarrier)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                        }
                        .placeholder(visible = loading)
                )

                Text(
                    text = stringResource(id = R.string.title_nickname),
                    color = disabledText,
                    modifier = Modifier.constrainAs(nickNameTitle) {
                        top.linkTo(nameTitle.bottom, margin = 16.dp)
                        start.linkTo(parent.start)
                    }
                )
                Row(
                    modifier = Modifier
                        .constrainAs(nickNameContent) {
                            top.linkTo(nickNameTitle.top)
                            bottom.linkTo(nickNameTitle.bottom)
                            start.linkTo(titleEndBarrier)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                        }
                        .placeholder(visible = loading)
                ) {
                    BaseTextField(
                        value = nickName,
                        onValueChange = { onNickNameChange?.invoke(it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        maxLines = 1,
                    )
                }

                Text(
                    text = stringResource(id = R.string.profile_sex),
                    color = disabledText,
                    modifier = Modifier.constrainAs(sexTitle) {
                        top.linkTo(nickNameTitle.bottom, margin = 16.dp)
                        start.linkTo(parent.start)
                    }
                )
                Row(
                    modifier = Modifier
                        .constrainAs(sexContent) {
                            top.linkTo(sexTitle.top)
                            bottom.linkTo(sexTitle.bottom)
                            start.linkTo(titleEndBarrier)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                        }
                        .block {
                            onModifySex?.let { clickableNoIndication(onClick = it) }
                        }
                        .placeholder(visible = loading)
                ) {
                    Text(
                        text = stringResource(
                            id = when (sex) {
                                1 -> R.string.profile_sex_male
                                2 -> R.string.profile_sex_female
                                else -> R.string.profile_sex_unset
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_round_chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.CenterVertically)
                    )
                }

                Text(
                    text = stringResource(id = R.string.title_intro),
                    color = disabledText,
                    modifier = Modifier.constrainAs(introTitle) {
                        top.linkTo(sexTitle.bottom, margin = 16.dp)
                        start.linkTo(parent.start)
                    }
                )
                CounterTextField(
                    value = intro,
                    onValueChange = { onIntroChange?.invoke(it) },
                    maxLength = 500,
                    countWhitespace = false,
                    onLengthBeyondRestrict = { context.toastShort(R.string.toast_intro_length_beyond_restrict) },
                    placeholder = { Text(text = stringResource(id = R.string.tip_no_intro)) },
                    modifier = Modifier
                        .constrainAs(introContent) {
                            top.linkTo(introTitle.top)
                            start.linkTo(titleEndBarrier)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                        }
                        .placeholder(visible = loading),
                )
            }
        }
    }
}

@Composable
private fun PageEditProfile(
    viewModel: EditProfileViewModel,
    onBackPressed: () -> Unit,
) {
    val isLoading by viewModel.uiState.collectPartialAsState(
        EditProfileState::isLoading,
        initial = false
    )
    if (!isLoading) {
        val uiState by viewModel.uiState.collectAsState()
        var sex by remember { mutableIntStateOf(uiState.sex) }
        val birthdayTime by remember { mutableLongStateOf(uiState.birthdayTime) }
        val birthdayShowStatus by remember { mutableStateOf(uiState.birthdayShowStatus) }
        var intro by remember { mutableStateOf(uiState.intro) }
        var nickName by remember { mutableStateOf(uiState.nickName) }
        Scaffold(
            topBar = {
                Toolbar(
                    title = stringResource(id = R.string.title_activity_edit_profile),
                    navigationIcon = { BackNavigationIcon(onBackPressed) },
                    actions = {
                        ActionItem(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_round_save_24),
                            contentDescription = stringResource(id = R.string.button_save_profile)
                        ) {
                            if (sex != uiState.sex ||
                                birthdayTime != uiState.birthdayTime ||
                                birthdayShowStatus != uiState.birthdayShowStatus ||
                                intro != uiState.intro ||
                                nickName != uiState.nickName
                            ) {
                                if (nickName.isNotEmpty()) {
                                    viewModel.send(
                                        EditProfileIntent.Submit(
                                            sex,
                                            birthdayTime,
                                            birthdayShowStatus,
                                            intro ?: "",
                                            nickName
                                        )
                                    )
                                }
                            } else {
                                viewModel.send(EditProfileIntent.SubmitWithoutChange)
                            }
                        }
                    }
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .padding(horizontal = dimensionResource(id = R.dimen.card_margin))
                    .imePadding()
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                val dialogState = rememberDialogState()
                Dialog(
                    dialogState = dialogState,
                    title = { Text(text = stringResource(id = R.string.title_modify_sex)) },
                    buttons = {
                        DialogNegativeButton(text = stringResource(id = R.string.button_cancel))
                    }
                ) {
                    ListSinglePicker(
                        items = persistentMapOf(
                            1 to R.string.profile_sex_male,
                            2 to R.string.profile_sex_female
                        ),
                        selected = sex,
                        onItemSelected = { value, changed ->
                            if (changed) sex = value
                            dismiss()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.card_margin)))
                EditProfileCard(
                    portrait = uiState.portrait,
                    name = uiState.name,
                    nickName = nickName,
                    sex = sex,
                    intro = intro ?: "",
                    loading = uiState.isLoading,
                    onNickNameChange = { nickName = it },
                    onIntroChange = { intro = it },
                    onUploadPortrait = { viewModel.send(EditProfileIntent.UploadPortraitStart) },
                    onModifySex = { dialogState.show = true }
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.card_margin)))
            }
        }
    } else {
        Scaffold(
            topBar = {
                Toolbar(
                    title = stringResource(id = R.string.title_activity_edit_profile),
                    navigationIcon = { BackNavigationIcon(onBackPressed) }
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .padding(contentPadding)
                    .padding(horizontal = dimensionResource(id = R.dimen.card_margin))
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.card_margin)))
                EditProfileCard(
                    portrait = "",
                    name = "",
                    nickName = "",
                    sex = 0,
                    intro = "",
                    loading = true
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.card_margin)))
            }
        }
    }
}

@Preview
@Composable
fun EditProfileCardPreview() {
    TiebaLiteTheme {
        EditProfileCard(
            portrait = "",
            name = "test",
            nickName = "test",
            sex = 1,
            intro = "test",
            loading = false,
        )
    }
}

@Preview
@Composable
fun EditProfileCardLoadingPreview() {
    TiebaLiteTheme {
        EditProfileCard(
            portrait = "",
            name = "",
            nickName = "",
            sex = 0,
            intro = "",
            loading = true,
        )
    }
}
