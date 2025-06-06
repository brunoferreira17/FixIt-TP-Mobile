package com.ipvc.fixit.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.ipvc.fixit.SupabaseClientInstance
import com.ipvc.fixit.database.AppDatabase
import com.ipvc.fixit.repository.FaultRepository
import com.ipvc.fixit.viewmodel.FaultViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun syncAllPendingFaults(context: Context, coroutineScope: CoroutineScope) {
    if (!SupabaseClientInstance.isConnectedToInternet(context)) return

    val db = AppDatabase.getDatabase(context)
    val faultViewModel = FaultViewModel(FaultRepository(db.faultDao()))

    coroutineScope.launch(Dispatchers.IO) {
        val unsynced = faultViewModel.getUnsynced()
        for (fault in unsynced) {
            val success = SupabaseClientInstance.syncFault(fault, fault.reportedBy)
            if (success) {
                faultViewModel.markAsSynced(fault.faultId)
                Log.d("SyncUtils", "Sincronizado: faultId=${fault.faultId}")
            } else {
                Log.e("SyncUtils", "Falha ao sincronizar: faultId=${fault.faultId}")
            }
        }

        if (unsynced.isNotEmpty()) {
            launch(Dispatchers.Main) {
                Toast.makeText(context, "Sincronização concluída.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
