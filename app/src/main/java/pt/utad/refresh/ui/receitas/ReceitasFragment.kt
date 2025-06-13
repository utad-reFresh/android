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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pt.utad.refresh.ApiClient
import pt.utad.refresh.R
import pt.utad.refresh.RecipeResponse
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
        ListAdapter<pt.utad.refresh.RecipeInListDto, ReceitasViewHolder>(object : DiffUtil.ItemCallback<pt.utad.refresh.RecipeInListDto>() {
            override fun areItemsTheSame(oldItem: pt.utad.refresh.RecipeInListDto, newItem: pt.utad.refresh.RecipeInListDto): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: pt.utad.refresh.RecipeInListDto, newItem: pt.utad.refresh.RecipeInListDto): Boolean =
                oldItem == newItem
        }) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceitasViewHolder {
            val binding = ItemReceitaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ReceitasViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ReceitasViewHolder, position: Int) {
            val recipe = getItem(position)
            holder.textView.text = recipe.name



            Glide.with(holder.imageView.context)
                .load(recipe.imageUrl)
                .placeholder(ResourcesCompat.getDrawable(holder.imageView.resources, R.drawable.egg_alt_24px, null))
                .into(holder.imageView)


            holder.itemView.setOnClickListener {
                // Fetch details and show dialog
                CoroutineScope(Dispatchers.Main).launch {
                    val response = ApiClient.apiService.getRecipe(recipe.id)
                    if (response.isSuccessful) {
                        val details = response.body()
                        if (details != null) {
                            mostrarDialogReceita(holder.itemView.context, details)
                        }
                    }
                }
            }

        }

        private fun mostrarDialogReceita(context: Context, recipe: RecipeResponse) {
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
            val passosContainer = dialog.findViewById<LinearLayout>(R.id.passos_container)


            val chipVegetarian = dialog.findViewById<View>(R.id.chip_vegetarian)
            val chipVegan = dialog.findViewById<View>(R.id.chip_vegan)
            val chipGlutenFree = dialog.findViewById<View>(R.id.chip_gluten_free)
            val chipDairyFree = dialog.findViewById<View>(R.id.chip_dairy_free)
            val chipVeryHealthy = dialog.findViewById<View>(R.id.chip_very_healthy)
            val chipCheap = dialog.findViewById<View>(R.id.chip_cheap)
            val chipVeryPopular = dialog.findViewById<View>(R.id.chip_very_popular)
            val chipSustainable = dialog.findViewById<View>(R.id.chip_sustainable)

            chipVegetarian.visibility = if (recipe.vegetarian == true) View.VISIBLE else View.GONE
            chipVegan.visibility = if (recipe.vegan == true) View.VISIBLE else View.GONE
            chipGlutenFree.visibility = if (recipe.glutenFree == true) View.VISIBLE else View.GONE
            chipDairyFree.visibility = if (recipe.dairyFree == true) View.VISIBLE else View.GONE
            chipVeryHealthy.visibility = if (recipe.veryHealthy == true) View.VISIBLE else View.GONE
            chipCheap.visibility = if (recipe.cheap == true) View.VISIBLE else View.GONE
            chipVeryPopular.visibility = if (recipe.veryPopular == true) View.VISIBLE else View.GONE
            chipSustainable.visibility = if (recipe.sustainable == true) View.VISIBLE else View.GONE


            val editTempo = dialog.findViewById<TextView>(R.id.edit_tempo)
            editTempo.text = recipe.readyInMinutes?.let { "$it min" } ?: "N/A"

            val editEquipamentos = dialog.findViewById<TextView>(R.id.edit_equipamentos)
            val layoutEquipamentos = editEquipamentos.parent.parent as View // TextInputLayout
            if (recipe.equipment.isNullOrEmpty()) {
                layoutEquipamentos.visibility = View.GONE
            } else {
                layoutEquipamentos.visibility = View.VISIBLE
                editEquipamentos.text = recipe.equipment.joinToString("\n") { it.name }
            }

            // Fill ingredients (comma separated)
            val editIngredientes = dialog.findViewById<TextView>(R.id.edit_ingredientes)
            editIngredientes.text = recipe.ingredientDets.joinToString("\n") { ing ->
                fun formatAmount(amount: Double, unit: String): String =
                    if (unit.isBlank()) {
                        if (amount % 1.0 == 0.0) "${amount.toInt()}" else "$amount"
                    } else {
                        if (amount % 1.0 == 0.0) "${amount.toInt()} $unit" else "$amount $unit"
                    }

                val metric = if (ing.amountMetric > 0) formatAmount(ing.amountMetric, ing.unitShortMetric) else null
                val imperial = if (ing.amountImperial > 0) formatAmount(ing.amountImperial, ing.unitShortImperial) else null

                when {
                    metric != null && imperial != null && metric == imperial -> "${ing.name} ($metric)"
                    metric != null && imperial != null -> "${ing.name} ($metric / $imperial)"
                    metric != null -> "${ing.name} ($metric)"
                    imperial != null -> "${ing.name} ($imperial)"
                    else -> ing.name
                }
            }

            receitaNome.text = recipe.title
            receitaDetalhes.text = recipe.summary.replace("<br>", "\n").replace("<[^>]*>".toRegex(), "")
            receitaImage.let {
                Glide.with(context)
                    .load(recipe.imageUrl)
                    .placeholder(ResourcesCompat.getDrawable(context.resources, R.drawable.egg_alt_24px, null))
                    .into(it)
            }



            passosContainer.removeAllViews()
            val stepMargin = context.resources.getDimensionPixelSize(R.dimen.margin_8dp)
            val dividerColor = ResourcesCompat.getColor(context.resources, R.color.mtrl_divider_color, null)

            recipe.steps.sortedBy { it.number }.forEachIndexed { idx, stepDto ->
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = if (idx == 0) 0 else stepMargin / 2
                    }
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }

                val numberView = TextView(context).apply {
                    text = stepDto.number.toString()
                    textSize = 18f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    gravity = android.view.Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        context.resources.getDimensionPixelSize(R.dimen.step_number_width),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(0, stepMargin / 2, 0, stepMargin / 2)
                }

                val textView = TextView(context).apply {
                    text = stepDto.step
                    textSize = 16f
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }


                row.addView(numberView)
                row.addView(textView)
                passosContainer.addView(row)

                if (idx < recipe.steps.size - 1) {
                    val divider = View(context).apply {
                        setBackgroundColor(dividerColor)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            context.resources.getDimensionPixelSize(R.dimen.divider_height)
                        ).apply {
                            topMargin = stepMargin / 2
                            bottomMargin = stepMargin / 2
                        }
                    }
                    passosContainer.addView(divider)
                }
            }

            if (recipe.steps.isNullOrEmpty()) {
                val noStepsView = TextView(context).apply {
                    text = "Esta receita não disponibiliza modo de preparação"
                    textSize = 16f
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, context.resources.getDimensionPixelSize(R.dimen.margin_8dp), 0, context.resources.getDimensionPixelSize(R.dimen.margin_8dp))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                passosContainer.addView(noStepsView)
            }

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