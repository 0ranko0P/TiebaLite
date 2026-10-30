package com.huanchengfly.tieba.post.components.dialogs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.core.net.toUri
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector
import com.huanchengfly.tieba.post.core.ui.component.NegativeButton
import com.huanchengfly.tieba.post.core.ui.component.dialog.DialogMinWidth
import com.huanchengfly.tieba.post.core.ui.component.dialog.ResultDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultDialogContentPadding
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.core.ui.R as CoreUiR

class ShareThreadDialog: ResultDialog<Unit>() {

    private var threadId: Long = -1
    private lateinit var threadUrl: String
    private lateinit var title: String
    private lateinit var user: String

    // TitleRes, Icon, ClickListener
    private val shareOptList by lazy {
        val icChrome = ImageVector.vectorResource(res = resources, resId = R.drawable.ic_chrome)
        listOf(
            Triple(R.string.title_copy_link, Icons.Rounded.ContentPaste, ::onCopyLinkClicked),
            Triple(R.string.title_open_in_browser, icChrome, ::onOpenInBrowserClicked),
            Triple(CoreUiR.string.core_ui_title_share, Icons.Rounded.Share, ::onShareClicked)
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val bundle = requireArguments()
        threadId = bundle.getLong(KEY_THREAD_ID)
        threadUrl = "https://tieba.baidu.com/p/$threadId"
        title = bundle.getString(KEY_THREAD_TITLE, "")
        user = bundle.getString(KEY_USER)!!
    }

    @Composable
    override fun ContentView() {
        Column(
            modifier = Modifier.padding(vertical = DefaultDialogContentPadding),
        ) {
            Text(
                text = stringResource(R.string.title_share_thread),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                maxLines = 1,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))

            shareOptList.fastForEach { (titleRes, icon, clickListener) ->
                Row(
                    modifier = Modifier
                        .width(DialogMinWidth)
                        .clickable(onClick = clickListener)
                        .padding(horizontal = DefaultDialogContentPadding, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = icon, contentDescription = stringResource(titleRes))
                    Text(
                        text = stringResource(titleRes),
                        modifier = Modifier.weight(1.0f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            NegativeButton(
                modifier = Modifier
                    .padding(start = DefaultDialogContentPadding, top = 8.dp, end = DefaultDialogContentPadding)
                    .align(Alignment.End),
                text = stringResource(android.R.string.cancel),
                onClick = ::dismiss
            )
        }
    }

    private fun onCopyLinkClicked() {
        TiebaUtil.copyText(context = requireContext(), text = threadUrl)
        ClipBoardLinkDetector.onCopyTiebaLink(threadUrl)
        dismiss()
    }

    private fun onShareClicked() {
        val content = "$title By ${user}:\n$threadUrl"
        val title = getString(CoreUiR.string.core_ui_title_share)
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, content)
            setType("text/plain")
        }
        runCatching {
            startActivity(Intent.createChooser(intent, title))
        }
        .onSuccess {
            ClipBoardLinkDetector.onCopyTiebaLink(content)
        }
        dismiss()
    }

    private fun onOpenInBrowserClicked() {
        runCatching {
            TiebaUtil.openInBrowser(requireActivity(), threadUrl)
        }
        dismiss()
    }

    companion object {

        private const val KEY_THREAD_ID = "com.huanchengfly.tieba.post.SHARE_THREAD_ID"
        private const val KEY_THREAD_TITLE = "com.huanchengfly.tieba.post.SHARE_THREAD_TITLE"
        private const val KEY_USER = "com.huanchengfly.tieba.post.SHARE_USER"

        fun newInstance(threadID: Long, title: String, user: String): ShareThreadDialog {
            return ShareThreadDialog().apply {
                arguments = Bundle().also {
                    it.putLong(KEY_THREAD_ID, threadID)
                    it.putString(KEY_THREAD_TITLE, title)
                    it.putString(KEY_USER, user)
                }
            }
        }

    }
}