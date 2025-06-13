package pt.utad.refresh.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import pt.utad.refresh.databinding.FragmentScannerBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.core.ExperimentalGetImage
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pt.utad.refresh.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import pt.utad.refresh.ApiClient
import pt.utad.refresh.ApiClient.apiService
import pt.utad.refresh.IngredientDto
import pt.utad.refresh.ui.ingredientes.IngredientSearchAdapter

class ScannerFragment : Fragment() {
    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var isHandlingResult = false
    private var camera: Camera? = null
    private var flashEnabled = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        return binding.root
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy: ImageProxy ->
                        if (isHandlingResult) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            val scanner = BarcodeScanning.getClient()
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    if (barcodes.isNotEmpty()) {
                                        val code = barcodes.first().rawValue
                                        if (!code.isNullOrEmpty()) {
                                            isHandlingResult = true
                                            cameraProvider?.unbindAll()
                                            fetchProduct(code) { name, brand, imageUrl, genericName, broadCategory ->
                                                if (name != null) {
                                                    showProductDialog(name, brand, imageUrl, genericName, broadCategory, code) {
                                                        isHandlingResult = false
                                                        startCamera()
                                                    }
                                                } else {
                                                    Toast.makeText(
                                                        requireContext(),
                                                        "$code\nProduto não encontrado.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    isHandlingResult = false
                                                    startCamera()
                                                }
                                            }
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

                binding.btnFocus.setOnClickListener {
                    // Trigger auto-focus at center
                    val factory = binding.viewFinder.meteringPointFactory
                    val point = factory.createPoint(
                        binding.viewFinder.width / 2f,
                        binding.viewFinder.height / 2f
                    )
                    val action = FocusMeteringAction.Builder(point).build()
                    camera?.cameraControl?.startFocusAndMetering(action)
                }

                binding.btnFlash.setOnClickListener {
                    flashEnabled = !flashEnabled
                    camera?.cameraControl?.enableTorch(flashEnabled)
                    binding.btnFlash.setImageResource(
                        if (flashEnabled) R.drawable.ic_flash_on else R.drawable.ic_flash_off
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun fetchProduct(
        barcode: String,
        onResult: (String?, String?, String?, String?, String?) -> Unit
    ) {
        // Show blur and throbber
        binding.blurOverlay.visibility = View.VISIBLE
        binding.loadingBar.visibility = View.VISIBLE

        if (android.os.Build.VERSION.SDK_INT >= 31) {
            binding.viewFinder.setRenderEffect(
                android.graphics.RenderEffect.createBlurEffect(20f, 20f, android.graphics.Shader.TileMode.CLAMP)
            )
        }

        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://world.openfoodfacts.org/api/v0/product/$barcode.json")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            withContext(Dispatchers.Main) {
                binding.blurOverlay.visibility = View.GONE
                binding.loadingBar.visibility = View.GONE
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    binding.viewFinder.setRenderEffect(null)
                }
                if (response.isSuccessful && body != null) {
                    val json = JSONObject(body)
                    val product = json.optJSONObject("product")
                    val name = product?.optString("product_name")
                    val brand = product?.optString("brands")
                    val imageUrl = product?.optString("image_front_url")
                    val genericName = product?.optString("generic_name")
                    val categoriesTags = product?.optJSONArray("categories_tags")
                    val broadCategory = if (categoriesTags != null && categoriesTags.length() > 0)
                        categoriesTags.getString(categoriesTags.length() - 1)
                            .removePrefix("en:") else null
                    onResult(name, brand, imageUrl, genericName, broadCategory)
                } else {
                    onResult(null, null, null, null, null)
                }
            }
        }
    }

    private fun showProductDialog(
        name: String?,
        brand: String?,
        imageUrl: String?,
        genericName: String?,
        broadCategory: String?,
        barcode: String, // add this
        onDismiss: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_product_details, null)

        dialogView.findViewById<TextInputEditText>(R.id.productName).setText("$name")
        dialogView.findViewById<TextInputEditText>(R.id.productBrand).setText("$brand")
        // Removed the productType/category field

        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(imageUrl)
                .into(dialogView.findViewById(R.id.productImage))
        }

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<MaterialButton>(R.id.btnConfirm).setOnClickListener {
            dialog.dismiss()
            fetchIngredientsByBarcode(barcode) { ingredients ->
                // showIngredientsSearch(ingredients)
                mostrarDialogPesquisaComResultadosIniciais(ingredients)
            }
            // showIngredientDetailsDialog(name, brand, imageUrl)
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
            startCamera()
            onDismiss()
        }

        dialog.show()
    }

    private fun mostrarDialogAdicionar(selectedIngredient: IngredientDto) {
        val dialog = AlertDialog.Builder(requireContext(), R.style.FullScreenDialog).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_adicionar_ingrediente, null)

        val ingredientImage = dialogView.findViewById<ImageView>(R.id.ingredient_image)
        val txtIngrediente = dialogView.findViewById<TextView>(R.id.txtIngrediente)
        val edtQuantidade = dialogView.findViewById<TextInputEditText>(R.id.edtQuantidade)
        val edtValidade = dialogView.findViewById<TextInputEditText>(R.id.edtValidade)
        val btnSalvar = dialogView.findViewById<MaterialButton>(R.id.save_button)
        val backText = dialogView.findViewById<TextView>(R.id.back_text)

