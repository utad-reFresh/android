package pt.utad.refresh.ui.ingredientes

import android.os.Bundle
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
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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