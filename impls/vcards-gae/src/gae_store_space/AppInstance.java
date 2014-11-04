package gae_store_space;

import static gae_store_space.OfyService.ofy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import pipeline.TextPipeline;
import pipeline.math.DistributionElement;
import servlets.protocols.PageSummaryValue;
import servlets.protocols.PathValue;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.UncheckedExecutionException;

import cross_cuttings_layer.CrossIO;

public class AppInstance {
	private static final Integer CACHE_SIZE = 5;	
	
	static public String getTestFileName() {
    return "./test_data/korra/etalon.srt";
  }
	
	// FIXME: если кеш убрать работает много стабильнее
	LoadingCache<String, Optional<PageKind>> pagesCache = CacheBuilder.newBuilder()
			.maximumSize(CACHE_SIZE)
			.build(
					new CacheLoader<String, Optional<PageKind>>() {
						@Override
						public Optional<PageKind> load(String key) { return PageKind.restore(key);	}	
					});
	
	public ImmutableList<DistributionElement> getDistribution(PathValue path) {
		if (!(path.getPageName().isPresent() && path.getGenName().isPresent())) 
			throw new IllegalArgumentException();
			
		// Срабатывает только один раз
		// TODO: Генератора реально может и не быть, или не найтись. Тогда лучше вернуть не ноль, а что-то другое 
		// FIXME: страница тоже может быть не найдена
  	Optional<PageKind> page = getPage(path.pageName); 
  	if (!page.isPresent())
  		throw new IllegalStateException();

  	return page.get().getDistribution(path.genName).get();
  }
	
	// скорее исследовательский метод
	// https://code.google.com/p/objectify-appengine/wiki/Transactions
	// FIXME: вот тут важна транзактивность
	public void createOrRecreatePage(String name, String text) {	
		fullDeletePage(name);
		pagesCache.invalidate(name);
		syncCreatePageIfNotExist(name, text);
		pagesCache.invalidate(name);
	}
	
	private void fullDeletePage(String name) {
		try {
			Optional<PageKind> page = getPage(name);
			if (page.isPresent())
				page.get().deleteFromStore();
		} catch (UncheckedExecutionException e) {
			// FIXME: удаляем все копии
			// FIXME: leak in store - active generators
			ofy().delete().keys(ofy().load().type(PageKind.class).filter("name = ", name).keys()).now();
		}
	}

	// FIXME: вот эту операцию лучше синхронизировать. И пользователю высветить, что идет процесс
	//   Иначе будут гонки. А может быть есть транзации на GAE?
	public PageKind syncCreatePageIfNotExist(String name, String text) {
		// FIXME: add user info
		List<PageKind> pages = 
			ofy().load().type(PageKind.class).filter("name = ", name).list();
		
		if (pages.isEmpty()) {
			TextPipeline processor = new TextPipeline();
	  	PageKind page = processor.pass(name, text);  
	  	
	  	GeneratorKind defaultGenerator = GeneratorKind.create(page.getRawDistribution(), TextPipeline.defaultGenName);
	  	defaultGenerator.syncCreateInStore(); 
	  	
	  	page.addGenerator(defaultGenerator);
	  	page.syncCreateInStore();
			return page;
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	public void resetFullStore() {
		// Call in case change store schema 
		//
		// пока создаем один раз и удаляем. классы могут менятся, лучше так, чтобы не было 
		//   конфликтов.
		//
		OfyService.clearStore();
  	
  	// clear cache
  	pagesCache.cleanUp();
  	
  	createDefaultPage();  // нельзя это в конструктор
	}
	
	private void createDefaultPage() {
		String name = TextPipeline.defaultPageName;
		String text = CrossIO.getGetPlainTextFromFile(getTestFileName());
		syncCreatePageIfNotExist(name, text);
	}
	
	public AppInstance() { }
	
	// FIXME: may be non thread safe. Да вроде бы должно быть база то потокобезопасная?
	public Optional<PageKind> getPage(String pageName) {
	  try {
			return pagesCache.get(pageName);
		} catch (ExecutionException e) {
    	throw new RuntimeException(e);
    }
	}

	public static class Holder {
		static final AppInstance w = new AppInstance();
	}
	
	public static AppInstance getInstance() {
		return Holder.w;
	} 
	
	// пока не ясно, что за идентификация будет для пользователя
	// данных может и не быть, так что 
	public List<PageSummaryValue> getUserInformation(String userId) {
		// FIXME: add user info
		List<PageKind> pages = ofy().load().type(PageKind.class).list();
		
		List<PageSummaryValue> r = new ArrayList<PageSummaryValue>();
		for (PageKind page: pages) {
			r.add(PageSummaryValue.create(page.getName(), page.getGenNames()));
		}
		
		return r;
	}
	
	public void disablePoint(PathValue p) {
		// FIXME: как добавить окно?
		PageKind page = getPage(p.pageName).get();
		page.disablePoint(p);		
	} 
}
