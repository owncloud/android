/*
 * ownCloud Android client application
 *
 * @author Fernando Sanz Velasco
 * Copyright (C) 2021 ownCloud GmbH.
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
 *
 */

package com.owncloud.android.presentation.ui.logging

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.owncloud.android.R
import com.owncloud.android.databinding.LogsListActivityBinding
import com.owncloud.android.extensions.openFile
import com.owncloud.android.extensions.sendFile
import com.owncloud.android.presentation.adapters.logging.RecyclerViewLogsAdapter
import com.owncloud.android.presentation.viewmodels.logging.LogListViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class LogsListActivity : AppCompatActivity() {

    private val viewModel by viewModel<LogListViewModel>()

    private var _binding: LogsListActivityBinding? = null
    val binding get() = _binding!!

    private val recyclerViewLogsAdapter = RecyclerViewLogsAdapter(object : RecyclerViewLogsAdapter.Listener {
        override fun share(file: File) {
            sendFile(file)
        }

        override fun delete(file: File) {
            file.delete()
            setData()
        }

        override fun open(file: File) {
            openFile(file)
        }
    }, context = this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = LogsListActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initToolbar()
        initList()
    }

    private fun initToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.standard_toolbar)
        toolbar.isVisible = true

        findViewById<ConstraintLayout>(R.id.root_toolbar).isVisible = false
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setTitle(R.string.prefs_log_open_logs_list_view)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initList() {
        binding.recyclerViewActivityLogsList.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            adapter = recyclerViewLogsAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        setData()
    }

    private fun setData() {
        val items = viewModel.getLogsFiles()

        binding.recyclerViewActivityLogsList.isVisible = items.isNotEmpty()
        binding.logsListEmpty.apply {
            root.isVisible = items.isEmpty()
            listEmptyDatasetIcon.setImageResource(R.drawable.ic_logs)
            listEmptyDatasetTitle.setText(R.string.prefs_log_no_logs_list_view)
            listEmptyDatasetSubTitle.setText(R.string.prefs_log_empty_subtitle)
        }

        recyclerViewLogsAdapter.setData(items)
    }
}
