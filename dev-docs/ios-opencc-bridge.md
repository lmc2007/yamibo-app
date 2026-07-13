# iOS OpenCC Bridge

The common app API is `ChineseConversionRepository`. Android uses `opencc4j`.
iOS loads a native OpenCC bridge dynamically so the shared KMP module can still
build on Windows and CI machines that do not have an iOS OpenCC framework.

## Expected Native ABI

Bundle one of these binary names into the iOS app:

- `YamiboOpenCC.framework/YamiboOpenCC`
- `libyamibo_opencc_bridge.dylib`

The binary must export these C symbols:

```c
const char* yamibo_opencc_convert(const char* text, const char* mode);
void yamibo_opencc_free(const char* value);
int yamibo_opencc_is_mode_available(const char* mode);
```

`mode` uses the basic OpenCC-compatible ids from `ChineseConversionMode`: `s2t`
and `t2s`.

`yamibo_opencc_convert` should allocate and return a UTF-8 string owned by the
bridge. The app will call `yamibo_opencc_free` after copying it into Kotlin.

## Runtime Behavior

If the bridge is present, `IOSChineseConversionRepository` delegates conversion
to the native OpenCC bridge. If the bridge is missing or any required symbol is
missing, the repository returns the original text and reports every mode as
unavailable.

This keeps the current Windows build usable while allowing the iOS app target to
gain full OpenCC support once the native framework is added from a macOS/Xcode
build pipeline.
