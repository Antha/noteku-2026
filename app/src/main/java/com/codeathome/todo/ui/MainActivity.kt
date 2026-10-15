package com.codeathome.todo.ui

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.codeathome.todo.*
import com.codeathome.todo.MyLibrary.getCustomTime
import com.codeathome.todo.Notification
import com.codeathome.todo.adapter.AdapterCard
import com.codeathome.todo.model.Todo
import com.codeathome.todo.network.ApiConfig
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import com.codeathome.todo.R
import com.codeathome.todo.model.TodoPost
import android.Manifest
import android.os.CountDownTimer
import android.view.WindowManager
import androidx.core.view.WindowCompat
import com.codeathome.todo.helper.AlarmUtils.getNextDayTriggerTime
import com.codeathome.todo.model.Item
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {
    private lateinit var adapter: AdapterCard
    private lateinit var rvParent: RecyclerView
    private lateinit var swPlayout: SwipeRefreshLayout
    private lateinit var btnAdd: FloatingActionButton
    private lateinit var tvNearestActivity: TextView
    private lateinit var tvNearestTimer: TextView
    private var countDownTimer: CountDownTimer? = null

    lateinit var machine_id: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.purple_200)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController?.isAppearanceLightStatusBars = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100 // requestCode bebas
                )
            }
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        swPlayout = findViewById(R.id.swpRef)
        swPlayout.setOnRefreshListener(this)
        machine_id = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        createNotificationChannel()
        getAllTodos()

        Log.d("Device_ID",machine_id);
        btnAdd = findViewById(R.id.btn_fload_add)
        rvParent = findViewById(R.id.rv_parent)
        tvNearestActivity = findViewById(R.id.tv_nearest_activity)
        tvNearestTimer = findViewById(R.id.tv_nearest_timer)

        btnAdd.setOnClickListener {
            val mDialog = LayoutInflater.from(this).inflate(R.layout.dialog_newtodo, null)
            val edtDialog = mDialog.findViewById<EditText>(R.id.edt_simpan_todo)
            val btnSave = mDialog.findViewById<Button>(R.id.btn_simpantodo)
            val spinner = mDialog.findViewById<Spinner>(R.id.spn_color)
            // Create an ArrayAdapter using the string array and a default spinner layout
            ArrayAdapter.createFromResource(
                this,
                R.array.planets_array,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                // Specify the layout to use when the list of choices appears
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                // Apply the adapter to the spinner
                spinner.adapter = adapter
            }

            val titleView = layoutInflater.inflate(R.layout.dialog_title_add_todo, null)

            val mBuild = AlertDialog.Builder(this)
                .setCustomTitle(titleView)
                .setView(mDialog)
                .create()

            mBuild.show()

            mBuild.window?.setBackgroundDrawableResource(android.R.color.transparent)

            btnSave.setOnClickListener {
                val name = edtDialog.text.toString()

                Log.d("API_REQUEST", "name=$name, status=${spinner.selectedItem.toString()}, machine_id=$machine_id")
                ApiConfig.instanceRetrofit.postTodo(name,spinner.selectedItem.toString(),machine_id).enqueue(object : Callback<TodoPost>{
                    override fun onResponse(call: Call<TodoPost>, response: Response<TodoPost>) {
                        if (response.isSuccessful){
                            Toast.makeText(this@MainActivity, "Data Berhasil Ditambahkan", Toast.LENGTH_SHORT).show()
                            getAllTodos()
                        } else{
                            Toast.makeText(this@MainActivity, "Data Gagal Ditambahkan", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<TodoPost>, t: Throwable) {
                        Log.e("API_ERROR", "onFailure: ${t.message}", t)
                    }
                })

                mBuild.dismiss()
            }
        }

        val layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
        rvParent.layoutManager = layoutManager
    }

    fun displayView(){
        rvParent = findViewById(R.id.rv_parent)

        val layoutManger = LinearLayoutManager(this)
        layoutManger.orientation = LinearLayoutManager.HORIZONTAL

        rvParent.adapter = adapter
        rvParent.layoutManager = layoutManger
    }

    fun getAllTodos() {
        ApiConfig.instanceRetrofit.getAllTodo(machine_id).enqueue(object : Callback<Todo> {
            override fun onResponse(call: Call<Todo>, response: Response<Todo>) {
                if (response.isSuccessful) {
                    val newData = response.body()?.data.orEmpty()

                    val resultNewData = findNearestActivityWithDuration(
                        newData.flatMap { it.items }.filter { it.is_active == 1 }
                    )

                    tvNearestActivity.text = resultNewData?.first?.name ?: "No activity"

                    resultNewData?.second?.let { durationStr ->
                        countDownTimer?.cancel()

                        val parts = durationStr.split(":")
                        val hours = parts[0].toInt()
                        val minutes = parts[1].toInt()
                        val totalMillis = (hours * 60 + minutes) * 60 * 1000L

                        countDownTimer = object : CountDownTimer(totalMillis, 1000) {
                            override fun onTick(millisUntilFinished: Long) {
                                val h = millisUntilFinished / (1000 * 60 * 60)
                                val m = (millisUntilFinished / (1000 * 60)) % 60
                                val s = (millisUntilFinished / 1000) % 60
                                tvNearestTimer.text = String.format("%02d:%02d:%02d", h, m, s)
                            }

                            override fun onFinish() {
                                tvNearestTimer.text = "Time's up!"
                            }
                        }.start()
                    }

                    println("Aktivitas terdekat: ${resultNewData?.first?.name}, mulai dalam ${resultNewData?.second}")

                    if (::adapter.isInitialized) {
                        adapter.updateData(newData)
                    } else {
                        adapter = AdapterCard(this@MainActivity, newData)
                        rvParent.adapter = adapter
                    }

                    swPlayout.isRefreshing = false

                    // loop autoScheduleNotification
                    for (i in newData) {
                        for (j in i.items) {
                            val strs = j.active_time.split(" ").toTypedArray()
                            val date = strs[0].split("-").toTypedArray()
                            val time = strs[1].split(":").toTypedArray()

                            if(j.is_active == 0) {
                                cancelAlarm(this@MainActivity, j.id)
                            }else if(j.is_active == 1){
                                autoScheduleNotification(
                                    date[0].toInt(),
                                    date[1].toInt(),
                                    date[2].toInt(),
                                    time[0].toInt(),
                                    time[1].toInt(),
                                    j.id,
                                    i.name,
                                    j.name,
                                    j.period_type,
                                    j.active_day,
                                    j.is_voice,
                                    j.voice_volume,
                                    j.is_vibra,
                                )

                                println("-----------")
                                println(j.name)
                                println(j.active_day)
                                println("-----------")
                            }
                        }
                    }
                }
            }

            override fun onFailure(call: Call<Todo>, t: Throwable) {
                Log.d("TAG", "Gagal Get data Todo")
                swPlayout.isRefreshing = false
            }
        })
    }

    fun formatDuration(minutes: Long): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return String.format("%02d:%02d", hours, mins)
    }

    fun findNearestActivityWithDuration(items: List<Item>): Pair<Item?, String>? {
        val now = LocalDateTime.now()
        val formatterDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formatterTime = DateTimeFormatter.ofPattern("HH:mm:ss")

        val nearest = items.minByOrNull { item: Item ->
            val diff: Long = if (item.period_type == "daily") {
                val itemTime = LocalTime.parse(item.active_time.substring(11), DateTimeFormatter.ofPattern("HH:mm:ss"))
                val nowTime = LocalTime.now()
                Duration.between(nowTime, itemTime).toMinutes().let {
                    if (it < 0) it + 24 * 60 else it
                }
            } else {
                val itemDateTime = LocalDateTime.parse(item.active_time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                Duration.between(LocalDateTime.now(), itemDateTime).toMinutes().let {
                    if (it < 0) Long.MAX_VALUE else it
                }
            }
            diff
        }

        nearest?.let { item ->
            val diffMinutes = if (item.period_type == "daily") {
                val itemTime = LocalTime.parse(item.active_time.substring(11), formatterTime)
                val nowTime = now.toLocalTime()
                Duration.between(nowTime, itemTime).toMinutes().let {
                    if (it < 0) it + 24 * 60 else it
                }
            } else {
                val itemDateTime = LocalDateTime.parse(item.active_time, formatterDateTime)
                Duration.between(now, itemDateTime).toMinutes()
            }
            return item to formatDuration(diffMinutes)
        }
        return null
    }

    override fun onRefresh() {
        getAllTodos()
    }

    private fun autoScheduleNotification(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        idNotif: Int,
        title: String,
        message: String,
        period_type: String,
        active_day: String?,
        isVoice: Int,
        voice_volume: Int,
        isVibra: Int
    ) {
        val intent = Intent(applicationContext, Notification::class.java).apply {
            putExtra(titleExtra, title)
            putExtra(messageExtra, message)
            putExtra(isVoiceExtra, isVoice)
            putExtra(voiceVolumeExtra, voice_volume)
            putExtra(isVibraExtra, isVibra)
            putExtra(periodTypeExtra, period_type)
            putExtra(activeDayExtra, active_day)
            putExtra(yearExtra, year)
            putExtra(monthExtra, month)
            putExtra(dayExtra, day)
            putExtra(hourExtra, hour)
            putExtra(minutExtra, minute)
            putExtra("notificationID", idNotif)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            idNotif,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (period_type == "specific") {
            val time = getCustomTime(year, month, day, hour, minute)
            if (time >= System.currentTimeMillis()) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        time,
                        pendingIntent
                    )
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
        } else if (period_type == "daily") {
            if(!active_day.isNullOrEmpty() && active_day != "-"){
                val triggerTime = getNextDayTriggerTime(
                    hour,
                    minute,
                    active_day
                )

                if (triggerTime > 0) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            }else{
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }

                val isToday = calendar.timeInMillis > System.currentTimeMillis()

                if (!isToday) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }

    fun cancelAlarm(context: Context, requestCode: Int) {
        val intent = Intent(context, Notification::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("Alarm_Pending", "Alarm dengan requestCode $requestCode berhasil dibatalkan")
        } else {
            Log.d("Alarm_Pending", "Alarm dengan requestCode $requestCode tidak ditemukan")
        }
    }



    private fun createNotificationChannel() {
        val name = "Notif Channel"
        val desc = "A Description of The Channel"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelID, name, importance)
        channel.description = desc
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}