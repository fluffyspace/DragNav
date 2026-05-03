package com.ingokodba.dragnav

import com.ingokodba.dragnav.modeli.AppInfo

class EncapsulatedAppInfoWithFolder(apps: List<AppInfo>, folderName: String?, favorite: Boolean?) {
    var apps: List<AppInfo> = apps
    var folderName: String? = folderName
    var favorite: Boolean? = favorite
}
