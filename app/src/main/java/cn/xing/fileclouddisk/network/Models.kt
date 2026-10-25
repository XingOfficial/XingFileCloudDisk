package cn.xing.fileclouddisk.network

import com.google.gson.annotations.SerializedName

data class AppConfigResponse(
    val maxFileSize: Long = 0,
    val allowRegister: Boolean = true,
    val notices: List<String> = emptyList()
)

data class ApiErrorResponse(
    val error: String? = null,
    val message: String? = null
)

data class SendCodeRequest(val email: String, val purpose: String)
data class SimpleSuccess(val success: Boolean = false)

data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String,
    val code: String,
    val securityQuestion: String,
    val securityAnswer: String
)
data class RegisterResponse(val success: Boolean = false, val username: String = "")

data class LoginRequest(val username: String, val password: String)
data class LoginResponse(
    val success: Boolean = false,
    val username: String = "",
    val quota: Long = 0,
    val avatar: String = "",
    val token: String = "" 
)

data class SecurityQuestionResponse(val success: Boolean = false, val securityQuestion: String = "")

data class ResetPasswordRequest(
    val username: String,
    val securityAnswer: String,
    val newPassword: String
)

data class ResetPasswordEmailRequest(
    val email: String,
    val code: String,
    val newPassword: String
)

data class FileItem(
    val id: String = "",
    val filename: String = "",
    val ownerUsername: String = "",
    val size: Long = 0,
    val mimeType: String = "",
    val category: String = "",
    val isPublic: Boolean = true,
    val isExpired: Boolean = false,
    val shareToken: String? = null,
    val shareExpiresAt: Long = 0,
    val createdAt: String = "",
    val url: String = "",
    val content: String? = null
)

data class FilesListResponse(val items: List<FileItem> = emptyList(), val total: Int = 0)

data class UploadRequest(
    val ownerUsername: String,
    val filename: String,
    val content: String,
    val mimeType: String
)
data class UploadResponse(
    val id: String = "",
    val filename: String = "",
    val size: Long = 0,
    val url: String = ""
)

data class TrashItem(
    val id: String = "",
    val filename: String = "",
    val size: Long = 0,
    val deletedAt: String = "",
    val daysLeft: Int = 0
)
data class TrashListResponse(val items: List<TrashItem> = emptyList())

data class RenameRequest(val newName: String)
data class RenameResponse(val success: Boolean = false, val newFilename: String = "")

data class PatchFileData(
    val isPublic: Boolean? = null,
    val shareToken: String? = null,
    val shareExpiresAt: Long? = null
)
data class PatchFileRequest(val data: PatchFileData)

data class ShareBundleCreateRequest(
    val fileIds: List<String>,
    val username: String,
    val expiresHours: Int = 168
)
data class ShareBundleCreateResponse(
    val success: Boolean = false,
    val bundleToken: String = "",
    val expiresAt: Long = 0
)
data class ShareBundleGetResponse(
    val items: List<FileItem> = emptyList(),
    @SerializedName("createdBy") val createdBy: String = "",
    val createdAt: String = "",
    val expiresAt: String = ""
)

data class ShareVerifyRequest(
    val token: String,
    val password: String
)

data class AvatarUploadRequest(val username: String, val avatar: String)

data class UserInfoResponse(
    val username: String = "",
    val email: String = "",
    val quota: Long = 0,
    val avatar: String = "",
    val regTime: Long = 0
)
