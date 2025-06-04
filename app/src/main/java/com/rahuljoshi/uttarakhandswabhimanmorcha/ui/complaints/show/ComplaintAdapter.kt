package com.rahuljoshi.uttarakhandswabhimanmorcha.ui.complaints.show


import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rahuljoshi.uttarakhandswabhimanmorcha.databinding.MyComplaintLayoutBinding
import com.rahuljoshi.uttarakhandswabhimanmorcha.interfaces.OnClickListeners
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.Complaint
import java.text.SimpleDateFormat
import java.util.Locale

class ComplaintAdapter(
    private var complaints: List<Complaint> = listOf(),
    private val listener: OnClickListeners
) : RecyclerView.Adapter<ComplaintAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d(TAG, "onCreateViewHolder: in $TAG'")
        val binding =
            MyComplaintLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: in $TAG")
        val currentComplaint = complaints[position]
        holder.bind(currentComplaint)
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount: complaint size = ${complaints.size}")
        return complaints.size
    }


    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newComplaints: List<Complaint>) {
        Log.d(TAG, "updateList: updating complaint = $newComplaints")
        complaints = newComplaints
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: MyComplaintLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("ResourceAsColor")
        fun bind(complaint: Complaint) {
            Log.d(TAG, "bind: bind $complaints")
            val timestamp = complaint.timestamp
            val date = timestamp.toDate()
            val formattedDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(date)

            binding.apply {
                complaintName.text = complaint.complaintTitle
                complaintDescription.text = complaint.complaintDescription
                complaintDate.text = formattedDate
                //complaintStatus.text = complaint.status

                /*// Set status color based on complaint status
                when (complaint.status.lowercase()) {
                    "solved" -> {
                        complaintStatus.setBackgroundColor(context.getColor(R.color.md_theme_secondaryContainer))
                        complaintStatus.setTextColor(ContextCompat.getColor(R.color.md_theme_onSecondaryContainer))
                    }
                    "in progress" -> {
                        complaintStatus.setBackgroundColor(context.getColor(R.color.md_theme_tertiaryContainer))
                        complaintStatus.setTextColor(context.getColor(R.color.md_theme_onTertiaryContainer))
                    }
                    "rejected" -> {
                        complaintStatus.setBackgroundColor(context.getColor(R.color.md_theme_errorContainer))
                        complaintStatus.setTextColor(context.getColor(R.color.md_theme_onErrorContainer))
                    }
                    else -> {
                        complaintStatus.setBackgroundColor(context.getColor(R.color.md_theme_primaryContainer))
                        complaintStatus.setTextColor(context.getColor(R.color.md_theme_onPrimaryContainer))
                    }
                }*/
            }

            // Handle item click
            itemView.setOnClickListener {
                Log.d(TAG, "bind: ${itemView.toString()} is clicked in $TAG")
                listener.onComplaintClickListener(complaint)
            }
        }

    }

    companion object {
        private const val TAG = "ComplaintAdapter"
    }
}