package com.ftr.utils;

/**
 * Created by lhrn on 2016/6/1.
 */
public interface FTRCallback<V> {
  V process(Object object, int what, int param1, int parma2);
}
