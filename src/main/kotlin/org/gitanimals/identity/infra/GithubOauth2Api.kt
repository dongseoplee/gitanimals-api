package org.gitanimals.identity.infra

import com.fasterxml.jackson.annotation.JsonProperty
import org.gitanimals.identity.app.Oauth2Api
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class GithubOauth2Api(
    @Value("\${oauth.client.id.github}") private val clientId: String,
    @Value("\${oauth.client.secret.github}") private val clientSecret: String,
) : Oauth2Api {

    private val githubClient = RestClient.create("https://github.com")
    private val githubApiClient = RestClient.create("https://api.github.com")

    override fun getToken(temporaryToken: String): String {
        val tokenResponse = githubClient.post()
            .uri(
                "/login/oauth/access_token?client_id=$clientId" +
                        "&client_secret=$clientSecret" +
                        "&code=$temporaryToken"
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange { _, response ->
                response.bodyTo(TokenResponse::class.java)
                    ?: throw IllegalArgumentException(
                        "Cannot get token cause ${response.bodyTo(String::class.java)}"
                    )
            }

        return "${tokenResponse.tokenType} ${tokenResponse.accessToken}"
    }

    override fun getOauthUsername(token: String): String {
        val userResponse = githubApiClient.get()
            .uri("/user")
            .header(HttpHeaders.AUTHORIZATION, token)
            .accept(MediaType.APPLICATION_JSON)
            .exchange { _, response ->
                response.bodyTo(UserResponse::class.java)
                    ?: throw IllegalArgumentException(
                        "Cannot get userResponse cause ${response.bodyTo(String::class.java)}"
                    )
            }

        return userResponse.login
    }

    private data class TokenResponse(
        @JsonProperty("access_token")
        val accessToken: String,
        val scope: String,
        @JsonProperty("token_type")
        val tokenType: String,
    )

    private data class UserResponse(
        val login: String,
    )
}
