package com.ipvc.fixit

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

class OperatorDashboardActivity : ComponentActivity() {

    data class Issue(val equipmentName: String, val status: String, val date: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_dashboard)

        val issuesContainer = findViewById<LinearLayout>(R.id.issuesContainer)

        // Exemplo de dados simulados
        val issues = listOf(
            Issue("CNC Máquina 1", "Pendente", "2025-05-10"),
            Issue("Torno Hidráulico", "Resolvida", "2025-04-28"),
            Issue("Impressora 3D", "Pendente", "2025-05-13")
        )

        for (issue in issues) {
            val view = LayoutInflater.from(this)
                .inflate(R.layout.component_issue_item, issuesContainer, false)

            val tituloCompleto = "${issue.equipmentName} - ${issue.status}"
            view.findViewById<TextView>(R.id.issueTitleAndStatus).text = tituloCompleto
            view.findViewById<TextView>(R.id.issueDate).text = issue.date

            issuesContainer.addView(view)
        }
    }
}
