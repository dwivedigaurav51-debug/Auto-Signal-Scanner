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
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO);private val client=OkHttpClient();private val pending=mutableListOf<P>()
 data class P(val pair:String,val dir:String,val entry:Double,val due:Long,val time:String)
 override fun onCreate(){super.onCreate();channel()}
 override fun onStartCommand(i:Intent?,f:Int,id:Int):Int{if(i?.action=="STOP"){scope.cancel();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY};startForeground(1,n("Scanner चालू है","Strong setup का इंतज़ार…"));scope.launch{loop()};return START_STICKY}
 private suspend fun loop(){while(isActive){try{scan();verify()}catch(_:Exception){};delay(15000)}}
 private fun get(url:String):JSONObject?{val r=client.newCall(Request.Builder().url(url).build()).execute();val s=r.body?.string();r.close();return if(s!=null)JSONObject(s)else null}
 private fun scan(){val d=get("https://audusd-2-min-signal.hatchable.site/api/best-signal")?:return;val sig=d.optString("signal");if(sig!="UP"&&sig!="DOWN")return;val pair=d.optString("pair");val entry=d.optDouble("entryPrice",Double.NaN);if(entry.isNaN())return;val nowMs=System.currentTimeMillis();val t=SimpleDateFormat("hh:mm:ss a",Locale.getDefault()).format(Date(nowMs));val key=pair+"-"+sig+"-"+(nowMs/60000);val sp=getSharedPreferences("scanner",0);if(sp.getString("last","")==key)return;sp.edit().putString("last",key).apply();pending.add(P(pair,sig,entry,nowMs+120000,t));notifyUser(100+pending.size,pair+" • "+sig+" • 2 MIN","Signal time "+t+" • setup "+d.optInt("score"))}
 private fun verify(){val nowMs=System.currentTimeMillis();val it=pending.iterator();while(it.hasNext()){val p=it.next();if(nowMs<p.due)continue;val u="https://audusd-2-min-signal.hatchable.site/api/result?pair="+java.net.URLEncoder.encode(p.pair,"UTF-8")+"&signal="+p.dir+"&entry="+p.entry;val d=get(u);val res=d?.optString("result","PENDING")?:"PENDING";if(res=="PENDING")continue;val sp=getSharedPreferences("scanner",0);val old=sp.getString("history","")?:"";val line=p.time+" • "+p.pair+" • "+p.dir+" • "+res;sp.edit().putString("history",line+"\n"+old).apply();notifyUser(500+line.hashCode(),line,"2-minute automatic result");it.remove()}}
 private fun channel(){if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("scan","Signal Scanner",NotificationManager.IMPORTANCE_HIGH))}
 private fun n(t:String,x:String)=NotificationCompat.Builder(this,"scan").setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(t).setContentText(x).setPriority(NotificationCompat.PRIORITY_HIGH).setOngoing(t.contains("चालू")).build()
 private fun notifyUser(id:Int,t:String,x:String)=getSystemService(NotificationManager::class.java).notify(id,n(t,x))
 override fun onBind(i:Intent?)=null
}