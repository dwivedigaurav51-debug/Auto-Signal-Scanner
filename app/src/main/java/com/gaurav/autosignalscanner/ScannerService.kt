package com.gaurav.autosignalscanner
import android.app.*
import android.content.*
import android.os.*
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
class ScannerService:Service(){
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO);private val client=OkHttpClient();private val pending=mutableListOf<P>();private var loopStarted=false
 data class P(val pair:String,val dir:String,val entry:Double,val due:Long,val entryMs:Long,val time:String)
 override fun onCreate(){super.onCreate();channel()}
 override fun onStartCommand(i:Intent?,f:Int,id:Int):Int{
  val sp=getSharedPreferences("scanner",0)
  if(i?.action=="STOP"){sp.edit().putBoolean("running",false).putString("lastReason","Scanner stopped by user").apply();scope.cancel();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY}
  startForeground(1,n("Scanner चालू है","Health monitor active • strong setup का इंतज़ार…"));sp.edit().putBoolean("running",true).putLong("heartbeat",System.currentTimeMillis()).apply()
  if(i?.action=="TEST"){notifyUser(999,"DEMO • EUR/USD • UP • 2 MIN","TEST SIGNAL ONLY — real trade नहीं");sp.edit().putString("lastReason","DEMO notification test passed").apply()}
  if(!loopStarted){loopStarted=true;scope.launch{loop()}};return START_STICKY
 }
 private suspend fun loop(){while(scope.isActive){val sp=getSharedPreferences("scanner",0);sp.edit().putLong("heartbeat",System.currentTimeMillis()).apply();try{scan();verify()}catch(e:Exception){sp.edit().putString("lastReason","Feed/error: "+(e.message?:"unknown")).apply()};delay(15000)}}
 private fun get(url:String):JSONObject?{val r=client.newCall(Request.Builder().url(url).build()).execute();val s=r.body?.string();r.close();return if(s!=null)JSONObject(s)else null}
 private fun scan(){
  val sp=getSharedPreferences("scanner",0);val d=get("https://audusd-2-min-signal.hatchable.site/api/best-signal")
  if(d==null){sp.edit().putString("lastReason","No data returned by feed").apply();return}
  val pair=d.optString("pair","—");val entry=d.optDouble("entryPrice",Double.NaN);val sig=d.optString("signal","NONE");val now=System.currentTimeMillis()
  sp.edit().putLong("heartbeat",now).putString("lastScan",fmt(now)).putInt("scanCount",sp.getInt("scanCount",0)+1).putString("lastPair",pair).putString("lastPrice",if(entry.isNaN())"—" else entry.toString()).apply()
  if(sig!="UP"&&sig!="DOWN"){sp.edit().putString("lastReason","No signal: feed returned "+sig).apply();return}
  if(entry.isNaN()){sp.edit().putString("lastReason","Signal rejected: entry price missing").apply();return}
  val t=fmt(now);val key=pair+"-"+sig+"-"+(now/60000);if(sp.getString("last","")==key){sp.edit().putString("lastReason","Duplicate "+sig+" signal ignored").apply();return}
  sp.edit().putString("last",key).putString("lastReason","REAL signal accepted: "+pair+" "+sig+" • score "+d.optInt("score")).apply();pending.add(P(pair,sig,entry,now+120000,now,t));notifyUser(100+pending.size,pair+" • "+sig+" • 2 MIN","Entry "+t+" • Market "+entry+" • setup "+d.optInt("score"))
 }
 private fun verify(){val now=System.currentTimeMillis();val it=pending.iterator();while(it.hasNext()){val p=it.next();if(now<p.due)continue;val u="https://audusd-2-min-signal.hatchable.site/api/result?pair="+java.net.URLEncoder.encode(p.pair,"UTF-8")+"&signal="+p.dir+"&entry="+p.entry;val d=get(u);val res=d?.optString("result","PENDING")?:"PENDING";if(res=="PENDING")continue;val exit=d?.optDouble("exitPrice",Double.NaN)?:Double.NaN;val sp=getSharedPreferences("scanner",0);if(res=="WIN")sp.edit().putInt("mwin",sp.getInt("mwin",0)+1).apply() else if(res=="LOSS")sp.edit().putInt("mloss",sp.getInt("mloss",0)+1).apply();sp.edit().putInt("opending",sp.getInt("opending",0)+1).apply();val exitT=fmt(now);val old=sp.getString("history","")?:"";val marketPrices="Market "+p.entry+" → "+(if(exit.isNaN())"?" else exit.toString());val line=p.time+" → "+exitT+" • "+p.pair+" "+p.dir+"\n"+marketPrices+" • "+res+"\nOlymptrade: awaiting verified feed • difference: —";sp.edit().putString("history",line+"\n\n"+old).apply();notifyUser(500+line.hashCode(),p.pair+" • "+p.dir+" • Market "+res,"Exit "+exitT+" • Olymptrade audit pending");it.remove()}}
 private fun fmt(ms:Long)=SimpleDateFormat("hh:mm:ss a",Locale.getDefault()).format(Date(ms))
 private fun channel(){if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("scan","Signal Scanner",NotificationManager.IMPORTANCE_HIGH))}
 private fun n(t:String,x:String)=NotificationCompat.Builder(this,"scan").setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(t).setContentText(x).setPriority(NotificationCompat.PRIORITY_HIGH).setOngoing(t.contains("चालू")).build()
 private fun notifyUser(id:Int,t:String,x:String)=getSystemService(NotificationManager::class.java).notify(id,n(t,x))
 override fun onBind(i:Intent?)=null
}