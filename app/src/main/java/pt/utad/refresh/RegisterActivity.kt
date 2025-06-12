package pt.utad.refresh

import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var tilName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var btnRegister: MaterialButton
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        tilName = findViewById(R.id.tilName)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)
    }

    private fun setupListeners() {
        btnRegister.setOnClickListener {
            if (validateInputs()) {
                performRegister()
            }
        }
        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateInputs(): Boolean {
        val name = tilName.editText?.text.toString()
        val email = tilEmail.editText?.text.toString()
        val password = tilPassword.editText?.text.toString()
        val confirmPassword = tilConfirmPassword.editText?.text.toString()

        var isValid = true

        // Limpa erros anteriores
        tilName.error = null
        tilEmail.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null

        if (name.isEmpty()) {
            tilName.error = "Nome é obrigatório"
            isValid = false
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Email inválido"
            isValid = false
        }

        if (password.length < 6) {
            tilPassword.error = "Senha deve ter pelo menos 6 caracteres"
            isValid = false
        }

        if (password != confirmPassword) {
            tilConfirmPassword.error = "Senhas não conferem"
            isValid = false
        }

        return isValid
    }

    private fun performRegister() {
        lifecycleScope.launch {
            try {
                val request = RegisterRequest(
                    displayName = tilName.editText?.text.toString(),
                    userName = tilName.editText?.text.toString().lowercase().replace(" ", ""),
                    email = tilEmail.editText?.text.toString(),
                    password = tilPassword.editText?.text.toString()
                )

                val response = ApiClient.apiService.register(request)

                if (response.code() == 200) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Registro realizado com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Erro no registro. Código: ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@RegisterActivity,
                    "Erro de conexão: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}