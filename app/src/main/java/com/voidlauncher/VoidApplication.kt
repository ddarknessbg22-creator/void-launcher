package com.voidlauncher

import android.app.Application
import com.voidlauncher.util.TokenVault
import com.voidlauncher.util.VoidPaths

class VoidApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenVault.init(this)
        // Prepare VOID's private game-data layout. No permission or root required.
        val root = VoidPaths.root(this)
        VoidPaths.versions(root)
        VoidPaths.libraries(root)
        VoidPaths.objects(root)
        VoidPaths.indexes(root)
        VoidPaths.natives(root)
        VoidPaths.runtimes(root)
        VoidPaths.logs(root)
        VoidPaths.exports(root)
    }
}
