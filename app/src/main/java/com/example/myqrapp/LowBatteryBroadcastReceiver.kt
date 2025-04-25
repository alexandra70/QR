package com.example.myqrapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.widget.Toast

class LowBatteryBroadcastReceiver : BroadcastReceiver() {

    var batteryStatus: Boolean = true

    override fun onReceive(context: Context, intent: Intent?) {

        if(intent != null && intent.action == Intent.ACTION_BATTERY_CHANGED) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            var batteryPct = level / scale.toFloat() * 100

            if (batteryPct < 15) {
                Toast.makeText(
                    context,
                    "battery bellow 15%",
                    Toast.LENGTH_SHORT
                ).show()

                batteryStatus = false

            } else {
                Toast.makeText(
                    context,
                    "battery over 15%",
                    Toast.LENGTH_SHORT
                ).show()

                batteryStatus = true
            }
        }
    }
}