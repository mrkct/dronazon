package it.cutecchia.sdp.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class ThreadUtils {
  public static <T> void spawnThreadForEach(Collection<T> elements, Consumer<T> function) {
    List<Thread> threads = new ArrayList<>(elements.size());
    for (T element : elements) {
      Thread t = new Thread(() -> function.accept(element));
      threads.add(t);
      t.start();
    }
    for (Thread t : threads) {
      try {
        t.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
