package com.kameda.firebaseauth

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.kameda.firebaseauth.databinding.ActivityMainBinding
import com.kameda.firebaseauth.service.ForegroundIbeaconOutputServise
import okhttp3.*
import pub.devrel.easypermissions.EasyPermissions
import java.io.IOException


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var mAuth: FirebaseAuth? = null
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    private val PERMISSION_REQUEST_CODE = 1

    private val _uuid = MutableLiveData<String>("")
    val uuid: LiveData<String> = _uuid
    fun setUuid (input: String) {
        Log.d("LiveData set", input)
        val input2 = input
        Log.d("LiveData set", input2)
        _uuid.postValue(input2)
        Log.d("LiveData set", uuid.value.toString())
        Log.d("LiveData set", input2)
    }

//    var UuidStringBuilder=StringBuilder("e7d61ea3f8dd49c88f2ff2484c07ac1c")
    var UuidStringBuilder=StringBuilder()
//    var UseUuid = "e7d61ea3f8dd49c88f2ff2484c07ac1c"
//    var UuidStringBuilder=StringBuilder("e7d61ea3f8dd49c88f2ff2484c07ac08")
//    var UuidStringBuilder = StringBuilder()

//    var UseUuid = "e7d61ea3f8dd49c88f2ff2484c07ac08"

    var UseMajor = "1"
    var UseMinor = "128"

    val permissions = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
        arrayOf(
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE

        )
    }else{
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }

    override fun onStart() {
        super.onStart()
        val currentUser = mAuth!!.currentUser
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()

        mAuth!!.setLanguageCode("ja")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)



        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(this.binding.root)

        binding.googleSignInButton.setOnClickListener { signIn() }

        if (!EasyPermissions.hasPermissions(this, *permissions)) {
            // パーミッションが許可されていない時の処理
            EasyPermissions.requestPermissions(this, "パーミッションに関する説明", PERMISSION_REQUEST_CODE, *permissions)
        }

        //ibeaconの出力を開始する。
        binding.ibeeconStartButton.setOnClickListener {

            val intent = Intent(this, ForegroundIbeaconOutputServise::class.java)
            //値をintentした時に受け渡しをする用
            var UseUuid = UuidStringBuilder.toString()
            Log.d("直前",UseUuid.toString())
            Log.d("直前",uuid.value.toString())
            intent.putExtra("UUID",UseUuid)
            intent.putExtra("MAJOR",UseMajor)
            intent.putExtra("MINOR",UseMinor)

            //サービスの開始
            //パーミッションの確認をする。
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && EasyPermissions.hasPermissions(this, *permissions)) {
                startForegroundService(intent)
            }

        }

        binding.ibeaconStopButton.setOnClickListener {
            val targetIntent = Intent(this, ForegroundIbeaconOutputServise::class.java)
            stopService(targetIntent)
        }
    }




    private fun signIn() {
        val signInIntent: Intent = mGoogleSignInClient.getSignInIntent()
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken)

            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w("LOGIN", "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth!!.signInWithCredential(credential)
            .addOnCompleteListener(
                this
            ) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information

                    val user = mAuth!!.currentUser
                    val intent = Intent(this@MainActivity, MainActivity::class.java)
                    startActivity(intent)
                    val token = user!!.getIdToken(false).result.token

                    val client = OkHttpClient().newBuilder()
                        .build()
                    var urlStr = "https://go-staywatch.kajilab.tk/api/v1/signup"


                    val request = Request.Builder()
                        .url(urlStr)
                        .addHeader("Authorization", "Bearer ${token}")
                        .build()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onResponse(call: Call, response: Response) {
                            // Responseの読み出し
                            val responseBody = response.body?.string().orEmpty()
                            // 必要に応じてCallback
                            Log.d("ビーコン情報",responseBody)
                            var count =responseBody.indexOf("UUID")
                            count+=7
                            Log.d("testuuid",responseBody.substring(count,count+32))
                            println(responseBody)
                            UuidStringBuilder.append(responseBody.substring(count,count+32))
                            setUuid(responseBody.substring(count,count+32))
                            Log.d("testuuidBuilder",UuidStringBuilder.toString())
                        }
                        override fun onFailure(call: Call, e: IOException) {
                            Log.e("Error", e.toString())
                            // 必要に応じてCallback
                        }
                    })

                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("LOGIN", "signInWithCredential:failure", task.exception)
                    Toast.makeText(this@MainActivity, "Authentication failed.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

}




