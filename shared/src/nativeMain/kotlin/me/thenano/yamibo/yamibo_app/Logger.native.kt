package me.thenano.yamibo.yamibo_app

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object Logger {
    actual fun v(tag: String, message: String, throwable: Throwable?) {
        println("FUCK_VERBOSE($tag): $message")
        throwable?.printStackTrace()
    }

    actual fun d(tag: String, message: String, throwable: Throwable?) {
        println("FUCK_DEBUG($tag): $message")
        throwable?.printStackTrace()
    }

    actual fun i(tag: String, message: String, throwable: Throwable?) {
        println("FUCK_INFO($tag): $message")
        throwable?.printStackTrace()
    }

    actual fun w(tag: String, message: String, throwable: Throwable?) {
        println("FUCK_WARN($tag): $message")
        throwable?.printStackTrace()
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        println("FUCK_ERROR($tag): $message")
        throwable?.printStackTrace()
    }
}
