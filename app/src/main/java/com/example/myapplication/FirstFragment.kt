package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.fragment.app.Fragment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private val TAG = "ScreenCaptureFragment"
    private val STATE_RESULT_CODE = "result_code"
    private val STATE_RESULT_DATA = "result_data"
    private val REQUEST_MEDIA_PROJECTION = 1
    private var mScreenDensity = 0
    private var mResultCode = 0
    private var mResultData: Intent? = null
    private var mSurface: Surface? = null
    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mMediaProjectionManager: MediaProjectionManager? = null
    private var mButtonToggle: Button? = null
    private var mSurfaceView: SurfaceView? = null
    var imageReader: ImageReader? = null
    private val handler: Handler? = null
    private var displayWidth = 0
    private var displayHeight: Int = 0
    private var imagesProduced: Int = 0
    private val max_imageno = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            mResultCode = savedInstanceState.getInt(STATE_RESULT_CODE)
            mResultData = savedInstanceState.getParcelable(STATE_RESULT_DATA)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        view.findViewById<Button>(R.id.button_first).setOnClickListener {
//            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
//        }
        mSurfaceView = view.findViewById(R.id.surface) as SurfaceView
        mSurface = mSurfaceView!!.holder.surface
        mButtonToggle = view.findViewById(R.id.toggle) as Button
        mButtonToggle!!.setOnClickListener {
            if (mVirtualDisplay == null) {
                startScreenCapture()
            } else {
                stopScreenCapture()
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val activity: Activity? = activity
        val metrics = DisplayMetrics()
//        activity.applicationContext.display
        activity!!.windowManager.defaultDisplay.getMetrics(metrics)
        mScreenDensity = metrics.densityDpi
        mMediaProjectionManager =
            activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mResultData != null) {
            outState.putInt(STATE_RESULT_CODE, mResultCode)
            outState.putParcelable(STATE_RESULT_DATA, mResultData)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                Log.i(TAG, "User cancelled")
                return
            }
            val activity = activity ?: return
            Log.i(TAG, "Starting screen capture")
            mResultCode = resultCode
            mResultData = data

            setUpMediaProjection()

            val display: Display = activity!!.windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            displayHeight = size.y
            displayWidth = size.x
            imageReader = ImageReader.newInstance(size.x, size.y, PixelFormat.RGBA_8888, 2);

            setUpVirtualDisplay()

            imageReader?.setOnImageAvailableListener(ImageAvailableListener(), handler)
        }
    }

    override fun onPause() {
        super.onPause()
        stopScreenCapture()
    }

    override fun onDestroy() {
        super.onDestroy()
        tearDownMediaProjection()
    }

    private fun setUpMediaProjection() {
        mMediaProjection = mMediaProjectionManager!!.getMediaProjection(mResultCode, mResultData!!)
    }

    private fun tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection!!.stop()
            mMediaProjection = null
        }
    }

    private fun startScreenCapture() {
        val activity: Activity? = activity
        if (mSurface == null || activity == null) {
            return
        }
        if (mMediaProjection != null) {
            setUpVirtualDisplay()
        } else if (mResultCode != 0 && mResultData != null) {
            setUpMediaProjection()
            setUpVirtualDisplay()
        } else {
            Log.i(TAG, "Requesting confirmation")
            // This initiates a prompt dialog for the user to confirm screen projection.
            startActivityForResult(
                mMediaProjectionManager!!.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION
            )
        }
    }

    private fun setUpVirtualDisplay() {
        Log.i(
            TAG, "Setting up a VirtualDisplay: " +
                    mSurfaceView!!.width + "x" + mSurfaceView!!.height +
                    " (" + mScreenDensity + ")"
        )
        mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
            "ScreenCapture",
            mSurfaceView!!.width, mSurfaceView!!.height, mScreenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mSurface, null, null
        )
        mButtonToggle?.text = "stop"
    }

    private fun stopScreenCapture() {
        if (mVirtualDisplay == null) {
            return
        }
        mVirtualDisplay!!.release()
        mVirtualDisplay = null
        mButtonToggle?.text = "start"
    }

    class ImageAvailableListener() : OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            Log.i("ScreenCaptureFragment", "onImageAvailable 1")
            var image: Image? = null
            val fos: FileOutputStream? = null
            var bitmap: Bitmap? = null
            var stream: ByteArrayOutputStream? = null
            try {
                image = FirstFragment().imageReader?.acquireLatestImage()
                if (image != null) {
                    val planes: Array<Image.Plane> = image.getPlanes()
                    val buffer: ByteBuffer = planes[0].getBuffer()
                    val pixelStride: Int = planes[0].getPixelStride()
                    val rowStride: Int = planes[0].getRowStride()
                    val rowPadding: Int = rowStride - pixelStride * FirstFragment().displayWidth

                    // create bitmap
                    bitmap = Bitmap.createBitmap(
                        FirstFragment().displayWidth + rowPadding / pixelStride,
                        FirstFragment().displayHeight, Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)

                    //if (skylinkConnection != null && !TextUtils.isEmpty(currentRemotePeerId)) {
                    stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 5, stream)
                    FirstFragment().createImage(bitmap, FirstFragment().imagesProduced)
                    //skylinkConnection.sendData(currentRemotePeerId, stream.toByteArray());
                    //Log.d(TAG, "sending data to peer :" + currentRemotePeerId);
                    //}
                    Log.i("ScreenCaptureFragment", "onImageAvailable 2")
                    FirstFragment().imagesProduced++
                    if (FirstFragment().imagesProduced == FirstFragment().max_imageno) {
                        FirstFragment().imagesProduced = 0
                    }
                    Log.e("hi", "captured image: ${FirstFragment().imagesProduced}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (fos != null) {
                    try {
                        fos.close()
                    } catch (ioe: IOException) {
                        ioe.printStackTrace()
                    }
                }
                if (stream != null) {
                    try {
                        stream.close()
                    } catch (ioe: IOException) {
                        ioe.printStackTrace()
                    }
                }
                bitmap?.recycle()
                image?.close()
            }
        }
    }


    fun createImage(bmp: Bitmap, i: Int) {
        val bytes = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 10, bytes)
        val file1 = File(Environment.getExternalStorageDirectory().toString() + "/captures")
        file1.mkdir()
        val file = File(
            Environment.getDataDirectory().toString() +
                    "/captures/capturedscreenandroid" + i + ".jpg"
        )
        try {
            file.createNewFile()
            val outputStream = FileOutputStream(file)
            outputStream.write(bytes.toByteArray())
            outputStream.close()
            //Toast.makeText(getApplicationContext(),"success",Toast.LENGTH_SHORT).show();
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}