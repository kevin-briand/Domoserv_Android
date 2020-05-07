package Domoserv_Pi.domoserv_android

import Domoserv_Pi.domoserv_android.Common.NetworkError
import Domoserv_Pi.domoserv_android.Common.WebSock
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

enum class Zone { unused, Z1, Z2 }

class MainActivity : AppCompatActivity() {

    private var ws = WebSocket()

    private var stateList = List(3) {""}
    private var modeList = List(3) {""}
    private var indoorTemp = List(3) {""}
    private var outdoorTemp = List(3) {""}

    inner class WebSocket() : WebSock() {
        override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
            super.onMessage(webSocket, text)
            val result = mDecryptedText
            Handler(Looper.getMainLooper()).post(Runnable { updateField(result) })
            mDecryptedText = ""
        }

        fun changeState(zone: Int, state: Int) {
            ws.send(getString(R.string.setZoneState).replace("{zone}",zone.toString()).replace("{state}",state.toString()))
        }

        fun changeMode(zone: Int, mode: Int) {
            ws.send(getString(R.string.setZoneMode).replace("{zone}",zone.toString()).replace("{mode}",mode.toString()))
        }

        fun startUpdate() {
            ws.send(getString(R.string.getZ1State))
            ws.send(getString(R.string.getZ2State))
            ws.send(getString(R.string.getZ1Mode))
            ws.send(getString(R.string.getZ2Mode))
            ws.send(getString(R.string.getZ1RemainingTime))
            ws.send(getString(R.string.getZ2RemainingTime))
            ws.send(getString(R.string.getIndoorTemp))
            ws.send(getString(R.string.getOutdoorTemp))
            val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val endDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val dateYear = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy")) + "-01-01"
            ws.send(getString(R.string.getDataEnergy).replace("{date}",date).replace("{endDate}",endDate))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        stateList = listOf(getString(R.string.comfort),getString(R.string.eco),getString(R.string.frostFree))
        modeList = listOf(getString(R.string.auto), getString(R.string.semiAuto), getString(R.string.manual))

        val intent = Intent(this, ConnectionActivity::class.java)

        intent.putExtra("Path",this.filesDir.path)
        intent.putExtra("FirstAttempt",true)
        startActivityForResult(intent,0)

        //define state click listener
        val stateLayout = findViewById<LinearLayout>(R.id.btnState)
        stateLayout.setOnClickListener {
            showHeaterDialog()
        }
        val tempLayout = findViewById<LinearLayout>(R.id.btnTemp)
        tempLayout.setOnClickListener {
            showTemperatureDialog()
        }
    }

