package cn.xing.fileclouddisk.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("api.php/app-config")
    suspend fun getAppConfig(): Response<AppConfigResponse>

    @POST("api.php/send-code")
    suspend fun sendCode(@Body body: SendCodeRequest): Response<SimpleSuccess>

    @POST("api.php/register")
    suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>

    @POST("api.php/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("api.php/logout")
    suspend fun logout(): Response<SimpleSuccess>

    @GET("api.php/security-question")
    suspend fun getSecurityQuestion(@Query("username") username: String): Response<SecurityQuestionResponse>

    @POST("api.php/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): Response<SimpleSuccess>

    @POST("api.php/reset-password-email")
    suspend fun resetPasswordEmail(@Body body: ResetPasswordEmailRequest): Response<SimpleSuccess>

    @GET("api.php/files")
    suspend fun getFiles(@Query("username") username: String = ""): Response<FilesListResponse>

    @POST("api.php/upload")
    suspend fun upload(@Body body: UploadRequest): Response<UploadResponse>

    @GET("api.php/files/{id}")
    suspend fun getFileDetail(@Path("id") id: String): Response<FileItem>

    @DELETE("api.php/files/{id}")
    suspend fun deleteFile(@Path("id") id: String, @Query("username") username: String): Response<SimpleSuccess>

    @POST("api.php/files/{id}/rename")
    suspend fun renameFile(
        @Path("id") id: String,
        @Query("username") username: String,
        @Body body: RenameRequest
    ): Response<RenameResponse>

    @PATCH("api.php/files/{id}")
    suspend fun patchFile(@Path("id") id: String, @Body body: PatchFileRequest): Response<SimpleSuccess>

    @GET("api.php/trash")
    suspend fun getTrash(@Query("username") username: String): Response<TrashListResponse>

    @POST("api.php/trash/{id}/restore")
    suspend fun restoreTrash(@Path("id") id: String, @Query("username") username: String): Response<SimpleSuccess>

    @POST("api.php/trash/clear")
    suspend fun clearTrash(@Query("username") username: String): Response<SimpleSuccess>

    @GET("api.php/share")
    suspend fun getShare(@Query("token") token: String): Response<FilesListResponse>

    @POST("api.php/share-bundle")
    suspend fun createShareBundle(@Body body: ShareBundleCreateRequest): Response<ShareBundleCreateResponse>

    @GET("api.php/share-bundle")
    suspend fun getShareBundle(@Query("token") token: String): Response<ShareBundleGetResponse>

    @POST("api.php/share-verify")
    suspend fun verifySharePassword(@Body body: ShareVerifyRequest): Response<FilesListResponse>

    @POST("api.php/user/avatar")
    suspend fun uploadAvatar(@Body body: AvatarUploadRequest): Response<SimpleSuccess>

    @GET("api.php/user/info")
    suspend fun getUserInfo(@Query("username") username: String): Response<UserInfoResponse>
}
