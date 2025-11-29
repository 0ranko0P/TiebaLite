package com.huanchengfly.tieba.post.ui.page.user.edit

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.TranslucentThemeActivity.Companion.currentThemeNoTrans
import com.huanchengfly.tieba.post.activities.UCropActivity
import com.huanchengfly.tieba.post.activities.UCropActivity.Companion.registerUCropResult
import com.huanchengfly.tieba.post.arch.BaseComposeActivity
import com.huanchengfly.tieba.post.arch.collectIn
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultFabEnterTransition
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultFabExitTransition
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.OutlineCounterTextField
import com.huanchengfly.tieba.post.ui.widgets.compose.placeholder
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.GlideUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class EditProfileActivity : BaseComposeActivity() {

    private val portraitFile: File by lazy { File(cacheDir, "cropped_portrait") }

    // Launch UCropActivity for result (cropped portrait)
    private val uCropLauncher = registerUCropResult { result ->
        result?.run { // null when RESULT_CANCELED, do nothing
            onSuccess { uri ->
                viewModel.send(EditProfileIntent.UploadPortrait(portraitFile))
            }
            onFailure { e ->
                toastShort(e.message ?: getString(R.string.error_unknown))
            }
        }
    }

    private val pickMediasLauncher = registerForActivityResult(PickVisualMedia()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            delay(240L) // Wait exit animation of MediaPicker Activity

            // Launch UCropActivity now
            val colorScheme = ThemeUtil.getRawTheme().colorScheme
            val primaryColor = colorScheme.primary.toArgb()
            uCropLauncher.launch(buildUCropOptions(uri, primaryColor))
        }
    }

    private val viewModel: EditProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(0)
        viewModel.uiEventFlow
            .filterIsInstance<EditProfileEvent>()
            .collectIn(lifecycleOwner = this, collector = ::handleEvent)
    }

    @Composable
    override fun Content() {
        LazyLoad(loaded = viewModel.initialized) {
            viewModel.send(EditProfileIntent.Init)
            viewModel.initialized = true
        }

        TiebaLiteTheme(colorSchemeExt = currentThemeNoTrans()) {
            EditProfileScaffold(viewModel, onBackPressed = ::finish)

            val colorScheme = MaterialTheme.colorScheme
            LaunchedEffect(colorScheme) {
                windowInsetsController.isAppearanceLightStatusBars = ThemeUtil.isStatusBarFontDark(colorScheme)
                windowInsetsController.isAppearanceLightNavigationBars = ThemeUtil.isNavigationBarFontDark(colorScheme)
            }
        }
    }

    private fun handleEvent(event: EditProfileEvent) {
        when (event) {
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
                setToolbarColor(primaryColor)
                setLogoColor(primaryColor)
                setCompressionFormat(Bitmap.CompressFormat.JPEG)
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditProfileScaffold(
    viewModel: EditProfileViewModel,
    onBackPressed: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val uiState by viewModel.uiState.collectAsState()

    var editProfile: EditProfile? by remember { mutableStateOf(null) }
    uiState.edit?.let {
        LaunchedEffect(editProfile == null) {
            editProfile = it // Copy this profile for edit
        }
    }

    MyScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                titleRes = R.string.title_activity_edit_profile,
                navigationIcon = { BackNavigationIcon(onBackPressed) },
                actions = {
                    val allowSubmit by remember {
                        derivedStateOf {
                           !uiState.isSubmitting && !editProfile?.nickName.isNullOrEmpty() && editProfile != uiState.edit
                        }
                    }
                    AnimatedVisibility(
                        visible = allowSubmit,
                        enter = DefaultFabEnterTransition,
                        exit = DefaultFabExitTransition
                    ) {
                        ActionItem(
                            icon = Icons.Rounded.Save,
                            contentDescription = R.string.button_save_profile,
                            enabled = allowSubmit,
                            onClick = { viewModel.onSubmitProfile(newProfile = editProfile!!) }
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        backgroundColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        StateScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            isError = uiState.error != null,
            isLoading = uiState.isLoading,
            errorScreen = {
                ErrorScreen(error = uiState.error, modifier = Modifier.safeContentPadding())
            }
        ) {
            editProfile?.let { profile ->
                Container(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .imeNestedScroll()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .verticalScroll(rememberScrollState()),
                ) {
                    EditProfileCard(
                        avatar = uiState.avatarUrl ?: "",
                        name = uiState.name,
                        nickName = profile.nickName,
                        sex = profile.sex,
                        intro = profile.intro,
                        onNickNameChange = {
                            editProfile = profile.copy(nickName = it.trim())
                        },
                        onIntroChange = {
                            editProfile = profile.copy(intro = it)
                        },
                        onSexChange = {
                            editProfile = profile.copy(sex = it)
                        },
                        onUploadPortrait = { viewModel.send(EditProfileIntent.UploadPortraitStart) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SexDropdownMenuBox(modifier: Modifier = Modifier, sex: Int, onSexChange: (Int) -> Unit) {
    val context = LocalContext.current
    val options = remember {
        persistentMapOf(
            1 to context.getString(R.string.profile_sex_male),
            2 to context.getString(R.string.profile_sex_female)
        )
    }
    val (expanded, setExpanded) = remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = setExpanded,
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = options[sex] ?: "?",
            onValueChange = {/* ReadOnly */},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            label = {
                Text(text = stringResource(id = R.string.profile_sex))
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )

        ExposedDropdownMenu(
            modifier = Modifier.heightIn(max = 280.dp),
            expanded = expanded,
            onDismissRequest = { setExpanded(false) },
        ) {
            options.forEach { (option, title) ->
                DropdownMenuItem(
                    text = { Text(title, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        setExpanded(false)
                        if (option != sex) {
                            onSexChange(option)
                        }
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun UserAvatarPicker(modifier: Modifier = Modifier, avatar: String, onPick: () -> Unit) {
    Box(
        modifier = modifier.clickableNoIndication(onClick = onPick)
    ) {
        GlideImage(
            model = avatar,
            contentDescription = stringResource(id = R.string.upload_portrait),
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .placeholder(avatar.isEmpty()),
            failure = GlideUtil.DefaultErrorPlaceholder,
        )
        // Picker icon with border
        Surface(
            modifier = Modifier
                .size(26.dp)
                .background(color = MaterialTheme.colorScheme.background, shape = CircleShape)
                .padding(1.dp)
                .align(Alignment.BottomEnd),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
        ) {
            Icon(
                imageVector = Icons.Rounded.PhotoCamera,
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .padding(4.dp),
            )
        }
    }
}

@Composable
private fun EditProfileCard(
    modifier: Modifier = Modifier,
    avatar: String,
    name: String,
    nickName: String,
    sex: Int,
    intro: String?,
    //birthdayShowStatus: Boolean,
    //birthdayTime: Long,
    onNickNameChange: (String) -> Unit = {},
    onSexChange: (Int) -> Unit = {},
    onIntroChange: (String) -> Unit = {},
    onUploadPortrait: () -> Unit = {},
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        UserAvatarPicker(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .size(96.dp),
            avatar = avatar,
            onPick = onUploadPortrait
        )

        OutlinedTextField(
            value = name,
            onValueChange = {/* ReadOnly */ },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            label = { Text(text = stringResource(id = R.string.title_username)) },
            singleLine = true,
        )

        OutlinedTextField(
            value = nickName,
            onValueChange = onNickNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(text = stringResource(id = R.string.title_nickname))
            },
            singleLine = true,
        )

        SexDropdownMenuBox(sex = sex, onSexChange = onSexChange)

        OutlineCounterTextField(
            value = intro.orEmpty(),
            onValueChange = onIntroChange,
            modifier = Modifier.fillMaxWidth(),
            maxLength = 500,
            onLengthBeyondRestrict = {
                context.toastShort(R.string.toast_intro_length_beyond_restrict)
            },
            label = {
                Text(text = stringResource(id = R.string.title_intro))
            },
            placeholder = { Text(text = stringResource(id = R.string.tip_no_intro)) },
        )
    }
}

@Preview
@Composable
private fun EditProfileCardPreview() = TiebaLiteTheme {
    Surface {
        EditProfileCard(
            avatar = "",
            name = "test",
            nickName = "test",
            sex = 1,
            intro = "test",
        )
    }
}
