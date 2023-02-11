-keepclassmembers class rem.tools.workflows_webview.WorkflowsWebview{
    *;
}
-keepclassmembers class rem.tools.workflows_webview.WorkflowsJavascriptInterface{
    *;
}
-keepclassmembers class rem.tools.workflows_webview.Step{
    *;
}
-keepclassmembers class rem.tools.workflows_webview.Workflow{
    *;
}
-keepclassmembers class rem.tools.workflows_webview.WorkflowError{
    *;
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
-keepattributes *Annotation*
