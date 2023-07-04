/**
 * ownCloud Android client application
 *
 * @author Parneet Singh
 *
 * Copyright (C) 2023 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.preview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.owncloud.android.R
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin

class PreviewFormatTextFragmentStateAdapter(
    fragment: Fragment,
    private val text: String,
    private val mimeType: String
) : FragmentStateAdapter(fragment) {

    val formatTypes =
        mapOf(TYPE_MARKDOWN to fragment.getString(R.string.tab_label_markdown), TYPE_PLAIN to fragment.getString(R.string.tab_label_ascii))

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PreviewFormatTextFragment.newInstance(text, mimeType)
            else -> PreviewFormatTextFragment.newInstance(text)
        }
    }

    class PreviewFormatTextFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.preview_format_text_fragment, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val args = requireArguments()
            val text = args.getString(TEXT_KEY)
            val mimeType: String? = args.getString(MIME_TYPE_KEY)

            val textView: TextView = view.findViewById(R.id.text_preview)

            if (mimeType == TYPE_MARKDOWN) {
                setMarkdown(textView, text!!)
            } else {
                textView.text = text
            }
        }

        private fun setMarkdown(textView: TextView, text: String) {
            val context: Context = textView.context
            val markwon =
                Markwon.builder(context).usePlugin(TablePlugin.create(context)).usePlugin(StrikethroughPlugin.create())
                    .usePlugin(TaskListPlugin.create(context)).usePlugin(HtmlPlugin.create()).build()
            markwon.setMarkdown(textView, text)
        }

        companion object {
            private const val TEXT_KEY = "TEXT_KEY"
            private const val MIME_TYPE_KEY = "MIME_TYPE_KEY"

            fun newInstance(text: String, mimeType: String? = null): PreviewFormatTextFragment {
                val args = Bundle()
                args.apply {
                    putString(TEXT_KEY, text)
                    putString(MIME_TYPE_KEY, mimeType)
                }
                val fragment = PreviewFormatTextFragment()
                fragment.arguments = args
                return fragment
            }

        }
    }

    companion object {
        const val TYPE_PLAIN = "text/plain"
        private const val TYPE_MARKDOWN = "text/markdown"
    }
}

