package pt.utad.refresh.ui.receitas

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import com.google.android.material.button.MaterialButton
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pt.utad.refresh.R
import pt.utad.refresh.databinding.FragmentReceitasBinding
import pt.utad.refresh.databinding.ItemReceitaBinding

class ReceitasFragment : Fragment() {
    private var _binding: FragmentReceitasBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val receitasViewModel = ViewModelProvider(this).get(ReceitasViewModel::class.java)
        _binding = FragmentReceitasBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val recyclerView = binding.recyclerviewReceitas
        val adapter = ReceitasAdapter()
        recyclerView.adapter = adapter
        receitasViewModel.receitas.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class ReceitasAdapter :
        ListAdapter<String, ReceitasViewHolder>(object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem
        }) {

        private val receitasImages = listOf(
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceitasViewHolder {
            val binding = ItemReceitaBinding.inflate(LayoutInflater.from(parent.context))
            return ReceitasViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ReceitasViewHolder, position: Int) {
            holder.textView.text = getItem(position)
            holder.imageView.setImageDrawable(
                ResourcesCompat.getDrawable(holder.imageView.resources, receitasImages[position], null)
            )

            holder.itemView.setOnClickListener {
                mostrarDialogReceita(holder.itemView.context, getItem(position), receitasImages[position])
            }
        }

        private fun mostrarDialogReceita(context: Context, nome: String, imagem: Int) {
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_receita)

            val window = dialog.window
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val receitaImage = dialog.findViewById<ImageView>(R.id.receita_image)
            val receitaNome = dialog.findViewById<TextView>(R.id.receita_nome)
            val receitaDetalhes = dialog.findViewById<TextView>(R.id.receita_detalhes)
            val fecharButton = dialog.findViewById<MaterialButton>(R.id.fechar_button)

            receitaImage.setImageDrawable(ResourcesCompat.getDrawable(context.resources, imagem, null))
            receitaNome.text = nome
            receitaDetalhes.text = "Detalhes da receita ${nome}..."

            fecharButton.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    class ReceitasViewHolder(binding: ItemReceitaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val imageView: ImageView = binding.imageViewItemReceita
        val textView: TextView = binding.textViewItemReceita
    }
}