package com.snopf.snopf

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.account_list_item.view.*

class AccountAdapter(val accountList: List<AccountData>, val clickListener: (AccountData) -> Unit, val longClickListener: (Int) -> Boolean) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.account_list_item, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as AccountViewHolder).bind(accountList[holder.adapterPosition], clickListener, longClickListener)
    }

    override fun getItemCount() = accountList.size

    class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(account: AccountData, clickListener: (AccountData) -> Unit, longClickListener: (Int) -> Boolean) {
            itemView.tv_hostname.text = account.hostname
            itemView.tv_username.text = account.username
            itemView.setOnClickListener { clickListener(account)}
            itemView.setOnLongClickListener { longClickListener(adapterPosition)}
        }
    }
}