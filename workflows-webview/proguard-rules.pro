# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep public class rem.tools.workflows_webview.WorkflowsWebview {
    *;
}
-keep public class rem.tools.workflows_webview.WorkflowsWebview$Companion {
    *;
}
-keep public class rem.tools.workflows_webview.WorkflowsJavascriptInterface {
    *;
}
-keep public class rem.tools.workflows_webview.Step {
    *;
}
-keep public class rem.tools.workflows_webview.Workflow {
    *;
}
-keep public class rem.tools.workflows_webview.WorkflowError{
    *;
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
-keepattributes *Annotation*
