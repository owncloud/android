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

package com.owncloud.android.presentation.logging

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
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
import com.owncloud.android.extensions.showMessageInSnackbar
import com.owncloud.android.presentation.settings.logging.SettingsLogsViewModel
import com.owncloud.android.providers.LogsProvider
import com.owncloud.android.providers.MdmProvider
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class LogsListActivity : AppCompatActivity() {

    private val viewModel by viewModel<LogListViewModel>()

    private val logsViewModel by viewModel<SettingsLogsViewModel>()

    private var _binding: LogsListActivityBinding? = null

    private var createNewLogFile: Boolean = false
    val binding get() = _binding!!

    private val recyclerViewLogsAdapter = RecyclerViewLogsAdapter(object : RecyclerViewLogsAdapter.Listener {
        override fun share(file: File) {
            sendFile(file)
        }

        override fun delete(file: File, isLastLogFileDeleted: Boolean) {
            file.delete()
            setData()
            createNewLogFile = isLastLogFileDeleted
        }

        override fun open(file: File) {
            openFile(file)
        }

        override fun download(file: File) {
            downloadFile(file)
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
        if (createNewLogFile && logsViewModel.isLoggingEnabled()) {
            val mdmProvider = MdmProvider(applicationContext)
            LogsProvider(applicationContext, mdmProvider).startLogging()
        }
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

    private fun downloadFile(file: File) {
        val destinationPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        val destinationFolder = File(destinationPath)
        val originalName = file.name
        var uniqueName = originalName
        var fileNumber = 1

        while (File(destinationFolder, uniqueName).exists()) {
            uniqueName = "$originalName ($fileNumber).log"
            fileNumber++
        }

        val destination = File(destinationFolder, uniqueName)

        try {
            val sourceChannel = FileInputStream(file).channel
            val destinationChannel = FileOutputStream(destination).channel
            sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel)
            sourceChannel.close()
            destinationChannel.close()
            showDownloadDialog(destination.name)

        } catch (e: IOException) {
            e.printStackTrace()
            Timber.e(e, "There was a problem to download the file to Download folder.")
        }
    }

    private fun showDownloadDialog(fileName: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.log_file_downloaded))
            .setIcon(R.drawable.ic_baseline_download_grey)
            .setMessage(getString(R.string.log_file_downloaded_description, fileName))
            .setPositiveButton(R.string.go_to_download_folder) { dialog, _ ->
                dialog.dismiss()
                goToDownloadsFolder()
            }
            .setNegativeButton(R.string.drawer_close) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        dialog.show()
    }

    private fun goToDownloadsFolder() {
        try {
            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showMessageInSnackbar(message = this.getString(R.string.file_list_no_app_for_perform_action))
            Timber.e("No Activity found to handle Intent")
        }
    }
}
