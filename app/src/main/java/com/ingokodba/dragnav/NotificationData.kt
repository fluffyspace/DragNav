package com.ingokodba.dragnav

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Parcel
import android.os.Parcelable
import androidx.core.graphics.drawable.toBitmap
import com.ingokodba.dragnav.compose.AppNotification

data class NotificationData(
    val packageName: String,
    val iconBitmap: Bitmap?,
    val title: String,
    val content: String,
    val contentIntent: android.app.PendingIntent? = null
) : Parcelable {

    constructor(appNotification: AppNotification) : this(
        packageName = appNotification.packageName,
        iconBitmap = appNotification.appIcon?.toBitmap(),
        title = appNotification.title,
        content = appNotification.content,
        contentIntent = appNotification.contentIntent
    )

    constructor(parcel: Parcel) : this(
        packageName = parcel.readString() ?: "",
        iconBitmap = parcel.readParcelable(Bitmap::class.java.classLoader),
        title = parcel.readString() ?: "",
        content = parcel.readString() ?: "",
        contentIntent = parcel.readParcelable(android.app.PendingIntent::class.java.classLoader)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(packageName)
        parcel.writeParcelable(iconBitmap, flags)
        parcel.writeString(title)
        parcel.writeString(content)
        parcel.writeParcelable(contentIntent, flags)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<NotificationData> {
        override fun createFromParcel(parcel: Parcel): NotificationData {
            return NotificationData(parcel)
        }

        override fun newArray(size: Int): Array<NotificationData?> {
            return arrayOfNulls(size)
        }
    }

    fun toAppNotification(): AppNotification {
        return AppNotification(
            packageName = packageName,
            appIcon = iconBitmap?.let { BitmapDrawable(null, it) },
            title = title,
            content = content,
            contentIntent = contentIntent
        )
    }
}
