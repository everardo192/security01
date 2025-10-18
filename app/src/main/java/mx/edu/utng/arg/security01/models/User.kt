package mx.edu.utng.arg.security01.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String,
    val email: String,
    val name: String,
    val token: String? = null,
    val refreshToken: String? = null,
    val profileImage: String? = null,
    val lastLogin: String? = null
) : Parcelable