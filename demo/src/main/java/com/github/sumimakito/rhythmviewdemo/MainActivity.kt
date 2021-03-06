package com.github.sumimakito.rhythmviewdemo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.sumimakito.cappuccino.ContentHelper
import com.github.sumimakito.rhythmview.datasource.PlaybackSource
import com.github.sumimakito.rhythmview.effect.RainbowRay
import com.github.sumimakito.rhythmview.effect.Ray
import com.github.sumimakito.rhythmview.effect.Ripple
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.max
import kotlin.math.min


class MainActivity : AppCompatActivity() {
    companion object {
        const val PERMISSION_OPEN_MUSIC_REQUEST_CODE = 0xCEE
        const val PERMISSION_OPEN_IMAGE_REQUEST_CODE = 0xCED
        const val OPEN_MUSIC_REQUEST_CODE = 0xCEC
        const val OPEN_IMAGE_REQUEST_CODE = 0xCEA
    }

    private var dataSource: PlaybackSource? = null
    private var mediaPlayer: MediaPlayer? = null
    private var divisionValue: Int = 16
    private var waveSpeedValue: Float = 0.06f
    private var particleSpeedValue: Float = 0.005f
    private var colorH: Int = 0xFFFFFFFF.toInt()
    private var colorM: Int = 0xFFFFFFFF.toInt()
    private var colorL: Int = 0xFFFFFFFF.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("initial_help_shown", false)) {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("initial_help_shown", true).apply()
            val builder = AlertDialog.Builder(this)
            builder.setView(R.layout.view_help)
            builder.create().show()
        }

        rhythmView.showFpsCounter = true

        rhythmView.onRhythmViewLayoutChangedListener = { view ->
            if (view.albumCover == null) {
                val cover = BitmapFactory.decodeResource(resources, R.raw.cover)
                updateColor(cover)
                view.albumCover = cover
            }
            reloadVisualEffect()
        }

        helpButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setView(R.layout.view_help)
            builder.create().show()
        }

        settingsToggle.setOnClickListener {
            optionsInnerContainer.visibility = if (optionsInnerContainer.visibility == View.GONE) VISIBLE else GONE
        }

        openImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_OPEN_IMAGE_REQUEST_CODE)
            } else {
                openImageAndSetup()
            }
        }

        openMusic.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO), PERMISSION_OPEN_MUSIC_REQUEST_CODE)
            } else {
                openMusicAndSetup()
            }
        }

        viewOnGitHub.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/SumiMakito/RhythmView"))
            startActivity(browserIntent)
        }

        spinningSpeed.progress = (rhythmView.coverSpinningSpeed * 10).toInt() + 30
        spinningSpeedDisplay.text = "${rhythmView.coverSpinningSpeed}"
        spinningSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                rhythmView.coverSpinningSpeed = (progress - 30) / 10f
                spinningSpeedDisplay.text = "${rhythmView.coverSpinningSpeed}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        innerPadding.progress = (rhythmView.innerDrawingPaddingScale * 100).toInt()
        innerPaddingDisplay.text = "${rhythmView.innerDrawingPaddingScale}"
        innerPadding.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                rhythmView.innerDrawingPaddingScale = progress / 100f
                innerPaddingDisplay.text = "${rhythmView.innerDrawingPaddingScale}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        outerDrawingSize.progress = (rhythmView.maxDrawingWidthScale * 100).toInt()
        outerDrawingSizeDisplay.text = "${rhythmView.maxDrawingWidthScale}"
        outerDrawingSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                rhythmView.maxDrawingWidthScale = progress / 100f
                outerDrawingSizeDisplay.text = "${rhythmView.maxDrawingWidthScale}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        division.progress = divisionValue - 4
        divisionDisplay.text = "$divisionValue"
        division.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                divisionValue = progress + 4
                divisionDisplay.text = "$divisionValue"
                reloadVisualEffect()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        waveSpeed.progress = (waveSpeedValue * 1000).toInt()
        waveSpeedDisplay.text = "$waveSpeedValue"
        waveSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                waveSpeedValue = progress / 1000f
                waveSpeedDisplay.text = "$waveSpeedValue"
                reloadVisualEffect()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        particleSpeed.progress = (particleSpeedValue * 1000).toInt() - 5
        particleSpeedDisplay.text = "$particleSpeedValue"
        particleSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                particleSpeedValue = (progress + 5) / 1000f
                particleSpeedDisplay.text = "$particleSpeedValue"
                reloadVisualEffect()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        visualEffect.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioButtonRipple -> {
                    divisionValue = 16
                    reloadVisualEffect()
                    waveSpeedValue = 0.04f
                    particleSpeedValue = 0.005f
                    waveSpeed.progress = (waveSpeedValue * 1000).toInt()
                    division.max = 12
                    division.progress = divisionValue - 4
                    waveSpeedDisplay.text = "$waveSpeedValue"
                    divisionDisplay.text = "$divisionValue"
                    particleSpeed.progress = (particleSpeedValue * 1000).toInt() - 5
                    particleSpeedDisplay.text = "$particleSpeedValue"
                    particleSpeed.isEnabled = true
                }
                R.id.radioButtonRay -> {
                    divisionValue = 256
                    waveSpeedValue = 0.04f
                    waveSpeed.progress = (waveSpeedValue * 1000).toInt()
                    division.max = 252
                    division.progress = divisionValue - 4
                    waveSpeedDisplay.text = "$waveSpeedValue"
                    divisionDisplay.text = "$divisionValue"
                    particleSpeed.isEnabled = false
                }
                R.id.radioButtonRainbowRay -> {
                    divisionValue = 360
                    waveSpeedValue = 0.04f
                    waveSpeed.progress = (waveSpeedValue * 1000).toInt()
                    division.max = 356
                    division.progress = divisionValue - 4
                    waveSpeedDisplay.text = "$waveSpeedValue"
                    divisionDisplay.text = "$divisionValue"
                    particleSpeed.isEnabled = false
                }
            }
            reloadVisualEffect()
        }
    }

    private fun openMusicAndSetup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "audio/*"
        startActivityForResult(intent, OPEN_MUSIC_REQUEST_CODE);
    }

    private fun openImageAndSetup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        startActivityForResult(intent, OPEN_IMAGE_REQUEST_CODE);
    }

    private fun updateColor(bitmap: Bitmap) {
        colorM = getDominantColor(bitmap)
        val hsvL = FloatArray(3)
        val hsvM = FloatArray(3)
        val hsvD = FloatArray(3)
        Color.colorToHSV(colorM, hsvL)
        Color.colorToHSV(colorM, hsvM)
        Color.colorToHSV(colorM, hsvD)
        hsvL[1] = max(0.2f, min(0.3f, hsvL[1] * 0.20f))
        hsvM[1] = max(0.2f, min(0.3f, hsvM[1] * 0.20f))
        hsvD[1] = max(0.2f, min(0.3f, hsvD[1] * 0.20f))
        hsvL[2] = 0.98f
        hsvM[2] = 0.92f
        hsvD[2] = 0.86f
        colorH = Color.HSVToColor(hsvL)
        colorM = Color.HSVToColor(hsvM)
        colorL = Color.HSVToColor(hsvD)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_OPEN_MUSIC_REQUEST_CODE -> {
                if (grantResults.isEmpty()) return
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) return
                }
                openMusicAndSetup()
            }
            PERMISSION_OPEN_IMAGE_REQUEST_CODE -> {
                if (grantResults.isEmpty()) return
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) return
                }
                openImageAndSetup()
            }
        }
    }


    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            when (requestCode) {
                OPEN_MUSIC_REQUEST_CODE -> {
                    try {
                        if (mediaPlayer != null) mediaPlayer!!.stop()
                        mediaPlayer = MediaPlayer.create(this, data.data)
                        mediaPlayer!!.isLooping = true
                        mediaPlayer!!.start()
                        reloadVisualEffect()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return
                    }
                }
                OPEN_IMAGE_REQUEST_CODE -> {
                    try {
                        val coverImage = ContentHelper.absolutePathFromUri(this, data.data)
                        if (coverImage != null) {
                            rhythmView.albumCover = BitmapFactory.decodeFile(coverImage)
                            updateColor(rhythmView.albumCover!!)
                            reloadVisualEffect()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return
                    }
                }
            }
        } else
            super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getDominantColor(bitmap: Bitmap): Int {
        val newBitmap = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
        var red = 0
        var green = 0
        var blue = 0
        var c = 0
        var r: Int
        var g: Int
        var b: Int
        for (y in 0 until newBitmap.getHeight()) {
            for (x in 0 until newBitmap.getHeight()) {
                val color = newBitmap.getPixel(x, y)
                r = color shr 16 and 0xFF
                g = color shr 8 and 0xFF
                b = color and 0xFF
                if (r > 200 || g > 200 || b > 200) continue
                red += r
                green += g
                blue += b
                c++
            }
        }
        newBitmap.recycle()
        if (c == 0) {
            return 0xFFFFFFFF.toInt()
        } else {
            red = Math.max(0, Math.min(0xFF, red / c))
            green = Math.max(0, Math.min(0xFF, green / c))
            blue = Math.max(0, Math.min(0xFF, blue / c))

            val hsv = FloatArray(3)
            Color.RGBToHSV(red, green, blue, hsv)
            hsv[2] = Math.max(hsv[2], 0.7f)

            return 0xFF shl 24 or Color.HSVToColor(hsv)
        }
    }

    private fun reloadVisualEffect() {
        if (mediaPlayer == null) return
        /*
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.creativeminds)
            mediaPlayer!!.isLooping = true
            mediaPlayer!!.start()
        }
        */

        when (visualEffect.checkedRadioButtonId) {
            R.id.radioButtonRipple -> {
                dataSource = PlaybackSource(mediaPlayer!!, 3 * divisionValue)
                val ripple = Ripple(rhythmView, divisionValue, waveSpeedValue, particleSpeedValue)
                // ripple.colorLF = colorL
                // ripple.colorMF = colorM
                // ripple.colorHF = colorH
                // ripple.colorParticle = colorM
                ripple.dataSource = dataSource
                rhythmView.visualEffect = ripple
            }
            R.id.radioButtonRay -> {
                dataSource = PlaybackSource(mediaPlayer!!, 3 * divisionValue)
                val ray = Ray(rhythmView, divisionValue, waveSpeedValue)
                ray.dataSource = dataSource
                rhythmView.visualEffect = ray
            }
            R.id.radioButtonRainbowRay -> {
                dataSource = PlaybackSource(mediaPlayer!!, divisionValue)
                val rayMono = RainbowRay(rhythmView, divisionValue, waveSpeedValue)
                rayMono.dataSource = dataSource
                rhythmView.visualEffect = rayMono
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
    }

    override fun onPause() {
        super.onPause()
        rhythmView?.isPaused = true
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(this, PromptActivity::class.java))
            this.finish()
            return
        }
        rhythmView?.isPaused = false
        if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
            mediaPlayer!!.start()
        }
    }
}