    private fun showHeaterDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.set_state_mode_dialog)

        val stateZ1 = dialog.findViewById(R.id.selectStateZ1) as Spinner
        val stateZ2 = dialog.findViewById(R.id.selectStateZ2) as Spinner
        val modeZ1 = dialog.findViewById(R.id.selectModeZ1) as Spinner
        val modeZ2 = dialog.findViewById(R.id.selectModeZ2) as Spinner

        var dataAdapter = ArrayAdapter(this, R.layout.spinner, stateList)
        stateZ1.adapter = dataAdapter
        stateZ2.adapter = dataAdapter

        dataAdapter = ArrayAdapter<String>(this, R.layout.spinner, modeList)
        modeZ1.adapter = dataAdapter
        modeZ2.adapter = dataAdapter

        stateZ1.setSelection(stateList.indexOf(findViewById<TextView>(R.id.stateZ1).text))
        stateZ2.setSelection(stateList.indexOf(findViewById<TextView>(R.id.stateZ2).text))
        modeZ1.setSelection(modeList.indexOf(findViewById<TextView>(R.id.modeZ1).text))
        modeZ2.setSelection(modeList.indexOf(findViewById<TextView>(R.id.modeZ2).text))

        val submit = dialog.findViewById(R.id.submit) as Button
        submit.setOnClickListener {
            if(stateZ1.selectedItemPosition != stateList.indexOf(findViewById<TextView>(R.id.stateZ1).text)) {
                ws.changeState(Zone.Z1.ordinal,stateZ1.selectedItemPosition)
            }
            if(stateZ2.selectedItemPosition != stateList.indexOf(findViewById<TextView>(R.id.stateZ2).text)) {
                ws.changeState(Zone.Z2.ordinal,stateZ2.selectedItemPosition)
            }
            if(modeZ1.selectedItemPosition != modeList.indexOf(findViewById<TextView>(R.id.modeZ1).text)) {
                ws.changeMode(Zone.Z1.ordinal,modeZ1.selectedItemPosition)
            }
            if(modeZ2.selectedItemPosition != modeList.indexOf(findViewById<TextView>(R.id.modeZ2).text)) {
                ws.changeMode(Zone.Z2.ordinal,modeZ2.selectedItemPosition)
            }
            ws.startUpdate()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showTemperatureDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.temperature_main_dialog)

        dialog.findViewById<TextView>(R.id.tempIntMin).text = indoorTemp[0]
        dialog.findViewById<TextView>(R.id.tempIntMax).text = indoorTemp[1]
        dialog.findViewById<TextView>(R.id.tempIntActual).text = indoorTemp[2]

        dialog.findViewById<TextView>(R.id.tempExtMin).text = outdoorTemp[0]
        dialog.findViewById<TextView>(R.id.tempExtMax).text = outdoorTemp[1]
        dialog.findViewById<TextView>(R.id.tempExtActual).text = outdoorTemp[2]

        dialog.show()
    }

    fun updateField(data: String) {
        println(data)
        when(data.split("=").first()) {
            getString(R.string.getZ1State) -> findViewById<TextView>(R.id.stateZ1).text = stateList[data.split("=").last().toInt()]
            getString(R.string.getZ2State) -> findViewById<TextView>(R.id.stateZ2).text = stateList[data.split("=").last().toInt()]
            getString(R.string.getZ1Mode) -> findViewById<TextView>(R.id.modeZ1).text = modeList[data.split("=").last().toInt()]
            getString(R.string.getZ2Mode) -> findViewById<TextView>(R.id.modeZ2).text = modeList[data.split("=").last().toInt()]
            getString(R.string.getZ1RemainingTime) -> findViewById<TextView>(R.id.timerZ1).text = toTime(data.split("=").last().toInt())
            getString(R.string.getZ2RemainingTime) -> findViewById<TextView>(R.id.timerZ2).text = toTime(data.split("=").last().toInt())
            getString(R.string.getIndoorTemp) -> {
                val temp = data.split("=").last().split(":")
                if (temp.count() == 3) {
                    val min = temp.first()
                    val max = temp[1]
                    val actual = temp.last()
                    indoorTemp = listOf(min, max, actual)
                    findViewById<TextView>(R.id.tempIntActual).text = actual
                }
            }
            getString(R.string.getOutdoorTemp) -> {
                val temp = data.split("=").last().split(":")
                if (temp.count() == 3) {
                    val min = temp.first()
                    val max = temp[1]
                    val actual = temp.last()
                    outdoorTemp = listOf(min, max, actual)
                    findViewById<TextView>(R.id.tempExtActual).text = actual
                }
            }
            getString(R.string.getDataEnergy).split(";").first() -> {
                val all = data.split("=").last().split("\r").toMutableList()
                all.removeAt(all.count() - 1)
                var daily = 0
                for (value in all) {
                    daily += value.split("|").last().toInt()
                }

                val dailyCons = (daily / 10.toDouble()).roundToInt() / 100.toDouble()
                val dailyCost = (daily / 10 * 0.1781).roundToInt() / 100.toDouble()
                findViewById<TextView>(R.id.dailyCons).text = dailyCons.toString()
                findViewById<TextView>(R.id.dailyCost).text = dailyCost.toString()
            }
        }
    }

    private fun toTime(second: Int): String {
        val h = second / 60 / 60
        val mn = second / 60 % 60

        return "${when (h) {
            in 0..9 -> "0$h"
            else -> h
        }}:${when (mn) {
            in 0..9 -> "0$mn"
            else -> mn
        }}"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 0) {
            if (data != null && data.hasExtra("Ip") && data.hasExtra("Port") && data.hasExtra("Password")) {
                val ip = data.extras?.getString("Ip") ?: String()
                val port = data.extras?.getString("Port") ?: "0"
                val password = data.extras?.getString("Password") ?: String()

                ws.connect(ip, port.toInt(), password)

                while (ws.isOpen()) {
                    if (ws.isReady()) {
                        break
                    }
                }
                if (!ws.isReady()) {
                    val result = when (ws.getLastError()) {
                        NetworkError.PasswordError.ordinal -> getString(R.string.wsPasswordError)
                        NetworkError.DataError.ordinal -> getString(R.string.wsDataError)
                        else -> ws.getLastError().toString()
                    }
                    Toast.makeText(this, result, Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, ConnectionActivity::class.java)
                    intent.putExtra("Path", this.filesDir.path)
                    intent.putExtra("FirstAttempt", false)
                    startActivityForResult(intent, 0)
                } else {
                    Toast.makeText(this, getString(R.string.connected), Toast.LENGTH_SHORT).show()
                    ws.startUpdate()
                }
            }
        }
    }

    override fun finish() {
        ws.disconnect()
        super.finish()
    }
}
