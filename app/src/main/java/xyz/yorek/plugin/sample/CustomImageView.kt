package xyz.yorek.plugin.sample

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.AttributeSet

class CustomImageView(
    context: Context,
    attributeSet: AttributeSet? = null
): androidx.appcompat.widget.AppCompatImageView(context, attributeSet) {

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
    }
}