package com.lexnicholls.lovecounter.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class CinemaSearchResponse(
    @SerializedName("results") val results: List<NetworkMeldMovieDto>
)

data class NetworkMeldMovieDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val overview: String,
    @SerializedName("image_url") val posterUrl: String?,
    @SerializedName("year") val releaseYear: String,
    @SerializedName("vote_average") val rating: Double,
    @SerializedName("media_type") val mediaType: String?,
    @SerializedName("platforms") val platforms: List<NetworkMeldPlatformDto>?,
    @SerializedName("runtime") val duration: Int?,
    @SerializedName("number_of_episodes") val episodeCountLong: Int?,
    @SerializedName("episodes") val episodeCountShort: Int?,
    @SerializedName("number_of_seasons") val seasonCountLong: Int?,
    @SerializedName("seasons") val seasonCountShort: Int?
) {
    val finalEpisodeCount: Int? get() = episodeCountLong ?: episodeCountShort
    val finalSeasonCount: Int? get() = seasonCountLong ?: seasonCountShort
}

data class NetworkMeldPlatformDto(
    @SerializedName("name") val name: String,
    @SerializedName("logo_url") val logoUrl: String,
    @SerializedName("url") val url: String?,
    @SerializedName("link") val link: String?,
    @SerializedName("web_url") val webUrl: String?
)

interface CinemaApiService {
    @GET("trending")
    suspend fun fetchTrending(
        @Query("region") region: String,
        @Query("lang") lang: String
    ): CinemaSearchResponse

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String? = null,
        @Query("region") region: String,
        @Query("lang") lang: String
    ): CinemaSearchResponse

    @GET("detail/{id}")
    suspend fun getMediaDetail(
        @Path("id") id: String,
        @Query("type") type: String,
        @Query("region") region: String,
        @Query("lang") lang: String
    ): NetworkMeldMovieDto

    @GET("detail/{id}")
    suspend fun getMediaDetailRaw(
        @Path("id") id: String,
        @Query("type") type: String,
        @Query("region") region: String,
        @Query("lang") lang: String
    ): okhttp3.ResponseBody
}
