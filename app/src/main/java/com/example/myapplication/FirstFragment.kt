package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.fragment.app.Fragment


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
            setUpVirtualDisplay()
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
}