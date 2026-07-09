package com.example.appointmed.features.admin.fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appointmed.features.admin.adapters.AdminPatientAdapter
import com.example.appointmed.databinding.FragmentAdminPatientsBinding
import com.example.appointmed.features.admin.repository.AdminRepository
class AdminPatientsFragment : Fragment() {

    private var _binding: FragmentAdminPatientsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminPatientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvAdminPatients.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAdminPatients.adapter = AdminPatientAdapter(AdminRepository.patients)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}