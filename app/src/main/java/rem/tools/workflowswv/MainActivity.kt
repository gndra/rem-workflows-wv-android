package rem.tools.workflowswv

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import rem.tools.workflows_webview.Step
import rem.tools.workflows_webview.Workflow
import rem.tools.workflows_webview.WorkflowWebview

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var webView: WebView = findViewById(R.id.test_wv)

        var wi = WorkflowWebview(
            wf_base_url = "https://api.test.rem.tools/workflows",
            wf_apikey = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
            wf_webview = webView,
        )
        wi.startWorkflow(
            activity = this@MainActivity,
            workflow_id = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
            step_callback = fun (step: Step) { println(step) },
            workflow_callback = fun (workflow: Workflow) { println(workflow) }
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        WorkflowWebview.onPermissionsResults(requestCode, grantResults)
    }
}