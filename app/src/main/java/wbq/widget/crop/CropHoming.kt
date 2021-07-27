package wbq.widget.crop

import android.animation.Animator
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.graphics.Matrix
import android.graphics.RectF
import androidx.annotation.UiThread
import kotlin.math.max

/**
 *
 *
 * @description 处理归位回弹
 * @author wubinqi
 * @date 2021/7/23 5:30 下午
 */
class CropHoming {
    private val animator by lazy { ValueAnimator() }
    private val evaluator by lazy { HomingEvaluator() }

    fun isAnimating(): Boolean {
        return animator.isRunning
    }

    /**
     * @param contentRect 内容区域
     * @param cropRect 裁剪框区域
     * @param view
     */
    @UiThread
    fun trigger(contentRect: RectF, cropRect: RectF, view: IContentView) {
        if (isAnimating() || needHoming(contentRect, cropRect).not()) {
            return
        }
        val endRect = calculateEndRect(contentRect, cropRect)
        val startRect = RectF(contentRect)
        val endScale = endRect.width() / startRect.width()
        val start = HomingValue(startRect.centerX(), startRect.centerY(), 1f, 0f)
        val end = HomingValue(endRect.centerX(), endRect.centerY(), endScale, 0f)
        val startMatrix = Matrix()
        view.getTransform(startMatrix)
        animator.setObjectValues(start, end)
        animator.setEvaluator(evaluator)
        animator.addUpdateListener {
            val h = it.animatedValue as HomingValue
            val matrix = Matrix()
            matrix.set(startMatrix)
            matrix.postScale(h.scale, h.scale, startRect.centerX(), startRect.centerY())
            matrix.postTranslate(h.translateX, h.translateY)
            view.updateTransform(matrix)
        }
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                clear()

                val h = (animation as ValueAnimator).animatedValue as HomingValue
                val matrix = Matrix()
                matrix.postScale(h.scale, h.scale, startRect.centerX(), startRect.centerY())
                matrix.postTranslate(h.translateX, h.translateY)
                matrix.mapRect(contentRect)
            }

            override fun onAnimationCancel(animation: Animator?) {
                clear()
            }

            override fun onAnimationRepeat(animation: Animator?) {
            }

            private fun clear() {
                animator.removeAllListeners()
                animator.removeAllUpdateListeners()
            }
        })
        animator.start()
    }

    private fun needHoming(contentRect: RectF, cropRect: RectF): Boolean {
        return contentRect.isClosureTo(cropRect).not()
    }

    private fun calculateEndRect(contentRect: RectF, cropRect: RectF): RectF {
        val sx = cropRect.width() / contentRect.width()
        val sy = cropRect.height() / contentRect.height()
        val matrix = Matrix()
        if (sx > 1 || sy > 1) {
            val scale = max(sx, sy)
            matrix.postScale(scale, scale, contentRect.centerX(), contentRect.centerY())
        }
        val scaledContentRect = RectF(contentRect).also {
            matrix.mapRect(it)
        }
        if (scaledContentRect.isClosureTo(cropRect)) {
            return scaledContentRect
        }
        val transX = when {
            scaledContentRect.left > cropRect.left -> {
                cropRect.left - scaledContentRect.left
            }
            scaledContentRect.right < cropRect.right -> {
                cropRect.right - scaledContentRect.right
            }
            else -> {
                0f
            }
        }
        val transY = when {
            scaledContentRect.top > cropRect.top -> cropRect.top - scaledContentRect.top
            scaledContentRect.bottom < cropRect.bottom -> cropRect.bottom - scaledContentRect.bottom
            else -> 0f
        }
        matrix.setTranslate(transX, transY)
        matrix.mapRect(scaledContentRect)
        return scaledContentRect
    }

    private data class HomingValue(
        val translateX: Float = 0f,
        val translateY: Float = 0f,
        val scale: Float = 1f,
        val rotate: Float = 0f
    )

    private class HomingEvaluator : TypeEvaluator<HomingValue> {

        override fun evaluate(
            fraction: Float,
            startValue: HomingValue,
            endValue: HomingValue
        ): HomingValue {
            val x: Float = fraction * (endValue.translateX - startValue.translateX)
            val y: Float = fraction * (endValue.translateY - startValue.translateY)
            val scale = startValue.scale + fraction * (endValue.scale - startValue.scale)
            val rotate = startValue.rotate + fraction * (endValue.rotate - startValue.rotate)
            return HomingValue(x, y, scale, rotate)
        }
    }
}