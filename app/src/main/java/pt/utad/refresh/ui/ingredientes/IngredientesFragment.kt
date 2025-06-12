package pt.utad.refresh.ui.ingredientes
import android.R.attr.fragment
import android.content.Context
import androidx.fragment.app.FragmentActivity
import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Locale
import de.hdodenhof.circleimageview.CircleImageView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import pt.utad.refresh.ApiClient
import pt.utad.refresh.ApiService
import pt.utad.refresh.IngredientDto
import pt.utad.refresh.R
import pt.utad.refresh.UpdateIngredientRequest
import pt.utad.refresh.databinding.FragmentIngredientesBinding
import pt.utad.refresh.databinding.ItemTransformBinding
import pt.utad.refresh.ui.ingredientes.IngredientesFragment.TransformViewHolder.TransformAdapter
import pt.utad.refresh.ui.ingredientes.IngredientesViewModel.IngredientesViewModelFactory

class IngredientesFragment : Fragment() {

    private var _binding: FragmentIngredientesBinding? = null
    private val binding get() = _binding!!
    private lateinit var ingredientesViewModel: IngredientesViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val apiService = ApiClient.apiService
        val factory = IngredientesViewModelFactory(apiService)
        ingredientesViewModel = ViewModelProvider(this, factory)[IngredientesViewModel::class.java]
        _binding = FragmentIngredientesBinding.inflate(inflater, container, false)
        val root: View = binding.root


        val recyclerView = binding.recyclerviewTransform
        val adapter = TransformAdapter(apiService, this)
        recyclerView.adapter = adapter

        ingredientesViewModel.ingredients.observe(viewLifecycleOwner) { ingredientList ->
            // ingredientList is List<IngredientResponse>
            val dtoList = ingredientList.map { response ->
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


            binding.emptyMessage?.visibility = if (dtoList.isEmpty()) View.VISIBLE else View.GONE        }

        return root
    }

    override fun onResume() {
        super.onResume()
        ingredientesViewModel.fetchIngredients()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.titleIngredients?.setOnClickListener {
            mostrarDialogPesquisa() // Alterado de mostrarDialogAdicionar para mostrarDialogPesquisa
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun mostrarDialogPesquisa() {
        val dialog = AlertDialog.Builder(requireContext(), R.style.FullScreenDialog).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_pesquisa, null)
        val edtPesquisa = dialogView.findViewById<TextInputEditText>(R.id.edtPesquisa)
        val nextButton = dialogView.findViewById<MaterialButton>(R.id.next_button)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewResults)

        edtPesquisa.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Hide keyboard
                val imm = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                // Trigger search
                nextButton.performClick()
                true
            } else {
                false
            }
        }

        // Set up adapter (implement IngredientSearchAdapter)
        val adapter = IngredientSearchAdapter { selectedIngredient ->
            dialog.dismiss()
            mostrarDialogAdicionar(selectedIngredient)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

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

        dialog.setView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }


    private fun mostrarDialogAdicionar(selectedIngredient: IngredientDto) {
        val dialog = AlertDialog.Builder(requireContext(), R.style.FullScreenDialog).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_adicionar_ingrediente, null)

        val ingredientImage = dialogView.findViewById<CircleImageView>(R.id.ingredient_image)
        val txtIngrediente = dialogView.findViewById<TextView>(R.id.txtIngrediente)
        val edtQuantidade = dialogView.findViewById<TextInputEditText>(R.id.edtQuantidade)
        val edtValidade = dialogView.findViewById<TextInputEditText>(R.id.edtValidade)
        val btnSalvar = dialogView.findViewById<MaterialButton>(R.id.save_button)
        val backText = dialogView.findViewById<TextView>(R.id.back_text)

        Glide.with(ingredientImage.context)
            .load(selectedIngredient.imageUrl)
            .placeholder(R.drawable.egg_40px)
            .into(ingredientImage)
        txtIngrediente.text = selectedIngredient.name

        edtValidade.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecione a data de validade")
                .build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                edtValidade.setText(dateFormatter.format(selection))
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }

        backText.setOnClickListener {
            dialog.dismiss()
            mostrarDialogPesquisa()
        }

        btnSalvar.setOnClickListener {
            val quantidade = edtQuantidade.text.toString()
            val validade = edtValidade.text.toString()
            if (quantidade.isNotEmpty()) {
                val isoExpiration: String? = if (validade.isNotEmpty()) {
                    try {
                        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val date = inputFormat.parse(validade)
                        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                        isoFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        isoFormat.format(date!!)
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
                lifecycleScope.launch {
                    val request = UpdateIngredientRequest(
                        quantity = quantidade.toInt(),
                        isFavorite = selectedIngredient.isFavorite,
                        expirationDate = isoExpiration
                    )
                    val response = ApiClient.apiService.addOrUpdateIngredient(selectedIngredient.id, request)
                    if (response.isSuccessful) {
                        ingredientesViewModel.fetchIngredients()
                        dialog.dismiss()
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

    class TransformViewHolder(val binding: ItemTransformBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val imageView: ImageView? = binding.imageViewItemTransform
        val textView: TextView? = binding.textViewItemTransform

        fun showDetailsDialog(
            ingredient: IngredientDto,
            apiService: ApiService,
            fragment: Fragment
        ) {
            val dialog = AlertDialog.Builder(itemView.context, R.style.FullScreenDialog).create()
            val dialogView = LayoutInflater.from(itemView.context).inflate(R.layout.dialog_details, null)

            val avatarView = dialogView.findViewById<ImageView>(R.id.dialog_avatar)
            val titleView = dialogView.findViewById<TextView>(R.id.dialog_title)
            val quantidadeField = dialogView.findViewById<TextInputEditText>(R.id.edtQuantidade)
            val validadeField = dialogView.findViewById<TextInputEditText>(R.id.edtValidade)
            val deleteButton = dialogView.findViewById<MaterialButton>(R.id.button_delete)
            val saveButton = dialogView.findViewById<MaterialButton>(R.id.close_button)

            Glide.with(avatarView.context)
                .load(ingredient.imageUrl)
                .into(avatarView)
            titleView.text = ingredient.name
            quantidadeField.setText(ingredient.quantity.toString())

            validadeField.setOnClickListener {
                val datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Selecione a data de validade")
                    .build()
                datePicker.addOnPositiveButtonClickListener { selection ->
                    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    validadeField.setText(dateFormatter.format(selection))
                }
                datePicker.show((itemView.context as FragmentActivity).supportFragmentManager, "DATE_PICKER")
            }

            try {
                val isoFormatWithMs = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                isoFormatWithMs.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val isoFormatNoMs = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                isoFormatNoMs.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val date = try {
                    isoFormatWithMs.parse(ingredient.expirationDate)
                } catch (e: Exception) {
                    isoFormatNoMs.parse(ingredient.expirationDate)
                }
                val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                validadeField.setText(dateFormatter.format(date!!))
            } catch (e: Exception) {
                validadeField.setText(ingredient.expirationDate)
            }

            // Save (update) ingredient
            saveButton.setOnClickListener {
                val newQuantity = quantidadeField.text.toString().toIntOrNull() ?: ingredient.quantity
                val newExpirationStr = validadeField.text?.toString()?.trim() ?: ""
                val isoExpiration: String? = if (newExpirationStr.isEmpty()) {
                    null
                } else {
                    try {
                        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val date = inputFormat.parse(newExpirationStr)
                        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                        isoFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        isoFormat.format(date!!)
                    } catch (e: Exception) {
                        null
                    }
                }
                fragment.viewLifecycleOwner.lifecycleScope.launch {
                    val request = UpdateIngredientRequest(
                        quantity = newQuantity,
                        isFavorite = ingredient.isFavorite,
                        expirationDate = isoExpiration // will be null if field is empty
                    )
                    val response = apiService.addOrUpdateIngredient(ingredient.id, request)
                    if (response.isSuccessful) {
                        (fragment as? IngredientesFragment)?.let { frag ->
                            ViewModelProvider(frag)[IngredientesViewModel::class.java].fetchIngredients()
                        }
                        dialog.dismiss()
                    } else {
                        Snackbar.make(dialogView, "Erro ao guardar", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }

            // Delete ingredient
            deleteButton.setOnClickListener {
                fragment.viewLifecycleOwner.lifecycleScope.launch {
                    val response = apiService.deleteIngredient(ingredient.id)
                    if (response.isSuccessful) {
                        (fragment as? IngredientesFragment)?.let { frag ->
                            ViewModelProvider(frag)[IngredientesViewModel::class.java].fetchIngredients()
                        }
                        dialog.dismiss()
                    } else {
                        Snackbar.make(dialogView, "Erro ao apagar", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }

            dialog.setView(dialogView)
            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            dialog.show()
        }



        class TransformAdapter(
            private val apiService: ApiService,
            private val fragment: Fragment
        ) : ListAdapter<IngredientDto, TransformViewHolder>(
            object : DiffUtil.ItemCallback<IngredientDto>() {
                override fun areItemsTheSame(
                    oldItem: IngredientDto,
                    newItem: IngredientDto
                ): Boolean =
                    oldItem.id == newItem.id

                override fun areContentsTheSame(
                    oldItem: IngredientDto,
                    newItem: IngredientDto
                ): Boolean =
                    oldItem == newItem
            }
        ) {

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransformViewHolder {
                val binding = ItemTransformBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return TransformViewHolder(binding)
            }

            override fun onBindViewHolder(holder: TransformViewHolder, position: Int) {
                val ingredient = getItem(position)
                holder.textView?.text = ingredient.name
                holder.imageView?.let { imageView ->
                    Glide.with(imageView.context)
                        .load(ingredient.imageUrl)
                        .into(imageView)
                }
                holder.binding.buttonDetails?.setOnClickListener {
                    holder.showDetailsDialog(ingredient, apiService, fragment)
                }
            }
        }
    }
}