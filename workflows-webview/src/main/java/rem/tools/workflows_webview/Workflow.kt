package rem.tools.workflows_webview

import com.fasterxml.jackson.databind.JsonNode

data class Workflow(
    val callback_at: String?,
    val callback_url: String?,
    val created_at: String?,
    val error: JsonNode?,
    val expires_in: Int,
    val external_reference_id: String?,
    val finished_at: String?,
    val metadata: JsonNode?,
    val people_uuid: String?,
    val status: String,
    val steps: List<Step>,
    val updated_at: String,
    val uuid: String
)