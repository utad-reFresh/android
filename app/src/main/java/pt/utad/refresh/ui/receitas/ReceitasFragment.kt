package pt.utad.refresh.ui.receitas

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
        }
    }

    class ReceitasViewHolder(binding: ItemReceitaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val imageView: ImageView = binding.imageViewItemReceita
        val textView: TextView = binding.textViewItemReceita
    }
}