package com.example.siagabencana.network

import com.example.siagabencana.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<AuthData>>

    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AuthData>>

    @POST("logout")
    suspend fun logout(): Response<ApiResponse<Any>>

    @GET("me")
    suspend fun getMe(): Response<ApiResponse<User>>

    @POST("profile/update")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<ApiResponse<User>>

    @Multipart
    @POST("profile/photo")
    suspend fun uploadProfilePhoto(
        @Part foto: MultipartBody.Part
    ): Response<ApiResponse<User>>

    @POST("reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<ApiResponse<Any>>

    @GET("kategori")
    suspend fun getKategori(): Response<ApiResponse<List<Kategori>>>

    @GET("relawan")
    suspend fun getRelawan(
        @Query("search") search: String? = null
    ): Response<ApiResponse<List<Relawan>>>

    @GET("laporan")
    suspend fun getLaporan(
        @Query("search") search: String? = null,
        @Query("status") status: String? = null,
        @Query("urgensi") urgensi: String? = null,
        @Query("kategori_id") kategoriId: Int? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10
    ): Response<ApiResponse<PaginatedData<Laporan>>>

    @GET("laporan/{id}")
    suspend fun getDetailLaporan(
        @Path("id") id: Int
    ): Response<ApiResponse<Laporan>>

    @Multipart
    @POST("laporan")
    suspend fun createLaporan(
        @Part("judul") judul: RequestBody,
        @Part("deskripsi") deskripsi: RequestBody,
        @Part("kategori_id") kategoriId: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("jumlah_terdampak") jumlahTerdampak: RequestBody,
        @Part foto: MultipartBody.Part? = null,
        @Part("sos") sos: RequestBody? = null
    ): Response<ApiResponse<Laporan>>

    @Multipart
    @POST("laporan/{id}")
    suspend fun updateLaporanMultipart(
        @Path("id") id: Int,
        @Part("_method") method: RequestBody,
        @Part("judul") judul: RequestBody,
        @Part("deskripsi") deskripsi: RequestBody,
        @Part("kategori_id") kategoriId: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("jumlah_terdampak") jumlahTerdampak: RequestBody,
        @Part foto: MultipartBody.Part? = null
    ): Response<ApiResponse<Laporan>>

    @DELETE("laporan/{id}")
    suspend fun deleteLaporan(
        @Path("id") id: Int
    ): Response<ApiResponse<Any>>

    @POST("laporan/bulk-delete")
    suspend fun bulkDeleteLaporan(
        @Body request: BulkDeleteRequest
    ): Response<ApiResponse<Map<String, Any>>>

    @POST("laporan/{id}/tugaskan")
    suspend fun tugaskanRelawan(
        @Path("id") id: Int,
        @Body request: TugaskanRequest
    ): Response<ApiResponse<Penugasan>>

    @PATCH("laporan/{id}/status")
    suspend fun updateStatusLaporan(
        @Path("id") id: Int,
        @Body request: UpdateStatusRequest
    ): Response<ApiResponse<Laporan>>

    @PATCH("laporan/{id}/urgensi")
    suspend fun updateUrgensiLaporan(
        @Path("id") id: Int,
        @Body request: UpdateUrgensiRequest
    ): Response<ApiResponse<Laporan>>

    @GET("penugasan")
    suspend fun getPenugasan(
        @Query("status_tugas") statusTugas: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10
    ): Response<ApiResponse<PaginatedData<Penugasan>>>

    @PATCH("penugasan/{id}/status")
    suspend fun updateStatusPenugasan(
        @Path("id") id: Int,
        @Body request: UpdatePenugasanStatusRequest
    ): Response<ApiResponse<Penugasan>>

    @Multipart
    @POST("penugasan/{id}/bukti")
    suspend fun uploadBuktiPenugasan(
        @Path("id") id: Int,
        @Part fotoBukti: MultipartBody.Part,
        @Part("catatan") catatan: RequestBody? = null
    ): Response<ApiResponse<Penugasan>>

    @GET("riwayat")
    suspend fun getRiwayat(
        @Query("laporan_id") laporanId: Int? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10
    ): Response<ApiResponse<PaginatedData<RiwayatStatus>>>

    @GET("dashboard")
    suspend fun getDashboard(): Response<ApiResponse<DashboardData>>

    @GET("peta")
    suspend fun getPeta(
        @Query("status") status: String? = null,
        @Query("urgensi") urgensi: String? = null,
        @Query("kategori_id") kategoriId: Int? = null
    ): Response<ApiResponse<List<MapPoint>>>
}
