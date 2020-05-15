package com.snopf.snopf

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_accounts.*
import java.io.File

class AccountsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accounts)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rv_accounts.layoutManager = LinearLayoutManager(this)
        rv_accounts.setHasFixedSize(true)
        val accountsTable = loadAccountsTable()
        rv_accounts.adapter = AccountAdapter(accountsTable, { account : AccountData -> accountClicked(account) }, { position : Int -> deleteAccount( position) })
    }

    private fun deleteAccount(index: Int): Boolean {
        val fileName = "AccountsTable.json"
        val file = File(getExternalFilesDir(null).toString() + "/" + fileName)
        val inputStream: String = file.readText()
        val gson = GsonBuilder().create()
        val accountsTable = gson.fromJson(inputStream, AccountsTable::class.java)
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Are you sure you want to Delete?\n${accountsTable.accounts[index].hostname}")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                accountsTable.accounts.removeAt(index)
                rv_accounts.adapter = AccountAdapter(accountsTable.accounts, { account : AccountData -> accountClicked(account) }, {position : Int -> deleteAccount(position) })
                val accountsTableJson = gson.toJson(accountsTable)
                file.writeText(accountsTableJson)
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
        return true
    }

    private fun accountClicked(account: AccountData) {
        Toast.makeText(this, "Using: ${account.hostname}", Toast.LENGTH_LONG).show()

        val mainIntent = Intent(this, MainActivity::class.java)
        mainIntent.putExtra("HOSTNAME", account.hostname)
        mainIntent.putExtra("USERNAME", account.username)
        mainIntent.putExtra("PWD_LEN", account.password_length)
        mainIntent.putExtra("PWD_IT", account.password_iteration)
        setResult(Activity.RESULT_OK,mainIntent);
        finish();
    }

    private fun loadAccountsTable() : List<AccountData> {
        val fileName = "AccountsTable.json"
        val file = File(getExternalFilesDir(null).toString() + "/" + fileName)
        val isNewFileCreated = file.createNewFile()
        if(isNewFileCreated || file.readText().isEmpty()){
            file.writeText("{\"accounts\":[]}")
        }
        val inputStream: String = file.readText()
        val gson = GsonBuilder().create()
        val accountsTable = gson.fromJson(inputStream, AccountsTable::class.java)
        return accountsTable.accounts
    }
}
