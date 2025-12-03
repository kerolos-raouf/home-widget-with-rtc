package com.example.homewidgettocall

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.homewidgettocall.model.Friend

class FriendsAdapter(
    private var friends: List<Friend>,
    private val onFriendClick: (Friend) -> Unit
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    class FriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emailTextView: TextView = view.findViewById(R.id.tvFriendEmail)
        val tokenTextView: TextView = view.findViewById(R.id.tvFriendToken)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        holder.emailTextView.text = friend.email
        
        // Show only first 20 characters of token for display
        val tokenPreview = if (friend.fcmToken.length > 20) {
            "${friend.fcmToken.take(20)}..."
        } else {
            friend.fcmToken
        }
        holder.tokenTextView.text = "Token: $tokenPreview"
        
        holder.itemView.setOnClickListener {
            onFriendClick(friend)
        }
    }

    override fun getItemCount() = friends.size

    fun updateFriends(newFriends: List<Friend>) {
        friends = newFriends
        notifyDataSetChanged()
    }
}
