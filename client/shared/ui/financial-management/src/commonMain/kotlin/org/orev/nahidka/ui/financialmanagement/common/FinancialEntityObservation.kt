package org.orev.nahidka.ui.financialmanagement.common

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.orev.nahidka.feature.financial.dto.EntityChange
import org.orev.nahidka.feature.financial.subscription.FinancialSubscription

internal fun <Entity> FinancialSubscription<Entity>.observeEntities(
    entityIdentifier: (Entity) -> String,
): Flow<PersistentMap<String, Entity>> = callbackFlow {
    var entitiesByIdentifier = persistentMapOf<String, Entity>()

    this@observeEntities
        .onSnapshot { entitySnapshot ->
            entitiesByIdentifier = entitySnapshot.entities
                .associateBy(entityIdentifier)
                .toPersistentMap()
            send(entitiesByIdentifier)
        }
        .onBatch { changeBatch ->
            entitiesByIdentifier = changeBatch.changes.fold(entitiesByIdentifier) { changedEntities, entityChange ->
                when (entityChange) {
                    is EntityChange.Insert -> changedEntities.putting(entityIdentifier(entityChange.after), entityChange.after)
                    is EntityChange.Update -> changedEntities.putting(entityIdentifier(entityChange.after), entityChange.after)
                    is EntityChange.Delete -> changedEntities.removing(entityIdentifier(entityChange.before))
                }
            }
            send(entitiesByIdentifier)
        }
        .onError { failure -> close(failure) }
        .launchIn(this)

    awaitClose()
}
