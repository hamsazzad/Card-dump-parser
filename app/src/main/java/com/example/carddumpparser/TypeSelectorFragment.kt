package com.example.carddumpparser

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.carddumpparser.databinding.FragmentTypeSelectorBinding

/**
 * Fragment that presents a simple menu allowing the user to choose
 * between Visa, MasterCard and UnionPay. The selection is communicated
 * back to the hosting activity via the [OnTypeSelectedListener]
 * interface.
 */
class TypeSelectorFragment : Fragment() {

    private var _binding: FragmentTypeSelectorBinding? = null
    private val binding get() = _binding!!

    /** Callback interface implemented by the activity to receive
     * notification when the user chooses a card type.
     */
    interface OnTypeSelectedListener {
        fun onTypeSelected(type: String)
    }

    private var listener: OnTypeSelectedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Ensure the host implements the callback interface. If it does
        // not, a ClassCastException will be thrown to assist the
        // developer during integration.
        listener = context as? OnTypeSelectedListener
            ?: throw ClassCastException("Host activity must implement OnTypeSelectedListener")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTypeSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Wire button clicks to send the selected card type back to the host.
        binding.visaButton.setOnClickListener { listener?.onTypeSelected("VISA") }
        binding.masterButton.setOnClickListener { listener?.onTypeSelected("MASTERCARD") }
        binding.unionButton.setOnClickListener { listener?.onTypeSelected("UNIONPAY") }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /** Returns a new instance of the type selector fragment. */
        fun newInstance(): TypeSelectorFragment = TypeSelectorFragment()
    }
}