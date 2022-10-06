package rem.tools.workflowswv

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import rem.tools.workflows.Step
import rem.tools.workflows.Workflow
import rem.tools.workflows_webview.WorkflowError
import rem.tools.workflows_webview.WorkflowsWebview

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var webView: WebView = findViewById(R.id.test_wv)

        var workflow = WorkflowsWebview(
            baseUrl = "https://api.test.rem.tools",
            apiKey = "XXXXXXXXXXXXXXXXXXXXXXX"
        )

        workflow.onStepEvent = fun (step: Step) {
            Log.d("STEP", "Evento")
        }

        workflow.onWorkflowEvent = fun (workflow: Workflow) {
            Log.d("WORKFLOW", "Evento")
        }

        workflow.start(
            workflowId = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
            webView = webView,
            minimal = true,
            activity = this@MainActivity,
            callback = fun (success: Boolean, error: WorkflowError?) {
                // Initialization Callback
                Log.d("START", success.toString())
            }
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        WorkflowsWebview.onPermissionsResults(requestCode, grantResults)
    }
}