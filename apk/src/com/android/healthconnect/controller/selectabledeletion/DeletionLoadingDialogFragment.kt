/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.healthconnect.controller.selectabledeletion

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import com.android.healthconnect.controller.utils.logging.ProgressDialogElement

class DeletionLoadingDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view: View = layoutInflater.inflate(R.layout.dialog_progress, null)
        val title = view.findViewById<TextView>(R.id.progress_indicator_title)
        title.setText(R.string.delete_progress_indicator)
        // TODO: (b/341886932) add new telemetry for deletion dialogs
        return AlertDialogBuilder(this, ProgressDialogElement.DELETION_DIALOG_IN_PROGRESS_CONTAINER)
            .setView(view)
            .create()
    }

    companion object {
        const val TAG = "DeletionLoadingDialogFragment"
    }
}
