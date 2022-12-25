package com.raywenderlich.android.rwandroidtutorial

import android.Manifest.permission.ACTIVITY_RECOGNITION
import android.R.string
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MutableLiveData
import com.raywenderlich.android.runtracking.R
import com.raywenderlich.android.runtracking.databinding.ActivityInputBinding
import com.raywenderlich.android.runtracking.databinding.ActivityMainBinding
import com.vmadalin.easypermissions.EasyPermissions
import java.util.*

class MainActivity : AppCompatActivity(), SensorEventListener
    {
        private var initialStepCount = 0
        private var currentNumberOfStepCount =MutableLiveData(0)
        private var weight = 0f
        private var total_calories = 0f
        private var name : String=""
        private val timer = Timer()
        private lateinit var binding: ActivityMainBinding
        private lateinit var meninput: ActivityInputBinding
        lateinit var dataHelper: DataHelper

        companion object {
            private const val KEY_SHARED_PREFERENCE = "com.rwRunTrackingApp.sharedPreferences"
            private const val KEY_IS_TRACKING = "com.rwRunTrackingApp.isTracking"
            private const val REQUEST_CODE_FINE_LOCATION = 1
            private const val REQUEST_CODE_ACTIVITY_RECOGNITION = 2
        }

        private var isTracking: Boolean

        get() = this.getSharedPreferences(KEY_SHARED_PREFERENCE, Context.MODE_PRIVATE).getBoolean(KEY_IS_TRACKING, false)
        set(value) = this.getSharedPreferences(KEY_SHARED_PREFERENCE, Context.MODE_PRIVATE).edit().putBoolean(KEY_IS_TRACKING, value).apply()


        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityMainBinding.inflate(layoutInflater)
            meninput = ActivityInputBinding.inflate(layoutInflater)

            val menuutama = binding.root
            val gambarinput = meninput.root
            setContentView(gambarinput)
            dataHelper = DataHelper(applicationContext)

            binding.finishbutton.visibility = View.GONE
            binding.ssbtn.visibility = View.VISIBLE

            meninput.buttontomenuutama.setOnClickListener {
                weight = meninput.inputWeight.text.toString().toFloat()
                name = meninput.inputName.text.toString()
                binding.nameDisplay.text = name
                var test: Float? = null
                if (name.equals(""))
                {
                    AlertDialog.Builder(this)
                    .setTitle("Please Insert your Name")
                    .setPositiveButton("ok")
                    {
                            _, _ ->
                    }
                    .create()
                    .show()
                }
            else if(weight == test)
                {
                AlertDialog.Builder(this)
                    .setTitle("Please Insert your Weight")
                    .setPositiveButton("ok")
                    {
                            _, _ ->
                    }
                    .create()
                    .show()
                }
            else
            {
                setContentView(menuutama)
            }
        }
            binding.ssbtn.setOnClickListener{
                resetAction()
                updateAllDisplayText(0)
                isTracking = true
                updateButtonStatus()
                updateAllDisplayText(0)
                binding.totcal.text = "0"
                startTracking()
                startStopAction()
                binding.ssbtn.visibility = View.GONE
                binding.finishbutton.visibility = View.VISIBLE
            }

            binding.finishbutton.setOnClickListener { endButtonClicked() }

            updateButtonStatus()

            currentNumberOfStepCount.observe(this){
                it ?: return@observe
                val stepCount = currentNumberOfStepCount.value ?: 0
                updateAllDisplayText(stepCount)
            }
            if (isTracking)
            {
                startTracking()
            }
            if(dataHelper.timerCounting())
            {
                startTimer()
            }
            else
            {
                stopTimer()
                if(dataHelper.startTime() != null && dataHelper.stopTime() != null)
                {
                    val time = Date().time - calcRestartTime().time
                    binding.timeTV.text = timeStringFromLong(time)
                }
            }
            timer.scheduleAtFixedRate(TimeTask(), 0, 500)
        }

        //Timer
        private inner class TimeTask: TimerTask()
        {
            override fun run()
            {
                if(dataHelper.timerCounting())
                {
                    val time = Date().time - dataHelper.startTime()!!.time
                    binding.timeTV.text = timeStringFromLong(time)
                }
            }
        }

        private fun maketotaltime(hours: Long, minutes: Long, seconds: Long): String
        {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }

        private fun resetAction()
        {
            dataHelper.setStopTime(null)
            dataHelper.setstartTime(null)
            stopTimer()
            binding.timeTV.text = timeStringFromLong(0)
        }

        private fun stopTimer()
        {
            dataHelper.settimerCounting(false)
        }

        private fun startTimer()
        {
            dataHelper.settimerCounting(true)
        }

        private fun startStopAction()
        {
            if(dataHelper.timerCounting())
            {
                dataHelper.setStopTime(Date())
                stopTimer()
            }
            else
            {
                if(dataHelper.stopTime() != null)
                {
                    dataHelper.setstartTime(calcRestartTime())
                    dataHelper.setStopTime(null)
                }
                else
                {
                    dataHelper.setstartTime(Date())
                }
                startTimer()
            }
        }

        private fun calcRestartTime(): Date
        {
            val diff = dataHelper.startTime()!!.time - dataHelper.stopTime()!!.time
            return Date(System.currentTimeMillis() + diff)
        }

        private fun timeStringFromLong(ms: Long): String
        {
            val seconds = (ms / 1000) % 60
            val minutes = (ms / (1000 * 60) % 60)
            val hours = (ms / (1000 * 60 * 60) % 24)
            return makeTimeString(hours, minutes, seconds)
        }

        private fun makeTimeString(hours: Long, minutes: Long, seconds: Long): String
        {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)

        }
        //Timer

        //Step counter
        private fun updateButtonStatus()
        {
            binding.ssbtn.isEnabled = !isTracking
            binding.finishbutton.isEnabled = isTracking
        }

        private fun updateAllDisplayText(stepCount: Int)
        {
            binding.StepsCounter.text =  String.format("%d", stepCount)
        }

        private fun endButtonClicked()
        {
            AlertDialog.Builder(this)
                .setTitle("Are you sure to stop tracking?")
                .setPositiveButton("Confirm")
                { _, _ ->
                    isTracking = false
                    updateButtonStatus()
                    stopTracking()

                    dataHelper.settimerCounting(false)
                    //Perhitungan Calories
                    val waktu = (Date().time - dataHelper.startTime()!!.time).toFloat()/60000
                    total_calories = ((11.5*3.5*(weight/200))*waktu).toFloat()

                    binding.totcal.text = total_calories.toString()
                    binding.ssbtn.visibility = View.VISIBLE
                }
                .setNegativeButton("Cancel")
                {
                        _, _ ->
                }
                .create()
                .show()
        }

        @SuppressLint("CheckResult")
        private fun startTracking()
        {
            val isActivityRecognitionPermissionFree = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
            val isActivityRecognitionPermissionGranted = EasyPermissions.hasPermissions(this,
                ACTIVITY_RECOGNITION)
            Log.d("TAG", "Is ACTIVITY_RECOGNITION permission granted $isActivityRecognitionPermissionGranted")
            if (isActivityRecognitionPermissionFree || isActivityRecognitionPermissionGranted)
            {
                setupStepCounterListener()
            }
            else
            {
                EasyPermissions.requestPermissions(
                    host = this,
                    rationale = "For showing your step counts and calculate the average pace.",
                    requestCode = REQUEST_CODE_ACTIVITY_RECOGNITION,
                    perms = *arrayOf(ACTIVITY_RECOGNITION)
                )
            }
        }

        private fun setupStepCounterListener()
        {
            val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            stepCounterSensor ?: return
            sensorManager.registerListener(this@MainActivity, stepCounterSensor, SensorManager.SENSOR_DELAY_FASTEST)
        }

        private fun stopTracking()
        {
            val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            sensorManager.unregisterListener(this, stepCounterSensor)
        }

        override fun onSensorChanged(sensorEvent: SensorEvent?)
        {
            Log.d("TAG", "onSensorChanged")
            sensorEvent ?: return
            val firstSensorEvent = sensorEvent.values.firstOrNull() ?: return
            Log.d("TAG", "Steps count: $firstSensorEvent ")
            var isFirstStepCountRecord = currentNumberOfStepCount.value == 0
            if (isFirstStepCountRecord)
            {
                initialStepCount = firstSensorEvent.toInt()
                currentNumberOfStepCount.value= 1
            }
            else
            {
                currentNumberOfStepCount.value = firstSensorEvent.toInt() - initialStepCount
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int)
        {
            Log.d("TAG", "onAccuracyChanged: Sensor: $sensor; accuracy: $accuracy")
        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
        {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
        }
        //StepCounter
    }