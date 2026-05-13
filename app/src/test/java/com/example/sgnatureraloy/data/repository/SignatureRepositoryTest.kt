package com.example.sgnatureraloy.data.repository

import com.example.sgnatureraloy.core.network.ApiService
import com.example.sgnatureraloy.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class SignatureRepositoryTest {

    private val fakeApiService = object : ApiService {
        override suspend fun portalLogin(request: AuthRequest): Response<PortalResponse> = TODO()
        override suspend fun requestOtp(request: EmailRequest): Response<PortalResponse> = TODO()
        
        override suspend fun getPendingSignatures(request: EmailRequest): Response<SignatureListResponse> {
            return Response.success(SignatureListResponse(
                error = false, msj = "", count = 1, status = 200,
                data = listOf(
                    SignatureProcess("REF1", "PROCESSING", "2026-05-01", null, null)
                )
            ))
        }

        override suspend fun getCreatedSignatures(request: EmailRequest): Response<SignatureListResponse> {
            return Response.success(SignatureListResponse(
                error = false, msj = "", count = 2, status = 200,
                data = listOf(
                    SignatureProcess("REF2", "COMPLETED", "2026-05-02", null, null),
                    SignatureProcess("REF3", "PROCESSING", "2026-05-03", null, null)
                )
            ))
        }
    }

    @Test
    fun `getSignatures should sort COMPLETED last and then by date descending`() = runBlocking {
        val repository = SignatureRepository(fakeApiService)
        val result = repository.getSignatures("test@example.com").first()

        assertEquals(3, result.size)
        // REF3 (PROCESSING, 2026-05-03) should be first
        assertEquals("REF3", result[0].referenceId)
        // REF1 (PROCESSING, 2026-05-01) should be second
        assertEquals("REF1", result[1].referenceId)
        // REF2 (COMPLETED) should be last
        assertEquals("REF2", result[2].referenceId)
    }
}
