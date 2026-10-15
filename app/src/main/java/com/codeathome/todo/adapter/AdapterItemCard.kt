package com.codeathome.todo.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.RecyclerView
import com.codeathome.todo.MyLibrary
import com.codeathome.todo.R
import com.codeathome.todo.model.Item
import com.codeathome.todo.network.ApiConfig
import com.codeathome.todo.ui.MainActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.speech.tts.TextToSpeech
import java.util.Locale

class
AdapterItemCard (var activity: Activity, var item: List<Item>):
    RecyclerView.Adapter<AdapterItemCard.Holder>(){
    class Holder (view: View): RecyclerView.ViewHolder(view){
        val tvActiveTime = view.findViewById<TextView>(R.id.tv_active_tIme);
        val tvTitleItem = view.findViewById<TextView>(R.id.name_itemtodo);
        val tvPeriodType = view.findViewById<TextView>(R.id.tv_period_type);
        val tvDayMon = view.findViewById<TextView>(R.id.tv_day_mon);
        val tvDayTue = view.findViewById<TextView>(R.id.tv_day_tue);
        val tvDayWed = view.findViewById<TextView>(R.id.tv_day_wed);
        val tvDayThu = view.findViewById<TextView>(R.id.tv_day_thu);
        val tvDayFri = view.findViewById<TextView>(R.id.tv_day_fri);
        val tvDaySat = view.findViewById<TextView>(R.id.tv_day_sat);
        val tvDaySun = view.findViewById<TextView>(R.id.tv_day_sun);
        val btnEditItem = view.findViewById<ImageView>(R.id.btn_edt_itemtodo)
        val ctnItem = view.findViewById<LinearLayout>(R.id.container_item)
        val stoActive = view.findViewById<Switch>(R.id.stoActive)
        val tvIsVibra = view.findViewById<ImageView>(R.id.tv_isvibra)
        val tvIsVoice = view.findViewById<ImageView>(R.id.tv_isvoice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.carditemtodo_item,
                parent, false)
        return AdapterItemCard.Holder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.tvTitleItem.text = item[position].name

        val id = item[position].id
        val name = item[position].name
        val active_time = item[position].active_time
        val period_type = item[position].period_type
        val active_day = item[position].active_day
        val is_voice = item[position].is_voice
        val voice_volume = item[position].voice_volume
        val is_vibra = item[position].is_vibra
        val is_active = item[position].is_active
        val strs =  active_time.split(" ").toTypedArray()
        val date =  strs[0].split("-").toTypedArray()
        val time =  strs[1].split(":").toTypedArray()
        val timeInMillis = MyLibrary.getCustomTime(date[0].toInt(), date[1].toInt(), date[2].toInt(), time[0].toInt(), time[1].toInt(),)

        when (period_type) {
            "specific" -> {
                holder.tvPeriodType.setBackgroundResource(R.drawable.bg_badge_spesific)
                holder.tvPeriodType.setText("spesific time")
                holder.tvActiveTime.text = item[position].active_time
            }
            "daily" -> {
                holder.tvPeriodType.setBackgroundResource(R.drawable.bg_badge_daily)
                holder.tvPeriodType.setText(period_type)
                holder.tvActiveTime.text = strs[1].toString()
            }
        }

        holder.tvDayMon.visibility = View.GONE
        holder.tvDayTue.visibility = View.GONE
        holder.tvDayWed.visibility = View.GONE
        holder.tvDayThu.visibility = View.GONE
        holder.tvDayFri.visibility = View.GONE
        holder.tvDaySat.visibility = View.GONE
        holder.tvDaySun.visibility = View.GONE

        if(period_type != "daily") {
            if (timeInMillis < System.currentTimeMillis()) {
                holder.ctnItem.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.gray_light)
                )
                holder.stoActive.visibility = View.INVISIBLE
            }
        }else{
            holder.ctnItem.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.white)
            )
            holder.stoActive.visibility = View.VISIBLE

            if(!active_day.isNullOrEmpty()  && active_day != "-") {
                holder.tvPeriodType.text = "spesific day"
                val activeDays = active_day.split(",").map { it.trim().lowercase() }.toSet()

                val dayViews: Map<String, TextView> = mapOf(
                    "mon" to holder.tvDayMon,
                    "tue" to holder.tvDayTue,
                    "wed" to holder.tvDayWed,
                    "thu" to holder.tvDayThu,
                    "fri" to holder.tvDayFri,
                    "sat" to holder.tvDaySat,
                    "sun" to holder.tvDaySun
                )

                dayViews.forEach { (day, view) ->
                    view.visibility =
                        if (day in activeDays) View.VISIBLE else View.GONE
                }
            }
        }

        holder.tvIsVibra.visibility = View.GONE
        holder.tvIsVoice.visibility = View.GONE
        if(is_voice == 1){
            holder.tvIsVoice.visibility = View.VISIBLE
        }
        if(is_vibra == 1){
            holder.tvIsVibra.visibility = View.VISIBLE
        }

        holder.stoActive.setOnCheckedChangeListener(null)
        holder.stoActive.isChecked = is_active == 1
        if (is_active == 1) {
            holder.tvTitleItem.setTextColor(Color.BLACK)
        } else {
            holder.tvTitleItem.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.gray_super_light)
            )
        }

        holder.stoActive.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                println("Switch ON")
                holder.tvTitleItem.setTextColor(Color.BLACK)
            } else {
                println("Switch OFF")
                holder.tvTitleItem.setTextColor(Color.parseColor("#CACACA"))
            }

            val edtIsActive = when(isChecked){
                true -> 1
                false -> 0
            }

            val toastMessage = if (isChecked) {
                "Activity has been activated"
            } else {
                "Activity has been deactivated"
            }


            ApiConfig.instanceRetrofit.updateTodoItemIsActive(id,edtIsActive).enqueue(object : Callback<Item> {
                override fun onResponse(call: Call<Item>, response: Response<Item>) {
                    if (response.isSuccessful){
                        Toast.makeText(activity, toastMessage, Toast.LENGTH_SHORT).show()
                        (activity as MainActivity).getAllTodos()
                    } else{
                        val errorMsg = response.errorBody()?.string() ?: "Item Gagal di Update"
                        Log.d("Gagal update: ", errorMsg);
                    }
                }

                override fun onFailure(call: Call<Item>, t: Throwable) {
                    (activity as MainActivity).getAllTodos()
                }

            })
        }

        holder.btnEditItem.setOnClickListener {
            val mDialog = LayoutInflater.from(activity).inflate(R.layout.dialog_edititemtodo, null)
            val edtDialog = mDialog.findViewById<EditText>(R.id.edt_dialog_edit_item_todo)
            val btnUpdate = mDialog.findViewById<Button>(R.id.btn_update_dialog_edit_item_todos)
            val btnBatal = mDialog.findViewById<Button>(R.id.btn_batal_dialog_edit_item_todos)
            val btnDelete = mDialog.findViewById<Button>(R.id.btn_hapus_dialog_edit_item_todos)
            val btnDatePicker = mDialog.findViewById<DatePicker>(R.id.datePicker_editItem)
            val btnTimePicker = mDialog.findViewById<TimePicker>(R.id.timePicker_editItem)
            val spinnerOption: Spinner = mDialog.findViewById(R.id.spinner_option)
            val radioGroupRepeat: RadioGroup = mDialog.findViewById(R.id.radio_group_repeat)
            val gridDaysOfWeek: GridLayout = mDialog.findViewById(R.id.grid_days_of_week)
            val chkEnableVoice: CheckBox = mDialog.findViewById(R.id.chkEnableVoice)
            val chkEnableVibra: CheckBox = mDialog.findViewById(R.id.chkEnableVibra)
            val seekBar: SeekBar =  mDialog.findViewById(R.id.seekBarVolume)
            var tts: TextToSpeech? = null
            val audioManager = mDialog.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            spinnerOption.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    pos: Int,
                    id: Long
                ) {
                    val selectedItem = parent.getItemAtPosition(pos).toString()

                    if (selectedItem == "Daily") {
                        btnDatePicker.visibility = View.GONE
                        radioGroupRepeat.visibility = View.VISIBLE

                        val selectedId = radioGroupRepeat.checkedRadioButtonId
                        when (selectedId) {
                            R.id.radioDaily -> {
                                gridDaysOfWeek.visibility = View.GONE
                            }
                            R.id.radioSpecificDay -> {
                                gridDaysOfWeek.visibility = View.VISIBLE
                            }
                            else -> {
                            }
                        }

                    }else if (selectedItem == "Specific Time") {
                        btnDatePicker.visibility = View.VISIBLE
                        radioGroupRepeat.visibility = View.GONE
                        gridDaysOfWeek.visibility = View.GONE
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            radioGroupRepeat.setOnCheckedChangeListener { group, checkedId ->
                when (checkedId) {
                    R.id.radioDaily -> {
                        gridDaysOfWeek.visibility = View.GONE
                    }
                    R.id.radioSpecificDay -> {
                        gridDaysOfWeek.visibility = View.VISIBLE
                    }
                }
            }

            if(!active_day.isNullOrEmpty() && active_day != "-") {
                radioGroupRepeat.check(R.id.radioSpecificDay)
                val days = active_day?.split(",")?.map { it.trim().lowercase() } ?: emptyList()
                val gridDays = mDialog.findViewById<GridLayout>(R.id.grid_days_of_week)
                for (i in 0 until gridDays.childCount) {
                    val checkBox = gridDays.getChildAt(i) as? CheckBox
                    checkBox?.let {
                        val dayText = it.text.toString().lowercase()
                        it.isChecked = days.contains(dayText)
                    }
                }
            }

            chkEnableVoice.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    seekBar.visibility = View.VISIBLE
                } else {
                    seekBar.visibility = View.GONE
                }
            }

            chkEnableVibra.setOnCheckedChangeListener { _, isChecked ->
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager =
                        mDialog.context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    mDialog.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                if (isChecked) {
                    println("vibra checked")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(
                                longArrayOf(
                                    0L,
                                    1500L
                                ),
                                -1
                            )
                        )
                    }
                }else{
                    vibrator.cancel()
                }
            }


            tts = TextToSpeech(activity) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale("id", "ID")
                }
            }

            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            tts?.setAudioAttributes(audioAttrs)

            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            seekBar.max = maxVolume
            seekBar.progress = voice_volume

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, progress, 0)

                        val params = Bundle()
                        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f) // paksa full ratio
                        tts?.speak(edtDialog.text, TextToSpeech.QUEUE_FLUSH, params, "alarmID")
                        //println("progress volume : "+progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            val titleView = LayoutInflater.from(activity).inflate(R.layout.dialog_title_edit_item, null)
            val mBuild = AlertDialog.Builder(activity)
                .setCustomTitle(titleView)
                .setView(mDialog)
                .setTitle("Edit Item")

            val mAlertDialog = mBuild.show()

            mAlertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            edtDialog.setText(name)

            val strs =  active_time.split(" ").toTypedArray()
            val strs_date =  strs[0].split("-").toTypedArray()
            val strs_time =  strs[1].split(":").toTypedArray()

            if(period_type == "specific"){
                btnDatePicker.init(strs_date[0].toInt(),strs_date[1].toInt() - 1, strs_date[2].toInt())
                { view, year, month, day -> }
            }

            btnTimePicker.hour = strs_time[0].toInt()
            btnTimePicker.minute = strs_time[1].toInt()

            spinnerOption.setSelection(
                when (period_type) {
                    "specific" -> 1
                    "daily"   -> 0
                    else      -> 2
                }
            )

            chkEnableVoice.isChecked = is_voice == 1
            chkEnableVibra.isChecked = is_vibra == 1

            btnUpdate.setOnClickListener {
                val edtname = edtDialog.text.toString()
                val spinnerOptionPeriodType = spinnerOption.selectedItem.toString()
                val edtPeriod_Type = when(spinnerOptionPeriodType){
                    "Specific Time" -> "specific"
                    "Daily"   -> "daily"
                    else      -> ""
                }
                var month = btnDatePicker.month + 1
                val datetimenew = when (spinnerOptionPeriodType) {
                    "Specific Time" -> "${btnDatePicker.year}-${month}-${btnDatePicker.dayOfMonth} ${btnTimePicker.hour}:${btnTimePicker.minute}"
                    "Daily"   -> "2000-01-01 ${btnTimePicker.hour}:${btnTimePicker.minute}"
                    else      -> ""
                }

                val edtEnableVoice = when(chkEnableVoice.isChecked){
                    true -> 1
                    false -> 0
                }

                val edtEnableVibra = when(chkEnableVibra.isChecked){
                    true -> 1
                    false -> 0
                }

                val edtVoiceVolume  = seekBar.progress

                var edtActiveDayText: String? = null
                if(spinnerOption.selectedItemPosition == 0){
                    when (radioGroupRepeat.checkedRadioButtonId) {
                        R.id.radioDaily -> {
                            edtActiveDayText = "-"
                        }
                        R.id.radioSpecificDay -> {
                            val gridDays = mDialog.findViewById<GridLayout>(R.id.grid_days_of_week)
                            val selectedDays = mutableListOf<String>()
                            for (i in 0 until gridDays.childCount) {
                                val checkBox = gridDays.getChildAt(i) as? CheckBox
                                if (checkBox?.isChecked == true) {
                                    selectedDays.add(checkBox.text.toString())
                                }
                            }

                            edtActiveDayText = if (selectedDays.isNotEmpty()) {
                                selectedDays.joinToString(",")
                            } else {
                                "(NULL)"
                            }
                        }
                    }
                }else{
                    edtActiveDayText = "-"
                }

                Log.d("edtPeriod_Type", edtPeriod_Type)
                Log.d("datetimenew", datetimenew)
                println("seekBar.progress "+seekBar.progress)
                println("edtActiveDayText "+edtActiveDayText)

                ApiConfig.instanceRetrofit.updateTodoItem(id,datetimenew,edtname,edtPeriod_Type,
                    edtActiveDayText,edtEnableVoice,edtVoiceVolume,edtEnableVibra).enqueue(object : Callback<Item> {
                    override fun onResponse(call: Call<Item>, response: Response<Item>) {
                        if (response.isSuccessful){
                            Toast.makeText(activity, "Item Berhasil di Update", Toast.LENGTH_SHORT).show()
                            (activity as MainActivity).getAllTodos()
                        } else{
                            val errorMsg = response.errorBody()?.string() ?: "Item Gagal di Update"
                            Log.d("Gagal update: ", errorMsg);
                        }
                    }

                    override fun onFailure(call: Call<Item>, t: Throwable) {
                        (activity as MainActivity).getAllTodos()
                    }

                })
                mAlertDialog.dismiss()
            }

            btnDelete.setOnClickListener {
                if(!(period_type != "daily" && timeInMillis < System.currentTimeMillis())){
                    if (is_active == 1) {
                        Toast.makeText(
                            activity,
                            "Please deactivate the reminder before deleting.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                }

                ApiConfig.instanceRetrofit.deleteTodoItem(id).enqueue(object : Callback<Item> {
                    override fun onResponse(call: Call<Item>, response: Response<Item>) {
                        if (response.isSuccessful) {
                            Toast.makeText(
                                activity,
                                "Item deleted successfully",
                                Toast.LENGTH_SHORT
                            ).show()

                            (activity as MainActivity).getAllTodos()
                        } else {
                            Toast.makeText(
                                activity,
                                "Failed to delete item",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<Item>, t: Throwable) {
                        (activity as MainActivity).getAllTodos()
                    }

                })
                mAlertDialog.dismiss()
            }

            btnBatal.setOnClickListener{
                mAlertDialog.dismiss()
            }
        }
    }

    override fun getItemCount(): Int = item.size
}