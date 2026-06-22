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
            apiService.getMediaDetail(movieId, type, region, lang).toDomain()
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
            platforms = this.platforms?.map { it.toDomain() } ?: emptyList()
        )
    }

    private fun NetworkMeldPlatformDto.toDomain(): MeldPlatform {
        return MeldPlatform(
            name = this.name,
            logoUrl = this.logoUrl
        )
    }
}
