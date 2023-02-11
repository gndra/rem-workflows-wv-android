package rem.tools.workflowswv

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import rem.tools.workflows_webview.Step
import rem.tools.workflows_webview.Workflow
//import rem.tools.workflows.Step
//import rem.tools.workflows.Workflow
import rem.tools.workflows_webview.WorkflowError
import rem.tools.workflows_webview.WorkflowsWebview

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var webView: WebView = findViewById(R.id.test_wv)

        var workflow = WorkflowsWebview(
            baseUrl = "https://api.test.rem.tools",
            apiKey = "356e504c490ad5b06544d2f97f180e241159e72b"
        )

        workflow.onStepEvent = fun (step: Step) {
            Log.d("STEP", step.toString())
        }

        workflow.onWorkflowEvent = fun (workflow: Workflow) {
//            Log.d("WORKFLOW", workflow.toString())
            Toast.makeText(applicationContext, "Device IP " + workflow.metadata?.get("ip")?.asText()!!, Toast.LENGTH_LONG).show()
            Log.d("WORKFLOW", workflow.metadata?.get("ip")?.asText()!!)
        }

        workflow.start(
            workflowId = "b18d0a6a-80eb-4837-b3bc-47b295219004",
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