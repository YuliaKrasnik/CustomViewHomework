package com.android.homework.customviewspeedometer

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import com.android.homework.customviewspeedometer.customview.Speedometer

class MainActivity : AppCompatActivity() {
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val speedometer = findViewById<Speedometer>(R.id.speedometer)
        val btnGas = findViewById<Button>(R.id.btn_gas)

        btnGas.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    speedometer.increaseSpeed()
                }
                MotionEvent.ACTION_UP -> {
                    speedometer.reduceSpeed()
                }
            }
            return@setOnTouchListener true
        }
    }


}

