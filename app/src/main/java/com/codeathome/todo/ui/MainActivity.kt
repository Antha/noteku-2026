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


class MainActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {
    private lateinit var adapter: AdapterCard
    private lateinit var rvParent: RecyclerView
    private lateinit var swPlayout: SwipeRefreshLayout
    private lateinit var btnAdd: FloatingActionButton

    lateinit var machine_id: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

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

            val titleView = layoutInflater.inflate(R.layout.dialog_title, null)

            val mBuild = AlertDialog.Builder(this)
                .setCustomTitle(titleView)
                .setView(mDialog)
                .create()

            mBuild.show()

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

                    if (::adapter.isInitialized) {
                        adapter.updateData(newData)
                    } else {
                        adapter = AdapterCard(this@MainActivity, newData)
                        rvParent.adapter = adapter
                    }

                    swPlayout.isRefreshing = false

                    // loop autoScheduleNotification tetap jalan
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
                                    j.is_voice,
                                    j.is_vibra
                                )
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
        isVoice: Int,
        isVibra: Int
    ) {
        val intent = Intent(applicationContext, Notification::class.java).apply {
            putExtra(titleExtra, title)
            putExtra(messageExtra, message)
            putExtra(isVoiceExtra, isVoice)
            putExtra(isVibraExtra, isVibra)
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