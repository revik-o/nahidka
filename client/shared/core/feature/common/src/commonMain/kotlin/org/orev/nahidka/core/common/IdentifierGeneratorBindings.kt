package org.orev.nahidka.core.common

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

@BindingContainer
object IdentifierGeneratorBindings {

    @Provides
    private fun provideIdentifierGenerator(): IdentifierGenerator = RandomIdentifierGenerator()
}
