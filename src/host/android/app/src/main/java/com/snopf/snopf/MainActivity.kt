package com.snopf.snopf

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.hardware.biometrics.BiometricPrompt
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.NumberPicker
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.marginStart
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.settings_dialog.*
import java.io.File


class MainActivity : AppCompatActivity() {

    private val usbVendorRequest = 0x40

    private var passwordLength = MAX_PASSWORD_LENGTH
    private var passwordIteration = 0
    private var autoHitEnter = false

    private lateinit var biometricPromptManager: BiometricPromptManager

    fun resetPasswordSettings() {
        passwordLength = MAX_PASSWORD_LENGTH
        passwordIteration = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonBiometrics.isClickable = checkBiometricSupport(false)
        if (!checkBiometricSupport(false)) {
            buttonBiometrics.background.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    hostnameSelect.setText(data.getStringExtra("HOSTNAME"))
                    accountSelect.setText(data.getStringExtra("USERNAME"))
                    passwordLength = data.getIntExtra("PWD_LEN", MAX_PASSWORD_LENGTH)
                    passwordIteration = data.getIntExtra("PWD_IT", 0)
                }
            }
        }
    }

    private fun saveAccountData(accountData: AccountData) {
        val fileName = "AccountsTable.json"
        val file = File(getExternalFilesDir(null).toString() + "/" + fileName)
        val isNewFileCreated = file.createNewFile()
        if(isNewFileCreated || file.readText().isEmpty()){
            file.writeText("{\"accounts\":[]}")
        }
        val inputStream: String = file.readText()
        val gson = GsonBuilder().create()
        val accountsTable = gson.fromJson(inputStream, AccountsTable::class.java)
        if (accountData in accountsTable.accounts) {
            return
        }
        if (accountsTable.accounts.isEmpty()) {
            accountsTable.accounts.add(accountData)
        } else {
            for (i in 0 until accountsTable.accounts.size) {
                if (accountData.hostname < accountsTable.accounts[i].hostname) {
                    accountsTable.accounts.add(i, accountData)
                    val accountsTableJson = gson.toJson(accountsTable)
                    file.writeText(accountsTableJson)
                    return
                }
            }
            accountsTable.accounts.add(accountData)
        }
        val accountsTableJson = gson.toJson(accountsTable)
        file.writeText(accountsTableJson)
        return
    }

    fun onClickBiometrics(button: View) {
        biometricPromptManager = BiometricPromptManager(this)
        biometricPromptManager.decryptPrompt(
            failedAction = { showInfo("Error","You need to set the Master Pin first.") },
            successAction = {
                if (String(it).isEmpty()) {
                    showInfo("Error","You need to set the Master Pin first.")
                } else {
                    makeRequest(String(it))
                }
            }
        )
    }

    fun onClickOk(button: View) {
        val pin = editPin.text.toString()
        makeRequest(pin)
    }

    private fun makeRequest(pin: String) {
        val hostname = hostnameSelect.text.toString()
        val account = accountSelect.text.toString()

        val pwRequest = passwordRequest(hostname, account, pin, 0)

        val snopfDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice?
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val conn = usbManager.openDevice(snopfDevice)

        if (conn == null) {
            showInfo("Missing USB device", "USB device not found.")
            return
        }

        val rawRequest = usbRequest(pwRequest, passwordLength, autoHitEnter)

        // If the device is found we just send the request and close this activity because
        // we would just be in the way any ways

        if (saveNewCheckbox.isChecked) {
            saveAccountData(AccountData(hostname, account, passwordLength, passwordIteration))
        }

        conn.controlTransfer(usbVendorRequest, 0, 0, 0,
            rawRequest, rawRequest.size, 0)

        this.finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.accounts_table -> {
                val intent = Intent(this, AccountsActivity::class.java)
                startActivityForResult(intent, 1)
                true
            }
            R.id.password_settings -> {
                val alertDialog: AlertDialog? = this.let {
                    val builder = AlertDialog.Builder(it)
                    val inflater = layoutInflater
                    inflater.let {
                        builder.setView(inflater.inflate(R.layout.settings_dialog, null))
                            .setPositiveButton(R.string.ok)
                            {
                                dialogInterface, id ->
                                val dialog = dialogInterface as Dialog
                                passwordLength = dialog.findViewById<NumberPicker>(
                                    R.id.passwordLengthPicker).value
                                passwordIteration = dialog.findViewById<NumberPicker>(
                                    R.id.passwordIterationPicker).value
                            }
                            .setNegativeButton(R.string.cancel
                            ) { dialog, id ->
                                dialog.dismiss()
                            }
                    }
                    builder.create()
                }
                alertDialog?.let {
                    it.show()
                    it.passwordLengthPicker.maxValue = MAX_PASSWORD_LENGTH
                    it.passwordLengthPicker.minValue = MIN_PASSWORD_LENGTH
                    it.passwordLengthPicker.value = passwordLength
                    it.passwordLengthPicker.wrapSelectorWheel = false
                    it.passwordIterationPicker.minValue = 0
                    it.passwordIterationPicker.maxValue = 999
                    it.passwordIterationPicker.value = passwordIteration
                    it.passwordIterationPicker.wrapSelectorWheel = false
                }
                true
            }
            R.id.set_pin -> {
                if (checkBiometricSupport(true)) {
                    val builder = AlertDialog.Builder(this)
                    val pin = EditText(this)
                    pin.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    builder.setTitle("Enter your Master Pin:")
                        .setMessage("Remember this password.")
                        .setCancelable(false)
                        .setView(pin)
                        .setPositiveButton("Save") { dialog, id ->
                            biometricPromptManager = BiometricPromptManager(this)
                            biometricPromptManager.encryptPrompt(
                                data = pin.text.toString().toByteArray(),
                                failedAction = {
                                    showInfo(
                                        "Failed",
                                        "Something went wrong. Please try to set Master Pin again."
                                    )
                                },
                                successAction = {
                                    showInfo(
                                        "Success",
                                        "The Master Pin has been successfully stored and encrypted."
                                    )
                                }
                            )
                        }
                        .setNegativeButton("Cancel") { dialog, id ->
                            dialog.dismiss()
                        }
                    val alert = builder.create()
                    alert.show()
                }
                true
            }
            R.id.info -> {
                showInfo("Version", "Commit: " + BuildConfig.GitHash)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    fun showInfo(title: String, message: String) {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK") {
                dialog, which -> dialog.dismiss() }
        alertDialog.show()
    }

    private fun checkBiometricSupport(showInfo: Boolean): Boolean {
        val keyguardManager =
            getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val packageManager = this.packageManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (showInfo)
                showInfo("Error","This Android version does not support fingerprint authentication.")
            return false
        }
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            if (showInfo)
                showInfo("Error","Fingerprint Sensor not supported.")
            return false
        }
        if (!keyguardManager.isKeyguardSecure) {
            if (showInfo)
                showInfo("Error","Lock screen security not enabled in Settings.")
            return false
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_BIOMETRIC ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT ) != PackageManager.PERMISSION_GRANTED ) {
            if (showInfo)
                showInfo("Error","Fingerprint authentication permission not enabled.")
            return false
        }
        return true
    }
}
