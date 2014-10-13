// Tasks:
//   Сперва подключить кеш,
//   Затем думать о распределении
//   Затем думать об удалении.

// http://www.oracle.com/technetwork/java/javase/documentation/index-137868.html#styleguide
// TODO: http://www.oracle.com/technetwork/articles/marx-jpa-087268.html
// TODO: скрыть персистентность в этом классе, пусть сам себя сохраняет и удаляет.
// TODO: Функция очистки данных связанных со страницей, себя не удаляет.
// TODO: Добавить оценки текста
// не хочется выносить ofy()... выше. Но может быть, если использовать класс пользователя, то он может.
/**
 * About:
 *   Отражает один элемент данный пользователя, например, один файл субтитров.
 */

package store_gae_stuff;

import static store_gae_stuff.OfyService.ofy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.jcip.annotations.NotThreadSafe;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;

import core.math.DistributionElement;

@NotThreadSafe
@Entity
public class ContentPageKind {
  private ContentPageKind() { }
  
  public static final Integer MAX_CONTENT_ITEMS_IN_PACK = 5;

  @Id Long id;

  @Index String name;

  // Формированием не управляет, но остальным управляет.
  List<Key<WordItemKind>> wordKeys = new ArrayList<Key<WordItemKind>>();
  List<Key<ContentItemKind>> contentItems = new ArrayList<Key<ContentItemKind>>();
  
  // FIXME: почему отношение не работает?
  // Попытка сделать так чтобы g не стал нулевым указателем
  @Load
  Key<ActiveDistributionGenKind> g;

  // throws: 
  //   IllegalStateException - генератор не найден. Система замкнута, если 
  //     по имение не нашли генератора - это нарушение консистентности. Имена генереторов
  //     вводится только при создании, потом они только читаются.
  public ActiveDistributionGenKind getGenerator(String name) {  
  	if (g == null) {
  		throw new IllegalStateException();
  	}
  	
  	ActiveDistributionGenKind gen = ofy().load().key(g).now();
  	
  	if (gen == null) {
  		throw new IllegalStateException();
  	}
  	
  	gen.reset();
  	
  	return gen;
  }

  public void setGenerator(ActiveDistributionGenKind gen) {
    g = Key.create(gen);
  }

  public ContentPageKind(String name, ArrayList<ContentItemKind> items, ArrayList<WordItemKind> words) {
    this.name = Optional.of(name).get();
    for (WordItemKind word: words) this.wordKeys.add(Key.create(word));
    for (ContentItemKind item: items) this.contentItems.add(Key.create(item));
  }

  // About: Возвращать частоты, сортированные по убыванию.
  public ArrayList<DistributionElement> getRawDistribution() {
    // TODO: Отосортировать при выборке если можно
    // TODO: может при запросе можно отсортировать?
    List<WordItemKind> wordKinds = ofy().load().type(WordItemKind.class).filterKey("in", this.wordKeys).list();

    // Сортируем - элементы могут прийти в случайном порядке
    Collections.sort(wordKinds, WordItemKind.createFrequencyComparator());
    Collections.reverse(wordKinds);

    // Form result
    ArrayList<DistributionElement> distribution = new ArrayList<DistributionElement>();
    for (WordItemKind word : wordKinds)
      distribution.add(new DistributionElement(word.getRawFrequency()));

    return distribution;
  }
  
  private ImmutableList<ContentItemKind> getContendKinds(WordItemKind wordKind) {
  	// берем часть
  	// FIXME: делать выборки с перемешиванием
  	return ImmutableList.copyOf(
  			ofy().load().type(ContentItemKind.class)
  			.filterKey("in", wordKind.getItems())
  			.limit(MAX_CONTENT_ITEMS_IN_PACK).list());
  }
  
  public Optional<WordDataValue> getWordData(String genName) {
  	ActiveDistributionGenKind go = getGenerator(genName);
    
		Integer pointPosition = go.getPosition();
		WordItemKind wordKind =  getWordKind(pointPosition);
		ImmutableList<ContentItemKind> contentKinds = getContendKinds(wordKind);

		ArrayList<String> content = new ArrayList<String>(); 
		for (ContentItemKind e: contentKinds)
		  content.add(e.getSentence());
		
		return Optional.of(new WordDataValue(wordKind.getWord(), content));
  }
  
  // FIXME: а логика разрешает Отсутствующее значение?
  // http://stackoverflow.com/questions/2758224/assertion-in-java
  // генераторы могут быть разными, но набор слов один.
  private WordItemKind getWordKind(Integer pos) {
  	if (!(pos < this.wordKeys.size()))
  		throw new IllegalArgumentException();
  	
  	List<WordItemKind> kinds = 
				ofy().load().type(WordItemKind.class)
		    .filterKey("in", wordKeys).filter("pointPos =", pos)
		    .list();
  	
  	// FIXME: over pro. It's illegal state checking - не должно быть такого
  	//  Исключения лепить не хочется, но хотя исключения для искл ситуаций.
  	// Optional затрудняет поиск ситуаций - низкое разрешение по типам ошибок.
  	//
  	// It's IO - DbC wrong here.
  	if (kinds == null) {
  		//return Optional.absent();
  		throw new IllegalStateException();
  	}
  	
  	// не прошли не свои страницы
  	if (kinds.size() != 1) {
  		//return Optional.absent();  // code error
  		throw new IllegalStateException();
  	}
  	
  	// FIXME: А ноль то может быть? Вряд ли
		//return Optional.fromNullable(kinds.get(0));
		return kinds.get(0);
  }
  
  public static class WordDataValue {
  	public WordDataValue(String word, ArrayList<String> sentences) {
  		this.word = word;
  		this.sentences = sentences;
  	}
  	public final String word;  // хорошо бы Optional, но скорее всего не сереализуется
  	public final ArrayList<String> sentences;
  	
  	// cluster range name - важность слова - три или 4 группы
  }
}
