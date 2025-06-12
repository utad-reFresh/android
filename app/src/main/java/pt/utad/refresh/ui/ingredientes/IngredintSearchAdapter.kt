package pt.utad.refresh.ui.ingredientes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import pt.utad.refresh.IngredientDto
import pt.utad.refresh.R

class IngredientSearchAdapter(
    private val onClick: (IngredientDto) -> Unit
) : ListAdapter<IngredientDto, IngredientSearchAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<IngredientDto>() {
        override fun areItemsTheSame(oldItem: IngredientDto, newItem: IngredientDto) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: IngredientDto, newItem: IngredientDto) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingredient_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ingredient = getItem(position)
        holder.bind(ingredient)
        holder.itemView.setOnClickListener { onClick(ingredient) }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.ingredient_name)
        private val image: ImageView = itemView.findViewById(R.id.ingredient_image)

        fun bind(ingredient: IngredientDto) {
            name.text = ingredient.name
            Glide.with(image.context)
                .load(ingredient.imageUrl)
                .placeholder(R.drawable.egg_40px)
                .into(image)
        }
    }
}