package gae_store_space;

import static gae_store_space.OfyService.ofy;
import gae_store_space.high_perf.OnePageProcessor;

import java.util.ArrayList;
import java.util.List;

import servlets.protocols.PageSummaryValue;
import servlets.protocols.PathValue;

public class AppInstance {
	private final OnePageProcessor processor = new OnePageProcessor();
	
	public PageKind createPageIfNotExist(String name, String text) {//, String type) {
		// FIXME: add user info
		List<PageKind> pages = 
				ofy().load().type(PageKind.class).filter("name = ", name).list();
		
		if (pages.isEmpty()) {
	  	PageKind page = processor.buildPageKindFromPlainText(name, text);
	  	ofy().save().entity(page).now();
	  	
	  	// TODO: А если здесь проверить сохранена ли, то иногда будет несохранена!
	  	
	  	// add default generator
	  	GeneratorKind g = GeneratorKind.create(page.getRawDistribution());
	  	ofy().save().entity(g).now();
	  	page.setGenerator(g);
	  	ofy().save().entity(page).now();
			
			return page;
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	public AppInstance() {
		// пока создаем один раз и удаляем. классы могут менятся, лучше так, чтобы не было 
		//   конфликтов.
		//
  	ofy().delete().keys(ofy().load().type(PageKind.class).keys()).now();
  	ofy().delete().keys(ofy().load().type(GeneratorKind.class).keys()).now();
  	ofy().delete().keys(ofy().load().type(WordKind.class).keys()).now();
  	
  	{
	  	// Own tables
	  	// FIXME: GAE can't read file.
  		String name = OnePageProcessor.defaultPageName;
  		String text = processor.getGetPlainTextFromFile(processor.getTestFileName());
  		createPageIfNotExist(name, text);
	 	}
  	
  	{
  		String name = OnePageProcessor.defaultPageName+"_fake";
  		String text = processor.getGetPlainTextFromFile(processor.getTestFileName());
  		createPageIfNotExist(name, text);
  	}
  	
  	{
	  	// Try read
	  	///*
	  	// Скрыл, так как падало, но должно работать!!
	  	List<PageKind> pages = 
	  			ofy().load().type(PageKind.class).filter("name = ", OnePageProcessor.defaultPageName).list();
	  	
	  	// FIXME: иногда страбатывает - почему - не ясно - список пуст, все вроде бы синхронно
	  	if (pages.isEmpty()) 
	  		throw new IllegalStateException();
	  	
	  	if (pages.size() > 1) 
	  		throw new IllegalStateException();
  	}
	}
	
	// FIXME: may be non thread safe. Да вроде бы должно быть база то потокобезопасная?
	//synchronized  // не в этом дело 
	public PageKind getPage(String nameName) {
		
		List<PageKind> pages = 
    		ofy().load().type(PageKind.class).filter("name = ", nameName).list();
    
    if (pages.size() != 1)
  		throw new IllegalStateException();
    
    return pages.get(0);  // 1 item
	}

	private static class Holder {
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
		PageKind page = getPage(p.pageName);
		GeneratorKind g = page.getGenerator(p.genName);
		g.disablePoint(p.pointPos);
		
		ofy().save().entity(g).now();
	} 
}
