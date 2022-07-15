/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.tasklist.archiver.util;

public final class Tuple<L, R> {

  private L left;
  private R right;

  public Tuple(final L left, final R right) {
    this.right = right;
    this.left = left;
  }

  public static <L, R> Tuple<L, R> of(final L left, final R right) {
    return new Tuple<>(left, right);
  }

  public R getRight() {
    return right;
  }

  public void setRight(final R right) {
    this.right = right;
  }

  public L getLeft() {
    return left;
  }

  public void setLeft(final L left) {
    this.left = left;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((left == null) ? 0 : left.hashCode());
    result = prime * result + ((right == null) ? 0 : right.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Tuple<?, ?> other = (Tuple<?, ?>) obj;
    if (left == null) {
      if (other.left != null) {
        return false;
      }
    } else if (!left.equals(other.left)) {
      return false;
    }
    if (right == null) {
      if (other.right != null) {
        return false;
      }
    } else if (!right.equals(other.right)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append("<");
    builder.append(left);
    builder.append(", ");
    builder.append(right);
    builder.append(">");
    return builder.toString();
  }
}
