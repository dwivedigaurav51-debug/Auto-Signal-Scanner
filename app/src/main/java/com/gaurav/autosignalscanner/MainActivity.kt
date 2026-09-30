package com.gaurav.autosignalscanner
import android.Manifest
import android.content.*
import android.os.Bundle
import android.os.Build
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
 private val ui = Handler(Looper.getMainLooper())
 private val tick = object:Runnable{override fun run(){refresh();ui.postDelayed(this,2000)}}
 override fun onCreate(b: Bundle?) {
  super.onCreate(b); setContentView(R.layout.activity_main)
  if (Build.VERSION.SDK_INT >= 33) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
  findViewById<Button>(R.id.start).setOnClickListener { ContextCompat.startForegroundService(this, Intent(this, ScannerService::class.java).setAction("START")); refresh() }
  findViewById<Button>(R.id.stop).setOnClickListener { startService(Intent(this, ScannerService::class.java).setAction("STOP")); refresh() }
  findViewById<Button>(R.id.testSignal).setOnClickListener { ContextCompat.startForegroundService(this, Intent(this, ScannerService::class.java).setAction("TEST")); Toast.makeText(this,"DEMO test signal sent",Toast.LENGTH_SHORT).show() }
 }
 override fun onResume(){super.onResume();ui.post(tick)}
 override fun onPause(){ui.removeCallbacks(tick);super.onPause()}
 private fun refresh() {
  val sp=getSharedPreferences("scanner",0); val now=System.currentTimeMillis(); val hb=sp.getLong("heartbeat",0)
  val live=sp.getBoolean("running",false) && hb>0 && now-hb<45000
  findViewById<TextView>(R.id.status).text=if(live)"🟢 SCANNER LIVE" else "🔴 SCANNER OFF / NO HEARTBEAT"
  val lastScan=sp.getString("lastScan","Never"); val price=sp.getString("lastPrice","—"); val pair=sp.getString("lastPair","—")
  val candles=sp.getInt("scanCount",0); val reason=sp.getString("lastReason","Start scanner to begin diagnostics")
  findViewById<TextView>(R.id.health).text="Heartbeat: "+if(hb==0L)"—" else ((now-hb)/1000).toString()+" sec ago"+"
Last scan: "+lastScan+"
Feed: "+pair+" @ "+price+"
Scans received: "+candles+"
Signal engine: "+if(live)"RUNNING" else "STOPPED"+"
Last decision: "+reason
  findViewById<TextView>(R.id.history).text=sp.getString("history","No signals yet")
  findViewById<TextView>(R.id.marketStats).text="Wins "+sp.getInt("mwin",0)+" • Losses "+sp.getInt("mloss",0)
  findViewById<TextView>(R.id.olympStats).text="Wins "+sp.getInt("owin",0)+" • Losses "+sp.getInt("oloss",0)+" • Pending/Unavailable "+sp.getInt("opending",0)
 }
}