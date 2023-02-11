package rem.tools.workflows_webview

import android.webkit.JavascriptInterface
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * interface to implements for JavaScript Bridge execution on workflows web
 * */
interface WJavascriptInterface {
    @JavascriptInterface
    public fun onStepCompletion (message: String)

    @JavascriptInterface
    public fun onWorkflowCompletion (message: String)
}


public class WorkflowsJavascriptInterface (val workflowsWebview: WorkflowsWebview) : WJavascriptInterface  {

    @JavascriptInterface
    public override fun onStepCompletion(message: String) {
        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val step: Step = mapper.readValue(message, Step::class.java)

        workflowsWebview.onStepEvent?.invoke(step)
    }

    @JavascriptInterface
    public override fun onWorkflowCompletion(message: String) {
        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        val workflow: Workflow = mapper.readValue(message, Workflow::class.java)

        workflowsWebview.onWorkflowEvent?.invoke(workflow)
    }
}
