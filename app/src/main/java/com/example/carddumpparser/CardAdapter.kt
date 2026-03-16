package com.example.carddumpparser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.recyclerview.widget.RecyclerView
import com.example.carddumpparser.databinding.ItemCardBinding

/**
 * RecyclerView adapter used to display a list of parsed cards. Each item
 * contains the card holder name, card number formatted with spaces,
 * expiry date and CVV. The user can tap copy icons to copy values
 * directly to the clipboard. A simple toast provides feedback on
 * successful copies.
 */
class CardAdapter(
    private var items: List<Card>
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    /** ViewHolder class encapsulating the binding for each row. */
    inner class CardViewHolder(private val binding: ItemCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(card: Card, position: Int) {
            // Index badge starts at 1 to match the UI in the HTML version.
            binding.indexBadge.text = (position + 1).toString()
            binding.nameText.text = card.name
            binding.numberText.text = formatCardNumber(card.cardNumber)
            binding.expiryText.text = card.expiry
            binding.cvvText.text = card.cvv

            // Set up click listeners on copy icons. Each one copies a different
            // property of the card. A toast message indicates success.
            binding.copyName.setOnClickListener { copyToClipboard(it.context, card.name) }
            binding.copyNumber.setOnClickListener { copyToClipboard(it.context, card.cardNumber) }
            binding.copyExpiry.setOnClickListener { copyToClipboard(it.context, card.expiry) }
            binding.copyCvv.setOnClickListener { copyToClipboard(it.context, card.cvv) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCardBinding.inflate(inflater, parent, false)
        return CardViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    /**
     * Formats the card number into groups of 4 digits separated by spaces. If
     * the number does not divide evenly into groups, any remaining digits
     * appear at the end without extra padding.
     */
    private fun formatCardNumber(num: String): String {
        val sb = StringBuilder()
        num.forEachIndexed { index, c ->
            sb.append(c)
            if ((index + 1) % 4 == 0 && (index + 1) < num.length) {
                sb.append(' ')
            }
        }
        return sb.toString()
    }

    /**
     * Copies the supplied text to the clipboard using the system
     * ClipboardManager. Displays a short toast on success.
     */
    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService<ClipboardManager>()
        val clip = ClipData.newPlainText("card_data", text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
    }

    /**
     * Replaces the current list of cards with a new one and updates the
     * RecyclerView. This method is called from the fragment when the
     * user performs a new parse operation.
     */
    fun submitList(newItems: List<Card>) {
        items = newItems
        notifyDataSetChanged()
    }
}