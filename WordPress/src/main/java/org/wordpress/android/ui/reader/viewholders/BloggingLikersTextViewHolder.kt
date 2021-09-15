package org.wordpress.android.ui.reader.viewholders

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.UnderlineSpan
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.databinding.BloggerLikersTextItemBinding
import org.wordpress.android.ui.reader.adapters.FACE_ITEM_AVATAR_SIZE_DIMEN
import org.wordpress.android.ui.reader.adapters.FACE_ITEM_LEFT_OFFSET_DIMEN
import org.wordpress.android.ui.reader.adapters.TrainOfFacesItem.BloggersLikingTextItem
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.getColorFromAttribute
import org.wordpress.android.util.viewBinding

class BloggingLikersTextViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : TrainOfFacesViewHolder<BloggerLikersTextItemBinding>(parent.viewBinding(BloggerLikersTextItemBinding::inflate)) {
    fun bind(bloggersTextItem: BloggersLikingTextItem) = with(binding) {
        val position = adapterPosition

        if (position >= 0) {
            val displayWidth = DisplayUtils.getDisplayPixelWidth(itemView.context)
            val paddingWidth = 2 * itemView.context.resources.getDimensionPixelSize(R.dimen.reader_detail_margin)
            val avatarSize = itemView.context.resources.getDimensionPixelSize(FACE_ITEM_AVATAR_SIZE_DIMEN)
            val leftOffest = itemView.context.resources.getDimensionPixelSize(FACE_ITEM_LEFT_OFFSET_DIMEN)
            val facesWidth = position * avatarSize - (position - 1).coerceAtLeast(0) * leftOffest

            itemView.layoutParams.width = displayWidth - paddingWidth - facesWidth
        }

        numBloggers.text = with(bloggersTextItem) {
            val fullText = uiHelpers.getTextOfUiString(itemView.context, textWithParams)
            formatWithSpan(itemView.context, fullText)
        }
    }

    private fun formatWithSpan(context: Context, text: CharSequence): Spannable {
        val fullText = StringBuffer(text).toString()
        val underlineEnd = fullText.lastIndexOf("_")

        if (underlineEnd < 0) return SpannableString(fullText)

        val closureString = fullText.substring(underlineEnd).replace("_", "")
        val fullTextSanitized = fullText.replace("_", "")
        val start = 0
        val end = fullTextSanitized.lastIndexOf(closureString)
        return if (end <= start || end >= text.length - 1) {
            SpannableString(fullTextSanitized)
        } else {
            SpannableString(fullTextSanitized).apply {
                setSpan(
                        object : UnderlineSpan() {
                            override fun updateDrawState(ds: TextPaint) {
                                ds.isUnderlineText = true
                                ds.color = context.getColorFromAttribute(R.attr.colorPrimary)
                            }
                        },
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }
}
