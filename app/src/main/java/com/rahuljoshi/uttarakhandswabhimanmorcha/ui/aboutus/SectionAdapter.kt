package com.rahuljoshi.uttarakhandswabhimanmorcha.ui.aboutus

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rahuljoshi.uttarakhandswabhimanmorcha.R
import com.rahuljoshi.uttarakhandswabhimanmorcha.model.data.Section

class SectionAdapter(private val sections: List<Section>) :
    RecyclerView.Adapter<SectionAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.sectionTitle)
        val content: TextView = itemView.findViewById(R.id.sectionContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.about_us_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val section = sections[position]
        holder.title.text = section.title
        // Replace \n with <br> for HTML rendering while preserving special characters
        val htmlContent = section.content.replace("\n", "<br>")
        holder.content.text = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_LEGACY)
        holder.content.visibility = if (section.isExpanded) View.VISIBLE else View.GONE

        holder.title.setOnClickListener {
            section.isExpanded = !section.isExpanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = sections.size
}