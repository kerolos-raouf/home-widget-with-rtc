package com.example.homewidgettocall

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.homewidgettocall.model.VoiceRecording
import java.io.File

class VoiceRecordingsAdapter(
    private var items: List<VoiceRecording>,
    private val onPlayClick: (File) -> Unit,
    private val onPauseClick: () -> Unit
) : RecyclerView.Adapter<VoiceRecordingsAdapter.VoiceViewHolder>() {

    inner class VoiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileName = itemView.findViewById<TextView>(R.id.tvFileName)
        val playButton = itemView.findViewById<ImageView>(R.id.btnPlay)
        val pauseButton = itemView.findViewById<ImageView>(R.id.btnPause)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_voice_recording, parent, false)
        return VoiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: VoiceViewHolder, position: Int) {
        val item = items[position]

        holder.fileName.text = item.name

        holder.playButton.setOnClickListener {
            holder.pauseButton.visibility = View.VISIBLE
            holder.playButton.visibility = View.GONE
            onPlayClick(item.file)
        }

        holder.pauseButton.setOnClickListener {
            holder.pauseButton.visibility = View.GONE
            holder.playButton.visibility = View.VISIBLE
            onPauseClick()
        }

    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<VoiceRecording>) {
        items = newItems
        notifyDataSetChanged()
    }
}
