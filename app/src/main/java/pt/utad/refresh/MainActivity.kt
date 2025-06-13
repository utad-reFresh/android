package pt.utad.refresh

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.utad.refresh.databinding.ActivityMainBinding
import android.app.AlertDialog
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.withTimeout
import java.net.URL


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private fun checkAppVersionAndContinue() {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://refresh.jestev.es/apks/version.txt")
                val remoteHash = withTimeout(5000) { url.readText().trim() }
                val localHash = BuildConfig.GIT_HASH.trim()
                // Log the hashes for debugging
                println("Remote Hash: $remoteHash")
                println("Local Hash: $localHash")
                withContext(Dispatchers.Main) {
                    if (remoteHash.isNotEmpty() && remoteHash != localHash) {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Atualização disponível")
                            .setMessage("Existe uma nova versão do reFresh disponível. Deseja atualizar agora?")
                            .setPositiveButton("Update") { _, _ ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://refresh.jestev.es/apks/latest.apk"))
                                startActivity(intent)
                            }
                            .setNegativeButton("Continue") { dialog, _ ->
                                dialog.dismiss()
                                continueAppInit()
                            }
                            .setCancelable(false)
                            .show()
                    } else {
                        continueAppInit()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {

                    if (e is java.net.SocketTimeoutException) {
                        Toast.makeText(
                            this@MainActivity,
                            "Não foi possível verificar atualizações: Tempo limite excedido",
                            Toast.LENGTH_LONG
                        ).show()
                    } else if (e is java.net.UnknownHostException) {
                        Toast.makeText(
                            this@MainActivity,
                            "Não foi possível verificar atualizações: Sem conexão com a internet",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        e.printStackTrace()
                    }

                    Toast.makeText(
                        this@MainActivity,
                        "Não foi possível verificar atualizações:\n${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    continueAppInit()
                }
            }
        }

    }

    private fun continueAppInit() {
        val sessionManager = SessionManager(this)

        val token = sessionManager.getAuthToken()

        ApiClient.init(sessionManager)

        if (token.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // se tiver token, verifica se é válido com o /me

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getProfile()
                if (!response.isSuccessful && response.code() == 401) {
                    withContext(Dispatchers.Main) {
                        sessionManager.clearSession()
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    sessionManager.clearSession()
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment?)!!
        val navController = navHostFragment.navController

        binding.navView?.let {
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_transform, R.id.nav_scanner, R.id.nav_perfil, R.id.nav_settings
                ),
                binding.drawerLayout
            )
            it.setupWithNavController(navController)
        }

        binding.appBarMain.contentMain.bottomNavView?.let {
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_transform, R.id.nav_scanner, R.id.nav_perfil
                )
            )
            it.setupWithNavController(navController)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAppVersionAndContinue()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        val navView: NavigationView? = findViewById(R.id.nav_view)
        if (navView == null) {
            menuInflater.inflate(R.menu.overflow, menu)
        }
        return result
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_settings -> {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.nav_settings)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}