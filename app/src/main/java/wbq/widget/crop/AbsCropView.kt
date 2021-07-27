package wbq.widget.crop

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.SizeF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.res.use
import com.example.cropview.R
import wbq.widget.crop.ext.DensityUtils
import wbq.widget.crop.ext.fitCenterTo
import wbq.widget.crop.ext.getFitCenterMatrix

/**
 *
 *
 * @description 基础裁剪View
 * @author wubinqi
 * @date 2021/7/23 4:46 下午
 */
abstract class AbsCropView  @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private val M = Matrix()

        private const val DEFAULT_SHADE_COLOR = 0x80000000
        private const val DEFAULT_STROKE_DP = 2f
        private const val DEFAULT_MAX_SCALE = 20f
    }

    private var clipStrokeSize = DensityUtils.dp2px(DEFAULT_STROKE_DP).toFloat()
    private var clipColor = Color.YELLOW
    private var shadeColor = DEFAULT_SHADE_COLOR.toInt()
    private var maxScale = DEFAULT_MAX_SCALE

    private var cropInfo: CropInfo? = null

    private val cropWindowView by lazy { CropWindowView(context) }

    private val contentView by lazy { createContentView(context) }

    // 单指手势,如移动
    private val gDetector: GestureDetector

    // 缩放手势
    private val sGestureDetector: ScaleGestureDetector

    // 手指数量
    private var pointerCount = 0

    // 可视区域
    private val visibleRect = RectF()

    // 画面内容的区域 会动态变化
    private val contentRect = RectF()

    // 裁剪区域
    private val cropRect = RectF()

    private val cropHoming = CropHoming()

    private val moveGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            return this@AbsCropView.handleScroll(distanceX, distanceY)
        }
    }

    private val scaleGestureListener = object : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            detector ?: return false
            if (pointerCount > 1) {
                handleScale(
                    detector.scaleFactor,
                    scrollX + detector.focusX,
                    scaleY + detector.focusY
                )
                cropWindowView.invalidate()
                return true
            }
            return false
        }

        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            return pointerCount > 1
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
        }
    }

    init {
        attrs?.let {
            context.obtainStyledAttributes(it, R.styleable.AbsCropView).use { typeArray ->
                val strokeSize =
                    typeArray.getDimensionPixelOffset(R.styleable.AbsCropView_clipStrokeSize, -1)
                        .toFloat()
                if (strokeSize > 0) {
                    clipStrokeSize = strokeSize
                }
                clipColor = typeArray.getColor(R.styleable.AbsCropView_clipColor, clipColor)
                shadeColor = typeArray.getColor(R.styleable.AbsCropView_shadeColor, shadeColor)
                maxScale = typeArray.getFloat(R.styleable.AbsCropView_shadeColor, maxScale)
            }
        }
        addView(
            contentView.getRenderView(),
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        addView(
            cropWindowView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        gDetector = GestureDetector(context, moveGestureListener)
        sGestureDetector = ScaleGestureDetector(context, scaleGestureListener)
    }

    open fun setCropInfo(info: CropInfo) {
        cropInfo = info
    }

    fun getContentRect(): RectF {
        return contentRect
    }

    fun getCropRect(): RectF {
        return cropRect
    }

    /**
     * 获取资源内容的选择区域 转换出来的矩阵相对于 资源本身尺寸的坐标
     */
    fun getClipRect(): Rect {
        val clipRect = RectF(cropRect)
        val frameRect = RectF(contentRect)
        val imageScale = getContentScale()
        val imageLeft = (clipRect.left - frameRect.left)
        val imageTop = (clipRect.top - frameRect.top)
        val imageRight = imageLeft + clipRect.width()
        val imageBottom = imageTop + clipRect.height()
        return Rect(
            imageLeft.div(imageScale).toInt(), imageTop.div(imageScale).toInt(),
            imageRight.div(imageScale).toInt(), imageBottom.div(imageScale).toInt()
        )
    }

    final override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
        super.addView(child, params)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return super.onTouchEvent(event)

        if (cropHoming.isAnimating() || null == cropInfo) {
            return false
        }
        pointerCount = event.pointerCount
        val handle = sGestureDetector.onTouchEvent(event) or gDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    cropHoming.trigger(contentRect, cropRect, contentView)
        }
        return handle
    }

    protected open fun initRect(surfaceW: Float, surfaceH: Float) {
        val info = cropInfo ?: return
        visibleRect.set(0f, 0f, surfaceW, surfaceH)
        val mediaWidth = info.size.width
        val mediaHeight = info.size.height

        contentRect.set(0f, 0f, mediaWidth, mediaHeight)
        if (info.clipRect.isEmpty) {
            cropRect.set(0f, 0f, info.fillSize.width, info.fillSize.height)
            cropRect.fitCenterTo(contentRect)
        } else {
            cropRect.set(info.clipRect)
        }
        val frameMatrix = cropRect.getFitCenterMatrix(visibleRect)
        frameMatrix.mapRect(cropRect)
        frameMatrix.mapRect(contentRect)

        val matrix = adjustContentToTarget(RectF(visibleRect), RectF(contentRect), info.size)
        contentView.updateTransform(matrix)
        cropWindowView.invalidate()
    }

    protected abstract fun adjustContentToTarget(
        visibleRect: RectF,
        targetRect: RectF,
        mediaSize: SizeF
    ): Matrix

    protected abstract fun createContentView(context: Context): IContentView

    /**
     * 获取内容区域和资源实际尺寸的scale比例
     */
    private fun getContentScale(): Float {
        return cropInfo?.size?.width?.let {
            if (it > 0) contentRect.width() / it else 1f
        } ?: 1f
    }

    /**
     * 处理移动
     */
    private fun handleScroll(dx: Float, dy: Float): Boolean {
        val m = Matrix()
        contentView.getTransform(m)
        m.postTranslate(-dx, -dy)
        M.setTranslate(-dx, -dy)
        M.mapRect(contentRect)
        contentView.updateTransform(m)
        return true
    }

    /**
     * 处理缩放
     */
    private fun handleScale(fac: Float, focusX: Float, focusY: Float) {
        val info= cropInfo ?: return
        val tmp  = RectF(contentRect)
        M.setScale(fac, fac, focusX, focusY)
        M.mapRect(tmp)
        if (tmp.width() / info.size.width > DEFAULT_MAX_SCALE) {
            return
        }
        val m = Matrix()
        contentView.getTransform(m)
        m.postScale(fac, fac, focusX, focusY)
        M.mapRect(contentRect)
        contentView.updateTransform(m)
    }

    inner class CropWindowView(context: Context, private val drawDebug: Boolean = false) :
        View(context) {

        /**
         * 裁剪框画笔
         */
        private val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = clipColor
            strokeWidth = clipStrokeSize
        }

        /**
         * 蒙层画笔
         */
        private val shaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = shadeColor
            style = Paint.Style.FILL
        }
        /**
         * 蒙层路径
         */
        private val shaderPath = Path().apply {
            fillType = Path.FillType.WINDING
        }

        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)
            canvas?.let {
                it.drawRect(cropRect, clipPaint)
                drawShader(it)
                drawDebugRect(it)
            }
        }

        private fun drawShader(canvas: Canvas) {
            shaderPath.reset()
            shaderPath.addRect(
                visibleRect.left,
                visibleRect.top,
                visibleRect.right,
                visibleRect.bottom, Path.Direction.CW
            )
            shaderPath.addRect(cropRect, Path.Direction.CCW)
            canvas.drawPath(shaderPath, shaderPaint)
        }

        private fun drawDebugRect(canvas: Canvas) {
            if (drawDebug.not()) return
            canvas.run {
                val color = clipPaint.color
                clipPaint.color = Color.RED
                drawRect(contentRect, clipPaint)
                clipPaint.color = Color.GREEN
                drawRect(visibleRect, clipPaint)
                clipPaint.color = color
            }
        }
    }
}