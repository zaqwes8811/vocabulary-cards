package pages;

import com.google.common.base.Optional;
import com.googlecode.objectify.Key;
import gae_store_space.GeneratorKind;
import gae_store_space.PageKind;
import gae_store_space.StoreIsCorruptedException;
import pipeline.TextPipeline;

import java.util.Set;

public class PageBuilder {
  public static TextPipeline buildPipeline() {
    return new TextPipeline();
  }

  private static void checkNonEmpty(Set<Key<PageKind>> keys) {
    if (keys.isEmpty())
      throw new AssertionError();
  }

  // Транзакцией сделать нельзя - поиск это сразу больше 5 EG
  // Да кажется можно, просто не ясно зачем
  // DANGER: если не удача всегда! кидается исключение, это не дает загрузиться кешу!
  public static Optional<PageFrontend> restore(String pageName, Set<Key<PageKind>> keys) {
    checkNonEmpty(keys);
    Optional<PageFrontend> r = Optional.absent();
    try {
      // Load page data from store
      Optional<PageKind> rawPage = PageKind.getPageKind(pageName, keys);

      // Conditional processing raw page
      //if (true)
      {
        if (rawPage.isPresent()) {
          PageKind p = rawPage.get();
          PageWithBoundary tmp = buildPipeline().pass(p.getName(), p.getRawSource());

          PageWithBoundary frontend = PageWithBoundary.buildEmpty();

          frontend.assign(tmp);
          GeneratorKind g = GeneratorKind.restoreById(p.getGeneratorId()).get();
          frontend.setGeneratorCache(g);
          frontend.set(p);

          PageFrontend t = frontend;  // FIXME: polym. doesn't work
          r = Optional.of(t);
        }
      }

      return r;
    } catch (StoreIsCorruptedException ex) {
      throw new RuntimeException(ex.getCause());
    }
  }

}