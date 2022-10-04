package rem.tools.workflowswv

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import rem.tools.workflows_webview.WorkflowsWebview

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var webView: WebView = findViewById(R.id.test_wv)

        var workflow = WorkflowsWebview(
            baseUrl = "https://api.test.rem.tools",
            apiKey = "356e504c490ad5b06544d2f97f180e241159e72b",
            webView = webView,
            minimal = true,
            activity = this@MainActivity
        )

        workflow.onStepEvent = {
            // Step Event Callback
            Log.d("STEP", "Evento")
        }

        workflow.onWorkflowEvent = {
            // Workflow Event Callback
            Log.d("Workflow", "Evento")
        }

        workflow.start(
            workflowId = "a3eba6ff-a012-401b-85b4-a413402425c9",
            callback = { success, _ ->
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