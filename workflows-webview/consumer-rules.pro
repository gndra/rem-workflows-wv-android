-keepclassmembers class rem.tools.workflows_webview.WorkflowsWebview$WorkflowsJavascript{
    public *;
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface