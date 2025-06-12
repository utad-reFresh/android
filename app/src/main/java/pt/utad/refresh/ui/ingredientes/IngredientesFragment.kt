package pt.utad.refresh.ui.ingredientes

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
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import pt.utad.refresh.R
import pt.utad.refresh.databinding.FragmentIngredientesBinding
import pt.utad.refresh.databinding.ItemTransformBinding

class IngredientesFragment : Fragment() {

    private var _binding: FragmentIngredientesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ingredientesViewModel = ViewModelProvider(this).get(IngredientesViewModel::class.java)
        _binding = FragmentIngredientesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val recyclerView = binding.recyclerviewTransform
        val adapter = TransformAdapter()
        recyclerView.adapter = adapter
        ingredientesViewModel.texts.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.titleIngredients?.setOnClickListener {
            mostrarDialogAdicionar()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun mostrarDialogAdicionar() {
        val dialog = AlertDialog.Builder(requireContext(), R.style.FullScreenDialog)
            .create()

        val dialogView = layoutInflater.inflate(R.layout.dialog_adicionar_ingrediente, null)

        // Inicializar views
        val ingredientImage = dialogView.findViewById<CircleImageView>(R.id.ingredient_image)
        val txtIngrediente = dialogView.findViewById<TextView>(R.id.txtIngrediente)
        val edtQuantidade = dialogView.findViewById<TextInputEditText>(R.id.edtQuantidade)
        val edtValidade = dialogView.findViewById<TextInputEditText>(R.id.edtValidade)
        val btnSalvar = dialogView.findViewById<MaterialButton>(R.id.save_button)

        // Configurar campo de validade como date picker
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

        // Permitir selecionar imagem ao clicar
        ingredientImage.setOnClickListener {
            // Implementar lógica para selecionar imagem
            // Por exemplo, abrir galeria ou câmera
        }

        // Configurar botão de salvar
        btnSalvar.setOnClickListener {
            val ingrediente = txtIngrediente.text.toString()
            val quantidade = edtQuantidade.text.toString()
            val validade = edtValidade.text.toString()

            if (quantidade.isNotEmpty() && validade.isNotEmpty()) {
                // Implementar lógica para salvar os dados
                dialog.dismiss()
            } else {
                Snackbar.make(dialogView, "Preencha todos os campos", Snackbar.LENGTH_SHORT).show()
            }
        }

        // Configurar dialog
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

        fun showDetailsDialog(text: String, drawable: Int) {
            val dialogView = LayoutInflater.from(itemView.context).inflate(R.layout.dialog_details, null)

            val avatarView = dialogView.findViewById<ImageView>(R.id.dialog_avatar)
            val titleView = dialogView.findViewById<TextView>(R.id.dialog_title)
            val descriptionView = dialogView.findViewById<TextView>(R.id.dialog_description)

            avatarView.setImageResource(drawable)
            titleView.text = text
            descriptionView.text = "Descrição do ingrediente"

            AlertDialog.Builder(itemView.context)
                .setView(dialogView)
                .setPositiveButton("Fechar") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    class TransformAdapter : ListAdapter<String, TransformViewHolder>(
        object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem
        }
    ) {
        private val drawables = listOf(
            R.drawable.avatar_1,
            R.drawable.avatar_2,
            R.drawable.avatar_3,
            R.drawable.avatar_4,
            R.drawable.avatar_5,
            R.drawable.avatar_6,
            R.drawable.avatar_7,
            R.drawable.avatar_8,
            R.drawable.avatar_9,
            R.drawable.avatar_10,
            R.drawable.avatar_11,
            R.drawable.avatar_12,
            R.drawable.avatar_13,
            R.drawable.avatar_14,
            R.drawable.avatar_15,
            R.drawable.avatar_16
        )

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransformViewHolder {
            val binding = ItemTransformBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return TransformViewHolder(binding)
        }

        override fun onBindViewHolder(holder: TransformViewHolder, position: Int) {
            holder.textView?.text = getItem(position)
            holder.imageView?.let { imageView ->
                ResourcesCompat.getDrawable(imageView.resources, drawables[position], null)?.let { drawable ->
                    imageView.setImageDrawable(drawable)
                }
            }

            holder.binding.buttonDetails?.setOnClickListener {
                holder.showDetailsDialog(getItem(position), drawables[position])
            }
        }
    }
}