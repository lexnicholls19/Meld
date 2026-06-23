package com.lexnicholls.lovecounter.data.repository

import com.lexnicholls.lovecounter.data.remote.CinemaApiService
import com.lexnicholls.lovecounter.data.remote.NetworkMeldMovieDto
import com.lexnicholls.lovecounter.data.remote.NetworkMeldPlatformDto
import com.lexnicholls.lovecounter.domain.model.MeldMovie
import com.lexnicholls.lovecounter.domain.model.MeldPlatform
import com.lexnicholls.lovecounter.domain.repository.CinemaRepository
import javax.inject.Inject

class CinemaRepositoryImpl @Inject constructor(
    private val apiService: CinemaApiService
) : CinemaRepository {

    override suspend fun getTrendingMovies(region: String, lang: String): Result<List<MeldMovie>> {
        return runCatching {
            apiService.fetchTrending(region, lang).results.map { it.toDomain() }
        }
    }

    override suspend fun searchMovies(query: String, type: String?, region: String, lang: String): Result<List<MeldMovie>> {
        return runCatching {
            apiService.search(query, type, region, lang).results.map { it.toDomain() }
        }
    }

    override suspend fun getMovieDetail(movieId: String, type: String, region: String, lang: String): Result<MeldMovie> {
        return runCatching {
            val responseBody = apiService.getMediaDetailRaw(movieId, type, region, lang).string()
            android.util.Log.d("CinemaRepository", "RAW Response: $responseBody")
            
            val response = com.google.gson.Gson().fromJson(responseBody, com.lexnicholls.lovecounter.data.remote.NetworkMeldMovieDto::class.java)
            response.toDomain()
        }
    }

    private fun NetworkMeldMovieDto.toDomain(): MeldMovie {
        return MeldMovie(
            id = this.id,
            title = this.title,
            overview = this.overview,
            posterUrl = this.posterUrl,
            releaseYear = this.releaseYear,
            rating = this.rating,
            mediaType = this.mediaType ?: "movie",
            platforms = this.platforms?.map { it.toDomain() } ?: emptyList(),
            duration = this.duration,
            episodeCount = this.finalEpisodeCount,
            seasonCount = this.finalSeasonCount
        )
    }

    private fun NetworkMeldPlatformDto.toDomain(): MeldPlatform {
        android.util.Log.d("CinemaRepository", "Mapping platform: $name, raw_url: $url, raw_link: $link, raw_web_url: $webUrl")
        return MeldPlatform(
            name = this.name,
            logoUrl = this.logoUrl,
            url = this.url ?: this.webUrl,
            deepLink = this.link
        )
    }
}
