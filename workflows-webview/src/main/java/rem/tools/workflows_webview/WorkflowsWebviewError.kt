package rem.tools.workflows_webview

enum class WorkflowError {
    INTERNAL_ERROR,
    PERMISSIONS_DENIED,
    REQUEST_ERROR,
    WORKFLOW_NOT_FOUND,
    REQUEST_INTERNAL_ERROR,
    CANNOT_USE_WORKFLOW
}

class WorkflowsWebviewError (
    override val message: String?,
    val error: WorkflowError
) : Exception(message) {

    override fun toString(): String {
        return "[$error]: $message"
    }
}
