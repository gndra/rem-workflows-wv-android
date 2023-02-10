package rem.tools.workflows_webview

import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
//import com.google.protobuf.util.JsonFormat
import okhttp3.*
import org.json.JSONObject
//import rem.tools.workflows.Step
//import rem.tools.workflows.Workflow
import java.io.IOException
import rem.tools.workflows_webview.Workflow as WorkflowData
import rem.tools.workflows_webview.Step as StepData

const val PERMISSIONS_REQUEST_CODE = 101010

/**
 * interface to implements for JavaScript Bridge execution on workflows web
 * */
interface WorkflowsJavascriptInterface {
    @JavascriptInterface
    fun onStepCompletion (message: String)

    @JavascriptInterface
    fun onWorkflowCompletion (message: String)
}

/**
 * Configura un `WebView` para poder inicializar de manera adecuada un proceso web de _Workflows_.
 *
 * @author rem.tools
 *
 * @property baseUrl URL base de rem.tools
 * @property apiKey API Key de rem.tools
 * @constructor Crea una instancia para poder inicializar un flujo dentro de un _WebView_
 * */
class WorkflowsWebview(
    private var baseUrl: String? = "https://api.rem.tools",
    private var apiKey: String
) {
    private val client = OkHttpClient()

    /**
     * Callback de eventos del _Step_ durante la ejecucion del _Workflow_
     */
    var onStepEvent: ((step: StepData) -> Unit)? = null

    /**
     * Callback de eventos del _Workflow_ durante la ejecucion del _Workflow_
     */
    var onWorkflowEvent: ((workflow: WorkflowData) -> Unit)? = null

    companion object {
        /**
         * Revisa los permisos requeridos en el _Workflow_
         * @param requestCode `requestCode` de `onRequestPermissionsResult`
         * @param grantResults Lista de resultados `onRequestPermissionsResult`
         * @throws WorkflowsWebviewError Error en caso de no obtener permisos
         * */
        fun onPermissionsResults(requestCode: Int, grantResults: IntArray) {
            if (requestCode == PERMISSIONS_REQUEST_CODE) {
                // If request is cancelled, the result arrays are empty.
                val isPermissionDenied = grantResults.contains(PackageManager.PERMISSION_DENIED)

                if (grantResults.isEmpty() && isPermissionDenied) {
                    throw(WorkflowsWebviewError("Los permisos fueron denegados por el usuario", WorkflowError.PERMISSIONS_DENIED))
                }

                return
            }
        }
    }

    /**
    * Valida e inicializa el workflow configurado
    *
    * @param workflowId Workflows UUID
    * @param webView Webview donde se ejecuta el workflow
    * @param activity Actividad en la que se ejecuta el webview
    * @param callback Funcion de tipo `callback` que se llama una vez haya concluido con exito o error,
    * la inicializacion de un _Workflow_
    * @param minimal Bandera para indicar si se desea retirar el navbar default del workflow
    * @throws WorkflowsWebviewError Error al ejecutar `start`
    * */
    fun start(
        workflowId: String,
        webView: WebView,
        activity: Activity,
        callback: (success: Boolean, error: WorkflowError?) -> Unit,
        minimal: Boolean = false
    ) {
        try {
            webView.clearCache(true)
            webView.clearFormData()
            webView.settings.javaScriptEnabled = true
            webView.settings.allowContentAccess = true
            webView.settings.mediaPlaybackRequiresUserGesture = false
            webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            } else {
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }

            if (Build.VERSION.SDK_INT >= 33) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, false);
                }
            } else {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                    WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_OFF)
                }
            }

            webView.addJavascriptInterface(object : WorkflowsJavascriptInterface {
                @JavascriptInterface
                override fun onStepCompletion(message: String) {
//                    val stepBuilder = Step.newBuilder()
//                    val stepData = JsonFormat.parser().ignoringUnknownFields().merge(message, stepBuilder)

                    val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    val step: StepData = mapper.readValue(message, StepData::class.java)
//                    this@WorkflowsWebview.onStepEvent?.invoke(stepBuilder.build())
                    this@WorkflowsWebview.onStepEvent?.invoke(step)
                }

                @JavascriptInterface
                override fun onWorkflowCompletion(message: String) {
//                    val workflowBuilder = Workflow.newBuilder()
//                    JsonFormat.parser().ignoringUnknownFields().merge(message, workflowBuilder)

                    val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

                    val workflow: WorkflowData = mapper.readValue(message, WorkflowData::class.java)

//                    this@WorkflowsWebview.onWorkflowEvent?.invoke(workflowBuilder.build())
                    this@WorkflowsWebview.onWorkflowEvent?.invoke(workflow)
                }
            }, "workflowsWebview")

            webView.webChromeClient = object : WebChromeClient() {
                // Grant permissions for cam
                override fun onPermissionRequest(request: PermissionRequest) {
                    request.grant(request.resources)
                }
            }

            webView.webViewClient = object : WebViewClient () {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: RenderProcessGoneDetail?
                ): Boolean {
                    if (detail != null) {
                        if (!detail.didCrash()) {
                            Log.e("MY_APP_TAG", ("System killed the WebView rendering process " +
                                    "to reclaim memory. Recreating..."))

                            webView.destroy()
                            return true // The app continues executing.
                        }
                    }
                    Log.e("MY_APP_TAG", "The WebView rendering process crashed!")
                    return false
                }
            }

            val uri = Uri.parse(this.baseUrl)
                .buildUpon()
                .appendEncodedPath("/workflows/$workflowId/create-token")
                .build()

            val createTokenRequest = Request.Builder()
                .url(uri.toString())
                .addHeader("Rem-Apikey", this.apiKey)
                .get()
                .build()

            client.newCall(createTokenRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback.invoke(false, WorkflowError.REQUEST_INTERNAL_ERROR)
                }

                @RequiresApi(Build.VERSION_CODES.M)
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val workflowTokenData = JSONObject(response.body!!.string())

                        if (workflowTokenData.getJSONObject("result")
                                .getJSONObject("workflow")
                                .getString("status") != "pristine") {
                            return callback.invoke(false, WorkflowError.CANNOT_USE_WORKFLOW)
                        } else {
                            Handler(Looper.getMainLooper()).post {
                                val steps = workflowTokenData
                                    .getJSONObject("result")
                                    .getJSONObject("workflow")
                                    .getJSONArray("steps")

                                var workflowSteps = arrayListOf<String>()

                                for (i in 0 until steps.length()) {
                                    workflowSteps.add(steps.getJSONObject(i).getString("step"))
                                }

                                askPermissionsForSteps(activity, workflowSteps)

                                var publicUrl = Uri.parse(workflowTokenData.getJSONObject("result")
                                    .getString("public_url"))
                                    .buildUpon()

                                if (minimal) {
                                    publicUrl = publicUrl.appendQueryParameter("minimal", "true")
                                }

                                webView.loadUrl(publicUrl.toString())
                                callback.invoke(true, null)
                            }
                        }
                    } else {
                        return if (response.code == 404) {
                            callback.invoke(false, WorkflowError.WORKFLOW_NOT_FOUND)
                        } else {
                            callback.invoke(false, WorkflowError.REQUEST_ERROR)
                        }
                    }
                }
            })
        } catch (error: Exception) {
            throw(WorkflowsWebviewError(error.message, WorkflowError.INTERNAL_ERROR))
        }
    }

    /**
    * Pide los permisos necesarios respecto a los _Steps_ configurados
    *
    * @param activity `Activity` donde se encuentra el `WebView`
    * @param steps is the types of steps included in the workflow
    * */
    @RequiresApi(Build.VERSION_CODES.M)
    internal fun askPermissionsForSteps(
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
                steps.contains("enroll_selfie") ||
                        steps.contains("video_sign")
            ) permissions += android.Manifest.permission.CAMERA

            if (steps.contains("video_sign"))
                permissions += android.Manifest.permission.RECORD_AUDIO

            activity.requestPermissions(permissions, PERMISSIONS_REQUEST_CODE)
        }
    }
}