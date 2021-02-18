package com.example.myapplication

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.graphics.PixelFormat
import android.media.AudioManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.FrameLayout
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.debounce
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

class GlobalActionBarService : AccessibilityService() {
    var mLayout: FrameLayout? = null

    @InternalCoroutinesApi
    override fun onServiceConnected() {
        super.onServiceConnected()
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        mLayout = FrameLayout(this)
        val lp = WindowManager.LayoutParams()
        lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        lp.format = PixelFormat.TRANSLUCENT
        lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        lp.gravity = Gravity.TOP
        val inflater = LayoutInflater.from(this)
        inflater.inflate(R.layout.action_bar, mLayout)
        wm.addView(mLayout, lp)
//        configurePowerButton()
//        configureVolumeButton()
//        configureScrollButton()
//        configureSwipeButton()
//        buildSocket()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

    }

    override fun onInterrupt() {

    }

    @InternalCoroutinesApi
    suspend fun observe() {
        EchoWebSocketListener.channel
            .asFlow()
            .collect { value ->
                Log.v("GABS", value)
                if (value == "SwipeRight") {
                    val swipePath = Path()
                    swipePath.moveTo(500f, 500f)
                    swipePath.lineTo(50f, 500f)
                    val gestureBuilder = GestureDescription.Builder()
                    gestureBuilder.addStroke(StrokeDescription(swipePath, 0, 500))
                    dispatchGesture(gestureBuilder.build(), null, null)
                } else if (value == "SwipeLeft") {
                    val swipePath = Path()
                    swipePath.moveTo(50f, 500f)
                    swipePath.lineTo(500f, 500f)
                    val gestureBuilder = GestureDescription.Builder()
                    gestureBuilder.addStroke(StrokeDescription(swipePath, 0, 500))
                    dispatchGesture(gestureBuilder.build(), null, null)
                } else if (value == "ScrollDown") {
                    val scrollable = findScrollableNode(rootInActiveWindow)
                    scrollable?.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.id)
                } else if (value == "ScrollUp") {
                    val scrollable = findScrollableNode2(rootInActiveWindow)
                    scrollable?.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD.id)
                } else if (value == "Back") {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                } else if (value == "Home") {
                    Log.v("Home", value)
                    performGlobalAction(GLOBAL_ACTION_HOME)
                } else if (value == "Overview") {
                    performGlobalAction(GLOBAL_ACTION_RECENTS)
                } else if (value == "Tap") {
                    val swipePath = Path()
                    swipePath.moveTo(500f, 500f)
                    val gestureBuilder = GestureDescription.Builder()
                    gestureBuilder.addStroke(StrokeDescription(swipePath, 0, 50))
                    dispatchGesture(gestureBuilder.build(), null, null);
                } else if (value == "Press") {
                    val swipePath = Path()
                    swipePath.moveTo(500f, 500f)
                    val gestureBuilder = GestureDescription.Builder()
                    gestureBuilder.addStroke(StrokeDescription(swipePath, 0, 500))
                    dispatchGesture(gestureBuilder.build(), null, null);
                } else {
                    Log.v("else", value)
                }
            }
    }

    @InternalCoroutinesApi
    private fun buildSocket() {
        val client = OkHttpClient.Builder()
            .readTimeout(3, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url("ws://10.0.2.2:8080/websession")
            .build()
        val wsListener = EchoWebSocketListener
        val webSocket = client.newWebSocket(request, wsListener)
        runBlocking {
            observe()
        }
    }

    private fun configurePowerButton() {
        val powerButton: Button = mLayout!!.findViewById<View>(R.id.power) as Button
        powerButton.setOnClickListener {
            performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
        }
    }

    private fun configureVolumeButton() {
        val volumeUpButton: Button = mLayout!!.findViewById<View>(R.id.volume_up) as Button
        volumeUpButton.setOnClickListener {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI
            )
        }
    }

    private fun configureSwipeButton() {
        val swipeButton = mLayout!!.findViewById<View>(R.id.swipe) as Button
        swipeButton.setOnClickListener {
            val swipePath = Path()
            swipePath.moveTo(500f, 500f)
            swipePath.lineTo(50f, 500f)
            val gestureBuilder = GestureDescription.Builder()
            gestureBuilder.addStroke(StrokeDescription(swipePath, 0, 500))
            dispatchGesture(gestureBuilder.build(), null, null)
        }
    }

    private fun configureScrollButton() {
        val scrollButton = mLayout!!.findViewById<View>(R.id.scroll) as Button
        scrollButton.setOnClickListener {
            val scrollable = findScrollableNode(rootInActiveWindow)
            scrollable?.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.id)
        }
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val deque: ArrayDeque<AccessibilityNodeInfo> = ArrayDeque()
        deque.add(root)
        while (!deque.isEmpty()) {
            val node: AccessibilityNodeInfo = deque.removeFirst()
            if (node.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)) {
                return node
            }
            for (i in 0 until node.childCount) {
                deque.addLast(node.getChild(i))
            }
        }
        return null
    }

    private fun findScrollableNode2(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val deque: ArrayDeque<AccessibilityNodeInfo> = ArrayDeque()
        deque.add(root)
        while (!deque.isEmpty()) {
            val node: AccessibilityNodeInfo = deque.removeFirst()
            if (node.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)) {
                return node
            }
            for (i in 0 until node.childCount) {
                deque.addLast(node.getChild(i))
            }
        }
        return null
    }
}