package rem.tools.workflowswv

import android.content.Intent
import android.os.Build
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true)
        }

        var workflow = WorkflowsWebview(
            baseUrl = "https://api.test.rem.tools",
            apiKey = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
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
            workflowId = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
            webView = webView,
            minimal = true,
            activity = this@MainActivity,
            runHost = null,
            callback = fun (success: Boolean, error: WorkflowError?) {
                // Initialization Callback
                Log.d("ERROR", error.toString())
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        WorkflowsWebview.onActivityResult(requestCode, resultCode, data)
    }
}