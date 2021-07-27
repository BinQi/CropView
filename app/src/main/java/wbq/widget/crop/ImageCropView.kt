package wbq.widget.crop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.SizeF
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlin.math.min

/**
 * @description 图片裁剪View
 * @author wubinqi
 * @date 2021/7/26 4:39 下午
 */
class ImageCropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbsCropView(context, attrs, defStyleAttr), IContentView {

    private var imageView: AppCompatImageView? = null
    private val window by lazy { RectF() }

    override fun setCropInfo(info: CropInfo) {
        super.setCropInfo(info)
        Glide.with(this).asBitmap().load(info.mediaFilePath)
//            .resize(info.size.width.toInt(), info.size.height.toInt())
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    imageView?.setImageBitmap(resource)
                    checkInitRect()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
    }

    override fun adjustContentToTarget(
        visibleRect: RectF,
        targetRect: RectF,
        mediaSize: SizeF
    ): Matrix {
        return Matrix().also {
            val rect = RectF(0f, 0f, mediaSize.width, mediaSize.height)
            it.postTranslate(
                targetRect.centerX() - rect.centerX(),
                targetRect.centerY() - rect.centerY()
            )
            val scale = min(targetRect.width() / rect.width(), targetRect.height() / rect.height())
            it.postScale(scale, scale, targetRect.centerX(), targetRect.centerY())
        }
    }

    override fun createContentView(context: Context): IContentView {
        return this
    }

    override fun getRenderView(): View {
        return AppCompatImageView(context).also {
            it.scaleType = ImageView.ScaleType.MATRIX
            imageView = it
        }
    }

    override fun getTransform(matrix: Matrix): Matrix {
        return imageView!!.imageMatrix!!.let {
            matrix.set(it)
            matrix
        }
    }

    override fun updateTransform(matrix: Matrix, invalidate: Boolean) {
        imageView?.let {
            it.imageMatrix = matrix
            if (invalidate) it.invalidate()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            val width = (right - left).toFloat()
            val height = (bottom - top).toFloat()
            window.set(0f, 0f, width, height)
            checkInitRect()
        }
    }

    private fun checkInitRect() {
        imageView?.let {
            if (window.isEmpty.not()) {
                initRect(window.width(), window.height())
            }
        }
    }
}