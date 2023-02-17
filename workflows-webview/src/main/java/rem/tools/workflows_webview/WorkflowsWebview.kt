package rem.tools.workflows_webview

//import com.google.protobuf.util.JsonFormat
//import rem.tools.workflows.Step
//import rem.tools.workflows.Workflow

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.*
import android.webkit.WebSettings.LayoutAlgorithm
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.WebViewRenderProcess
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import rem.tools.workflows_webview.Step as StepData
import rem.tools.workflows_webview.Workflow as WorkflowData


const val PERMISSIONS_REQUEST_CODE = 101010

/**
 * Configura un `WebView` para poder inicializar de manera adecuada un proceso web de _Workflows_.
 *
 * @author rem.tools
 *
 * @property baseUrl URL base de rem.tools
 * @property apiKey API Key de rem.tools
 * @constructor Crea una instancia para poder inicializar un flujo dentro de un _WebView_
 * */
public class WorkflowsWebview(
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
    @SuppressLint("SetJavaScriptEnabled")
    fun start(
        workflowId: String,
        webView: WebView,
        activity: Activity,
        callback: (success: Boolean, error: WorkflowError?) -> Unit,
        minimal: Boolean = false
    ) {
        try {
            webView.setInitialScale(0)
            webView.isVerticalScrollBarEnabled = false
            val settings = webView.settings
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.layoutAlgorithm = LayoutAlgorithm.NORMAL
            settings.mediaPlaybackRequiresUserGesture = false
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.allowUniversalAccessFromFileURLs = true

            val databasePath =
                webView.context.applicationContext.getDir("database", Activity.MODE_PRIVATE).path
            settings.databaseEnabled = true

            settings.setGeolocationDatabasePath(databasePath)

            settings.setGeolocationEnabled(true)

            val intentFilter = IntentFilter()
            intentFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    settings.userAgentString
                }
            }
            webView.context.registerReceiver(receiver, intentFilter)

            if (Build.VERSION.SDK_INT >= 33) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, false);
                }
            } else {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                    WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_OFF)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                webView.webViewRenderProcessClient = object : WebViewRenderProcessClient() {
                    override fun onRenderProcessUnresponsive(
                        view: WebView,
                        renderer: android.webkit.WebViewRenderProcess?
                    ) {
                        renderer?.terminate()
                        view.reload()
                    }

                    override fun onRenderProcessResponsive(
                        view: WebView,
                        renderer: android.webkit.WebViewRenderProcess?
                    ) {
                        renderer?.terminate()
                        view.reload()
                    }
                }
            }

            webView.addJavascriptInterface(WorkflowsJavascriptInterface(this@WorkflowsWebview) , "workflowsWebview")

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