package me.thenano.yamibo.yamibo_app

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object Logger {
    fun v(tag: String, message: String, throwable: Throwable? = null)
    fun d(tag: String, message: String, throwable: Throwable? = null)
    fun i(tag: String, message: String, throwable: Throwable? = null)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}