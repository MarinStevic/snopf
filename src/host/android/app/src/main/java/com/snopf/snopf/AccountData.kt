package com.snopf.snopf

data class AccountsTable ( val accounts: ArrayList<AccountData>)

data class AccountData ( val hostname: String, val username: String, val password_length: Int, val password_iteration: Int)