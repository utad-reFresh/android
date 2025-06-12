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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import pt.utad.refresh.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

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
                                                    showProductDialog(name, brand, imageUrl, genericName, broadCategory) {
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
        onDismiss: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_product_details, null)

        dialogView.findViewById<TextInputEditText>(R.id.productName).setText("$name")
        dialogView.findViewById<TextInputEditText>(R.id.productBrand).setText("$brand")
        dialogView.findViewById<TextInputEditText>(R.id.productType).setText(
            genericName ?: broadCategory ?: "Desconhecido"
        )

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
            showIngredientDetailsDialog(name, brand, imageUrl)
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
            startCamera() // Reinicia a câmera apenas quando cancelar
            onDismiss()
        }

        dialog.show()
    }

    private fun showIngredientDetailsDialog(name: String?, brand: String?, imageUrl: String?) {
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
            showProductDialog(name, brand, imageUrl, null, null) {}
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