import com.example.terraconnection.api.LoginRequest
import com.example.terraconnection.api.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("login") // Ensure this matches your API's login endpoint
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}
