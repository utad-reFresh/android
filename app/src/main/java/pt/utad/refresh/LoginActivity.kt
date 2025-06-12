package pt.utad.refresh

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import pt.utad.refresh.databinding.ActivityLoginBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnLogin.setOnClickListener {
            if (validateInputs()) {
                performLogin()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val email = binding.tilEmail.editText?.text.toString()
        val password = binding.tilPassword.editText?.text.toString()
        var isValid = true

        binding.tilEmail.error = null
        binding.tilPassword.error = null

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Email inválido"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Senha é obrigatória"
            isValid = false
        }

        return isValid
    }

    private fun performLogin() {
        lifecycleScope.launch {
            try {
                val request = LoginRequest(
                    email = binding.tilEmail.editText?.text.toString(),
                    password = binding.tilPassword.editText?.text.toString()
                )

                val response = ApiClient.apiService.login(request)

                if (response.code() == 200) {
                    response.body()?.let { loginResponse ->
                        sessionManager.saveAuthToken(loginResponse.token)
                        Toast.makeText(
                            this@LoginActivity,
                            "Login realizado com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Email ou senha incorretos",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "Erro de conexão: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}