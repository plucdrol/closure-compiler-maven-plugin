package com.github.blutorange.maven.plugin.closurecompiler.common;

import java.util.Comparator;

public class TwoTuple<First extends Comparable<First>, Second extends Comparable<Second>> implements Comparable<TwoTuple<First, Second>> {
  private final First first;
  private final Second seocnd;

  public TwoTuple(First first, Second second) {
    this.first = first;
    this.seocnd = second;
  }

  public First getFirst() {
    return first;
  }

  public Second getSecond() {
    return seocnd;
  }

  @Override
  public String toString() {
    return "TwoTuple[first=" + first + ", seocnd=" + seocnd + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((first == null) ? 0 : first.hashCode());
    result = prime * result + ((seocnd == null) ? 0 : seocnd.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    TwoTuple<?, ?> other = (TwoTuple<?, ?>)obj;
    if (first == null) {
      if (other.first != null) return false;
    }
    else if (!first.equals(other.first)) return false;
    if (seocnd == null) {
      if (other.seocnd != null) return false;
    }
    else if (!seocnd.equals(other.seocnd)) return false;
    return true;
  }

  @Override
  public int compareTo(TwoTuple<First, Second> other) {
    return TwoTuple.<First, Second> getComparator().compare(this, other);
  }

  public static <First extends Comparable<First>, Second extends Comparable<Second>> Comparator<TwoTuple<First, Second>> getComparator() {
    return Comparator.<TwoTuple<First, Second>, First> comparing(TwoTuple::getFirst).thenComparing(TwoTuple::getSecond);
  }
}