        dialog.setOnDismissListener {
            isHandlingResult = false
            startCamera()
        }

        Glide.with(ingredientImage.context)
            .load(selectedIngredient.imageUrl)
            .placeholder(R.drawable.egg_40px)
            .into(ingredientImage)
        txtIngrediente.text = selectedIngredient.name

        edtValidade.setOnClickListener {
            val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecione a data de validade")
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                edtValidade.setText(dateFormatter.format(selection))
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }

        backText.setOnClickListener {
            dialog.dismiss()
            // Optionally, reopen the ingredient search dialog
            // mostrarDialogPesquisaComResultadosIniciais(emptyList())
        }

        btnSalvar.setOnClickListener {
            val quantidade = edtQuantidade.text.toString()
            val validade = edtValidade.text.toString()
            if (quantidade.isNotEmpty()) {
                val isoExpiration: String? = if (validade.isNotEmpty()) {
                    try {
                        val inputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        val date = inputFormat.parse(validade)
                        val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                        isoFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        isoFormat.format(date!!)
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
                lifecycleScope.launch {
                    val request = pt.utad.refresh.UpdateIngredientRequest(
                        quantity = quantidade.toInt(),
                        isFavorite = selectedIngredient.isFavorite,
                        expirationDate = isoExpiration
                    )
                    val response = pt.utad.refresh.ApiClient.apiService.addOrUpdateIngredient(selectedIngredient.id, request)
                    if (response.isSuccessful) {
                        dialog.dismiss()
                        Toast.makeText(requireContext(), "Ingrediente adicionado!", Toast.LENGTH_SHORT).show()
                        isHandlingResult = false
                        startCamera()
                    } else {
                        Snackbar.make(dialogView, "Erro ao adicionar", Snackbar.LENGTH_SHORT).show()
                    }
                }
            } else {
                Snackbar.make(dialogView, "Indique a quantidade", Snackbar.LENGTH_SHORT).show()
            }
        }

        dialog.setView(dialogView)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
    }

    private fun mostrarDialogPesquisaComResultadosIniciais(initialIngredients: List<IngredientDto>) {
        val dialog = AlertDialog.Builder(requireContext(), R.style.FullScreenDialog).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_pesquisa, null)
        val edtPesquisa = dialogView.findViewById<TextInputEditText>(R.id.edtPesquisa)
        val nextButton = dialogView.findViewById<MaterialButton>(R.id.next_button)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewResults)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        dialog.setOnDismissListener {
            isHandlingResult = false
            startCamera()
        }

        val adapter = IngredientSearchAdapter { selectedIngredient ->
            dialog.dismiss()
            mostrarDialogAdicionar(selectedIngredient)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Show initial ingredients
        adapter.submitList(initialIngredients)
        recyclerView.visibility = if (initialIngredients.isNotEmpty()) View.VISIBLE else View.GONE

        edtPesquisa.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH) {
                val imm = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                nextButton.performClick()
                true
            } else {
                false
            }
        }

        nextButton.setOnClickListener {
            val pesquisa = edtPesquisa.text.toString()
            if (pesquisa.isNotEmpty()) {
                lifecycleScope.launch {
                    val response = ApiClient.apiService.searchIngredients(pesquisa)
                    if (response.isSuccessful) {
                        val results = response.body() ?: emptyList()
                        val dtoList = results.map { response ->
                            IngredientDto(
                                id = response.id,
                                name = response.name,
                                imageUrl = response.imageUrl,
                                quantity = response.quantity,
                                isFavorite = response.isFavorite,
                                expirationDate = response.expirationDate ?: ""
                            )
                        }
                        adapter.submitList(dtoList)
                        recyclerView.visibility = if (dtoList.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                }
            } else {
                Snackbar.make(dialogView, "Insira o nome do ingrediente, em inglês.", Snackbar.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            isHandlingResult = false
            startCamera()
        }

        dialog.setView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun fetchIngredientsByBarcode(barcode: String, onResult: (List<IngredientDto>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Use your ApiService instance here (e.g., via dependency injection or singleton)
                val response = apiService.getProductByBarcode(barcode)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val product = response.body()
                        onResult(product?.ingredients ?: emptyList())
                    } else {
                        onResult(emptyList())
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(emptyList()) }
            }
        }
    }

    private fun showIngredientDetailsDialog(
        name: String?,
        brand: String?,
        imageUrl: String?,
        genericName: String?,
        broadCategory: String?,
        code: String?
    ) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_adicionar_ingrediente, null)

        dialogView.findViewById<TextView>(R.id.txtIngrediente).text = name ?: "Produto"

        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(imageUrl)
                .into(dialogView.findViewById(R.id.ingredient_image))
        }

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<MaterialButton>(R.id.save_button).setOnClickListener {
            val quantidade = dialogView.findViewById<TextInputEditText>(R.id.edtQuantidade).text.toString()
            val validade = dialogView.findViewById<TextInputEditText>(R.id.edtValidade).text.toString()

            dialog.dismiss()
            startCamera() // Reinicia a câmera apenas após salvar
        }

        dialogView.findViewById<TextView>(R.id.back_text).setOnClickListener {
            dialog.dismiss()
            showProductDialog(name, brand, imageUrl, genericName, broadCategory, code.toString()) {
                isHandlingResult = false
                startCamera()
            }

        }

        dialog.show()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}