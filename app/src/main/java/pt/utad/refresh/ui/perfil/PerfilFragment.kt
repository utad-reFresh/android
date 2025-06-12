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

    private var getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
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
        var selectedPhotoUri: android.net.Uri? = null
        var photoRemoved = false
        var changeMade = false

        binding.profileImage.setOnClickListener {
            getContent.launch("image/*")
        }

        binding.changePhotoButton.setOnClickListener {
            getContent.launch("image/*")
        }

        binding.removePhotoButton.setOnClickListener {
            selectedPhotoUri = null
            photoRemoved = true
            binding.profileImage.setImageResource(R.drawable.account_circle_40px)
        }

        getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
            uri?.let {
                selectedPhotoUri = it
                photoRemoved = false
                binding.profileImage.setImageURI(it)
            }
        }

        binding.saveButton.setOnClickListener {
            val currentProfile = viewModel.userProfile.value
            val newName = binding.nameField.text.toString()
            val currentPassword = binding.currentPasswordField.text.toString()
            val newPassword = binding.newPasswordField.text.toString()

            lifecycleScope.launch {
                // Change display name if needed
                if (currentProfile != null && newName.isNotBlank() && newName != currentProfile.displayName) {
                    viewModel.changeDisplayName(newName)
                    changeMade = true
                }

                // Change photo if a new photo was selected
                if (selectedPhotoUri != null) {
                    viewModel.changePhoto(requireContext(), selectedPhotoUri!!)
                    changeMade = true
                }

                // Remove photo if requested
                if (photoRemoved) {
                    viewModel.removePhoto()
                    changeMade = true
                }

                // Change password if both fields are filled
                if (currentPassword.isNotBlank() && newPassword.isNotBlank()
                    && currentProfile != null
                    && currentProfile.email.isNotBlank()
                    && context != null) {
                    viewModel.changePasswordAndReAuth(context, currentProfile.email, currentPassword, newPassword)
                    changeMade = true
                } else if (newPassword.isNotBlank() && currentPassword.isBlank()) {
                    Toast.makeText(context, "Preencha ambos os campos de senha", Toast.LENGTH_SHORT).show()
                }

                if (!changeMade) {
                    Toast.makeText(context, "Nenhuma alteração feita", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Perfil atualizado com sucesso", Toast.LENGTH_SHORT).show()
                }

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

        viewModel.passwordChanged.observe(viewLifecycleOwner) { changed ->
            if (changed == true) {
                Toast.makeText(context, "Password changed successfully", Toast.LENGTH_SHORT).show()
                binding.currentPasswordField.text?.clear()
                binding.newPasswordField.text?.clear()
                binding.currentPasswordField.clearFocus()
                binding.newPasswordField.clearFocus()
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}