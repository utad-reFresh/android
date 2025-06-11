package pt.utad.refresh.ui.slideshow

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
import pt.utad.refresh.databinding.FragmentSlideshowBinding

class SlideshowFragment : Fragment() {
    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SlideshowViewModel

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        uri?.let {
            binding.profileImage.setImageURI(it)
            lifecycleScope.launchWhenStarted {
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
        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[SlideshowViewModel::class.java]

        setupUI()
        observeViewModel()

        return binding.root
    }

    private fun setupUI() {
        binding.profileImage.setOnClickListener {
            getContent.launch("image/*")
        }

        binding.saveButton.setOnClickListener {
            lifecycleScope.launchWhenStarted {
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
                .load(profile.photoUrl)
                .placeholder(R.drawable.account_circle_40px)
                .into(binding.profileImage)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }
}