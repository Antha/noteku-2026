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
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import com.codeathome.todo.MyLibrary.colors
import com.codeathome.todo.R
import com.codeathome.todo.model.Data
import com.codeathome.todo.model.Item
import com.codeathome.todo.model.Todo
import com.codeathome.todo.network.ApiConfig
import com.codeathome.todo.ui.MainActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class AdapterCard(val activity: Activity, var data: List<Data>) :
    RecyclerView.Adapter<AdapterCard.Holder>() {
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTittleTodo = view.findViewById<TextView>(R.id.tv_title_todo)
        val btnAddItemCard = view.findViewById<Button>(R.id.btn_add_itemtodo)
        val btnEdit = view.findViewById<ImageView>(R.id.btn_edit_todo)
        val rvItemTodo = view.findViewById<RecyclerView>(R.id.rv_todo)
        val tvCreatedAtTodo = view.findViewById<TextView>(R.id.tv_createdAt_todo)
        val loAdapterItem = view.findViewById<LinearLayout>(R.id.lo_adapter_item)
    }

    fun updateData(newData: List<Data>) {
        data = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(
                R.layout.cardtodo_item,
                parent, false
            )
        return Holder(view)
    }

    @SuppressLint("ServiceCast")
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val strs =  data[position].created_at.split("T").toTypedArray()
        val time =  strs[1].split(".").toTypedArray()

        holder.loAdapterItem.setBackgroundColor(Color.parseColor(colors[data[position].color]))
        holder.tvTittleTodo.text = data[position].name
        holder.tvCreatedAtTodo.text = strs[0]+" "+time[0];

        val adapterItemCard = data[position].items.let { AdapterItemCard(activity, it) }
        holder.rvItemTodo.adapter = adapterItemCard

        val name = data[position].name
        val id = data[position].id

        holder.btnAddItemCard.setOnClickListener {
            val mDialog =
                LayoutInflater.from(activity).inflate(R.layout.dialog_newitemtodo, null)

            val edtDialog = mDialog.findViewById<EditText>(R.id.edt_dialog_newitem_todo)
            val btnSave = mDialog.findViewById<Button>(R.id.btn_simpan_dialog_newitem_todo)
            val btnCancel = mDialog.findViewById<Button>(R.id.btn_batal_dialog_newitem_todo)
            val btnDatePicker = mDialog.findViewById<DatePicker>(R.id.datePicker)
            val btnTimePicker = mDialog.findViewById<TimePicker>(R.id.timePicker)
            val spinnerOption: Spinner = mDialog.findViewById(R.id.spinner_option)
            val radioGroupRepeat: RadioGroup = mDialog.findViewById(R.id.radio_group_repeat)
            val gridDaysOfWeek: GridLayout = mDialog.findViewById(R.id.grid_days_of_week)
            val chkEnableVoice: CheckBox = mDialog.findViewById(R.id.chkEnableVoice)
            val chkEnableVibra: CheckBox = mDialog.findViewById(R.id.chkEnableVibra)
            val seekBar: SeekBar =  mDialog.findViewById(R.id.seekBarVolume)
            var tts: TextToSpeech? = null
            val audioManager = mDialog.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

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
                                    2000L
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

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, progress, 0)

                        val params = Bundle()
                        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f) // paksa full ratio
                        tts?.speak(edtDialog.text, TextToSpeech.QUEUE_FLUSH, params, "alarmID")
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            val titleView = LayoutInflater.from(activity).inflate(R.layout.dialog_title_add_item, null)
            val mAlertDialog = AlertDialog.Builder(activity)
                .setCustomTitle(titleView)
                .setView(mDialog)
                .create()

            mAlertDialog.show()

            mAlertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            btnCancel.setOnClickListener {
                mAlertDialog.dismiss()
            }

            btnSave.setOnClickListener {
                val namenew = edtDialog.text.toString()
                var month = btnDatePicker.month + 1
                val spinnerOptionPeriodType = spinnerOption.selectedItem.toString()
                val edtPeriod_Type = when(spinnerOptionPeriodType){
                    "Specific Time" -> "specific"
                    "Daily"   -> "daily"
                    else      -> ""
                }
                val datetimenew = when (spinnerOptionPeriodType) {
                    "Specific Time" -> "${btnDatePicker.year}-${month}-${btnDatePicker.dayOfMonth} ${btnTimePicker.hour}:${btnTimePicker.minute}"
                    "Daily"   -> "2000-01-01 ${btnTimePicker.hour}:${btnTimePicker.minute}"
                    else      -> ""
                }

                val edtVoiceVolume  = seekBar.progress

                val edtEnableVoice = when(chkEnableVoice.isChecked){
                    true -> 1
                    false -> 0
                }

                val edtEnableVibra = when(chkEnableVibra.isChecked){
                    true -> 1
                    false -> 0
                }

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

                ApiConfig.instanceRetrofit.postTodoItem(namenew,datetimenew,id,edtPeriod_Type, edtActiveDayText, edtEnableVoice, edtVoiceVolume, edtEnableVibra).enqueue(object : Callback<Item>{
                    override fun onResponse(call: Call<Item>, response: Response<Item>) {
                        Log.d("Respone",response.toString());
                        if (response.isSuccessful){
                            Toast.makeText(activity, "Item Berhasil diTambahkan", Toast.LENGTH_SHORT).show()
                            (activity as MainActivity).getAllTodos()
                        } else {
                            Toast.makeText(activity, "Item Gagal diTambahkan", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Item>, t: Throwable) {
                        (activity as MainActivity).getAllTodos()

                        Toast.makeText(activity, "Failure", Toast.LENGTH_SHORT).show()
                    }

                })

                mAlertDialog.dismiss()
            }

        }

        holder.btnEdit.setOnClickListener {
            val mDialog = LayoutInflater.from(activity).inflate(R.layout.dialog_edittodo, null)
            val edtDialog = mDialog.findViewById<EditText>(R.id.edt_dialog_edit_item_todo)
            val btnUpdate = mDialog.findViewById<Button>(R.id.btn_update_dialog_edit_item_todos)
            val btnBatal = mDialog.findViewById<Button>(R.id.btn_batal_dialog_edit_item_todos)
            val btnDelete = mDialog.findViewById<Button>(R.id.btn_hapus_dialog_edit_item_todos)
            val spinner: Spinner = mDialog.findViewById(R.id.spn_color_edit)

            val titleView = LayoutInflater.from(activity).inflate(R.layout.dialog_title_edit_todo, null)

            val mBuild = AlertDialog.Builder(activity)
                .setCustomTitle(titleView)
                .setView(mDialog)
                .create()

            mBuild.show()

            mBuild.window?.setBackgroundDrawableResource(android.R.color.transparent)

            // Create an ArrayAdapter using the string array and a default spinner layout
            ArrayAdapter.createFromResource(
                activity,
                R.array.planets_array,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                // Specify the layout to use when the list of choices appears
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                // Apply the adapter to the spinner
                spinner.adapter = adapter

                val spinnerPosition: Int = adapter.getPosition(data[position].color);
                spinner.setSelection(spinnerPosition);
            }

            edtDialog.setText(name)

            btnBatal.setOnClickListener {
                mBuild.dismiss()
            }

            btnUpdate.setOnClickListener {
                val edtname = edtDialog.text.toString()
                ApiConfig.instanceRetrofit.updateTodo(id,edtname,spinner.selectedItem.toString()).enqueue(object : Callback<Todo>{
                    override fun onResponse(call: Call<Todo>, response: Response<Todo>) {
                        if (response.isSuccessful){
                            Toast.makeText(activity, "Todo Berhasil di Update", Toast.LENGTH_SHORT).show()
                            (activity as MainActivity).getAllTodos()
                        } else{
                            Toast.makeText(activity, "Todo Gagal di Update", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Todo>, t: Throwable) {
                        (activity as MainActivity).getAllTodos()
                    }

                })
                mBuild.dismiss()
            }

            btnDelete.setOnClickListener {
                ApiConfig.instanceRetrofit.deletTodo(id).enqueue(object : Callback<Todo>{
                    override fun onResponse(call: Call<Todo>, response: Response<Todo>) {
                        if (response.isSuccessful){
                            Toast.makeText(activity, "Todo Berhasil di Hapus", Toast.LENGTH_SHORT).show()
                            (activity as MainActivity).getAllTodos()
                        } else{
                            Toast.makeText(activity, "Todo Gagal di Hapus", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Todo>, t: Throwable) {
                        (activity as MainActivity).getAllTodos()
                    }

                })
                mBuild.dismiss()
            }
        }
    }

    override fun getItemCount(): Int = data.size
}