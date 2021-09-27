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
import androidx.recyclerview.widget.LinearLayoutManager
import com.owncloud.android.R
import com.owncloud.android.databinding.LogsListActivityBinding
import com.owncloud.android.presentation.adapters.logging.RecyclerViewLogsAdapter
import com.owncloud.android.presentation.viewmodels.logging.LogListViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class LogsListActivity : AppCompatActivity() {

    private val viewModel by viewModel<LogListViewModel>()
    private val recyclerViewLogsAdapter = RecyclerViewLogsAdapter()

    private var _binding: LogsListActivityBinding? = null
    val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = LogsListActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initToolbar()
        initList()
        initSwipeToRefresh()
    }

    private fun initToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.standard_toolbar).apply {
            isVisible = true
        }
        findViewById<ConstraintLayout>(R.id.root_toolbar).apply {
            isVisible = false
        }
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setTitle(R.string.prefs_open_logs_list_view)
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
        }
        setData()
    }

    private fun initSwipeToRefresh() {
        binding.swipeRefreshActivityLogsList.setOnRefreshListener {
            setData()
            binding.swipeRefreshActivityLogsList.isRefreshing = false
        }
    }

    private fun setData() {
        recyclerViewLogsAdapter.apply {
            setData(viewModel.getData())
        }
    }
}
