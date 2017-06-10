/*
 * Tencent is pleased to support the open source community by making Angel available.
 *
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.angel.psagent.matrix.storage;

import com.tencent.angel.ml.math.TVector;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Matrix storage manager. It holds a {@link MatrixStorage} for each matrix.
 */
public class MatrixStorageManager {
  /**matrix id to matrix storage map*/
  private final ConcurrentHashMap<Integer, MatrixStorage> matrixIdToStorageMap;

  /**
   * Create a new MatrixStorageManager.
   */
  public MatrixStorageManager() {
    matrixIdToStorageMap = new ConcurrentHashMap<Integer, MatrixStorage>();
  }

  /**
   * Create the storage for the matrix.
   * 
   * @param matrixId matrix id
   */
  public void addMatrix(int matrixId) {
    matrixIdToStorageMap.putIfAbsent(matrixId, new MatrixStorage());
  }

  /**
   * Remove the storage for the matrix.
   * 
   * @param matrixId matrix id
   */
  public void removeMatrix(int matrixId) {
    matrixIdToStorageMap.remove(matrixId);
  }

  /**
   * Get the storage for the matrix.
   * 
   * @param matrixId matrix id
   * @return MatrixStorage matrix storage
   */
  public MatrixStorage getMatrixStoage(int matrixId) {
    MatrixStorage storage = matrixIdToStorageMap.get(matrixId);
    if (storage != null) {
      return storage;
    }
    addMatrix(matrixId);
    return matrixIdToStorageMap.get(matrixId);
  }

  /**
   * Get the row from matrix storage.
   * 
   * @param matrixId matrix id
   * @param rowIndex row index
   * @return TVector row
   */
  public TVector getRow(int matrixId, int rowIndex) {
    MatrixStorage storage = getMatrixStoage(matrixId);
    if (storage == null) {
      return null;
    }

    return storage.getRow(rowIndex);
  }

  /**
   * Add the row to matrix storage.
   * 
   * @param matrixId matrix id
   * @param rowIndex row index
   * @param TVector row
   */
  public void addRow(int matrixId, int rowIndex, TVector row) {
    if (!matrixIdToStorageMap.containsKey(matrixId)) {
      addMatrix(matrixId);
    }

    MatrixStorage storage = getMatrixStoage(matrixId);
    if (storage == null) {
      return;
    }

    storage.addRow(rowIndex, row);
  }

  /**
   * Remove the row from matrix storage.
   * 
   * @param matrixId matrix id
   * @param rowIndex row index
   * @return TVector row
   */
  public void removeRow(int matrixId, int rowIndex) {
    MatrixStorage storage = getMatrixStoage(matrixId);
    if (storage == null) {
      return;
    }

    storage.removeRow(rowIndex);
  }
}
