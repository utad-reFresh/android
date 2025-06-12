package pt.utad.refresh.ui.perfil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import androidx.activity.result.contract.ActivityResultContracts
import pt.utad.refresh.R
import pt.utad.refresh.databinding.FragmentPerfilBinding
import pt.utad.refresh.ApiClient
import pt.utad.refresh.ApiService
import kotlinx.coroutines.launch

class SlideshowViewModelFactory(
    private val apiService: ApiService
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PerfilViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PerfilViewModel(apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class SlideshowFragment : Fragment() {
    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: PerfilViewModel

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        uri?.let {
            binding.profileImage.setImageURI(it)
            lifecycleScope.launch {
                viewModel.updateProfile(
                    binding.profileName.text.toString(),
                    it.toString()
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)

        val apiService = ApiClient.apiService
        val factory = SlideshowViewModelFactory(apiService)
        viewModel = ViewModelProvider(this, factory)[PerfilViewModel::class.java]

        setupUI()
        observeViewModel()

        // Fetch user profile when fragment is created
        lifecycleScope.launch {
            viewModel.getProfile()
        }

        return binding.root
    }

    private fun setupUI() {
        binding.profileImage.setOnClickListener {
            getContent.launch("image/*")
        }

        binding.saveButton.setOnClickListener {
            lifecycleScope.launch {
                viewModel.updateProfile(
                    binding.profileName.text.toString(),
                    viewModel.userProfile.value?.photoUrl ?: ""
                )
            }
        }
    }

    private fun observeViewModel() {
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            binding.profileName.text = profile.displayName
            binding.emailField.setText(profile.email)
            Glide.with(this)
                .load(ApiClient.BASE_URL + profile.photoUrl)
                .placeholder(R.drawable.account_circle_40px)
                .into(binding.profileImage)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}