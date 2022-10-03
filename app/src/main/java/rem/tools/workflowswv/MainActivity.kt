package rem.tools.workflowswv

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import rem.tools.workflows.Step
import rem.tools.workflows.Workflow
import rem.tools.workflows_webview.WorkflowWebview

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var webView: WebView = findViewById(R.id.test_wv)

        var wi = WorkflowWebview(
            wf_base_url = "https://api.test.rem.tools/workflows",
            wf_apikey = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
            wf_webview = webView,
            minimal = true
        )
        wi.startWorkflow(
            activity = this@MainActivity,
            workflow_id = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
            step_callback = fun (step: Step) { Log.d("WF_I", step.createdAt) },
            workflow_callback = fun (workflow: Workflow) { Log.d("WF_I",
                workflow.metadata.fieldsMap["ip"].toString()
            ) }
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