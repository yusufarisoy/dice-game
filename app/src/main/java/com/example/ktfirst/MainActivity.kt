package com.example.ktfirst

import android.app.Dialog
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.*
import kotlin.math.sqrt

private const val THRESHOLD = 2.7f
private const val SLOP_TIME = 700
private const val RESET_TIME = 3000

class MainActivity : AppCompatActivity(), SensorEventListener {

    //Accelerometer
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var onShakeListener: OnShakeListener
    private var shakeTimeStamp = 0L
    private var shakeCount = 0
    //Views
    private lateinit var imageViewMain: ImageView
    private lateinit var dice1: ImageView
    private lateinit var dice2: ImageView
    private lateinit var dice3: ImageView
    private lateinit var buttonReplay: Button
    //Variables
    private var isGameFinished = false
    private val dices = ArrayList<ImageView>()
    private var diceHistory = ArrayList<Int>()
    private var lastDice: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initVariables()
    }

    private fun initViews() {
        imageViewMain = findViewById(R.id.imageViewDiceMain)
        dice1 = findViewById(R.id.imageViewDice1)
        dice2 = findViewById(R.id.imageViewDice2)
        dice3 = findViewById(R.id.imageViewDice3)
        dices.add(dice1)
        dices.add(dice2)
        dices.add(dice3)
        buttonReplay = findViewById(R.id.buttonReplay)
        buttonReplay.setOnClickListener {
            recreate()
        }
    }

    private fun initVariables() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        onShakeListener = object : OnShakeListener {
            override fun onShake(count: Int) {
                if (!isGameFinished) {
                    throwDice()
                }
            }
        }
    }

    private fun throwDice() {
        imageViewMain.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
        lastDice?.let {
            diceHistory = addAndShiftArray(diceHistory, it)
            for (i in diceHistory.indices) {
                dices[i].setImageResource(diceHistory[i])
            }
        }
        val diceImage = when((1..6).random()) {
            1 -> R.drawable.dice1
            2 -> R.drawable.dice2
            3 -> R.drawable.dice3
            4 -> R.drawable.dice4
            5 -> R.drawable.dice5
            else -> R.drawable.dice6
        }

        lastDice = diceImage
        imageViewMain.setImageResource(diceImage)
        if (diceHistory.size > 0 && lastDice == diceHistory[0]) {
            finishGame(lastDice!!)
        }
    }

    private fun addAndShiftArray(oldArray: ArrayList<Int>, newDice: Int): ArrayList<Int> {
        val newArray = ArrayList<Int>()
        newArray.add(newDice)
        val size = when (oldArray.size) {
            1 -> oldArray.size
            2 -> oldArray.size
            else -> oldArray.size - 1
        }
        for (i in 0 until size) {
            newArray.add(oldArray[i])
        }

        return newArray
    }

    private fun finishGame(dice: Int) {
        isGameFinished = true

        showFinishDialog(dice)
    }

    private fun showFinishDialog(dice: Int) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_layout)
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        val positiveButton: Button = dialog.findViewById(R.id.buttonReplay)
        val negativeButton: Button = dialog.findViewById(R.id.buttonCancel)
        val winningDice1: ImageView = dialog.findViewById(R.id.imageViewWinningDice1)
        val winningDice2: ImageView = dialog.findViewById(R.id.imageViewWinningDice2)
        winningDice1.setImageResource(dice)
        winningDice2.setImageResource(dice)
        positiveButton.setOnClickListener {
            recreate()
        }
        negativeButton.setOnClickListener {
            dialog.dismiss()
            buttonReplay.visibility = View.VISIBLE
        }
        dialog.setOnDismissListener {
            buttonReplay.visibility = View.VISIBLE
        }
        dialog.show()
    }

    //Accelerometer
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val gX = (event.values[0] / SensorManager.GRAVITY_EARTH).toDouble()
            val gY = (event.values[1] / SensorManager.GRAVITY_EARTH).toDouble()
            val gZ = (event.values[2] / SensorManager.GRAVITY_EARTH).toDouble()

            val gForce = sqrt(gX * gX + gY * gY + gZ * gZ).toFloat()
            if (gForce > THRESHOLD) {
                val now = System.currentTimeMillis()
                if (shakeTimeStamp + SLOP_TIME > now) {
                    return
                }; if(shakeTimeStamp + RESET_TIME < now) {
                    shakeCount = 0
                }

                shakeTimeStamp = now
                shakeCount++
                onShakeListener.onShake(shakeCount)
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) { }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        super.onPause()
    }
}