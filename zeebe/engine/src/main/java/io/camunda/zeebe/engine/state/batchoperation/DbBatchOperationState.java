/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.state.batchoperation;

import io.camunda.zeebe.db.ColumnFamily;
import io.camunda.zeebe.db.TransactionContext;
import io.camunda.zeebe.db.ZeebeDb;
import io.camunda.zeebe.db.impl.DbCompositeKey;
import io.camunda.zeebe.db.impl.DbForeignKey;
import io.camunda.zeebe.db.impl.DbLong;
import io.camunda.zeebe.db.impl.DbNil;
import io.camunda.zeebe.engine.state.batchoperation.PersistedBatchOperation.BatchOperationStatus;
import io.camunda.zeebe.engine.state.mutable.MutableBatchOperationState;
import io.camunda.zeebe.protocol.ZbColumnFamilies;
import io.camunda.zeebe.protocol.impl.record.value.batchoperation.BatchOperationCreationRecord;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbBatchOperationState implements MutableBatchOperationState {

  public static final long MAX_DB_CHUNK_SIZE = 3500;

  private static final Logger LOGGER = LoggerFactory.getLogger(DbBatchOperationState.class);

  private final DbLong batchKey = new DbLong();
  private final DbForeignKey<DbLong> fkBatchKey;
  private final DbLong chunkKey;
  private final DbCompositeKey<DbForeignKey<DbLong>, DbLong> fkBatchKeyAndChunkKey;

  private final ColumnFamily<DbLong, PersistedBatchOperation> batchOperationColumnFamily;
  private final ColumnFamily<
          DbCompositeKey<DbForeignKey<DbLong>, DbLong>, PersistedBatchOperationChunk>
      batchOperationChunksColumnFamily;
  private final ColumnFamily<DbLong, DbNil> pendingBatchOperationColumnFamily;

  public DbBatchOperationState(
      final ZeebeDb<ZbColumnFamilies> zeebeDb, final TransactionContext transactionContext) {
    fkBatchKey = new DbForeignKey<>(batchKey, ZbColumnFamilies.BATCH_OPERATION);
    chunkKey = new DbLong();
    fkBatchKeyAndChunkKey = new DbCompositeKey<>(fkBatchKey, chunkKey);

    batchOperationColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.BATCH_OPERATION,
            transactionContext,
            batchKey,
            new PersistedBatchOperation());
    batchOperationChunksColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.BATCH_OPERATION_CHUNKS,
            transactionContext,
            fkBatchKeyAndChunkKey,
            new PersistedBatchOperationChunk());
    pendingBatchOperationColumnFamily =
        zeebeDb.createColumnFamily(
            ZbColumnFamilies.PENDING_BATCH_OPERATION, transactionContext, batchKey, DbNil.INSTANCE);
  }

  @Override
  public void create(final long batchOperationKey, final BatchOperationCreationRecord record) {
    LOGGER.debug("Creating batch operation with key {}", record.getBatchOperationKey());
    batchKey.wrapLong(record.getBatchOperationKey());
    final var batchOperation = new PersistedBatchOperation();
    batchOperation
        .setKey(record.getBatchOperationKey())
        .setStatus(BatchOperationStatus.CREATED)
        .setBatchOperationType(record.getBatchOperationType())
        .setEntityFilter(record.getEntityFilterBuffer());
    batchOperationColumnFamily.upsert(batchKey, batchOperation);
    pendingBatchOperationColumnFamily.upsert(batchKey, DbNil.INSTANCE);
  }

  @Override
  public void appendItemKeys(final long batchOperationKey, final Set<Long> itemKeys) {
    LOGGER.trace(
        "Appending {} item keys to batch operation with key {}",
        itemKeys.size(),
        batchOperationKey);

    // First, get the batch operation
    final var batch = getBatchOperation(batchOperationKey);

    // Second, delete it from the pendingBatchOperationColumnFamily since we are already working on
    // it
    pendingBatchOperationColumnFamily.deleteIfExists(batchKey);

    // Third, get the chunk to append the keys to
    var chunk = getOrCreateChunk(batch);

    // Fourth, append the keys to the chunk, if the chunk is full, a new one is returned
    for (final long key : itemKeys) {
      chunk = appendKeyToChunk(batch, chunk, key);
    }

    // Finally, update the batch and the chunk in the column family
    updateChunkAndBatch(chunk, batch);
  }

  @Override
  public void removeItemKeys(final long batchOperationKey, final Set<Long> itemKeys) {
    LOGGER.trace(
        "Removing item keys {} from batch operation with key {}",
        itemKeys.size(),
        batchOperationKey);

    // First, get the batch operation
    final var batch = getBatchOperation(batchOperationKey);

    // Second, delete the keys from chunk
    final var chunk = getChunk(batch);
    chunk.removeItemKeys(itemKeys);

    // Finally, update the chunk and batch in the column family
    updateBatchAndChunkAfterRemoval(batch, chunk);
  }

  @Override
  public Optional<PersistedBatchOperation> get(final long key) {
    batchKey.wrapLong(key);
    return Optional.ofNullable(batchOperationColumnFamily.get(batchKey));
  }

  @Override
  public void foreachPendingBatchOperation(final BatchOperationVisitor visitor) {
    pendingBatchOperationColumnFamily.whileTrue(
        (key, nil) -> {
          final var batchOperation = batchOperationColumnFamily.get(key);
          if (batchOperation != null) {
            visitor.visit(batchOperation);
          }
          return true;
        });
  }

  @Override
  public List<Long> getNextItemKeys(final long batchOperationKey, final int batchSize) {
    batchKey.wrapLong(batchOperationKey);
    final var batch = batchOperationColumnFamily.get(batchKey);

    if (batch.getMinChunkKey() == -1) {
      return List.of();
    }

    chunkKey.wrapLong(batch.getMinChunkKey());
    final var chunk = batchOperationChunksColumnFamily.get(fkBatchKeyAndChunkKey);
    final var chunkKeys = chunk.getItemKeys();

    return chunkKeys.stream().limit(batchSize).toList();
  }

  private PersistedBatchOperationChunk createNewChunk(final PersistedBatchOperation batch) {
    final long currentChunkKey;
    final PersistedBatchOperationChunk batchChunk;
    currentChunkKey = batch.nextChunkKey();
    batchChunk = new PersistedBatchOperationChunk();
    batchChunk.setKey(currentChunkKey).setBatchOperationKey(batch.getKey());
    chunkKey.wrapLong(batchChunk.getKey());

    batchOperationChunksColumnFamily.insert(fkBatchKeyAndChunkKey, batchChunk);

    return batchChunk;
  }

  private PersistedBatchOperation getBatchOperation(final long batchOperationKey) {
    batchKey.wrapLong(batchOperationKey);
    return batchOperationColumnFamily.get(batchKey);
  }

  private PersistedBatchOperationChunk getChunk(final PersistedBatchOperation batch) {
    chunkKey.wrapLong(batch.getMinChunkKey());
    return batchOperationChunksColumnFamily.get(fkBatchKeyAndChunkKey);
  }

  /**
   * Updates the batch and chunk after removing keys from the chunk. If the chunk is empty, it is
   * deleted from the column family and the batch operation is updated.
   *
   * @param chunk the chunk to update
   * @param batch the batch operation to update
   */
  private void updateBatchAndChunkAfterRemoval(
      final PersistedBatchOperation batch, final PersistedBatchOperationChunk chunk) {
    if (chunk.getItemKeys().isEmpty()) {
      batchOperationChunksColumnFamily.deleteExisting(fkBatchKeyAndChunkKey);
      batch.removeChunkKey(chunk.getKey());
      batchOperationColumnFamily.update(batchKey, batch);
    } else {
      batchOperationChunksColumnFamily.update(fkBatchKeyAndChunkKey, chunk);
    }
  }

  /**
   * Gets the chunk for the batch operation. If the batch operation has no chunk (currentChunkKey =
   * -1), a new one is created.
   *
   * @param batch the batch operation to get the chunk for
   * @return the chunk for the batch operation
   */
  private PersistedBatchOperationChunk getOrCreateChunk(final PersistedBatchOperation batch) {
    final var currentChunkKey = batch.getMinChunkKey();
    if (currentChunkKey == -1) {
      return createNewChunk(batch);
    } else {
      chunkKey.wrapLong(currentChunkKey);
      return batchOperationChunksColumnFamily.get(fkBatchKeyAndChunkKey);
    }
  }

  /**
   * Appends the key to the chunk. If the chunk is full, a new one is created.
   *
   * @param chunk the current chunk to append the keys to, if it is full, a new one is created
   * @param key the key to append
   * @return the current updated chunk
   */
  private PersistedBatchOperationChunk appendKeyToChunk(
      final PersistedBatchOperation batch, PersistedBatchOperationChunk chunk, final long key) {
    if (chunk.getItemKeys().size() >= MAX_DB_CHUNK_SIZE) {
      batchOperationChunksColumnFamily.update(fkBatchKeyAndChunkKey, chunk);
      chunk = createNewChunk(batch);
    }

    chunk.appendItemKey(key);
    return chunk;
  }

  private void updateChunkAndBatch(
      final PersistedBatchOperationChunk chunk, final PersistedBatchOperation batch) {
    if (chunk != null) {
      batchOperationChunksColumnFamily.update(fkBatchKeyAndChunkKey, chunk);
    }
    batchOperationColumnFamily.update(batchKey, batch);
  }
}
