package com.gaurav.autosignalscanner
import android.Manifest
import android.content.*
import android.os.Bundle
import android.os.Build
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
class MainActivity:AppCompatActivity(){
 override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_main)
  if(Build.VERSION.SDK_INT>=33) ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.POST_NOTIFICATIONS),10)
  findViewById<Button>(R.id.start).setOnClickListener{ContextCompat.startForegroundService(this,Intent(this,ScannerService::class.java).setAction("START"));findViewById<TextView>(R.id.status).text="Scanner ON • background"}
  findViewById<Button>(R.id.stop).setOnClickListener{startService(Intent(this,ScannerService::class.java).setAction("STOP"));findViewById<TextView>(R.id.status).text="Scanner OFF"}}
 override fun onResume(){super.onResume();findViewById<TextView>(R.id.history).text=getSharedPreferences("scanner",0).getString("history","No signals yet")}
}