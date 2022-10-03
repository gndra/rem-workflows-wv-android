package rem.tools.workflows_webview

import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.protobuf.util.JsonFormat
import okhttp3.*
import org.json.JSONObject
import rem.tools.workflows.Step
import rem.tools.workflows.Workflow
import java.io.IOException


/*
* Configure a workflow previously created for webview use
*
* @author by Rem Tools
*
* @property wf_base_url
* @property wf_apikey is the apikey for the rem tools services
* @property workflow_id is the id of a workflow previously created
* @property wf_webview is the webview defined by the user for display the workflow
* @property step_callback is a function to call when a step finish
* @property workflow_callback is a function to call when a workflow finish
* */
class WorkflowWebview(
    wf_base_url: String?,
    private var wf_apikey: String,
    private var wf_webview: WebView,
    minimal: Boolean?,
) {
    private var wf_base_url: String = "https://api.rem.tools/workflows"
    private var minimal: Boolean = false
    private val client = OkHttpClient()

    companion object {
        const val PERMISSIONS_REQUEST_CODE = 101010
        /*
        * Check that all permission are granted otherwise throws a exception
        * @param requestCode is the requestCode from onRequestPermissionsResult function
        * @param grantResults is the array from onRequestPermissionsResult function
        * @throws java.lang.Exception for permissions denied
        * */
        fun onPermissionsResults(requestCode: Int, grantResults: IntArray) {
            if (requestCode == this.PERMISSIONS_REQUEST_CODE) {
                // If request is cancelled, the result arrays are empty.
                val permDenied = grantResults.contains(PackageManager.PERMISSION_DENIED)
                if (grantResults.isNotEmpty() && !permDenied) {
                    Log.d("Workflows", "Permissions Granted")
                } else {
                    Log.d("Workflows", "Permissions Denied")
                    throw java.lang.Exception("Permissions denied")
                }
                return
            }
        }
    }

    init {
        if (!wf_base_url.isNullOrEmpty()) {
            this.wf_base_url = wf_base_url
        }
        if (minimal == true) {
            this.minimal = true
        }
    }

    /*
    * Start the workflow on the corresponding webview
    *
    * @param applicationContext is the current aplication context
    * @param activity is the activity where the webview is running
    * @param step_callback is a function to execute after a step finish
    * @param workflow_callback is a function to execute after the workflow finish
    * */
    fun startWorkflow(
        activity: Activity,
        workflow_id: String,
        step_callback: (step: Step) -> Unit,
        workflow_callback: (workflow: Workflow) -> Unit
    ) {
        val self = this
        true.also { this.wf_webview.settings.javaScriptEnabled = it }
        this.wf_webview.settings.allowContentAccess = true
        this.wf_webview.settings.mediaPlaybackRequiresUserGesture = false
        this.wf_webview.addJavascriptInterface(object : WorkflowCommsInterface {
            @JavascriptInterface
            override fun onStepCompletion(message: String) {
                val SBuilder = Step.newBuilder()
                JsonFormat.parser().ignoringUnknownFields().merge(message, SBuilder)
                val step = SBuilder.build()
                step_callback(step)
            }

            @JavascriptInterface
            override fun onWorkflowCompletion(message: String) {
                val WBuider = Workflow.newBuilder()
                JsonFormat.parser().ignoringUnknownFields().merge(message, WBuider)
                val workflow = WBuider.build()
                workflow_callback(workflow)
            }
        }, "workflowsWebview")
        this.wf_webview.webChromeClient = object : WebChromeClient() {
            // Grant permissions for cam
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }
        this.wf_webview.webViewClient = object : WebViewClient () {

        }
        val uri = Uri.parse(this.wf_base_url)
            .buildUpon()
            .appendEncodedPath("/$workflow_id/create-token")
            .build()
        val req = Request.Builder()
            .url(uri.toString())
            .addHeader("Rem-Apikey", this.wf_apikey)
            .get()
            .build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("Error Create Token", e.message!!)
            }

            @RequiresApi(Build.VERSION_CODES.M)
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val resData = response.body!!.string()
                    Log.d("Res", resData)
                    val json = JSONObject(resData)
                    Handler(Looper.getMainLooper()).post {
                        val steps = json.getJSONObject("result").getJSONObject("workflow").getJSONArray("steps")
                        var permissions = arrayListOf<String>()
                        for (i in 0 until steps.length()) {
                            permissions.add(steps.getJSONObject(i).getString("step"))
                        }
                        askPermissions(activity, permissions)
//                        askPermissions(activity, arrayListOf("enroll_full", "video_sign"))
//                        Log.d("Url", json.getJSONObject("result").getString("public_url"))
                        var url = json.getJSONObject("result").getString("public_url")
//                        var url = "https://491d-189-203-96-149.ngrok.io/" + json.getJSONObject("result").getString("token_encoded")
                        if (self.minimal) {
                            url = "$url?minimal=true"
                        }
                        self.wf_webview.loadUrl(url)
                    }
                }
            }
        })
    }

    /*
    * Ask the necessary permissions for running the workflow
    *
    * @param applicationContext is the current aplication context
    * @param activity is the activity where the webview is running
    * @param steps is the types of steps included in the workflow
    * */
    @RequiresApi(Build.VERSION_CODES.M)
    internal fun askPermissions(
        activity: Activity, steps: ArrayList<String>
    ) {
        Log.d("Permissions", "onpermission request")
        if (ContextCompat.checkSelfPermission(
                activity.applicationContext,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                activity.applicationContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                activity.applicationContext,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("---", "PERMISSION REQUEST GRANTED")
        } else {
            Log.d("---", "ELSE")
            var permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)

            if (steps.contains("auth") ||
                steps.contains("enroll_full") ||
                steps.contains("liveness") ||
                steps.contains("enroll_selfie")
            ) permissions += android.Manifest.permission.CAMERA

            if (steps.contains("video_sign"))
                permissions += android.Manifest.permission.RECORD_AUDIO

            activity.requestPermissions(permissions, Companion.PERMISSIONS_REQUEST_CODE)
        }
    }
}

/*
* interface to implements for JavaScript Bridge execution on workflows web
* */
interface WorkflowCommsInterface {
    @JavascriptInterface
    fun onStepCompletion (message: String)

    fun onWorkflowCompletion (message: String)
}