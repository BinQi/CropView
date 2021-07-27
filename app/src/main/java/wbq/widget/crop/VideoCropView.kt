package wbq.widget.crop

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.AttributeSet
import android.util.SizeF
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import kotlin.math.min


/**
 *
 *
 * @description 视频裁剪View
 * @author wubinqi
 * @date 2021/7/21 9:32 下午
 */
class VideoCropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbsCropView(context, attrs, defStyleAttr), IContentView {

    private val player by lazy { MediaPlayer() }
    private var textureView: TextureView? = null

    init {
        player.setOnPreparedListener {
            player.isLooping = true
            player.start()
        }
        textureView?.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                initRect(width.toFloat(), height.toFloat())
                player.apply {
                    setSurface(Surface(surface))
                    setAudioAttributes(
                        AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_MUSIC)
                            .build()
                    )
                    prepareAsync()
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture?,
                width: Int,
                height: Int
            ) {
                initRect(width.toFloat(), height.toFloat())
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                player.stop()
                player.release()
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            }
        }
    }

    override fun setCropInfo(info: CropInfo) {
        super.setCropInfo(info)
        player.setDataSource(info.mediaFilePath)
    }

    override fun getRenderView(): View {
        return TextureView(context).also {
            textureView = it
        }
    }

    override fun getTransform(matrix: Matrix): Matrix {
        return textureView?.getTransform(matrix) ?: Matrix()
    }

    override fun updateTransform(matrix: Matrix, invalidate: Boolean) {
        textureView?.let {
            it.setTransform(matrix)
            if (invalidate) it.invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event).also {
            when (event?.actionMasked) {
                MotionEvent.ACTION_DOWN -> player.pause()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    player.start()
                }
            }
        }
    }

    override fun createContentView(context: Context): IContentView {
        return this
    }

    override fun adjustContentToTarget(visibleRect: RectF, targetRect: RectF, mediaSize: SizeF): Matrix {
        return fixVideoCenter(
            visibleRect.width(),
            visibleRect.height(),
            mediaSize.width,
            mediaSize.height
        ).also {
            it.mapRect(visibleRect)
            it.postTranslate(
                targetRect.centerX() - visibleRect.centerX(),
                targetRect.centerY() - visibleRect.centerY()
            )
            it.postScale(
                targetRect.width() / visibleRect.width(),
                targetRect.height() / visibleRect.height(),
                targetRect.centerX(),
                targetRect.centerY()
            )
        }
    }

    private fun fixVideoCenter(
        surfaceW: Float,
        surfaceH: Float,
        videoWidth: Float,
        videoHeight: Float
    ): Matrix {
        val rect = RectF(0f, 0f, surfaceW, surfaceH)
        val matrix = Matrix()
        //因为默认视频是fitXY的形式显示的,所以首先要缩放还原回来.
        matrix.postScale(videoWidth / surfaceW, videoHeight / surfaceH)
        //把视频区移动到View区,使两者中心点重合.
        matrix.postTranslate((surfaceW - videoWidth) / 2, (surfaceH - videoHeight) / 2)
        matrix.mapRect(rect)
        //等比例放大或缩小,直到视频区的一边和View一边相等.如果另一边和view的一边不相等，则留下空隙
        val scale = min(surfaceW / rect.width(), surfaceH / rect.height())
        matrix.postScale(scale, scale, surfaceW / 2, surfaceH / 2)
        return matrix
    }
}