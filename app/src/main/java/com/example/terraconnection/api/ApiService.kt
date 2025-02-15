import com.example.terraconnection.data.ClassSchedule
import com.example.terraconnection.data.LoginRequest
import com.example.terraconnection.data.LoginResponse
import com.example.terraconnection.data.User
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/auth/me")
    suspend fun getUsers(@Header("Authorization") token: String): Response<User>

    @GET("api/student/schedule")
    suspend fun getStudentSchedule(@Header("Authorization")token: String):Response<List<ClassSchedule>>
}
