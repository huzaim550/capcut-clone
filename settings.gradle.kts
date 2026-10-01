// Reelforge -- modular Android video editor.
//
// Module graph (see docs/ARCHITECTURE.md for the reasoning):
//
//   :app                        composition root, navigation, DI wiring
//   :core:common                dispatchers, error model, time utils, logging   [pure JVM]
//   :core:model                 the domain model + serialization                [pure JVM]
//   :core:database              Room persistence for projects
//   :core:media                 media import, metadata probing, thumbnail cache
//   :core:designsystem          theme, tokens, icons, shared composables
//   :editor:engine              timeline maths, commands, undo/redo             [pure JVM]
//   :playback                   Media3 preview player
//   :processing                 render/export engine
//   :feature:home               project browser
//   :feature:editor             the editing surface
//   :feature:export             export configuration + progress
//
// :core:model and :editor:engine are deliberately *pure Kotlin JVM* modules.
// They hold the rules that decide what the user's edit actually means, so
// keeping Android out of them makes those rules fast to test and impossible to
// accidentally couple to a Context.

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Reelforge"

include(":core:common")
// The remaining modules are added back as they are implemented; Gradle fails
// on an include() whose directory does not exist yet.
// include(":app")
// include(":core:model")
// include(":core:database")
// include(":core:media")
// include(":core:designsystem")
// include(":editor:engine")
// include(":playback")
// include(":processing")
// include(":feature:home")
// include(":feature:editor")
// include(":feature:export")
