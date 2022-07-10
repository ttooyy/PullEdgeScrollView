package com.example.myapplication

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.widget.NestedScrollView
import kotlin.math.abs

class PullEdgeScrollView : NestedScrollView {
    companion object {
        private const val INVALID_POINTER = -1
        private const val RESET_DURATION = 300L
        private const val SCROLL_RATIO = 0.5f
    }

    private lateinit var mDragView: View
    private var mLastMotionY: Int = 0
    private var mActivePointerId = INVALID_POINTER
    private var mIsBeingDragged = false
    private var mIsReset = false
    private var mDragStartY = 0
    private var mResetAnimation: ValueAnimator? = null
    private var mOnScrollListener: OnScrollListener? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    override fun onFinishInflate() {
        initView()
        super.onFinishInflate()
    }

    private fun initView() {
        overScrollMode = OVER_SCROLL_NEVER
        if (getChildAt(0) != null) {
            mDragView = getChildAt(0)
        }
        setOnScrollChangeListener { view: NestedScrollView, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->
            run {
                mOnScrollListener?.onScrollY(scrollY + mDragView.scrollY)
            }
        }
    }

    fun setOnScrollListener(l: OnScrollListener) {
        mOnScrollListener = l
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val actionMasked = ev.actionMasked
        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mLastMotionY = ev.y.toInt()
                mActivePointerId = ev.getPointerId(0)
                if (mIsReset) {
                    mResetAnimation?.cancel()
                    mIsReset = false
                    mIsBeingDragged = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val activePointerIndex = ev.findPointerIndex(mActivePointerId)
                if (activePointerIndex != INVALID_POINTER) {
                    val y = ev.getY(activePointerIndex).toInt()
                    val deltaY = (mLastMotionY - y)
                    val canDragUp = scrollY >= mDragView.height - height
                    val canDragDown = scrollY == 0
                    if (!mIsBeingDragged) {
                        if ((canDragUp && deltaY > 0) || (canDragDown && deltaY < 0)) {
                            if (abs(deltaY) > 0) {
                                mIsBeingDragged = true
                                mDragStartY = y
                            }
                        } else {
                            mLastMotionY = y
                        }
                    }
                    if (mIsBeingDragged) {
                        val dragScrollY = mDragView.scrollY
                        val obstacle =
                            (dragScrollY > 0 && deltaY > 0) || (dragScrollY < 0 && deltaY < 0)
                        var needScrollY = if (obstacle) (deltaY * SCROLL_RATIO).toInt() else deltaY
                        if ((dragScrollY > 0 && (dragScrollY < -needScrollY)) || (dragScrollY < 0 && (dragScrollY > -needScrollY))) {
                            needScrollY = -dragScrollY
                        }
                        mDragView.scrollBy(0, needScrollY)
                        mOnScrollListener?.onScrollY(mDragView.scrollY + scrollY)
                        if (mDragView.scrollY == 0) {
                            mIsBeingDragged = false
                        }
                        if (needScrollY != 0) {
                            mLastMotionY = y
                        }
                    }
                }
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                mActivePointerId = INVALID_POINTER
                mLastMotionY = 0
                mIsBeingDragged = false
                if (mDragView.scrollY != 0) {
                    resetDragView()
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val index = ev.actionIndex
                mLastMotionY = ev.getY(index).toInt()
                mActivePointerId = ev.getPointerId(index)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = ev.actionIndex
                val pointerId = ev.getPointerId(pointerIndex)
                if (pointerId == mActivePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    mActivePointerId = ev.getPointerId(newPointerIndex)
                }
                mLastMotionY = ev.getY(ev.findPointerIndex(mActivePointerId)).toInt()
            }
        }
        return super.onTouchEvent(ev)
    }

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        if (mIsBeingDragged || mIsReset) {
            return
        }
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
    }

    private fun resetDragView() {
        mIsReset = true
        mResetAnimation = ValueAnimator.ofInt(mDragView.scrollY, 0)
        mResetAnimation?.duration = RESET_DURATION
        mResetAnimation?.addUpdateListener {
            mDragView.scrollTo(mDragView.scrollX, it.animatedValue as Int)
            mOnScrollListener?.onScrollY(mDragView.scrollY + scrollY)
            if (mDragView.scrollY == 0) {
                mIsReset = false
            }
        }
        mResetAnimation?.start()
    }

    interface OnScrollListener {
        fun onScrollY(scrollY: Int)
    }
}