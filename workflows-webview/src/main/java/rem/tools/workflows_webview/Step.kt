package rem.tools.workflows_webview

import com.fasterxml.jackson.databind.JsonNode

data class Step(
    val created_at: String,
    val error: JsonNode?,
    val metadata: JsonNode?,
    val payload: JsonNode?,
    val status: String,
    val step: String,
    val updated_at: String,
    val uuid: String,
    val workflow_uuid: String
)