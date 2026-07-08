package com.example.appointmed.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appointmed.adapters.AppointmentAdapter
import com.example.appointmed.databinding.FragmentHistoryBinding
import com.example.appointmed.repository.AppointmentRepository

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        refreshList()
    }

    private fun refreshList() {
        binding.rvHistory.adapter = AppointmentAdapter(AppointmentRepository.appointments) { apt ->
            AppointmentRepository.cancel(apt.id)
            refreshList()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}