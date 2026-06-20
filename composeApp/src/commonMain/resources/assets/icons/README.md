# Yamibo icons

SVG files in this directory are the source of truth for `YamiboIcons`.
`GenerateYamiboIconsTask` converts them to `ImageVector` code before Kotlin compilation; the SVG
files are excluded from packaged runtime resources.

- Use lowercase `snake_case.svg` filenames. For example, `person_fill.svg` generates
  `YamiboIcons.PersonFill`.
- Every SVG must provide a `viewBox` beginning at `0 0`. `width` and `height` default to the
  viewport dimensions when omitted.
- Use SVG presentation attributes for fill, stroke, opacity, line cap/join, and transforms.
  CSS `style`, CSS classes, and unsupported SVG elements fail the build intentionally.
- Add or remove an SVG, then run `./gradlew :composeApp:generateYamiboIcons`.

The input directory can be changed with `yamibo.icons.sourceDir` in `gradle.properties` or with a
temporary `-Pyamibo.icons.sourceDir=<path>` command-line override.
