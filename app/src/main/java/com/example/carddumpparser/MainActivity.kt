package com.example.carddumpparser

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.carddumpparser.databinding.ActivityMainBinding

/**
 * The main entry point of the application. It hosts a MatrixView for the
 * background animation and uses a fragment container to switch between
 * the type selection screen and the terminal workspace. Implements
 * [TypeSelectorFragment.OnTypeSelectedListener] to handle card type
 * selection events.
 */
class MainActivity : AppCompatActivity(), TypeSelectorFragment.OnTypeSelectedListener {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // When first created, show the type selector fragment.
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, TypeSelectorFragment.newInstance())
                .commit()
        }
    }

    /** Called by the type selector fragment when a card type is selected. */
    override fun onTypeSelected(type: String) {
        // Replace the current fragment with a new TerminalFragment and add
        // the transaction to the back stack so the user can return to
        // the selection screen via the back button.
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, TerminalFragment.newInstance(type))
            .addToBackStack(null)
            .commit()
    }
}