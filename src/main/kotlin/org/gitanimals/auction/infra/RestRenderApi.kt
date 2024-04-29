package org.gitanimals.auction.infra

import org.gitanimals.auction.app.RenderApi
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component("auction.RestRenderApi")
class RestRenderApi(
    @Qualifier("auction.renderRestClient") private val restClient: RestClient,
) : RenderApi {

    override fun getPersonaById(token: String, personaId: Long): RenderApi.PersonaResponse {
        return restClient.get()
            .uri("/personas/$personaId")
            .header(HttpHeaders.AUTHORIZATION, token)
            .exchange { _, response ->
                runCatching {
                    response.bodyTo(RenderApi.PersonaResponse::class.java)
                }.getOrElse {
                    require(!response.statusCode.is4xxClientError) {
                        "Cannot get persona by personaId \"$personaId\""
                    }

                    throw IllegalStateException(it)
                }
            }
    }

    override fun deletePersonaById(token: String, personaId: Long) {
        return restClient.delete()
            .uri("/internals/personas?persona-id=$personaId")
            .header(HttpHeaders.AUTHORIZATION, token)
            .exchange { _, response ->
                require(response.statusCode.is2xxSuccessful) { "Cannot delete persona by personaId \"$personaId\"" }
            }
    }

    override fun addPersona(
        token: String,
        idempotencyKey: String,
        personaId: Long,
        personaLevel: Int,
        personaType: String
    ) {
        return restClient.post()
            .uri("/internals/personas?idempotency-key=$idempotencyKey")
            .header(HttpHeaders.AUTHORIZATION, token)
            .body(
                mapOf(
                    "id" to personaId,
                    "name" to personaType,
                    "level" to personaLevel,
                )
            )
            .exchange { _, response ->
                require(response.statusCode.is2xxSuccessful) {
                    "Cannot add persona \"$personaId\", \"$personaLevel\", \"$personaType\" to user"
                }
            }
    }
}
