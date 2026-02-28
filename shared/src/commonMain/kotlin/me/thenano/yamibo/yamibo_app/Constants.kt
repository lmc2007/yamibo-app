package me.thenano.yamibo.yamibo_app

//TAGS
val __error_tag: (String) -> String = { "FUCK_ERROR($it)" }
val __info__tag: (String) -> String = { "FUCK_INFO($it)" }

object YamiboColors {
    val TopBarBackground = 0x551200
    val ThreadBackground = 0xfcf4cf
    val BottomBarBackground = 0xffe6b7
    val HomePageBackground = 0xfff2da
}

object UserAgent {
    const val WINDOWS = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36"
    const val MOBILE = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
}